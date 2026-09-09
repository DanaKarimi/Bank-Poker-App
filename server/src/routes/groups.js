const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');
const { generateInviteCode } = require('../utils/helpers');
const { emitToGroup } = require('../socket');
const { sendNotification } = require('../services/notifications');

/**
 * POST /api/groups/create
 * (Requires Auth)
 * Create a new group with a unique 6-character invite code (creator = group ADMIN)
 */
const handleCreateGroup = async (req, res) => {
    try {
        const { name } = req.body;

        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Group name is required' });
        }

        const trimmedName = name.trim();
        const groupId = crypto.randomUUID();
        const now = Date.now();
        const ownerUserId = req.user.id;

        // Generate a unique 6-character invite code
        let inviteCode = generateInviteCode();
        let attempts = 0;
        while (attempts < 10) {
            const existing = await get('SELECT id FROM groups WHERE invite_code = ?', [inviteCode]);
            if (!existing) break;
            inviteCode = generateInviteCode();
            attempts++;
        }

        // Insert into groups table
        await run(
            `INSERT INTO groups (id, owner_user_id, name, invite_code, mode, created_by, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'ONLINE', ?, ?, ?, ?, 1, 0)`,
            [groupId, ownerUserId, trimmedName, inviteCode, ownerUserId, now, groupId, now]
        );

        // Add creator to group_members with role = 'ADMIN'
        await run(
            `INSERT OR REPLACE INTO group_members (user_id, group_id, role, joined_at)
             VALUES (?, ?, 'ADMIN', ?)`,
            [ownerUserId, groupId, now]
        );

        return res.status(201).json({
            message: 'Group created successfully',
            groupId,
            inviteCode,
            group: {
                id: groupId,
                name: trimmedName,
                invite_code: inviteCode,
                owner_user_id: ownerUserId,
                created_at: now,
                updated_at: now
            }
        });
    } catch (error) {
        console.error('Error creating group:', error);
        return res.status(500).json({ error: 'Internal server error while creating group' });
    }
};

router.post('/create', authenticateToken, handleCreateGroup);
router.post('/', authenticateToken, handleCreateGroup);

/**
 * POST /api/groups/import and POST /api/groups/publish
 * (Requires Auth)
 * Publish or re-link an offline/local group to the online server:
 * Re-link by server_id first -> update; else invite_code exists -> link; else create new (NEVER duplicate).
 */
