const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');
const { generateInviteCode } = require('../utils/helpers');

/**
 * POST /api/groups/create
 * (Requires Auth + role='ADMIN')
 * Create a new group with a unique 6-character invite code
 */
router.post('/create', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { name, mode } = req.body;

        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Group name is required' });
        }

        const trimmedName = name.trim();
        const groupMode = (mode && typeof mode === 'string' && mode.trim().toUpperCase() === 'ONLINE') ? 'ONLINE' : 'OFFLINE';
        const groupId = crypto.randomUUID();
        const now = Date.now();
        const createdBy = req.user.id;

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
            `INSERT INTO groups (id, name, invite_code, mode, created_by, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0)`,
            [groupId, trimmedName, inviteCode, groupMode, createdBy, now, groupId, now]
        );

        // Also add the admin creator to group_members
        await run(
            `INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at)
             VALUES (?, ?, ?)`,
            [createdBy, groupId, now]
        );

        return res.status(201).json({
            message: 'Group created successfully',
            groupId,
            inviteCode,
            mode: groupMode,
            group: {
                id: groupId,
                name: trimmedName,
                invite_code: inviteCode,
                mode: groupMode,
                created_by: createdBy,
                created_at: now,
                updated_at: now
            }
        });
    } catch (error) {
        console.error('Error creating group:', error);
        return res.status(500).json({ error: 'Internal server error while creating group' });
    }
});

/**
 * POST /api/groups/import
 * (Requires Auth + role='ADMIN')
 * Convert an offline group to online with bulk data sync and invite code generation
 */
