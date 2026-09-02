const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

/**
 * POST /api/tables/create
 * (Requires Auth + role='ADMIN')
 * Create a new table in a group
 */
router.post('/create', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { groupId, name, chipValue, entryFee } = req.body;

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

        const tableId = crypto.randomUUID();
        const now = Date.now();
        const numChipValue = chipValue != null && !isNaN(Number(chipValue)) ? Number(chipValue) : null;
        const numEntryFee = entryFee != null && !isNaN(Number(entryFee)) ? Number(entryFee) : null;
        const hasEntryFee = numEntryFee != null && numEntryFee > 0 ? 1 : 0;

        await run(
            `INSERT INTO tables (id, group_id, name, chip_value, status, created_at, closed_at, has_entry_fee, entry_fee, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'ACTIVE', ?, NULL, ?, ?, ?, ?, 1, 0)`,
            [tableId, groupId, name.trim(), numChipValue, now, hasEntryFee, numEntryFee, tableId, now]
        );

        return res.status(201).json({
            message: 'Table created successfully',
            tableId,
            table: {
                id: tableId,
                groupId,
                name: name.trim(),
                chip_value: numChipValue,
                has_entry_fee: hasEntryFee,
                entry_fee: numEntryFee,
                status: 'ACTIVE',
                created_at: now
            }
        });
    } catch (error) {
        console.error('Error creating table:', error);
        return res.status(500).json({ error: 'Internal server error while creating table' });
    }
});

/**
 * GET /api/tables/:id/status
 * (Requires Auth)
 * Return table status
 */
router.get('/:id/status', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const isClosed = table.status === 'CLOSED' || table.is_active === 0;
        return res.status(200).json({
            tableId,
            status: isClosed ? 'CLOSED' : 'ACTIVE',
            isActive: !isClosed,
            closedAt: table.closed_at
        });
    } catch (error) {
        console.error('Error fetching table status:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table status' });
    }
});

/**
 * POST /api/tables/:id/close
 * (Requires Auth + role='ADMIN')
 * Closes an active table
 */
router.post('/:id/close', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const now = Date.now();
        await run(
            `UPDATE tables 
             SET status = 'CLOSED', is_active = 0, closed_at = ?, updated_at = ?, is_synced = 1 
             WHERE id = ?`,
            [now, now, tableId]
        );

        console.log(`[Tables] Closed table: ${tableId} (${table.name}) at timestamp ${now}`);

        return res.status(200).json({
            message: 'Table closed',
            tableId,
            status: 'CLOSED',
            isActive: false,
            closedAt: now
        });
    } catch (error) {
        console.error('Error closing table:', error);
        return res.status(500).json({ error: 'Internal server error while closing table' });
    }
});

/**
 * GET /api/tables/:id
 * (Requires Auth)
 * Return full details of a specific table
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

        const isClosed = table.status === 'CLOSED' || table.is_active === 0;
        const formattedTable = {
            id: table.id,
            groupId: table.group_id,
            group_id: table.group_id,
            name: table.name,
            chipValue: table.chip_value,
            chip_value: table.chip_value,
            status: isClosed ? 'CLOSED' : 'ACTIVE',
            isActive: !isClosed,
            is_active: isClosed ? 0 : 1,
            hasEntryFee: Boolean(table.has_entry_fee),
            has_entry_fee: table.has_entry_fee,
            entryFee: table.entry_fee,
            entry_fee: table.entry_fee,
            myEntryFeePaid: table.myEntryFeePaid != null ? Boolean(table.myEntryFeePaid) : null,
            my_entry_fee_paid: table.myEntryFeePaid != null ? Number(table.myEntryFeePaid) : null,
            hasJoinedTable: Boolean(table.myPlayerId),
            createdAt: table.created_at,
            created_at: table.created_at,
            closedAt: table.closed_at,
            closed_at: table.closed_at,
            playerCount: Number(table.playerCount) || 0
        };

        return res.status(200).json({ table: formattedTable });
    } catch (error) {
        console.error('Error fetching table detail:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table detail' });
    }
});

/**
 * GET /api/tables/:id/players
 * (Requires Auth)
 * Returns all players currently at a table with their total buy-ins, exits, and balance
 */