const handleImportOrPublishGroup = async (req, res) => {
    try {
        const {
            group,
            tables = [],
            players = [],
            buyIns = [],
            exits = [],
            payments = [],
            settlements = [],
            entryFees = []
        } = req.body;

        if (!group || !group.name || !group.name.trim()) {
            return res.status(400).json({ error: 'Group data with valid name is required' });
        }

        const now = Date.now();
        const createdBy = req.user.id;
        const idMapping = {};

        // 1. Re-link check: server_id first, then invite_code, else create new
        let targetGroupId = null;
        let inviteCode = null;

        const candidateServerId = group.server_id || group.serverId || group.id;
        if (candidateServerId) {
            const existingGroup = await get(
                'SELECT * FROM groups WHERE (id = ? OR server_id = ?) AND is_deleted = 0',
                [candidateServerId, candidateServerId]
            );
            if (existingGroup) {
                targetGroupId = existingGroup.id;
                inviteCode = existingGroup.invite_code;
            }
        }

        const localCode = (group.inviteCode || group.invite_code || '').trim().toUpperCase();
        if (!targetGroupId && localCode) {
            const existingByCode = await get(
                'SELECT * FROM groups WHERE UPPER(TRIM(invite_code)) = ? AND is_deleted = 0',
                [localCode]
            );
            if (existingByCode) {
                targetGroupId = existingByCode.id;
                inviteCode = existingByCode.invite_code;
            }
        }

        if (targetGroupId) {
            // Update existing group (NEVER create duplicate)
            await run(
                `UPDATE groups 
                 SET name = ?, mode = 'ONLINE', updated_at = ? 
                 WHERE id = ?`,
                [group.name.trim(), now, targetGroupId]
            );
        } else {
            // Create new group
            targetGroupId = crypto.randomUUID();
            inviteCode = localCode;
            if (!inviteCode) {
                inviteCode = generateInviteCode();
                let attempts = 0;
                while (attempts < 10) {
                    const existing = await get('SELECT id FROM groups WHERE UPPER(TRIM(invite_code)) = UPPER(TRIM(?))', [inviteCode]);
                    if (!existing) break;
                    inviteCode = generateInviteCode();
                    attempts++;
                }
            }

            await run(
                `INSERT INTO groups (id, owner_user_id, name, invite_code, mode, created_by, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'ONLINE', ?, ?, ?, ?, 1, 0)`,
                [targetGroupId, createdBy, group.name.trim(), inviteCode, createdBy, group.createdAt || group.created_at || now, targetGroupId, now]
            );
        }

        if (group.id) {
            idMapping[group.id] = targetGroupId;
        }

        // Add creator to group_members with role = 'ADMIN'
        await run(
            `INSERT OR REPLACE INTO group_members (user_id, group_id, role, joined_at)
             VALUES (?, ?, 'ADMIN', ?)`,
            [createdBy, targetGroupId, now]
        );

        // 2. Insert or Re-link Tables
        for (const t of tables) {
            const tCandidateId = t.server_id || t.serverId || t.id;
            let targetTable = null;
            if (tCandidateId) {
                targetTable = await get(
                    'SELECT * FROM tables WHERE (id = ? OR server_id = ?) AND group_id = ? AND is_deleted = 0',
                    [tCandidateId, tCandidateId, targetGroupId]
                );
            }
            if (!targetTable && t.name) {
                targetTable = await get(
                    'SELECT * FROM tables WHERE UPPER(TRIM(name)) = UPPER(TRIM(?)) AND group_id = ? AND is_deleted = 0',
                    [t.name.trim(), targetGroupId]
                );
            }

            let newTableId;
            const status = t.status || (t.isActive === false ? 'CLOSED' : 'ACTIVE');
            if (targetTable) {
                newTableId = targetTable.id;
                await run(
                    `UPDATE tables 
                     SET name = ?, chip_value = ?, status = ?, closed_at = ?, has_entry_fee = ?, entry_fee = ?, updated_at = ?
                     WHERE id = ?`,
                    [
                        t.name || targetTable.name,
                        t.chipValue ?? t.chip_value ?? targetTable.chip_value,
                        status,
                        t.closedAt ?? t.closed_at ?? targetTable.closed_at,
                        t.hasEntryFee ? 1 : (targetTable.has_entry_fee ? 1 : 0),
                        t.entryFee ?? t.entry_fee ?? targetTable.entry_fee,
                        now,
                        newTableId
                    ]
                );
            } else {
                newTableId = crypto.randomUUID();
                const tableCode = await generateInviteCode();
                await run(
                    `INSERT INTO tables (id, group_id, creator_user_id, name, code, chip_value, status, created_at, closed_at, published_at, has_entry_fee, entry_fee, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newTableId,
                        targetGroupId,
                        createdBy,
                        t.name || 'Table',
                        tableCode,
                        t.chipValue ?? t.chip_value ?? null,
                        status,
                        t.createdAt ?? t.created_at ?? now,
                        t.closedAt ?? t.closed_at ?? null,
                        now,
                        t.hasEntryFee ? 1 : 0,
                        t.entryFee ?? t.entry_fee ?? null,
                        newTableId,
                        now
                    ]
                );
            }
            idMapping[t.id] = newTableId;
        }

        // 3. Insert or Re-link Players
        for (const p of players) {
            const mappedTableId = idMapping[p.tableId || p.table_id] || p.tableId || p.table_id;
            if (!mappedTableId) continue;

            const pCandidateId = p.server_id || p.serverId || p.id;
            let targetPlayer = null;
            if (pCandidateId) {
                targetPlayer = await get(
                    'SELECT * FROM players WHERE (id = ? OR server_id = ?) AND table_id = ? AND is_deleted = 0',
                    [pCandidateId, pCandidateId, mappedTableId]
                );
            }
            if (!targetPlayer && p.name) {
                targetPlayer = await get(
                    'SELECT * FROM players WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND table_id = ? AND is_deleted = 0',
                    [p.name.trim(), mappedTableId]
                );
            }

            let newPlayerId;
            if (targetPlayer) {
                newPlayerId = targetPlayer.id;
                await run(
                    `UPDATE players 
                     SET name = ?, status = ?, entry_fee_paid = ?, updated_at = ?
                     WHERE id = ?`,
                    [p.name.trim(), p.status || targetPlayer.status, p.entryFeePaid ? 1 : targetPlayer.entry_fee_paid, now, newPlayerId]
                );
            } else {
                newPlayerId = crypto.randomUUID();
                await run(
                    `INSERT INTO players (id, table_id, user_id, name, status, created_at, entry_fee_paid, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newPlayerId,
                        mappedTableId,
                        p.name.trim(),
                        p.status || 'ACTIVE',
                        p.createdAt ?? p.created_at ?? now,
                        p.entryFeePaid ? 1 : 0,
                        newPlayerId,
                        now
                    ]
                );
            }
            idMapping[p.id] = newPlayerId;
        }

        // 4. Insert BuyIns (avoid duplicates)
        for (const b of buyIns) {
            const mappedTableId = idMapping[b.tableId || b.table_id] || b.tableId || b.table_id;
            const mappedPlayerId = idMapping[b.playerId || b.player_id] || b.playerId || b.player_id;
            if (!mappedTableId || !mappedPlayerId) continue;

            const bCandidateId = b.server_id || b.serverId || b.id;
            const existingBuyIn = bCandidateId ? await get('SELECT id FROM buy_ins WHERE (id = ? OR server_id = ?)', [bCandidateId, bCandidateId]) : null;
            if (!existingBuyIn) {
                const newBuyInId = crypto.randomUUID();
                idMapping[b.id] = newBuyInId;
                await run(
                    `INSERT INTO buy_ins (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newBuyInId,
                        mappedTableId,
                        mappedPlayerId,
                        Number(b.amount) || 0,
                        b.note || null,
                        createdBy,
                        b.createdAt ?? b.created_at ?? now,
                        newBuyInId,
                        now
                    ]
                );
            }
        }

        // 5. Insert Exits (avoid duplicates)
        for (const e of exits) {
            const mappedTableId = idMapping[e.tableId || e.table_id] || e.tableId || e.table_id;
            const mappedPlayerId = idMapping[e.playerId || e.player_id] || e.playerId || e.player_id;
            if (!mappedTableId || !mappedPlayerId) continue;

            const eCandidateId = e.server_id || e.serverId || e.id;
            const existingExit = eCandidateId ? await get('SELECT id FROM exit_records WHERE (id = ? OR server_id = ?)', [eCandidateId, eCandidateId]) : null;
            if (!existingExit) {
                const newExitId = crypto.randomUUID();
                idMapping[e.id] = newExitId;
                await run(
                    `INSERT INTO exit_records (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newExitId,
                        mappedTableId,
                        mappedPlayerId,
                        Number(e.amount) || 0,
                        e.note || null,
                        createdBy,
                        e.createdAt ?? e.created_at ?? now,
                        newExitId,
                        now
                    ]
                );
            }
        }

        // 6. Insert Payments
        for (const pm of payments) {
            const pmCandidateId = pm.server_id || pm.serverId || pm.id;
            const existingPm = pmCandidateId ? await get('SELECT id FROM payments WHERE (id = ? OR server_id = ?)', [pmCandidateId, pmCandidateId]) : null;
            if (!existingPm) {
                const newPaymentId = crypto.randomUUID();
                idMapping[pm.id] = newPaymentId;
                await run(
                    `INSERT INTO payments (id, group_id, from_player, to_player, amount, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newPaymentId,
                        targetGroupId,
                        pm.fromPlayer || pm.from_player || '',
                        pm.toPlayer || pm.to_player || '',
                        Number(pm.amount) || 0,
                        createdBy,
                        pm.createdAt ?? pm.created_at ?? now,
                        newPaymentId,
                        now
                    ]
                );
            }
        }

        // 7. Insert Settlements
        for (const s of settlements) {
            const sCandidateId = s.server_id || s.serverId || s.id;
            const existingS = sCandidateId ? await get('SELECT id FROM settlement_records WHERE (id = ? OR server_id = ?)', [sCandidateId, sCandidateId]) : null;
            if (!existingS) {
                const newSettlementId = crypto.randomUUID();
                idMapping[s.id] = newSettlementId;
                const mappedTableId = idMapping[s.tableId || s.table_id] || s.tableId || s.table_id || targetGroupId;

                await run(
                    `INSERT INTO settlement_records (id, group_id, table_id, table_name, payer_name, receiver_name, amount, initial_amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newSettlementId,
                        targetGroupId,
                        mappedTableId,
                        s.tableName || s.table_name || 'Table',
                        s.payerName || s.payer_name || s.fromPlayer || '',
                        s.receiverName || s.receiver_name || s.toPlayer || '',
                        Number(s.amount) || 0,
                        Number(s.initialAmount || s.initial_amount || s.amount) || 0,
                        (s.paid || s.isPaid) ? 1 : 0,
                        s.timestamp ?? s.created_at ?? now,
                        newSettlementId,
                        now
                    ]
                );
            }
        }

        // 8. Insert Entry Fees
        for (const ef of entryFees) {
            const efCandidateId = ef.server_id || ef.serverId || ef.id;
            const existingEf = efCandidateId ? await get('SELECT id FROM entry_fee_records WHERE (id = ? OR server_id = ?)', [efCandidateId, efCandidateId]) : null;
            if (!existingEf) {
                const newEfId = crypto.randomUUID();
                idMapping[ef.id] = newEfId;
                const mappedTableId = idMapping[ef.tableId || ef.table_id] || ef.tableId || ef.table_id || targetGroupId;

                await run(
                    `INSERT INTO entry_fee_records (id, group_id, table_id, table_name, player_name, amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        newEfId,
                        targetGroupId,
                        mappedTableId,
                        ef.tableName || ef.table_name || 'Table',
                        ef.playerName || ef.player_name || '',
                        Number(ef.amount) || 0,
                        ef.paid ? 1 : 0,
                        ef.timestamp ?? ef.created_at ?? now,
                        newEfId,
                        now
                    ]
                );
            }
        }

        emitToGroup(targetGroupId, 'group_updated', {
            groupId: targetGroupId,
            name: group.name.trim(),
            inviteCode
        });

        return res.status(200).json({
            message: 'Group published and synced successfully',
            groupId: targetGroupId,
            inviteCode,
            idMapping
        });
    } catch (error) {
        console.error('Error publishing group:', error);
        return res.status(500).json({ error: 'Internal server error while publishing group' });
    }
};

router.post('/import', authenticateToken, handleImportOrPublishGroup);
router.post('/publish', authenticateToken, handleImportOrPublishGroup);

/**
 * GET /api/groups/:id/invite-code
 * (Requires Auth + role='ADMIN')
 * Return the invite code for a group (accessible by the creator or admins)
 */
router.get('/:id/invite-code', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get(
            'SELECT * FROM groups WHERE id = ? AND is_deleted = 0',
            [groupId]
        );

        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // Check if the requesting admin created the group (if created_by is set)
        if (group.created_by && group.created_by !== req.user.id) {
            return res.status(403).json({ error: 'Forbidden: Only the group creator can view the invite code' });
        }

        return res.status(200).json({
            groupId: group.id,
            inviteCode: group.invite_code
        });
    } catch (error) {
        console.error('Error fetching invite code:', error);
        return res.status(500).json({ error: 'Internal server error while fetching invite code' });
    }
});

/**
 * POST /api/groups/:id/invite-code
 * (Requires Auth + role='ADMIN')
 * Sync/update the invite code for a group (self-heal)
 */
router.post('/:id/invite-code', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const groupId = req.params.id;
        const { inviteCode } = req.body;

        if (!inviteCode || !inviteCode.trim()) {
            return res.status(400).json({ error: 'inviteCode is required' });
        }

        const cleanCode = inviteCode.trim().toUpperCase();
        const now = Date.now();

        // Update group by id or server_id
        const result = await run(
            `UPDATE groups
             SET invite_code = ?, mode = 'ONLINE', updated_at = ?
             WHERE (id = ? OR server_id = ?) AND is_deleted = 0`,
            [cleanCode, now, groupId, groupId]
        );

        console.log("Synced invite code for group:", groupId, "to:", cleanCode, "changes:", result.changes);

        return res.status(200).json({
            message: 'Invite code synced',
            groupId,
            inviteCode: cleanCode
        });
    } catch (error) {
        console.error('Error syncing invite code:', error);
        return res.status(500).json({ error: 'Internal server error while syncing invite code' });
    }
});

/**
 * GET /api/groups/:id/members
 * (Requires Auth + role='ADMIN')
 * Return list of users who joined this group
 */
router.get('/:id/members', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get(
            'SELECT * FROM groups WHERE id = ? AND is_deleted = 0',
            [groupId]
        );

        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const members = await all(
            `SELECT u.id as user_id, u.username, u.role, gm.joined_at
             FROM group_members gm
             INNER JOIN users u ON gm.user_id = u.id
             WHERE gm.group_id = ?
             ORDER BY gm.joined_at ASC`,
            [groupId]
        );

        return res.status(200).json({
            groupId: group.id,
            groupName: group.name,
            members
        });
    } catch (error) {
        console.error('Error fetching group members:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group members' });
    }
});

/**
 * GET /api/groups/by-invite/:code
 * (Requires Auth)
 * Inspect group metadata & player claim status before joining
 */
router.get('/by-invite/:code', authenticateToken, async (req, res) => {
    try {
        const cleanCode = (req.params.code || '').trim().toUpperCase();
        if (!cleanCode) {
            return res.status(400).json({ error: 'Invite code is required' });
        }

        const group = await get(
            'SELECT * FROM groups WHERE UPPER(TRIM(invite_code)) = UPPER(TRIM(?)) AND is_deleted = 0',
            [cleanCode]
        );

        if (!group) {
            return res.status(404).json({ error: 'Group not found with that invite code' });
        }

        const groupId = group.id;
        const userId = req.user.id;

        const players = await getGroupUniquePlayers(groupId, userId);
        const hasUnclaimedPlayers = players.some(p => !p.isClaimed);
        const userHasPlayer = players.some(p => p.isMe);
        const claimedPlayer = players.find(p => p.isMe);

        return res.status(200).json({
            groupId: group.id,
            name: group.name,
            mode: group.mode || 'ONLINE',
            hasUnclaimedPlayers,
            userHasPlayer,
            claimedPlayerName: claimedPlayer ? claimedPlayer.name : null
        });
    } catch (error) {
        console.error('Error fetching group by invite code:', error);
        return res.status(500).json({ error: 'Internal server error while inspecting invite code' });
    }
});

/**
 * POST /api/groups/join
 * Join a group using its unique invite_code
 */
router.post('/join', authenticateToken, async (req, res) => {
    try {
        const { invite_code } = req.body;

        const cleanCode = (invite_code || '').trim().toUpperCase();
        console.log("Join attempt with code:", cleanCode);

        const group = await get(
            'SELECT * FROM groups WHERE UPPER(TRIM(invite_code)) = UPPER(TRIM(?)) AND is_deleted = 0',
            [cleanCode]
        );

        console.log("Join attempt with code:", cleanCode, "found group:", group ? group.id : null);

        if (!group) {
            return res.status(404).json({ error: 'Group not found with that invite code' });
        }

        const userId = req.user.id;
        const groupId = group.id;

        // Check if user is already a member
        const existingMembership = await get(
            'SELECT * FROM group_members WHERE user_id = ? AND group_id = ?',
            [userId, groupId]
        );

        if (existingMembership) {
            return res.status(200).json({
                message: 'Already a member of this group',
                group: {
                    id: group.id,
                    name: group.name,
                    invite_code: group.invite_code,
                    mode: group.mode || 'ONLINE',
                    joined_at: existingMembership.joined_at
                }
            });
        }

        const joinedAt = Date.now();
        await run(
            'INSERT INTO group_members (user_id, group_id, joined_at) VALUES (?, ?, ?)',
            [userId, groupId, joinedAt]
        );

        return res.status(200).json({
            message: 'Joined group successfully',
            group: {
                id: group.id,
                name: group.name,
                invite_code: group.invite_code,
                mode: group.mode || 'ONLINE',
                joined_at: joinedAt
            }
        });
    } catch (error) {
        console.error('Error joining group:', error);
        return res.status(500).json({ error: 'Internal server error while joining group' });
    }
});

/**
 * GET /api/groups/my-groups
 * List groups for the authenticated user:
 * - If user is ADMIN: returns all groups created by them or joined by them
 * - If user is PLAYER: returns all groups joined by them
 */
router.get('/my-groups', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const userRole = req.user.role;
        const username = req.user.username;

        let groups;
        if (userRole === 'SUPER_ADMIN') {
            groups = await all(
                `SELECT DISTINCT g.id, g.name, g.invite_code, g.mode, g.owner_user_id, g.created_by, g.created_at, g.server_id, g.updated_at,
                    (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) as member_count,
                    (g.owner_user_id = ? OR g.created_by = ?) as is_creator
                 FROM groups g
                 WHERE g.is_deleted = 0
                 ORDER BY g.name ASC`,
                [userId, userId]
            );
        } else if (userRole === 'ADMIN') {
            groups = await all(
                `SELECT DISTINCT g.id, g.name, g.invite_code, g.mode, g.owner_user_id, g.created_by, g.created_at, g.server_id, g.updated_at, gm.joined_at,
                    (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) as member_count,
                    (g.owner_user_id = ? OR g.created_by = ?) as is_creator
                 FROM groups g
                 LEFT JOIN group_members gm ON g.id = gm.group_id AND gm.user_id = ?
                 WHERE (g.owner_user_id = ? OR g.created_by = ? OR gm.user_id = ?) AND g.is_deleted = 0
                 ORDER BY g.name ASC`,
                [userId, userId, userId, userId, userId, userId]
            );
        } else {
            groups = await all(
                `SELECT g.id, g.name, g.invite_code, g.mode, g.owner_user_id, g.created_by, g.created_at, g.server_id, g.updated_at, gm.joined_at,
                    (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) as member_count,
                    (g.owner_user_id = ? OR g.created_by = ?) as is_creator
                 FROM groups g
                 INNER JOIN group_members gm ON g.id = gm.group_id
                 WHERE gm.user_id = ? AND g.is_deleted = 0
                 ORDER BY g.name ASC`,
                [userId, userId, userId]
            );
        }

        for (const g of groups) {
            const buyIns = await get(
                `SELECT COALESCE(SUM(b.amount), 0) as total
                 FROM buy_ins b
                 JOIN players p ON b.player_id = p.id
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND (p.user_id = ? OR p.name = ?) AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
                [g.id, userId, username]
            );
            const exits = await get(
                `SELECT COALESCE(SUM(e.amount), 0) as total
                 FROM exit_records e
                 JOIN players p ON e.player_id = p.id
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND (p.user_id = ? OR p.name = ?) AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
                [g.id, userId, username]
            );
            const paid = await get(
                `SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE group_id = ? AND from_player = ? AND is_deleted = 0`,
                [g.id, username]
            );
            const received = await get(
                `SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE group_id = ? AND to_player = ? AND is_deleted = 0`,
                [g.id, username]
            );
            const netGame = (exits?.total || 0) - (buyIns?.total || 0);
            const netPayments = (paid?.total || 0) - (received?.total || 0);
            g.net_balance = netGame + netPayments;
            g.member_count = Number(g.member_count) || 0;
            g.is_creator = Boolean(g.is_creator);
        }

        return res.status(200).json({ groups });
    } catch (error) {
        console.error('Error fetching user groups:', error);
        return res.status(500).json({ error: 'Internal server error while fetching groups' });
    }
});

/**
 * GET /api/groups/:id/my-stats
 * Calculate personal stats for the logged-in user in that group
 */
router.get('/:id/my-stats', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const userId = req.user.id;
        const username = req.user.username;

        const group = await get(
            'SELECT * FROM groups WHERE id = ? AND is_deleted = 0',
            [groupId]
        );

        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // Calculate Overall Group Totals across all tables in this group
        const groupBuyInsRow = await get(
            `SELECT COALESCE(SUM(b.amount), 0) as total
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId]
        );
        const totalGroupBuyIns = groupBuyInsRow ? Number(groupBuyInsRow.total) : 0;

        const groupExitsRow = await get(
            `SELECT COALESCE(SUM(e.amount), 0) as total
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId]
        );
        const totalGroupExits = groupExitsRow ? Number(groupExitsRow.total) : 0;
        const totalGroupBalance = totalGroupBuyIns - totalGroupExits;

        // Get all player names associated with this user in this group
        const linkedNames = await all(
            `SELECT DISTINCT p.name FROM players p
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name = ?) AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, userId, username]
        );
        const nameList = Array.from(new Set([username, ...linkedNames.map(r => r.name)]));
        const placeholders = nameList.map(() => '?').join(',');

        // 1. Tables count
        const tablesResult = await get(
            `SELECT COUNT(DISTINCT t.id) as tables_count
             FROM tables t
             JOIN players p ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name IN (${placeholders})) AND t.is_deleted = 0 AND p.is_deleted = 0`,
            [groupId, userId, ...nameList]
        );
        const tablesPlayed = tablesResult ? tablesResult.tables_count : 0;

        // 2. Total Buy-ins
        const buyInsResult = await get(
            `SELECT COALESCE(SUM(b.amount), 0) as total_buy_ins
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name IN (${placeholders})) AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, userId, ...nameList]
        );
        const totalBuyIns = buyInsResult ? Number(buyInsResult.total_buy_ins) : 0;

        // 3. Total Exits
        const exitsResult = await get(
            `SELECT COALESCE(SUM(e.amount), 0) as total_exits
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name IN (${placeholders})) AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, userId, ...nameList]
        );
        const totalExits = exitsResult ? Number(exitsResult.total_exits) : 0;

        // 4. Payments sent
        const paymentsSentResult = await get(
            `SELECT COALESCE(SUM(amount), 0) as payments_sent
             FROM payments
             WHERE group_id = ? AND from_player IN (${placeholders}) AND is_deleted = 0`,
            [groupId, ...nameList]
        );
        const paymentsSent = paymentsSentResult ? Number(paymentsSentResult.payments_sent) : 0;

        // 5. Payments received
        const paymentsReceivedResult = await get(
            `SELECT COALESCE(SUM(amount), 0) as payments_received
             FROM payments
             WHERE group_id = ? AND to_player IN (${placeholders}) AND is_deleted = 0`,
            [groupId, ...nameList]
        );
        const paymentsReceived = paymentsReceivedResult ? Number(paymentsReceivedResult.payments_received) : 0;

        // Calculations
        const totalPayments = paymentsSent + paymentsReceived;
        const netGameBalance = totalExits - totalBuyIns;
        let currentBalance = netGameBalance + paymentsSent - paymentsReceived;

        // Server-calculated live balance
        const myBalance = currentBalance;
        const myBuyIns = totalBuyIns;
        const myExits = totalExits;

        // 6. Recent Transactions
        const recentBuyIns = await all(
            `SELECT b.id, 'BUY_IN' as type, b.amount, b.note, b.created_at as timestamp, t.name as table_name
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name IN (${placeholders})) AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY b.created_at DESC
             LIMIT 10`,
            [groupId, userId, ...nameList]
        );

        const recentExits = await all(
            `SELECT e.id, 'EXIT' as type, e.amount, e.note, e.created_at as timestamp, t.name as table_name
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND (p.user_id = ? OR p.name IN (${placeholders})) AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY e.created_at DESC
             LIMIT 10`,
            [groupId, userId, ...nameList]
        );

        const recentPayments = await all(
            `SELECT p.id,
                    CASE WHEN p.from_player IN (${placeholders}) THEN 'PAYMENT_SENT' ELSE 'PAYMENT_RECEIVED' END as type,
                    p.amount,
                    p.from_player || ' -> ' || p.to_player as note,
                    p.created_at as timestamp,
                    'Settlement Payment' as table_name
             FROM payments p
             WHERE p.group_id = ? AND (p.from_player IN (${placeholders}) OR p.to_player IN (${placeholders})) AND p.is_deleted = 0
             ORDER BY p.created_at DESC
             LIMIT 10`,
            [...nameList, groupId, ...nameList, ...nameList]
        );

        const recentTransactions = [...recentBuyIns, ...recentExits, ...recentPayments]
            .sort((a, b) => b.timestamp - a.timestamp)
            .slice(0, 20);

        return res.status(200).json({
            groupId: group.id,
            groupName: group.name,
            username,
            balance: myBalance,
            myBalance,
            myBuyIns,
            myExits,
            currentBalance,
            netGameBalance,
            totalBuyIns: myBuyIns,
            totalExits: myExits,
            userTotalBuyIns: myBuyIns,
            userTotalExits: myExits,
            totalPayments,
            paymentsSent,
            paymentsReceived,
            tablesPlayed,
            recentTransactions
        });
    } catch (error) {
        console.error('Error fetching group stats:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group stats' });
    }
});