router.post('/import', authenticateToken, requireAdmin, async (req, res) => {
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
        const newGroupId = crypto.randomUUID();
        const idMapping = {};

        if (group.id) {
            idMapping[group.id] = newGroupId;
        }

        // Use provided invite code or generate unique 6-character invite code
        const localCode = (group.inviteCode || group.invite_code || '').trim().toUpperCase();
        let inviteCode = localCode;
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

        // 1. Insert Group with mode = ONLINE
        await run(
            `INSERT INTO groups (id, name, invite_code, mode, created_by, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, 'ONLINE', ?, ?, ?, ?, 1, 0)`,
            [newGroupId, group.name.trim(), inviteCode, createdBy, group.createdAt || group.created_at || now, newGroupId, now]
        );

        console.log("Imported group:", newGroupId, "name:", group.name.trim(), "invite code:", inviteCode);

        // 2. Add creator to group_members
        await run(
            `INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at)
             VALUES (?, ?, ?)`,
            [createdBy, newGroupId, now]
        );

        // 3. Insert Tables
        for (const t of tables) {
            const newTableId = crypto.randomUUID();
            idMapping[t.id] = newTableId;
            const status = t.status || (t.isActive === false ? 'CLOSED' : 'ACTIVE');
            const isActive = (status === 'ACTIVE' || t.isActive === true || t.is_active === 1) ? 1 : 0;

            await run(
                `INSERT INTO tables (id, group_id, name, chip_value, status, created_at, closed_at, has_entry_fee, entry_fee, server_id, updated_at, is_synced, is_deleted, is_active)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)`,
                [
                    newTableId,
                    newGroupId,
                    t.name || 'Table',
                    t.chipValue ?? t.chip_value ?? null,
                    status,
                    t.createdAt ?? t.created_at ?? now,
                    t.closedAt ?? t.closed_at ?? null,
                    t.hasEntryFee ? 1 : 0,
                    t.entryFee ?? t.entry_fee ?? null,
                    newTableId,
                    now,
                    isActive
                ]
            );
        }

        // 4. Insert Players (user_id = null so players can claim via invite code)
        for (const p of players) {
            const newPlayerId = crypto.randomUUID();
            idMapping[p.id] = newPlayerId;
            const mappedTableId = idMapping[p.tableId || p.table_id] || p.tableId || p.table_id;

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

        // 5. Insert BuyIns
        for (const b of buyIns) {
            const newBuyInId = crypto.randomUUID();
            idMapping[b.id] = newBuyInId;
            const mappedTableId = idMapping[b.tableId || b.table_id] || b.tableId || b.table_id;
            const mappedPlayerId = idMapping[b.playerId || b.player_id] || b.playerId || b.player_id;

            await run(
                `INSERT INTO buy_ins (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                [
                    newBuyInId,
                    mappedTableId,
                    mappedPlayerId,
                    Number(b.amount) || 0,
                    b.note || null,
                    b.createdAt ?? b.created_at ?? now,
                    newBuyInId,
                    now
                ]
            );
        }

        // 6. Insert Exits
        for (const e of exits) {
            const newExitId = crypto.randomUUID();
            idMapping[e.id] = newExitId;
            const mappedTableId = idMapping[e.tableId || e.table_id] || e.tableId || e.table_id;
            const mappedPlayerId = idMapping[e.playerId || e.player_id] || e.playerId || e.player_id;

            await run(
                `INSERT INTO exit_records (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                [
                    newExitId,
                    mappedTableId,
                    mappedPlayerId,
                    Number(e.amount) || 0,
                    e.note || null,
                    e.createdAt ?? e.created_at ?? now,
                    newExitId,
                    now
                ]
            );
        }

        // 7. Insert Payments
        for (const pm of payments) {
            const newPaymentId = crypto.randomUUID();
            idMapping[pm.id] = newPaymentId;

            await run(
                `INSERT INTO payments (id, group_id, from_player, to_player, amount, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                [
                    newPaymentId,
                    newGroupId,
                    pm.fromPlayer || pm.from_player || '',
                    pm.toPlayer || pm.to_player || '',
                    Number(pm.amount) || 0,
                    pm.createdAt ?? pm.created_at ?? now,
                    newPaymentId,
                    now
                ]
            );
        }

        // 8. Insert Settlements
        for (const s of settlements) {
            const newSettlementId = crypto.randomUUID();
            idMapping[s.id] = newSettlementId;
            const mappedTableId = idMapping[s.tableId || s.table_id] || s.tableId || s.table_id || newGroupId;

            await run(
                `INSERT INTO settlement_records (id, group_id, table_id, table_name, payer_name, receiver_name, amount, initial_amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                [
                    newSettlementId,
                    newGroupId,
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

        // 9. Insert Entry Fees
        for (const ef of entryFees) {
            const newEfId = crypto.randomUUID();
            idMapping[ef.id] = newEfId;
            const mappedTableId = idMapping[ef.tableId || ef.table_id] || ef.tableId || ef.table_id || newGroupId;

            await run(
                `INSERT INTO entry_fee_records (id, group_id, table_id, table_name, player_name, amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                [
                    newEfId,
                    newGroupId,
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

        return res.status(201).json({
            message: 'Group imported and converted to online successfully',
            groupId: newGroupId,
            inviteCode,
            idMapping
        });
    } catch (error) {
        console.error('Error importing group to online:', error);
        return res.status(500).json({ error: 'Internal server error while importing group' });
    }
});

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
                    mode: group.mode || 'OFFLINE',
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
                mode: group.mode || 'OFFLINE',
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

        let groups;
        if (userRole === 'ADMIN') {
            groups = await all(
                `SELECT DISTINCT g.id, g.name, g.invite_code, g.mode, g.created_by, g.created_at, g.server_id, g.updated_at, gm.joined_at
                 FROM groups g
                 LEFT JOIN group_members gm ON g.id = gm.group_id AND gm.user_id = ?
                 WHERE (g.created_by = ? OR gm.user_id = ?) AND g.is_deleted = 0
                 ORDER BY g.name ASC`,
                [userId, userId, userId]
            );
        } else {
            groups = await all(
                `SELECT g.id, g.name, g.invite_code, g.mode, g.created_by, g.created_at, g.server_id, g.updated_at, gm.joined_at
                 FROM groups g
                 INNER JOIN group_members gm ON g.id = gm.group_id
                 WHERE gm.user_id = ? AND g.is_deleted = 0
                 ORDER BY g.name ASC`,
                [userId]
            );
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
        const currentBalance = netGameBalance + paymentsSent - paymentsReceived;
        const balance = (totalBuyIns - totalExits) + (paymentsReceived - paymentsSent);

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
            balance,
            currentBalance,
            netGameBalance,
            totalBuyIns: totalGroupBuyIns,
            totalExits: totalGroupExits,
            totalGroupBuyIns,
            totalGroupExits,
            totalGroupBalance,
            userTotalBuyIns: totalBuyIns,
            userTotalExits: totalExits,
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

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const rawTables = await all(
            `SELECT t.*, (
                SELECT COUNT(*) FROM players p WHERE p.table_id = t.id AND p.is_deleted = 0 AND p.status = 'ACTIVE'
             ) as playerCount
             FROM tables t
             WHERE t.group_id = ? AND t.is_deleted = 0
             ORDER BY t.created_at DESC`,
            [groupId]
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
 * Helper to calculate all player balances in a group
 */
async function calculateGroupBalances(groupId) {
    const playerRows = await all(
        `SELECT DISTINCT p.name, p.user_id, u.username
         FROM players p
         JOIN tables t ON p.table_id = t.id
         LEFT JOIN users u ON p.user_id = u.id
         WHERE t.group_id = ? AND p.is_deleted = 0 AND t.is_deleted = 0`,
        [groupId]
    );

    const paymentPlayerRows = await all(
        `SELECT DISTINCT from_player as name FROM payments WHERE group_id = ? AND is_deleted = 0
         UNION
         SELECT DISTINCT to_player as name FROM payments WHERE group_id = ? AND is_deleted = 0`,
        [groupId, groupId]
    );

    const playerMap = new Map();
    for (const r of playerRows) {
        if (!playerMap.has(r.name)) {
            playerMap.set(r.name, {
                userId: r.user_id || null,
                username: r.name
            });
        }
    }
    for (const r of paymentPlayerRows) {
        if (!playerMap.has(r.name)) {
            playerMap.set(r.name, {
                userId: null,
                username: r.name
            });
        }
    }

    const balances = [];
    for (const [name, info] of playerMap.entries()) {
        const buyInsRow = await get(
            `SELECT COALESCE(SUM(b.amount), 0) as total_buy_ins
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, name]
        );
        const totalBuyIns = buyInsRow ? Number(buyInsRow.total_buy_ins) : 0;

        const exitsRow = await get(
            `SELECT COALESCE(SUM(e.amount), 0) as total_exits
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, name]
        );
        const totalExits = exitsRow ? Number(exitsRow.total_exits) : 0;

        const sentRow = await get(
            `SELECT COALESCE(SUM(amount), 0) as sent
             FROM payments
             WHERE group_id = ? AND from_player = ? AND is_deleted = 0`,
            [groupId, name]
        );
        const paymentsSent = sentRow ? Number(sentRow.sent) : 0;

        const recRow = await get(
            `SELECT COALESCE(SUM(amount), 0) as received
             FROM payments
             WHERE group_id = ? AND to_player = ? AND is_deleted = 0`,
            [groupId, name]
        );
        const paymentsReceived = recRow ? Number(recRow.received) : 0;

        const balance = (totalExits - totalBuyIns) + (paymentsSent - paymentsReceived);

        balances.push({
            userId: info.userId,
            username: info.username,
            totalBuyIns,
            totalExits,
            paymentsSent,
            paymentsReceived,
            balance
        });
    }

    balances.sort((a, b) => b.balance - a.balance);
    return balances;
}

/**
 * GET /api/groups/:id/balances
 * (Requires Auth)
 * Return calculated balances for all players in this group
 */
router.get('/:id/balances', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const balances = await calculateGroupBalances(groupId);
        return res.status(200).json({ balances });
    } catch (error) {
        console.error('Error fetching group balances:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group balances' });
    }
});

/**
 * GET /api/groups/:id/settlement-plan
 * (Requires Auth)
 * Return the settlement plan (who pays whom) synced from Android
 */
router.get('/:id/settlement-plan', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const records = await all(
            `SELECT * FROM settlement_records WHERE group_id = ? AND is_deleted = 0 ORDER BY timestamp ASC, id ASC`,
            [groupId]
        );

        const settlement = (records || []).map(r => ({
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
        }));

        return res.status(200).json({ settlement });
    } catch (error) {
        console.error('Error fetching settlement plan:', error);
        return res.status(500).json({ error: 'Internal server error while fetching settlement plan' });
    }
});

/**
 * POST /api/groups/:id/settlement
 * (Requires Auth)
 * Sync/save settlement snapshot from Android app (idempotent full replace)
 */
router.post('/:id/settlement', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const settlements = req.body.settlement || req.body.settlements || [];

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        if (!Array.isArray(settlements)) {
            return res.status(400).json({ error: 'settlement array is required' });
        }

        const now = Date.now();
        await run('DELETE FROM settlement_records WHERE group_id = ?', [groupId]);

        for (const s of settlements) {
            const settlementId = s.id || crypto.randomUUID();
            const payerName = (s.debtorName || s.payerName || s.fromPlayer || '').trim();
            const receiverName = (s.creditorName || s.receiverName || s.toPlayer || '').trim();
            const amount = Number(s.amount) || 0;
            const initialAmount = Number(s.initialAmount || s.amount) || 0;
            const isPaid = (s.isPaid === true || s.paid === true || s.paid === 1 || s.isPaid === 1) ? 1 : 0;

            if (payerName && receiverName && amount > 0) {
                await run(
                    `INSERT INTO settlement_records (id, group_id, table_id, table_name, payer_name, receiver_name, amount, initial_amount, paid, timestamp, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
                    [
                        settlementId,
                        groupId,
                        s.tableId || groupId,
                        s.tableName || 'Group Settlement',
                        payerName,
                        receiverName,
                        amount,
                        initialAmount,
                        isPaid,
                        s.timestamp || now,
                        settlementId,
                        now
                    ]
                );
            }
        }

        console.log(`Synced ${settlements.length} settlements for group ${groupId}`);
        return res.status(200).json({ message: 'Settlement plan synced successfully' });
    } catch (error) {
        console.error('Error syncing settlement plan:', error);
        return res.status(500).json({ error: 'Internal server error while syncing settlement plan' });
    }
});

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
 * GET /api/groups/:id/players-list
 * (Requires Auth)
 * Return list of player identities in this group with claim status
 */
router.get('/:id/players-list', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const rawPlayers = await all(
            `SELECT p.id, p.name, p.user_id, p.table_id
             FROM players p
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY p.name ASC, p.created_at ASC`,
            [groupId]
        );

        const playerMap = new Map();
        for (const p of rawPlayers) {
            const name = p.name.trim();
            if (!playerMap.has(name)) {
                playerMap.set(name, {
                    id: p.id,
                    name: name,
                    isClaimed: Boolean(p.user_id)
                });
            } else {
                if (p.user_id) {
                    playerMap.get(name).isClaimed = true;
                }
            }
        }

        const players = Array.from(playerMap.values());

        return res.status(200).json({
            groupId: group.id,
            groupName: group.name,
            players
        });
    } catch (error) {
        console.error('Error fetching group players list:', error);
        return res.status(500).json({ error: 'Internal server error while fetching players list' });
    }
});

/**
 * POST /api/groups/:id/claim-player
 * (Requires Auth)
 * Claim an existing player record in an offline-to-online converted group
 */
router.post('/:id/claim-player', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const { playerId, playerName } = req.body;
        const userId = req.user.id;

        if (!playerId && !playerName) {
            return res.status(400).json({ error: 'playerId or playerName is required' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        let targetPlayer;
        if (playerId) {
            targetPlayer = await get(
                `SELECT p.* FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND p.id = ? AND p.is_deleted = 0 AND t.is_deleted = 0`,
                [groupId, playerId]
            );
        }

        if (!targetPlayer && playerName) {
            targetPlayer = await get(
                `SELECT p.* FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND p.name = ? AND p.is_deleted = 0 AND t.is_deleted = 0
                 ORDER BY p.created_at ASC LIMIT 1`,
                [groupId, playerName.trim()]
            );
        }

        if (!targetPlayer) {
            return res.status(404).json({ error: 'Player identity not found in this group' });
        }

        if (targetPlayer.user_id && targetPlayer.user_id !== userId) {
            return res.status(400).json({ error: 'This player identity has already been claimed by another user' });
        }

        const now = Date.now();

        await run(
            `UPDATE players
             SET user_id = ?, updated_at = ?
             WHERE id IN (
                 SELECT p.id FROM players p
                 JOIN tables t ON p.table_id = t.id
                 WHERE t.group_id = ? AND p.name = ? AND (p.user_id IS NULL OR p.user_id = ?)
             )`,
            [userId, now, groupId, targetPlayer.name, userId]
        );

        await run(
            `INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at)
             VALUES (?, ?, ?)`,
            [userId, groupId, now]
        );

        return res.status(200).json({
            message: 'Player claimed successfully',
            playerId: targetPlayer.id,
            playerName: targetPlayer.name
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

module.exports = router;