router.get('/:id/players', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const rawPlayers = await all(
            `SELECT p.id, p.table_id, p.user_id, p.name, p.status, p.created_at, p.entry_fee_paid, u.username,
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
                table_id: p.table_id,
                userId: p.user_id,
                user_id: p.user_id,
                name: p.name,
                status: p.status,
                createdAt: p.created_at,
                created_at: p.created_at,
                entryFeePaid: Boolean(p.entry_fee_paid),
                entry_fee_paid: p.entry_fee_paid,
                username: p.username,
                totalBuyIns,
                total_buy_ins: totalBuyIns,
                totalExits,
                total_exits: totalExits,
                balance
            };
        });

        console.log(`[Tables] Fetched ${players.length} players for table: ${tableId}`);

        return res.status(200).json({
            tableId,
            players
        });
    } catch (error) {
        console.error('Error fetching table players:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table players' });
    }
});

/**
 * GET /api/tables/:id/buy-ins
 * (Requires Auth)
 * Returns all buy-ins recorded for a table
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
            table_id: b.table_id,
            playerId: b.player_id,
            player_id: b.player_id,
            amount: Number(b.amount) || 0,
            note: b.note,
            createdAt: b.created_at,
            created_at: b.created_at,
            timestamp: b.created_at,
            playerName: b.player_name
        }));

        console.log(`[Tables] Fetched ${buyIns.length} buy-ins for table: ${tableId}`);

        return res.status(200).json({
            tableId,
            buyIns
        });
    } catch (error) {
        console.error('Error fetching table buy-ins:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table buy-ins' });
    }
});

/**
 * GET /api/tables/:id/exits
 * (Requires Auth)
 * Returns all exit records recorded for a table
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
            table_id: e.table_id,
            playerId: e.player_id,
            player_id: e.player_id,
            amount: Number(e.amount) || 0,
            note: e.note,
            createdAt: e.created_at,
            created_at: e.created_at,
            timestamp: e.created_at,
            playerName: e.player_name
        }));

        console.log(`[Tables] Fetched ${exits.length} exit records for table: ${tableId}`);

        return res.status(200).json({
            tableId,
            exits
        });
    } catch (error) {
        console.error('Error fetching table exits:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table exits' });
    }
});

/**
 * GET /api/tables/:id/activity
 * (Requires Auth)
 * Returns all table transactions (both direct and request-based Buy-Ins and Exits)
 */
router.get('/:id/activity', authenticateToken, async (req, res) => {
    try {
        const tableId = req.params.id;

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const rawBuyIns = await all(
            `SELECT b.id, b.table_id, b.player_id, b.amount, b.note, b.created_at,
                    COALESCE(u.username, p.name) as playerName, b.created_at as timestamp
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             LEFT JOIN users u ON p.user_id = u.id
             WHERE b.table_id = ? AND b.is_deleted = 0
             ORDER BY b.created_at DESC`,
            [tableId]
        );

        const rawExits = await all(
            `SELECT e.id, e.table_id, e.player_id, e.amount, e.note, e.created_at,
                    COALESCE(u.username, p.name) as playerName, e.created_at as timestamp
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             LEFT JOIN users u ON p.user_id = u.id
             WHERE e.table_id = ? AND e.is_deleted = 0
             ORDER BY e.created_at DESC`,
            [tableId]
        );

        const buyIns = rawBuyIns.map(b => ({
            id: b.id,
            tableId: b.table_id,
            table_id: b.table_id,
            playerId: b.player_id,
            player_id: b.player_id,
            amount: Number(b.amount) || 0,
            note: b.note,
            createdAt: b.created_at,
            created_at: b.created_at,
            timestamp: b.created_at,
            playerName: b.playerName,
            type: 'buy-in'
        }));

        const exits = rawExits.map(e => ({
            id: e.id,
            tableId: e.table_id,
            table_id: e.table_id,
            playerId: e.player_id,
            player_id: e.player_id,
            amount: Number(e.amount) || 0,
            note: e.note,
            createdAt: e.created_at,
            created_at: e.created_at,
            timestamp: e.created_at,
            playerName: e.playerName,
            type: 'exit'
        }));

        console.log(`[Tables] Fetched activity for table ${tableId}: ${buyIns.length} buy-ins, ${exits.length} exits`);

        return res.status(200).json({
            tableId,
            buyIns,
            exits
        });
    } catch (error) {
        console.error('Error fetching table activity:', error);
        return res.status(500).json({ error: 'Internal server error while fetching table activity' });
    }
});

/**
 * POST /api/tables/:id/buy-in-direct
 * (Requires Auth + role='ADMIN')
 * Directly create a BuyIn record for a player in a table
 */
router.post('/:id/buy-in-direct', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { userId, playerId, username, amount, note } = req.body;

        if (amount == null) {
            return res.status(400).json({ error: 'amount is required' });
        }

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount <= 0) {
            return res.status(400).json({ error: 'amount must be a positive number' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        // Find Player record
        let player = null;
        if (playerId) {
            player = await get(
                'SELECT * FROM players WHERE id = ? AND table_id = ? AND is_deleted = 0',
                [playerId, tableId]
            );
        }

        if (!player && userId) {
            player = await get(
                'SELECT * FROM players WHERE user_id = ? AND table_id = ? AND is_deleted = 0',
                [userId, tableId]
            );
        }

        if (!player && username) {
            player = await get(
                'SELECT * FROM players WHERE name = ? AND table_id = ? AND is_deleted = 0',
                [username.trim(), tableId]
            );
        }

        if (!player) {
            return res.status(404).json({ error: 'Player not in this table' });
        }

        const now = Date.now();

        // Reactivate player if exited
        if (player.status === 'EXITED') {
            await run('UPDATE players SET status = "ACTIVE", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        const buyInId = crypto.randomUUID();
        await run(
            `INSERT INTO buy_ins (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
            [buyInId, tableId, player.id, numAmount, note || 'Direct Admin Buy-In', now, buyInId, now]
        );

        console.log(`[Direct] Recorded buy-in: ${buyInId} for player: ${player.name} (${player.id}), amount: ${numAmount}, table: ${tableId}`);

        return res.status(201).json({
            message: 'Buy-in recorded',
            buyInId,
            amount: numAmount
        });
    } catch (error) {
        console.error('Error recording direct buy-in:', error);
        return res.status(500).json({ error: 'Internal server error while recording direct buy-in' });
    }
});