/**
 * GET /api/groups/:id/tables
 * (Requires Auth)
 * Return all active tables for a specific group
 */
router.get('/:id/tables', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const userId = req.user?.id;

        const group = await get('SELECT * FROM groups WHERE (id = ? OR server_id = ?) AND is_deleted = 0', [groupId, groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const rawTables = await all(
            `SELECT t.*, 
                (SELECT COUNT(*) FROM players p WHERE p.table_id = t.id AND p.is_deleted = 0 AND p.status = 'ACTIVE') as playerCount,
                (SELECT p.entry_fee_paid FROM players p WHERE p.table_id = t.id AND p.user_id = ? AND p.is_deleted = 0 LIMIT 1) as myEntryFeePaid,
                (SELECT p.id FROM players p WHERE p.table_id = t.id AND p.user_id = ? AND p.is_deleted = 0 LIMIT 1) as myPlayerId
             FROM tables t
             WHERE t.group_id = ? AND t.is_deleted = 0
             ORDER BY t.created_at DESC`,
            [userId, userId, group.id]
        );

        const tables = rawTables.map(t => {
            const isClosed = t.status === 'CLOSED' || t.is_active === 0;
            return {
                id: t.id,
                groupId: t.group_id,
                group_id: t.group_id,
                name: t.name,
                chipValue: t.chip_value,
                chip_value: t.chip_value,
                status: isClosed ? 'CLOSED' : 'ACTIVE',
                isActive: !isClosed,
                is_active: isClosed ? 0 : 1,
                hasEntryFee: Boolean(t.has_entry_fee),
                has_entry_fee: t.has_entry_fee,
                entryFee: t.entry_fee,
                entry_fee: t.entry_fee,
                myEntryFeePaid: t.myEntryFeePaid != null ? Boolean(t.myEntryFeePaid) : null,
                my_entry_fee_paid: t.myEntryFeePaid != null ? Number(t.myEntryFeePaid) : null,
                hasJoinedTable: Boolean(t.myPlayerId),
                createdAt: t.created_at,
                created_at: t.created_at,
                closedAt: t.closed_at,
                closed_at: t.closed_at,
                playerCount: Number(t.playerCount) || 0
            };
        });

        return res.status(200).json({ tables });
    } catch (error) {
        console.error('Error fetching group tables:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group tables' });
    }
});

