const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken } = require('../middleware/auth');
const { generateInviteCode } = require('../utils/helpers');
const { emitToTable, emitToGroup } = require('../socket');
const { sendNotification } = require('../services/notifications');

/**
 * Check if user has permission to administer a table:
 * Table creator, group owner, or SUPER_ADMIN.
 */
async function canManageTable(userId, role, table) {
    if (role === 'SUPER_ADMIN') return true;
    if (table.creator_user_id && table.creator_user_id === userId) return true;
    if (table.group_id) {
        const group = await get('SELECT owner_user_id FROM groups WHERE id = ?', [table.group_id]);
        if (group && group.owner_user_id === userId) return true;
    }
    return false;
}

/**
 * Generate a unique 6-character table code
 */
async function generateUniqueTableCode() {
    let code = generateInviteCode();
    let attempts = 0;
    while (attempts < 20) {
        const existing = await get('SELECT id FROM tables WHERE UPPER(TRIM(code)) = ?', [code]);
        if (!existing) return code;
        code = generateInviteCode();
        attempts++;
    }
    return code;
}

/**
 * POST /api/tables/quick
 * Create a Quick Table (group_id is NULL, code is NULL until published)
 */
router.post('/quick', authenticateToken, async (req, res) => {
    try {
        const { name, chipValue, entryFee, playerNames = [] } = req.body;
        const userId = req.user.id;

        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Table name is required' });
        }

        const tableId = crypto.randomUUID();
        const now = Date.now();
        const numChipValue = chipValue != null && !isNaN(Number(chipValue)) ? Number(chipValue) : null;
        const numEntryFee = entryFee != null && !isNaN(Number(entryFee)) ? Number(entryFee) : null;
        const hasEntryFee = numEntryFee != null && numEntryFee > 0 ? 1 : 0;

        // Insert quick table with group_id = NULL and code = NULL
        await run(
            `INSERT INTO tables (id, group_id, creator_user_id, name, code, chip_value, status, created_at, closed_at, published_at, has_entry_fee, entry_fee, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, NULL, ?, ?, NULL, ?, 'ACTIVE', ?, NULL, NULL, ?, ?, ?, ?, 1, 0)`,
            [tableId, userId, name.trim(), numChipValue, now, hasEntryFee, numEntryFee, tableId, now]
        );

        // Add initial players if provided
        const createdPlayers = [];
        if (Array.isArray(playerNames)) {
            for (const pName of playerNames) {
                if (pName && pName.trim()) {
                    const pId = crypto.randomUUID();
                    await run(
                        `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                         VALUES (?, ?, NULL, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                        [pId, tableId, pName.trim(), now, pId, now]
                    );
                    createdPlayers.push({ id: pId, name: pName.trim() });
                }
            }
        }

        return res.status(201).json({
            message: 'Quick table created successfully',
            tableId,
            table: {
                id: tableId,
                groupId: null,
                name: name.trim(),
                code: null, // Code remains NULL and hidden until published
                chip_value: numChipValue,
                has_entry_fee: Boolean(hasEntryFee),
                entry_fee: numEntryFee,
                status: 'ACTIVE',
                creator_user_id: userId,
                created_at: now,
                published_at: null,
                players: createdPlayers
            }
        });
    } catch (error) {
        console.error('Error creating quick table:', error);
        return res.status(500).json({ error: 'Failed to create quick table' });
    }
});

/**
 * POST /api/tables/:id/publish
 * Assigns unique 6-character code and sets published_at (activates online sharing)
 */
router.post('/:id/publish', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to publish this table' });
        }

        const now = Date.now();
        let code = table.code;
        if (!code) {
            code = await generateUniqueTableCode();
        }

        await run(
            `UPDATE tables 
             SET code = ?, published_at = COALESCE(published_at, ?), updated_at = ?
             WHERE id = ?`,
            [code, now, now, tableId]
        );

        const publishedData = { tableId, code, publishedAt: table.published_at || now };
        emitToTable(tableId, 'table_published', publishedData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'table_published', publishedData);
        }

        return res.json({
            message: 'Table published successfully',
            tableId,
            code,
            published_at: table.published_at || now
        });
    } catch (error) {
        console.error('Error publishing table:', error);
        return res.status(500).json({ error: 'Failed to publish table' });
    }
});

/**
 * GET /api/tables/by-code/:code
 * Fetch table information by table code
 */
router.get('/by-code/:code', async (req, res) => {
    try {
        const cleanCode = req.params.code.trim().toUpperCase();
        const table = await get(
            `SELECT t.*, g.name as group_name, u.username as creator_username, u.display_name as creator_display_name, u.avatar_id as creator_avatar_id
             FROM tables t
             LEFT JOIN groups g ON t.group_id = g.id
             LEFT JOIN users u ON t.creator_user_id = u.id
             WHERE UPPER(TRIM(t.code)) = ? AND t.is_deleted = 0`,
            [cleanCode]
        );

        if (!table) {
            return res.status(404).json({ error: 'Table not found for code' });
        }

        const playerCountRow = await get('SELECT COUNT(*) as count FROM players WHERE table_id = ? AND is_deleted = 0', [table.id]);

        return res.json({
            table: {
                id: table.id,
                groupId: table.group_id,
                groupName: table.group_name || null,
                name: table.name,
                code: table.code,
                status: table.status,
                chipValue: table.chip_value,
                hasEntryFee: Boolean(table.has_entry_fee),
                entryFee: table.entry_fee,
                isQuickTable: !table.group_id,
                playerCount: playerCountRow?.count || 0,
                creator: table.creator_user_id ? {
                    id: table.creator_user_id,
                    username: table.creator_username,
                    displayName: table.creator_display_name || table.creator_username,
                    avatarId: table.creator_avatar_id || 'avatar_1'
                } : null,
                createdAt: table.created_at,
                publishedAt: table.published_at
            }
        });
    } catch (err) {
        console.error('Error fetching table by code:', err);
        return res.status(500).json({ error: 'Failed to fetch table by code' });
    }
});

/**
 * POST /api/tables/create
 * Create a new table inside a group with member player IDs and manual new player names
 */
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { groupId, name, chipValue, entryFee, memberPlayerIds = [], newPlayerNames = [] } = req.body;
        const userId = req.user.id;

        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Table name is required' });
        }

        if (!groupId) {
            return res.status(400).json({ error: 'groupId is required' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // Verify membership or super admin
        const member = await get('SELECT * FROM group_members WHERE user_id = ? AND group_id = ?', [userId, groupId]);
        if (!member && group.owner_user_id !== userId && req.user.role !== 'SUPER_ADMIN') {
            return res.status(403).json({ error: 'Must be a group member to create a table' });
        }

        const tableId = crypto.randomUUID();
        const now = Date.now();
        const numChipValue = chipValue != null && !isNaN(Number(chipValue)) ? Number(chipValue) : null;
        const numEntryFee = entryFee != null && !isNaN(Number(entryFee)) ? Number(entryFee) : null;
        const hasEntryFee = numEntryFee != null && numEntryFee > 0 ? 1 : 0;
        const code = await generateUniqueTableCode();

        await run(
            `INSERT INTO tables (id, group_id, creator_user_id, name, code, chip_value, status, created_at, closed_at, published_at, has_entry_fee, entry_fee, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NULL, ?, ?, ?, ?, ?, 1, 0)`,
            [tableId, groupId, userId, name.trim(), code, numChipValue, now, now, hasEntryFee, numEntryFee, tableId, now]
        );

        // Add selected group members
        const createdPlayers = [];
        if (Array.isArray(memberPlayerIds)) {
            for (const mId of memberPlayerIds) {
                const sourcePlayer = await get('SELECT name, user_id FROM players WHERE id = ?', [mId]);
                if (sourcePlayer) {
                    const pId = crypto.randomUUID();
                    await run(
                        `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                         VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                        [pId, tableId, sourcePlayer.user_id || null, sourcePlayer.name, now, pId, now]
                    );
                    createdPlayers.push({ id: pId, name: sourcePlayer.name, userId: sourcePlayer.user_id });
                }
            }
        }

        // Add manual new players
        if (Array.isArray(newPlayerNames)) {
            for (const pName of newPlayerNames) {
                if (pName && pName.trim()) {
                    const pId = crypto.randomUUID();
                    await run(
                        `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                         VALUES (?, ?, NULL, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                        [pId, tableId, pName.trim(), now, pId, now]
                    );
                    createdPlayers.push({ id: pId, name: pName.trim(), userId: null });
                }
            }
        }

        const tablePayload = {
            id: tableId,
            groupId,
            name: name.trim(),
            code,
            chip_value: numChipValue,
            has_entry_fee: Boolean(hasEntryFee),
            entry_fee: numEntryFee,
            status: 'ACTIVE',
            created_at: now,
            published_at: now,
            playerCount: createdPlayers.length
        };

        // Realtime notification & event
        emitToGroup(groupId, 'table_created', tablePayload);

        // Notify other group members
        const groupMembers = await all('SELECT user_id FROM group_members WHERE group_id = ? AND user_id != ?', [groupId, userId]);
        for (const gm of groupMembers) {
            sendNotification(gm.user_id, {
                type: 'table_created',
                title: 'New Table Created',
                message: `Table "${name.trim()}" was created in ${group.name}.`,
                targetId: tableId,
                targetType: 'TABLE',
                data: { groupId, tableId }
            }).catch(() => {});
        }

        return res.status(201).json({
            message: 'Table created successfully',
            tableId,
            code,
            table: tablePayload,
            players: createdPlayers
        });
    } catch (error) {
        console.error('Error creating table in group:', error);
        return res.status(500).json({ error: 'Failed to create table' });
    }
});

/**
 * DELETE /api/tables/:tableId/players/:playerId
 * Delete player: only table creator / group owner / SUPER_ADMIN AND only if player has ZERO buy-ins;
 * else 400 "This player has buy-ins and cannot be deleted"
 */
router.delete('/:tableId/players/:playerId', authenticateToken, async (req, res) => {
    try {
        const { tableId, playerId } = req.params;

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to delete player' });
        }

        const player = await get('SELECT * FROM players WHERE id = ? AND table_id = ? AND is_deleted = 0', [playerId, tableId]);
        if (!player) {
            return res.status(404).json({ error: 'Player not found on this table' });
        }

        // Check if player has any buy-ins
        const buyInCountRow = await get(
            'SELECT COUNT(*) as cnt FROM buy_ins WHERE player_id = ? AND table_id = ? AND is_deleted = 0',
            [playerId, tableId]
        );

        if (buyInCountRow && buyInCountRow.cnt > 0) {
            return res.status(400).json({ error: 'This player has buy-ins and cannot be deleted' });
        }

        // Delete player safely
        await run('DELETE FROM players WHERE id = ?', [playerId]);

        const deletePayload = { tableId, playerId, playerName: player.name };
        emitToTable(tableId, 'player_deleted', deletePayload);
        if (table.group_id) {
            emitToGroup(table.group_id, 'player_deleted', deletePayload);
        }

        return res.json({ message: 'Player deleted successfully', playerId });
    } catch (err) {
        console.error('Error deleting player:', err);
        return res.status(500).json({ error: 'Failed to delete player' });
    }
});

/**
 * POST /api/tables/:id/players
 * Table creator or group owner manually adds player to table
 */
router.post('/:id/players', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { name, userId } = req.body;

        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Player name is required' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to add player' });
        }

        const playerId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
            [playerId, tableId, userId || null, name.trim(), now, playerId, now]
        );

        const newPlayer = {
            id: playerId,
            tableId,
            userId: userId || null,
            name: name.trim(),
            status: 'ACTIVE',
            totalBuyIns: 0,
            totalExits: 0,
            balance: 0,
            createdAt: now
        };

        emitToTable(tableId, 'player_added', newPlayer);
        if (table.group_id) {
            emitToGroup(table.group_id, 'player_added', newPlayer);
        }

        return res.status(201).json({
            message: 'Player added successfully',
            player: newPlayer
        });
    } catch (err) {
        console.error('Error adding player:', err);
        return res.status(500).json({ error: 'Failed to add player' });
    }
});

/**
 * POST /api/tables/:id/buy-ins
 * Manual buy-in recorded by table manager
 */
router.post('/:id/buy-ins', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { playerId, userId, username, amount, note } = req.body;

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount <= 0) {
            return res.status(400).json({ error: 'amount must be a positive number' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to record buy-in' });
        }

        let player = null;
        if (playerId) {
            player = await get('SELECT * FROM players WHERE id = ? AND table_id = ? AND is_deleted = 0', [playerId, tableId]);
        }
        if (!player && userId) {
            player = await get('SELECT * FROM players WHERE user_id = ? AND table_id = ? AND is_deleted = 0', [userId, tableId]);
        }
        if (!player && username) {
            player = await get('SELECT * FROM players WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND table_id = ? AND is_deleted = 0', [username, tableId]);
        }

        if (!player) {
            return res.status(404).json({ error: 'Player not found in this table' });
        }

        const now = Date.now();
        if (player.status === 'EXITED') {
            await run('UPDATE players SET status = "ACTIVE", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        const buyInId = crypto.randomUUID();
        await run(
            `INSERT INTO buy_ins (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
            [buyInId, tableId, player.id, numAmount, note || null, req.user.id, now, buyInId, now]
        );

        const eventData = {
            buyInId,
            tableId,
            playerId: player.id,
            playerName: player.name,
            amount: numAmount,
            timestamp: now
        };

        emitToTable(tableId, 'buyin_recorded', eventData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'buyin_recorded', eventData);
        }

        return res.status(201).json({
            message: 'Buy-in recorded',
            buyInId,
            amount: numAmount
        });
    } catch (err) {
        console.error('Error recording buy-in:', err);
        return res.status(500).json({ error: 'Failed to record buy-in' });
    }
});

// Backward-compatible alias
router.post('/:id/buy-in-direct', authenticateToken, async (req, res, next) => {
    req.url = `/${req.params.id}/buy-ins`;
    router.handle(req, res, next);
});

/**
 * POST /api/tables/:id/exits
 * Manual exit recorded by table manager
 */
router.post('/:id/exits', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { playerId, userId, username, amount, note } = req.body;

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount < 0) {
            return res.status(400).json({ error: 'amount must be a non-negative number' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to record exit' });
        }

        let player = null;
        if (playerId) {
            player = await get('SELECT * FROM players WHERE id = ? AND table_id = ? AND is_deleted = 0', [playerId, tableId]);
        }
        if (!player && userId) {
            player = await get('SELECT * FROM players WHERE user_id = ? AND table_id = ? AND is_deleted = 0', [userId, tableId]);
        }
        if (!player && username) {
            player = await get('SELECT * FROM players WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND table_id = ? AND is_deleted = 0', [username, tableId]);
        }

        if (!player) {
            return res.status(404).json({ error: 'Player not found in this table' });
        }

        const now = Date.now();
        await run('UPDATE players SET status = "EXITED", updated_at = ? WHERE id = ?', [now, player.id]);

        const exitId = crypto.randomUUID();
        await run(
            `INSERT INTO exit_records (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
            [exitId, tableId, player.id, numAmount, note || null, req.user.id, now, exitId, now]
        );

        const eventData = {
            exitId,
            tableId,
            playerId: player.id,
            playerName: player.name,
            amount: numAmount,
            timestamp: now
        };

        emitToTable(tableId, 'exit_recorded', eventData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'exit_recorded', eventData);
        }

        return res.status(201).json({
            message: 'Exit recorded',
            exitId,
            amount: numAmount
        });
    } catch (err) {
        console.error('Error recording exit:', err);
        return res.status(500).json({ error: 'Failed to record exit' });
    }
});

// Backward-compatible alias
router.post('/:id/exit-direct', authenticateToken, async (req, res, next) => {
    req.url = `/${req.params.id}/exits`;
    router.handle(req, res, next);
});

/**
 * POST /api/tables/:id/close
 * Closes an active table
 */
router.post('/:id/close', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) {
            return res.status(403).json({ error: 'Permission denied to close table' });
        }

        const now = Date.now();
        await run(
            `UPDATE tables 
             SET status = 'CLOSED', closed_at = ?, updated_at = ? 
             WHERE id = ?`,
            [now, now, tableId]
        );

        emitToTable(tableId, 'table_closed', { tableId, closedAt: now });
        if (table.group_id) {
            emitToGroup(table.group_id, 'table_closed', { tableId, closedAt: now });
        }

        return res.status(200).json({
            message: 'Table closed',
            tableId,
            status: 'CLOSED',
            closedAt: now
        });
    } catch (error) {
        console.error('Error closing table:', error);
        return res.status(500).json({ error: 'Internal server error while closing table' });
    }
});

/**
 * GET /api/tables/:id
 * Return table details
 */
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const userId = req.user?.id;
        const table = await get(
            `SELECT t.*, 
                (SELECT COUNT(*) FROM players p WHERE p.table_id = t.id AND p.is_deleted = 0 AND p.status = 'ACTIVE') as playerCount,
                (SELECT p.entry_fee_paid FROM players p WHERE p.table_id = t.id AND p.user_id = ? AND p.is_deleted = 0 LIMIT 1) as myEntryFeePaid,
                (SELECT p.id FROM players p WHERE p.table_id = t.id AND p.user_id = ? AND p.is_deleted = 0 LIMIT 1) as myPlayerId
             FROM tables t
             WHERE (t.id = ? OR t.server_id = ?) AND t.is_deleted = 0`,
            [userId, userId, tableId, tableId]
        );

        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isClosed = table.status === 'CLOSED';
        const formattedTable = {
            id: table.id,
            groupId: table.group_id,
            name: table.name,
            code: table.code,
            chipValue: table.chip_value,
            status: table.status,
            isActive: !isClosed,
            hasEntryFee: Boolean(table.has_entry_fee),
            entryFee: table.entry_fee,
            myEntryFeePaid: table.myEntryFeePaid != null ? Boolean(table.myEntryFeePaid) : null,
            hasJoinedTable: Boolean(table.myPlayerId),
            createdAt: table.created_at,
            closedAt: table.closed_at,
            publishedAt: table.published_at,
            playerCount: Number(table.playerCount) || 0
        };

        return res.status(200).json({ table: formattedTable });
    } catch (error) {
        console.error('Error fetching table detail:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table detail' });
    }
});

/**
 * GET /api/tables/:id/status
 */
router.get('/:id/status', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isClosed = table.status === 'CLOSED';
        return res.status(200).json({
            tableId,
            status: table.status,
            isActive: !isClosed,
            closedAt: table.closed_at
        });
    } catch (error) {
        console.error('Error fetching table status:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table status' });
    }
});

/**
 * GET /api/tables/:id/players
 */
router.get('/:id/players', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const rawPlayers = await all(
            `SELECT p.id, p.table_id, p.user_id, p.name, p.status, p.created_at, p.entry_fee_paid, 
                    u.username, u.display_name, u.avatar_id,
                    COALESCE((SELECT SUM(amount) FROM buy_ins WHERE player_id = p.id AND is_deleted = 0), 0) as total_buy_ins,
                    COALESCE((SELECT SUM(amount) FROM exit_records WHERE player_id = p.id AND is_deleted = 0), 0) as total_exits
             FROM players p
             LEFT JOIN users u ON p.user_id = u.id
             WHERE p.table_id = ? AND p.is_deleted = 0
             ORDER BY p.created_at ASC`,
            [tableId]
        );

        const players = rawPlayers.map(p => {
            const totalBuyIns = Number(p.total_buy_ins) || 0;
            const totalExits = Number(p.total_exits) || 0;
            const balance = totalExits - totalBuyIns;
            return {
                id: p.id,
                tableId: p.table_id,
                userId: p.user_id,
                name: p.name,
                status: p.status,
                createdAt: p.created_at,
                entryFeePaid: Boolean(p.entry_fee_paid),
                username: p.username,
                displayName: p.display_name || p.name,
                avatarId: p.avatar_id || 'avatar_1',
                totalBuyIns,
                totalExits,
                balance
            };
        });

        return res.status(200).json({ tableId, players });
    } catch (error) {
        console.error('Error fetching table players:', error);
        return res.status(500).json({ error: 'Failed to fetch table players' });
    }
});

/**
 * GET /api/tables/:id/buy-ins
 */
router.get('/:id/buy-ins', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const rawBuyIns = await all(
            `SELECT b.id, b.table_id, b.player_id, b.amount, b.note, b.created_at, p.name as player_name
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             WHERE b.table_id = ? AND b.is_deleted = 0
             ORDER BY b.created_at ASC`,
            [tableId]
        );

        const buyIns = rawBuyIns.map(b => ({
            id: b.id,
            tableId: b.table_id,
            playerId: b.player_id,
            playerName: b.player_name,
            amount: Number(b.amount) || 0,
            note: b.note,
            createdAt: b.created_at
        }));

        return res.status(200).json({ tableId, buyIns });
    } catch (error) {
        console.error('Error fetching table buy-ins:', error);
        return res.status(500).json({ error: 'Failed to fetch table buy-ins' });
    }
});

/**
 * GET /api/tables/:id/exits
 */
router.get('/:id/exits', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const rawExits = await all(
            `SELECT e.id, e.table_id, e.player_id, e.amount, e.note, e.created_at, p.name as player_name
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             WHERE e.table_id = ? AND e.is_deleted = 0
             ORDER BY e.created_at ASC`,
            [tableId]
        );

        const exits = rawExits.map(e => ({
            id: e.id,
            tableId: e.table_id,
            playerId: e.player_id,
            playerName: e.player_name,
            amount: Number(e.amount) || 0,
            note: e.note,
            createdAt: e.created_at
        }));

        return res.status(200).json({ tableId, exits });
    } catch (error) {
        console.error('Error fetching table exits:', error);
        return res.status(500).json({ error: 'Failed to fetch table exits' });
    }
});

/**
 * POST /api/tables/:id/entry-fee-sync
 */
router.post('/:id/entry-fee-sync', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { statuses } = req.body;

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) return res.status(404).json({ error: 'Table not found' });

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) return res.status(403).json({ error: 'Permission denied' });

        if (!Array.isArray(statuses)) return res.status(400).json({ error: 'statuses array required' });

        const now = Date.now();
        let updatedCount = 0;
        for (const item of statuses) {
            const playerName = item.playerName || item.name;
            const isPaid = (item.isPaid === true || item.isPaid === 1) ? 1 : 0;
            if (playerName) {
                const resUp = await run(
                    `UPDATE players SET entry_fee_paid = ?, updated_at = ?
                     WHERE table_id = ? AND UPPER(TRIM(name)) = UPPER(TRIM(?)) AND is_deleted = 0`,
                    [isPaid, now, tableId, playerName]
                );
                updatedCount += resUp.changes || 0;
            }
        }

        return res.json({ message: 'Entry fee status synced', updatedCount });
    } catch (err) {
        console.error('Error syncing entry fee:', err);
        return res.status(500).json({ error: 'Failed to sync entry fee' });
    }
});

/**
 * Unified Requests on Table (Buy-in / Exit)
 */

// POST /api/tables/:tableId/requests
router.post('/:tableId/requests', authenticateToken, async (req, res) => {
    try {
        const { tableId } = req.params;
        const { type, amount } = req.body;
        const userId = req.user.id;

        if (!type || (type !== 'BUY_IN' && type !== 'EXIT')) {
            return res.status(400).json({ error: 'Type must be BUY_IN or EXIT' });
        }

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount <= 0) {
            return res.status(400).json({ error: 'Amount must be a positive number' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) return res.status(404).json({ error: 'Table not found' });
        if (table.status === 'CLOSED') return res.status(400).json({ error: 'Table is closed' });

        const requestId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO requests (id, table_id, user_id, type, amount, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
            [requestId, tableId, userId, type, numAmount, now, now]
        );

        const requestData = {
            id: requestId,
            tableId,
            userId,
            username: req.user.username,
            type,
            amount: numAmount,
            status: 'PENDING',
            createdAt: now
        };

        emitToTable(tableId, 'request_created', requestData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'request_created', requestData);
        }

        // Notify table creator / manager
        if (table.creator_user_id && table.creator_user_id !== userId) {
            sendNotification(table.creator_user_id, {
                type: 'request_to_me',
                title: `${type === 'BUY_IN' ? 'Buy-In' : 'Exit'} Request`,
                message: `${req.user.username} requested ${type === 'BUY_IN' ? 'buy-in' : 'exit'} of ${numAmount} at table "${table.name}".`,
                targetId: requestId,
                targetType: 'REQUEST',
                data: { tableId, requestId }
            }).catch(() => {});
        }

        return res.status(201).json({ message: 'Request created', request: requestData });
    } catch (err) {
        console.error('Error creating table request:', err);
        return res.status(500).json({ error: 'Failed to create request' });
    }
});

// GET /api/tables/:tableId/requests
router.get('/:tableId/requests', authenticateToken, async (req, res) => {
    try {
        const { tableId } = req.params;
        const rows = await all(
            `SELECT r.*, u.username, u.display_name, u.avatar_id
             FROM requests r
             JOIN users u ON r.user_id = u.id
             WHERE r.table_id = ?
             ORDER BY r.created_at DESC`,
            [tableId]
        );

        const requests = rows.map(r => ({
            id: r.id,
            tableId: r.table_id,
            userId: r.user_id,
            username: r.username,
            displayName: r.display_name || r.username,
            avatarId: r.avatar_id || 'avatar_1',
            type: r.type,
            amount: r.amount,
            status: r.status,
            createdAt: r.created_at,
            updatedAt: r.updated_at
        }));

        return res.json({ tableId, requests });
    } catch (err) {
        console.error('Error fetching table requests:', err);
        return res.status(500).json({ error: 'Failed to fetch requests' });
    }
});

// POST /api/tables/:tableId/requests/:requestId/approve
router.post('/:tableId/requests/:requestId/approve', authenticateToken, async (req, res) => {
    try {
        const { tableId, requestId } = req.params;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) return res.status(404).json({ error: 'Table not found' });

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) return res.status(403).json({ error: 'Permission denied to approve request' });

        const request = await get('SELECT * FROM requests WHERE id = ? AND table_id = ?', [requestId, tableId]);
        if (!request) return res.status(404).json({ error: 'Request not found' });
        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Request is already ${request.status}` });
        }

        const now = Date.now();

        // Match or create player on table
        let player = await get('SELECT * FROM players WHERE table_id = ? AND user_id = ? AND is_deleted = 0', [tableId, request.user_id]);
        if (!player) {
            const reqUser = await get('SELECT username, display_name FROM users WHERE id = ?', [request.user_id]);
            const pId = crypto.randomUUID();
            const pName = reqUser?.display_name || reqUser?.username || 'Player';
            await run(
                `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                [pId, tableId, request.user_id, pName, now, pId, now]
            );
            player = { id: pId, name: pName };
        }

        if (request.type === 'BUY_IN') {
            const buyInId = crypto.randomUUID();
            await run(
                `INSERT INTO buy_ins (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'Approved Buy-In Request', ?, ?, ?, ?, 1, 0)`,
                [buyInId, tableId, player.id, request.amount, req.user.id, now, buyInId, now]
            );
            emitToTable(tableId, 'buyin_recorded', {
                buyInId,
                tableId,
                playerId: player.id,
                playerName: player.name,
                amount: request.amount,
                timestamp: now
            });
        } else if (request.type === 'EXIT') {
            const exitId = crypto.randomUUID();
            await run(
                `INSERT INTO exit_records (id, table_id, player_id, amount, note, actor_user_id, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'Approved Exit Request', ?, ?, ?, ?, 1, 0)`,
                [exitId, tableId, player.id, request.amount, req.user.id, now, exitId, now]
            );
            await run('UPDATE players SET status = "EXITED", updated_at = ? WHERE id = ?', [now, player.id]);
            emitToTable(tableId, 'exit_recorded', {
                exitId,
                tableId,
                playerId: player.id,
                playerName: player.name,
                amount: request.amount,
                timestamp: now
            });
        }

        await run('UPDATE requests SET status = "APPROVED", updated_at = ? WHERE id = ?', [now, requestId]);

        const resolveData = { requestId, tableId, status: 'APPROVED', updatedAt: now };
        emitToTable(tableId, 'request_resolved', resolveData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'request_resolved', resolveData);
        }

        sendNotification(request.user_id, {
            type: 'request_to_me',
            title: 'Request Approved',
            message: `Your ${request.type === 'BUY_IN' ? 'buy-in' : 'exit'} request for ${request.amount} was approved.`,
            targetId: requestId,
            targetType: 'REQUEST'
        }).catch(() => {});

        return res.json({ message: 'Request approved successfully', requestId, status: 'APPROVED' });
    } catch (err) {
        console.error('Error approving request:', err);
        return res.status(500).json({ error: 'Failed to approve request' });
    }
});

// POST /api/tables/:tableId/requests/:requestId/reject
router.post('/:tableId/requests/:requestId/reject', authenticateToken, async (req, res) => {
    try {
        const { tableId, requestId } = req.params;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) return res.status(404).json({ error: 'Table not found' });

        const isAllowed = await canManageTable(req.user.id, req.user.role, table);
        if (!isAllowed) return res.status(403).json({ error: 'Permission denied to reject request' });

        const request = await get('SELECT * FROM requests WHERE id = ? AND table_id = ?', [requestId, tableId]);
        if (!request) return res.status(404).json({ error: 'Request not found' });
        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Request is already ${request.status}` });
        }

        const now = Date.now();
        await run('UPDATE requests SET status = "REJECTED", updated_at = ? WHERE id = ?', [now, requestId]);

        const resolveData = { requestId, tableId, status: 'REJECTED', updatedAt: now };
        emitToTable(tableId, 'request_resolved', resolveData);
        if (table.group_id) {
            emitToGroup(table.group_id, 'request_resolved', resolveData);
        }

        sendNotification(request.user_id, {
            type: 'request_to_me',
            title: 'Request Rejected',
            message: `Your ${request.type === 'BUY_IN' ? 'buy-in' : 'exit'} request for ${request.amount} was rejected.`,
            targetId: requestId,
            targetType: 'REQUEST'
        }).catch(() => {});

        return res.json({ message: 'Request rejected', requestId, status: 'REJECTED' });
    } catch (err) {
        console.error('Error rejecting request:', err);
        return res.status(500).json({ error: 'Failed to reject request' });
    }
});

module.exports = router;
