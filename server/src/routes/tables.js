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
 * GET /api/tables
 * Query param: groupId
 * Returns all active tables for a group including playerCount
 */
router.get('/', authenticateToken, async (req, res) => {
    try {
        const { groupId } = req.query;
        let query = `
            SELECT t.*, (
                SELECT COUNT(*) FROM players p WHERE p.table_id = t.id AND p.is_deleted = 0 AND p.status = 'ACTIVE'
            ) as playerCount
            FROM tables t
            WHERE t.is_deleted = 0
        `;
        const params = [];

        if (groupId) {
            query += ' AND t.group_id = ?';
            params.push(groupId);
        }

        query += ' ORDER BY t.created_at DESC';
        const tables = await all(query, params);
        return res.status(200).json({ tables });
    } catch (error) {
        console.error('Error fetching tables:', error);
        return res.status(500).json({ error: 'Internal server error while fetching tables' });
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
 * POST /api/tables/:id/buy-in-direct
 * (Requires Auth + role='ADMIN')
 * Directly create a BuyIn record for a user
 */
router.post('/:id/buy-in-direct', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { userId, username, amount } = req.body;

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

        let targetUsername = username;
        if (userId) {
            const user = await get('SELECT * FROM users WHERE id = ?', [userId]);
            if (!user) {
                return res.status(404).json({ error: 'User not found' });
            }
            targetUsername = user.username;
        }

        if (!targetUsername || !targetUsername.trim()) {
            return res.status(400).json({ error: 'userId or username is required' });
        }

        const now = Date.now();
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND name = ? AND is_deleted = 0',
            [tableId, targetUsername.trim()]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                [playerId, tableId, targetUsername.trim(), now, playerId, now]
            );
            player = { id: playerId };
        } else if (player.status === 'EXITED') {
            await run('UPDATE players SET status = "ACTIVE", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        const buyInId = crypto.randomUUID();
        await run(
            `INSERT INTO buy_ins (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'Direct Admin Buy-In', ?, ?, ?, 1, 0)`,
            [buyInId, tableId, player.id, numAmount, now, buyInId, now]
        );

        return res.status(201).json({
            message: 'Buy-in recorded directly',
            buyInId
        });
    } catch (error) {
        console.error('Error recording direct buy-in:', error);
        return res.status(500).json({ error: 'Internal server error while recording direct buy-in' });
    }
});

/**
 * POST /api/tables/:id/exit-direct
 * (Requires Auth + role='ADMIN')
 * Directly create an Exit record for a user
 */
router.post('/:id/exit-direct', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const tableId = req.params.id;
        const { userId, username, amount } = req.body;

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

        let targetUsername = username;
        if (userId) {
            const user = await get('SELECT * FROM users WHERE id = ?', [userId]);
            if (!user) {
                return res.status(404).json({ error: 'User not found' });
            }
            targetUsername = user.username;
        }

        if (!targetUsername || !targetUsername.trim()) {
            return res.status(400).json({ error: 'userId or username is required' });
        }

        const now = Date.now();
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND name = ? AND is_deleted = 0',
            [tableId, targetUsername.trim()]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, 'EXITED', ?, ?, ?, 1, 0)`,
                [playerId, tableId, targetUsername.trim(), now, playerId, now]
            );
            player = { id: playerId };
        } else {
            await run('UPDATE players SET status = "EXITED", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        const exitId = crypto.randomUUID();
        await run(
            `INSERT INTO exit_records (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'Direct Admin Exit', ?, ?, ?, 1, 0)`,
            [exitId, tableId, player.id, numAmount, now, exitId, now]
        );

        return res.status(201).json({
            message: 'Exit recorded directly',
            exitId
        });
    } catch (error) {
        console.error('Error recording direct exit:', error);
        return res.status(500).json({ error: 'Internal server error while recording direct exit' });
    }
});

module.exports = router;