/**
 * Helper to get deduplicated player identities for a group.
 * Matches offline semantics:
 * - Deduplicate by COALESCE(user_id, lower(trim(name)))
 * - Representative row = the row with MAX(created_at) for that identity
 * - Returns id, name, status, user_id, balance, totalBuyIns, totalExits, etc.
 * - Balances and history are aggregated across ALL rows of that identity.
 */
async function getGroupUniquePlayers(groupId, currentUserId = null) {
    const rawPlayers = await all(
        `SELECT p.id, p.table_id, p.user_id, p.name, p.status, p.created_at, p.entry_fee_paid,
                u.username as user_username
         FROM players p
         JOIN tables t ON p.table_id = t.id
         LEFT JOIN users u ON p.user_id = u.id
         WHERE t.group_id = ? AND p.is_deleted = 0 AND t.is_deleted = 0
         ORDER BY p.created_at DESC`,
        [groupId]
    );

    // Build map of lower(trim(name)) -> user_id for players who have a claimed user_id
    const nameToUserId = new Map();
    for (const p of rawPlayers) {
        if (p.user_id && p.name) {
            const clean = p.name.trim().toLowerCase();
            if (!nameToUserId.has(clean)) {
                nameToUserId.set(clean, p.user_id);
            }
        }
    }

    // Group rows by identity: COALESCE(user_id, lower(trim(name)))
    const identityMap = new Map();

    for (const p of rawPlayers) {
        const cleanName = (p.name || '').trim().toLowerCase();
        const effectiveUserId = p.user_id || nameToUserId.get(cleanName) || null;
        const identityKey = effectiveUserId ? `user:${effectiveUserId}` : `name:${cleanName}`;

        if (!identityMap.has(identityKey)) {
            identityMap.set(identityKey, {
                repRow: p, // first row is MAX(created_at) because ORDER BY p.created_at DESC
                effectiveUserId,
                rows: [p],
                playerIds: [p.id],
                names: new Set([p.name.trim()])
            });
        } else {
            const entry = identityMap.get(identityKey);
            entry.rows.push(p);
            entry.playerIds.push(p.id);
            entry.names.add(p.name.trim());
            if (!entry.effectiveUserId && effectiveUserId) {
                entry.effectiveUserId = effectiveUserId;
            }
            if (p.created_at > entry.repRow.created_at) {
                entry.repRow = p;
            }
        }
    }

    // Also check players from payments table
    const paymentPlayerRows = await all(
        `SELECT DISTINCT from_player as name FROM payments WHERE group_id = ? AND is_deleted = 0
         UNION
         SELECT DISTINCT to_player as name FROM payments WHERE group_id = ? AND is_deleted = 0`,
        [groupId, groupId]
    );

    for (const r of paymentPlayerRows) {
        const cleanName = (r.name || '').trim().toLowerCase();
        if (!cleanName) continue;
        const effectiveUserId = nameToUserId.get(cleanName) || null;
        const identityKey = effectiveUserId ? `user:${effectiveUserId}` : `name:${cleanName}`;

        if (!identityMap.has(identityKey)) {
            const placeholderRow = {
                id: null,
                table_id: null,
                user_id: effectiveUserId,
                name: r.name.trim(),
                status: 'ACTIVE',
                created_at: 0,
                entry_fee_paid: 0,
                user_username: null
            };
            identityMap.set(identityKey, {
                repRow: placeholderRow,
                effectiveUserId,
                rows: [],
                playerIds: [],
                names: new Set([r.name.trim()])
            });
        } else {
            identityMap.get(identityKey).names.add(r.name.trim());
        }
    }

    // Pre-fetch all buy-ins and exits for this group
    const groupBuyIns = await all(
        `SELECT b.player_id, COALESCE(SUM(b.amount), 0) as total
         FROM buy_ins b
         JOIN players p ON b.player_id = p.id
         JOIN tables t ON p.table_id = t.id
         WHERE t.group_id = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
         GROUP BY b.player_id`,
        [groupId]
    );
    const buyInMap = new Map();
    for (const b of groupBuyIns) {
        buyInMap.set(b.player_id, Number(b.total) || 0);
    }

    const groupExits = await all(
        `SELECT e.player_id, COALESCE(SUM(e.amount), 0) as total
         FROM exit_records e
         JOIN players p ON e.player_id = p.id
         JOIN tables t ON p.table_id = t.id
         WHERE t.group_id = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
         GROUP BY e.player_id`,
        [groupId]
    );
    const exitMap = new Map();
    for (const e of groupExits) {
        exitMap.set(e.player_id, Number(e.total) || 0);
    }

    const groupPayments = await all(
        `SELECT from_player, to_player, COALESCE(amount, 0) as amount
         FROM payments
         WHERE group_id = ? AND is_deleted = 0`,
        [groupId]
    );

    const uniquePlayers = [];

    for (const [, entry] of identityMap.entries()) {
        const { repRow, effectiveUserId, rows, playerIds, names } = entry;

        let totalBuyIns = 0;
        let totalExits = 0;
        for (const pid of playerIds) {
            totalBuyIns += (buyInMap.get(pid) || 0);
            totalExits += (exitMap.get(pid) || 0);
        }

        const nameList = Array.from(names).map(n => n.toLowerCase());
        let paymentsSent = 0;
        let paymentsReceived = 0;
        for (const pm of groupPayments) {
            const fromLower = (pm.from_player || '').trim().toLowerCase();
            const toLower = (pm.to_player || '').trim().toLowerCase();
            if (nameList.includes(fromLower)) {
                paymentsSent += Number(pm.amount) || 0;
            }
            if (nameList.includes(toLower)) {
                paymentsReceived += Number(pm.amount) || 0;
            }
        }

        const balance = (totalExits - totalBuyIns) + (paymentsSent - paymentsReceived);
        const isMe = Boolean(currentUserId && effectiveUserId && String(effectiveUserId) === String(currentUserId));

        uniquePlayers.push({
            id: repRow.id,
            name: repRow.name.trim(),
            status: repRow.status || 'ACTIVE',
            userId: effectiveUserId,
            user_id: effectiveUserId,
            username: repRow.user_username || repRow.name.trim(),
            isClaimed: Boolean(effectiveUserId),
            isMe: isMe,
            createdAt: repRow.created_at,
            created_at: repRow.created_at,
            sessionCount: rows.length,
            totalBuyIns,
            total_buy_ins: totalBuyIns,
            totalExits,
            total_exits: totalExits,
            paymentsSent,
            paymentsReceived,
            balance
        });
    }

    return uniquePlayers;
}

