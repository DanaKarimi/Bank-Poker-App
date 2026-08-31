const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

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
