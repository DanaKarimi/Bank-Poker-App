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
 * Returns all active tables for a group
 */
router.get('/', authenticateToken, async (req, res) => {
    try {
        const { groupId } = req.query;
        let query = 'SELECT * FROM tables WHERE is_deleted = 0';
        const params = [];

        if (groupId) {
            query += ' AND group_id = ?';
            params.push(groupId);
        }

        query += ' ORDER BY created_at DESC';
        const tables = await all(query, params);
        return res.status(200).json({ tables });
    } catch (error) {
        console.error('Error fetching tables:', error);
        return res.status(500).json({ error: 'Internal server error while fetching tables' });
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