/**
 * Helper to calculate all player balances in a group (deduplicated)
 */
async function calculateGroupBalances(groupId, currentUserId = null) {
    const players = await getGroupUniquePlayers(groupId, currentUserId);
    return players.map(p => ({
        id: p.id,
        playerId: p.id,
        userId: p.userId,
        username: p.name,
        name: p.name,
        totalBuyIns: p.totalBuyIns,
        totalExits: p.totalExits,
        paymentsSent: p.paymentsSent,
        paymentsReceived: p.paymentsReceived,
        balance: p.balance,
        isMe: p.isMe
    })).sort((a, b) => b.balance - a.balance);
}

/**
 * GET /api/groups/:id/balances
 * (Requires Auth)
 * Return calculated balances for all players in this group (checking synced snapshot first)
 */
router.get('/:id/balances', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const userId = req.user?.id;

        const group = await get('SELECT * FROM groups WHERE (id = ? OR server_id = ?) AND is_deleted = 0', [groupId, groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const balances = await calculateGroupBalances(group.id, userId);
        return res.status(200).json({ balances });
    } catch (error) {
        console.error('Error fetching group balances:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group balances' });
    }
});

/**
 * POST /api/groups/:id/sync-balances
 * (Requires Auth)
 * Snapshot player balances pushed directly from Android app
 */