/**
 * POST /api/tables/:id/exit-direct
 * (Requires Auth + role='ADMIN')
 * Directly create an Exit record for a player in a table
 */
router.post('/:id/exit-direct', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { userId, playerId, username, amount, note } = req.body;

        if (amount == null) {
            return res.status(400).json({ error: 'amount is required' });
        }

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount < 0) {
            return res.status(400).json({ error: 'amount must be a non-negative number' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        // Find Player record
        let player = null;
        if (playerId) {
            player = await get(
                'SELECT * FROM players WHERE id = ? AND table_id = ? AND is_deleted = 0',
                [playerId, tableId]
            );
        }

        if (!player && userId) {
            player = await get(
                'SELECT * FROM players WHERE user_id = ? AND table_id = ? AND is_deleted = 0',
                [userId, tableId]
            );
        }

        if (!player && username) {
            player = await get(
                'SELECT * FROM players WHERE name = ? AND table_id = ? AND is_deleted = 0',
                [username.trim(), tableId]
            );
        }

        if (!player) {
            return res.status(404).json({ error: 'Player not in this table' });
        }

        const now = Date.now();

        // Mark player as EXITED
        await run('UPDATE players SET status = "EXITED", updated_at = ? WHERE id = ?', [now, player.id]);

        const exitId = crypto.randomUUID();
        await run(
            `INSERT INTO exit_records (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)`,
            [exitId, tableId, player.id, numAmount, note || 'Direct Admin Exit', now, exitId, now]
        );

        console.log(`[Direct] Recorded exit: ${exitId} for player: ${player.name} (${player.id}), amount: ${numAmount}, table: ${tableId}`);

        return res.status(201).json({
            message: 'Exit recorded',
            exitId,
            amount: numAmount
        });
    } catch (error) {
        console.error('Error recording direct exit:', error);
        return res.status(500).json({ error: 'Internal server error while recording direct exit' });
    }
});

/**
 * POST /api/tables/:id/entry-fee-sync
 * (Requires Auth + role='ADMIN')
 * Sync player entry fee payment statuses for a table from Android Admin app
 */
router.post('/:id/entry-fee-sync', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { statuses } = req.body;

        const table = await get(
            'SELECT * FROM tables WHERE (id = ? OR server_id = ?) AND is_deleted = 0',
            [tableId, tableId]
        );

        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        if (!Array.isArray(statuses)) {
            return res.status(400).json({ error: 'statuses array is required' });
        }

        const now = Date.now();
        let updatedCount = 0;

        for (const item of statuses) {
            const playerName = item.playerName || item.player_name || item.name;
            const isPaid = (item.isPaid === true || item.isPaid === 1 || item.paid === true || item.paid === 1 || item.is_paid === true || item.is_paid === 1) ? 1 : 0;

            if (playerName) {
                const result = await run(
                    `UPDATE players
                     SET entry_fee_paid = ?, updated_at = ?, is_synced = 1
                     WHERE (table_id = ? OR table_id = ?)
                       AND UPPER(TRIM(name)) = UPPER(TRIM(?))
                       AND is_deleted = 0`,
                    [isPaid, now, table.id, table.server_id || table.id, playerName]
                );
                if (result.changes > 0) {
                    updatedCount += result.changes;
                }
            }
        }

        console.log(`[EntryFeeSync] Synced entry fee statuses for table ${table.id} (${table.name}): ${updatedCount} player records updated`);

        return res.status(200).json({
            message: 'Entry fee status synced',
            tableId: table.id,
            updatedCount
        });
    } catch (error) {
        console.error('Error syncing entry fee status:', error);
        return res.status(500).json({ error: 'Internal server error while syncing entry fee status' });
    }
});

module.exports = router;