router.post('/:id/sync-balances', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const balances = req.body.balances || [];

        console.log("=== BALANCES SYNC ===");
        console.log("Group ID:", groupId);
        console.log("Balances count:", balances.length);
        console.log("Data:", JSON.stringify(balances, null, 2));

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            console.error(`Group not found for balance sync: ${groupId}`);
            return res.status(404).json({ error: 'Group not found' });
        }

        if (!Array.isArray(balances)) {
            return res.status(400).json({ error: 'balances array is required' });
        }

        const now = Date.now();
        await run('DELETE FROM synced_balances WHERE group_id = ?', [groupId]);

        for (const b of balances) {
            const username = (b.username || b.playerName || b.name || '').trim();
            const balance = Number(b.balance) || 0;
            const userId = b.userId || null;

            if (username) {
                const id = crypto.randomUUID();
                await run(
                    `INSERT INTO synced_balances (id, group_id, user_id, username, balance, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?)`,
                    [id, groupId, userId, username, balance, now]
                );
            }
        }

        console.log(`Successfully synced ${balances.length} balances for group ${groupId}`);
        return res.status(200).json({ message: 'Balances synced successfully' });
    } catch (error) {
        console.error('Error syncing balances:', error);
        return res.status(500).json({ error: 'Internal server error while syncing balances' });
    }
});

/**
 * Minimal-transfers greedy max-max settlement algorithm
 * with debt absorption / matching rule:
 * - NEVER split a debt across multiple creditors while any single creditor can absorb it fully.
 * - At most (n-1) transfers.
 *
 * @param {Array<{ name: string, balance?: number, net?: number }>} playerNets
 * @returns {Array<{ payerName: string, receiverName: string, amount: number }>}
 */
function computeSettlementPlan(playerNets) {
    const debtors = [];
    const creditors = [];

    for (const p of playerNets) {
        const net = Math.round(Number(p.net ?? p.balance) || 0);
        const name = (p.name || p.username || p.playerName || '').trim();
        if (!name) continue;
        if (net < 0) {
            debtors.push({ name, debt: -net });
        } else if (net > 0) {
            creditors.push({ name, credit: net });
        }
    }

    const transfers = [];

    while (debtors.length > 0 && creditors.length > 0) {
        debtors.sort((a, b) => b.debt - a.debt);
        creditors.sort((a, b) => b.credit - a.credit);

        // 1. Check for exact match (debt == credit)
        let matched = false;
        for (let dIdx = 0; dIdx < debtors.length; dIdx++) {
            const d = debtors[dIdx];
            const cIdx = creditors.findIndex(c => c.credit === d.debt);
            if (cIdx !== -1) {
                const c = creditors[cIdx];
                transfers.push({
                    payerName: d.name,
                    receiverName: c.name,
                    amount: d.debt
                });
                debtors.splice(dIdx, 1);
                creditors.splice(cIdx, 1);
                matched = true;
                break;
            }
        }
        if (matched) continue;

        // 2. Rule: NEVER split a debt across multiple creditors while any single creditor can absorb it fully.
        let absorbed = false;
        for (let dIdx = 0; dIdx < debtors.length; dIdx++) {
            const d = debtors[dIdx];
            const cIdx = creditors.findIndex(c => c.credit >= d.debt);
            if (cIdx !== -1) {
                const c = creditors[cIdx];
                transfers.push({
                    payerName: d.name,
                    receiverName: c.name,
                    amount: d.debt
                });
                c.credit -= d.debt;
                debtors.splice(dIdx, 1);
                if (c.credit === 0) {
                    creditors.splice(cIdx, 1);
                }
                absorbed = true;
                break;
            }
        }
        if (absorbed) continue;

        // 3. Fallback to greedy max-max: largest debtor pays largest creditor min(debt, credit)
        const d = debtors[0];
        const c = creditors[0];
        const amount = Math.min(d.debt, c.credit);

        transfers.push({
            payerName: d.name,
            receiverName: c.name,
            amount
        });

        d.debt -= amount;
        c.credit -= amount;

        if (d.debt === 0) debtors.splice(0, 1);
        if (c.credit === 0) creditors.splice(0, 1);
    }

    return transfers;
}

function formatSettlementRecord(r) {
    return {
        id: r.id,
        debtorName: r.payer_name,
        creditorName: r.receiver_name,
        payerName: r.payer_name,
        fromPlayer: r.payer_name,
        receiverName: r.receiver_name,
        toPlayer: r.receiver_name,
        amount: Number(r.amount) || 0,
        initialAmount: Number(r.initial_amount || r.amount) || 0,
        isPaid: Boolean(r.paid),
        paid: Boolean(r.paid),
        timestamp: r.timestamp
    };
}

async function generateOrUpdateSettlementPlan(groupId, forceRegenerate = false) {
    const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
    if (!group) {
        throw new Error('Group not found');
    }

    const existingRecords = await all(
        `SELECT * FROM settlement_records WHERE group_id = ? AND is_deleted = 0 ORDER BY timestamp ASC, id ASC`,
        [groupId]
    );

    if (!forceRegenerate && existingRecords && existingRecords.length > 0) {
        return existingRecords;
    }

    // Calculate balances
    const balances = await calculateGroupBalances(groupId);
    const plan = computeSettlementPlan(balances);

    // If forceRegenerate: REPLACES old unpaid settlement_records
    await run(
        `DELETE FROM settlement_records WHERE group_id = ? AND paid = 0`,
        [groupId]
    );

    // Fetch already paid settlement records to preserve paid status
    const paidRecords = await all(
        `SELECT payer_name, receiver_name, amount FROM settlement_records WHERE group_id = ? AND paid = 1 AND is_deleted = 0`,
        [groupId]
    );

    const now = Date.now();
    for (const t of plan) {
        const isAlreadyPaid = paidRecords.some(pr =>
            pr.payer_name?.toLowerCase().trim() === t.payerName?.toLowerCase().trim() &&
            pr.receiver_name?.toLowerCase().trim() === t.receiverName?.toLowerCase().trim() &&
            Number(pr.amount) === Number(t.amount)
        );

        if (!isAlreadyPaid) {
            const id = crypto.randomUUID();
            await run(
                `INSERT INTO settlement_records (id, group_id, table_id, table_name, payer_name, receiver_name, amount, initial_amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, 1, 0)`,
                [id, groupId, groupId, 'Group Settlement', t.payerName, t.receiverName, t.amount, t.amount, now, id, now]
            );
        }
    }

    return all(
        `SELECT * FROM settlement_records WHERE group_id = ? AND is_deleted = 0 ORDER BY timestamp ASC, id ASC`,
        [groupId]
    );
}

/**
 * GET /api/groups/:id/settlement-plan and GET /api/groups/:id/settlement
 * (Requires Auth)
 * Return the settlement plan (who pays whom). Generates on demand if empty.
 */
const handleGetSettlementPlan = async (req, res) => {
    try {
        const groupId = req.params.id;
        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const records = await generateOrUpdateSettlementPlan(groupId, false);
        const settlement = (records || []).map(formatSettlementRecord);
        return res.status(200).json({ settlement });
    } catch (error) {
        console.error('Error fetching settlement plan:', error);
        return res.status(500).json({ error: 'Internal server error while fetching settlement plan' });
    }
};

router.get('/:id/settlement-plan', authenticateToken, handleGetSettlementPlan);
router.get('/:id/settlement', authenticateToken, handleGetSettlementPlan);

/**
 * POST /api/groups/:id/settlement/regenerate and POST /api/groups/:id/settlement-plan/regenerate
 * (Requires Auth)
 * Regenerate settlement plan (replaces unpaid settlement_records) and broadcasts settlement_done.
 */
const handleRegenerateSettlement = async (req, res) => {
    try {
        const groupId = req.params.id;
        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const records = await generateOrUpdateSettlementPlan(groupId, true);
        const settlement = (records || []).map(formatSettlementRecord);
        emitToGroup(groupId, 'settlement_done', { groupId });
        return res.status(200).json({ message: 'Settlement plan regenerated successfully', settlement });
    } catch (error) {
        console.error('Error regenerating settlement plan:', error);
        return res.status(500).json({ error: 'Internal server error while regenerating settlement plan' });
    }
};

router.post('/:id/settlement/regenerate', authenticateToken, handleRegenerateSettlement);
router.post('/:id/settlement-plan/regenerate', authenticateToken, handleRegenerateSettlement);

/**
 * POST /api/groups/:id/settlement
 * (Requires Auth)
 * Sync/save settlement snapshot from Android app (idempotent replace of unpaid)
 */
router.post('/:id/settlement', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const settlements = req.body.settlement || req.body.settlements || [];

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        if (req.body.regenerate === true || (!req.body.settlement && !req.body.settlements)) {
            const records = await generateOrUpdateSettlementPlan(groupId, true);
            emitToGroup(groupId, 'settlement_done', { groupId });
            const settlement = (records || []).map(formatSettlementRecord);
            return res.status(200).json({ message: 'Settlement plan regenerated successfully', settlement });
        }

        if (!Array.isArray(settlements)) {
            return res.status(400).json({ error: 'settlement array is required' });
        }

        const now = Date.now();
        // Replace unpaid settlement records
        await run('DELETE FROM settlement_records WHERE group_id = ? AND paid = 0', [groupId]);

        for (const s of settlements) {
            const settlementId = s.id || crypto.randomUUID();
            const payerName = (s.debtorName || s.payerName || s.fromPlayer || '').trim();
            const receiverName = (s.creditorName || s.receiverName || s.toPlayer || '').trim();
            const amount = Number(s.amount) || 0;
            const initialAmount = Number(s.initialAmount || s.amount) || 0;
            const isPaid = (s.isPaid === true || s.paid === true || s.paid === 1 || s.isPaid === 1) ? 1 : 0;

            if (payerName && receiverName && amount > 0) {
                const existing = await get('SELECT id, paid FROM settlement_records WHERE id = ?', [settlementId]);
                if (!existing) {
                    await run(
                        `INSERT INTO settlement_records (id, group_id, table_id, table_name, payer_name, receiver_name, amount, initial_amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                        [settlementId, groupId, s.tableId || groupId, s.tableName || 'Group Settlement', payerName, receiverName, amount, initialAmount, isPaid, s.timestamp || now, settlementId, now]
                    );
                }
            }
        }

        emitToGroup(groupId, 'settlement_done', { groupId });
        const records = await all('SELECT * FROM settlement_records WHERE group_id = ? AND is_deleted = 0 ORDER BY timestamp ASC, id ASC', [groupId]);
        const settlement = (records || []).map(formatSettlementRecord);
        return res.status(200).json({ message: 'Settlement plan synced successfully', settlement });
    } catch (error) {
        console.error('Error syncing settlement plan:', error);
        return res.status(500).json({ error: 'Internal server error while syncing settlement plan' });
    }
});

/**
 * Toggle / update paid status of a settlement record
 */
const handleToggleSettlementPaid = async (req, res) => {
    try {
        const groupId = req.params.id;
        const recordId = req.params.recordId || req.body.recordId || req.body.id;

        if (!recordId) {
            return res.status(400).json({ error: 'recordId is required' });
        }

        const record = await get(
            'SELECT * FROM settlement_records WHERE (id = ? OR server_id = ?) AND group_id = ? AND is_deleted = 0',
            [recordId, recordId, groupId]
        );

        if (!record) {
            return res.status(404).json({ error: 'Settlement record not found' });
        }

        const now = Date.now();
        const nextPaid = req.body.paid !== undefined ? (req.body.paid ? 1 : 0) : (record.paid ? 0 : 1);

        await run(
            'UPDATE settlement_records SET paid = ?, updated_at = ? WHERE id = ?',
            [nextPaid, now, record.id]
        );

        emitToGroup(groupId, 'settlement_done', { groupId, recordId: record.id, paid: Boolean(nextPaid) });
        return res.status(200).json({
            message: 'Settlement record updated',
            recordId: record.id,
            isPaid: Boolean(nextPaid),
            paid: Boolean(nextPaid)
        });
    } catch (error) {
        console.error('Error toggling settlement paid:', error);
        return res.status(500).json({ error: 'Internal server error while toggling settlement paid' });
    }
};

router.post('/:id/settlement/:recordId/toggle-paid', authenticateToken, handleToggleSettlementPaid);
router.post('/:id/settlement/toggle-paid', authenticateToken, handleToggleSettlementPaid);
router.patch('/:id/settlement/:recordId/paid', authenticateToken, handleToggleSettlementPaid);

/**
 * POST /api/groups/:id/payments
 * (Requires Auth)
 * Record a payment between two players in a group
 */
router.post('/:id/payments', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const { fromPlayer, toPlayer, amount } = req.body;

        if (!fromPlayer || !toPlayer || amount == null || Number(amount) <= 0) {
            return res.status(400).json({ error: 'fromPlayer, toPlayer, and positive amount are required' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const paymentId = crypto.randomUUID();
        const now = Date.now();
        const numAmount = Number(amount);

        await run(
            `INSERT INTO payments (id, group_id, from_player, to_player, amount, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
            [paymentId, groupId, fromPlayer.trim(), toPlayer.trim(), numAmount, now, paymentId, now]
        );

        await run(
            `UPDATE settlement_records 
             SET paid = 1, updated_at = ?
             WHERE group_id = ? AND payer_name = ? AND receiver_name = ? AND paid = 0`,
            [now, groupId, fromPlayer.trim(), toPlayer.trim()]
        );

        emitToGroup(groupId, 'payment_created', {
            groupId,
            paymentId,
            fromPlayer: fromPlayer.trim(),
            toPlayer: toPlayer.trim(),
            amount: numAmount
        });
        emitToGroup(groupId, 'settlement_done', { groupId });

        return res.status(201).json({ message: 'Payment recorded successfully', paymentId });
    } catch (error) {
        console.error('Error recording payment:', error);
        return res.status(500).json({ error: 'Internal server error while recording payment' });
    }
});

/**
 * GET /api/groups/:id/stats
 * (Requires Auth)
 * Return group statistics: tables count, closed tables count, players count, biggest winner, biggest debtor
 */
router.get('/:id/stats', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const tablesCountRow = await get(
            'SELECT COUNT(*) as total FROM tables WHERE group_id = ? AND is_deleted = 0',
            [groupId]
        );
        const totalTables = tablesCountRow ? Number(tablesCountRow.total) : 0;

        const closedTablesCountRow = await get(
            `SELECT COUNT(*) as closed_total 
             FROM tables 
             WHERE group_id = ? AND is_deleted = 0 AND (status = 'CLOSED' OR is_active = 0)`,
            [groupId]
        );
        const closedTables = closedTablesCountRow ? Number(closedTablesCountRow.closed_total) : 0;

        // Calculate Overall Group Totals across all tables in this group
        const groupBuyInsRow = await get(
            `SELECT COALESCE(SUM(b.amount), 0) as total
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId]
        );
        const totalGroupBuyIns = groupBuyInsRow ? Number(groupBuyInsRow.total) : 0;

        const groupExitsRow = await get(
            `SELECT COALESCE(SUM(e.amount), 0) as total
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId]
        );
        const totalGroupExits = groupExitsRow ? Number(groupExitsRow.total) : 0;
        const totalGroupBalance = totalGroupBuyIns - totalGroupExits;

        const balances = await calculateGroupBalances(groupId);
        const totalPlayers = balances.length;

        const winners = balances.filter(b => b.balance > 0);
        const biggestWinner = winners.length > 0 ? { name: winners[0].username, balance: winners[0].balance } : null;

        const debtors = balances.filter(b => b.balance < 0).sort((a, b) => a.balance - b.balance);
        const biggestDebtor = debtors.length > 0 ? { name: debtors[0].username, balance: debtors[0].balance } : null;

        return res.status(200).json({
            totalTables,
            closedTables,
            totalPlayers,
            totalGroupBuyIns,
            totalGroupExits,
            totalGroupBalance,
            totalBuyIns: totalGroupBuyIns,
            totalExits: totalGroupExits,
            totalBalance: totalGroupBalance,
            biggestWinner,
            biggestDebtor
        });
    } catch (error) {
        console.error('Error fetching group stats:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group stats' });
    }
});

/**
 * GET /api/groups/:id/players-list and GET /api/groups/:id/players
 * (Requires Auth)
 * Return list of player identities in this group with claim status and aggregate balance
 */
const handleGetGroupPlayersList = async (req, res) => {
    try {
        const groupId = req.params.id;
        const userId = req.user?.id;

        const group = await get('SELECT * FROM groups WHERE (id = ? OR server_id = ?) AND is_deleted = 0', [groupId, groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const players = await getGroupUniquePlayers(group.id, userId);
        // Sort players alphabetically by name for clear roster selection
        players.sort((a, b) => a.name.localeCompare(b.name));

        const userHasClaimed = players.some(p => p.isMe);
        const hasUnclaimedPlayers = players.some(p => !p.isClaimed);

        return res.status(200).json({
            groupId: group.id,
            groupName: group.name,
            userHasClaimed,
            hasUnclaimedPlayers,
            players
        });
    } catch (error) {
        console.error('Error fetching group players list:', error);
        return res.status(500).json({ error: 'Internal server error while fetching players list' });
    }
};

router.get('/:id/players-list', authenticateToken, handleGetGroupPlayersList);
router.get('/:id/players', authenticateToken, handleGetGroupPlayersList);

/**
 * POST /api/groups/:id/claim-player
 * (Requires Auth)
 * Claim an existing player record in an offline-to-online converted group.
 * Allows safe RE-CLAIM by unlinking user_id from previously claimed rows.
 */
router.post('/:id/claim-player', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const { playerId, playerName } = req.body;
        const userId = req.user.id;

        if (!playerId && !playerName) {
            return res.status(400).json({ error: 'playerId or playerName is required' });
        }

        const group = await get('SELECT * FROM groups WHERE (id = ? OR server_id = ?) AND is_deleted = 0', [groupId, groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        let targetPlayer;
        if (playerId) {
            targetPlayer = await get(
                `SELECT p.* FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND p.id = ? AND p.is_deleted = 0 AND t.is_deleted = 0`,
                [group.id, playerId]
            );
        }

        if (!targetPlayer && playerName) {
            targetPlayer = await get(
                `SELECT p.* FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND LOWER(TRIM(p.name)) = LOWER(TRIM(?)) AND p.is_deleted = 0 AND t.is_deleted = 0
                 ORDER BY p.created_at DESC LIMIT 1`,
                [group.id, playerName.trim()]
            );
        }

        if (!targetPlayer) {
            return res.status(404).json({ error: 'Player identity not found in this group' });
        }

        const targetName = targetPlayer.name.trim();

        // Check if this player identity has already been claimed by another user
        const claimedByOther = await get(
            `SELECT p.id, p.user_id FROM players p
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND LOWER(TRIM(p.name)) = LOWER(TRIM(?))
               AND p.user_id IS NOT NULL AND p.user_id != ?
               AND p.is_deleted = 0 AND t.is_deleted = 0
             LIMIT 1`,
            [group.id, targetName, userId]
        );

        if (claimedByOther) {
            return res.status(400).json({ error: 'This player identity has already been claimed by another user' });
        }

        const now = Date.now();

        // 10-minute re-claim check: if user already claimed an identity in this group, verify within 10 minutes
        const existingClaimRow = await get(
            `SELECT p.user_linked_at FROM players p
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.user_id = ? AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY p.user_linked_at DESC LIMIT 1`,
            [group.id, userId]
        );

        if (existingClaimRow && existingClaimRow.user_linked_at) {
            const tenMinutesMs = 10 * 60 * 1000;
            if (now - existingClaimRow.user_linked_at > tenMinutesMs) {
                return res.status(400).json({ error: 'Re-claim window has expired (10 minutes limit)' });
            }
        }

        const { newDisplayName } = req.body;
        const finalName = (newDisplayName && newDisplayName.trim()) ? newDisplayName.trim() : targetName;

        // RE-CLAIM: If user already claimed another player identity in this group, unlink the old one
        await run(
            `UPDATE players
             SET user_id = NULL, user_linked_at = NULL, updated_at = ?
             WHERE id IN (
                 SELECT p.id FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND p.user_id = ? AND LOWER(TRIM(p.name)) != LOWER(TRIM(?))
                   AND p.is_deleted = 0 AND t.is_deleted = 0
             )`,
            [now, group.id, userId, targetName]
        );

        // Set user_id and user_linked_at on ALL rows of the new identity in this group
        await run(
            `UPDATE players
             SET user_id = ?, name = ?, user_linked_at = ?, updated_at = ?
             WHERE id IN (
                 SELECT p.id FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND LOWER(TRIM(p.name)) = LOWER(TRIM(?))
                   AND p.is_deleted = 0 AND t.is_deleted = 0
             )`,
            [userId, finalName, now, now, group.id, targetName]
        );

        // Also update synced_balances if present
        await run(
            `UPDATE synced_balances
             SET user_id = NULL, updated_at = ?
             WHERE group_id = ? AND user_id = ? AND LOWER(TRIM(username)) != LOWER(TRIM(?))`,
            [now, group.id, userId, targetName]
        );
        await run(
            `UPDATE synced_balances
             SET user_id = ?, username = ?, updated_at = ?
             WHERE group_id = ? AND LOWER(TRIM(username)) = LOWER(TRIM(?))`,
            [userId, finalName, now, group.id, targetName]
        );

        // Ensure user is in group_members
        await run(
            `INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at)
             VALUES (?, ?, ?)`,
            [userId, group.id, now]
        );

        emitToGroup(group.id, 'claim_done', {
            groupId: group.id,
            userId,
            playerName: finalName,
            userLinkedAt: now
        });

        return res.status(200).json({
            message: 'Player claimed successfully',
            playerId: targetPlayer.id,
            playerName: finalName,
            userId: userId,
            userLinkedAt: now,
            isMe: true
        });
    } catch (error) {
        console.error('Error claiming player:', error);
        return res.status(500).json({ error: 'Internal server error while claiming player' });
    }
});

/**
 * POST /api/groups/:id/join-new-player
 * (Requires Auth)
 * Create a new player identity in this group
 */
router.post('/:id/join-new-player', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const { playerName } = req.body;
        const userId = req.user.id;

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const chosenName = (playerName && playerName.trim()) ? playerName.trim() : req.user.username;
        const now = Date.now();

        await run(
            `INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at)
             VALUES (?, ?, ?)`,
            [userId, groupId, now]
        );

        const activeTables = await all(
            `SELECT id FROM tables WHERE group_id = ? AND is_deleted = 0 AND (status = 'ACTIVE' OR is_active = 1)`,
            [groupId]
        );

        let createdPlayerId = crypto.randomUUID();
        for (const t of activeTables) {
            const existing = await get(
                'SELECT id FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0',
                [t.id, userId, chosenName]
            );
            if (!existing) {
                const pid = crypto.randomUUID();
                createdPlayerId = pid;
                await run(
                    `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                    [pid, t.id, userId, chosenName, now, pid, now]
                );
            }
        }

        return res.status(200).json({
            message: 'New player created',
            playerId: createdPlayerId,
            playerName: chosenName
        });
    } catch (error) {
        console.error('Error joining as new player:', error);
        return res.status(500).json({ error: 'Internal server error while creating new player' });
    }
});

router.computeSettlementPlan = computeSettlementPlan;

module.exports = router;
