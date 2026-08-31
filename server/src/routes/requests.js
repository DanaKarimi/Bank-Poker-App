const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

/**
 * POST /api/requests/join
 * (Requires Auth)
 * Player requests to join an online group
 */
router.post('/join', authenticateToken, async (req, res) => {
    try {
        const { groupId } = req.body;
        const userId = req.user.id;

        if (!groupId) {
            return res.status(400).json({ error: 'groupId is required' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // Check if already a member
        const existingMember = await get(
            'SELECT * FROM group_members WHERE user_id = ? AND group_id = ?',
            [userId, groupId]
        );
        if (existingMember) {
            return res.status(400).json({ error: 'Already a member of this group' });
        }

        // Check if a pending join request exists
        const pendingRequest = await get(
            'SELECT * FROM join_requests WHERE user_id = ? AND group_id = ? AND status = "PENDING"',
            [userId, groupId]
        );
        if (pendingRequest) {
            return res.status(400).json({
                error: 'A pending join request already exists for this group',
                requestId: pendingRequest.id
            });
        }

        const requestId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO join_requests (id, group_id, user_id, status, created_at, updated_at)
             VALUES (?, ?, ?, 'PENDING', ?, ?)`,
            [requestId, groupId, userId, now, now]
        );

        return res.status(201).json({
            message: 'Join request submitted successfully',
            requestId,
            status: 'PENDING'
        });
    } catch (error) {
        console.error('Error submitting join request:', error);
        return res.status(500).json({ error: 'Internal server error while submitting join request' });
    }
});

/**
 * POST /api/requests/join/:id/approve
 * (Requires Auth + role='ADMIN')
 * Admin approves a join request and adds player to group_members
 */
router.post('/join/:id/approve', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM join_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Join request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Join request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE join_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Add player to group_members
        await run(
            'INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at) VALUES (?, ?, ?)',
            [request.user_id, request.group_id, now]
        );

        return res.status(200).json({ message: 'Join request approved successfully' });
    } catch (error) {
        console.error('Error approving join request:', error);
        return res.status(500).json({ error: 'Internal server error while approving join request' });
    }
});

/**
 * POST /api/requests/join/:id/reject
 * (Requires Auth + role='ADMIN')
 * Admin rejects a join request
 */
router.post('/join/:id/reject', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM join_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Join request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Join request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE join_requests SET status = "REJECTED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        return res.status(200).json({ message: 'Join request rejected successfully' });
    } catch (error) {
        console.error('Error rejecting join request:', error);
        return res.status(500).json({ error: 'Internal server error while rejecting join request' });
    }
});

/**
 * POST /api/requests/buy-in
 * (Requires Auth)
 * Player submits a buy-in request for a table
 */
router.post('/buy-in', authenticateToken, async (req, res) => {
    try {
        const { groupId, tableId, amount } = req.body;
        const userId = req.user.id;

        if (!groupId || !tableId || amount == null) {
            return res.status(400).json({ error: 'groupId, tableId, and amount are required' });
        }

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount <= 0) {
            return res.status(400).json({ error: 'amount must be a positive number' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const requestId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO buy_in_requests (id, group_id, table_id, user_id, amount, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
            [requestId, groupId, tableId, userId, numAmount, now, now]
        );

        return res.status(201).json({
            message: 'Buy-in request submitted successfully',
            requestId,
            status: 'PENDING'
        });
    } catch (error) {
        console.error('Error submitting buy-in request:', error);
        return res.status(500).json({ error: 'Internal server error while submitting buy-in request' });
    }
});

/**
 * POST /api/requests/buy-in/:id/approve
 * (Requires Auth + role='ADMIN')
 * Admin approves a buy-in request
 */
router.post('/buy-in/:id/approve', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM buy_in_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Buy-in request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Buy-in request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE buy_in_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        return res.status(200).json({ message: 'Buy-in request approved successfully' });
    } catch (error) {
        console.error('Error approving buy-in request:', error);
        return res.status(500).json({ error: 'Internal server error while approving buy-in request' });
    }
});

/**
 * POST /api/requests/buy-in/:id/reject
 * (Requires Auth + role='ADMIN')
 * Admin rejects a buy-in request
 */
router.post('/buy-in/:id/reject', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM buy_in_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Buy-in request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Buy-in request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE buy_in_requests SET status = "REJECTED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        return res.status(200).json({ message: 'Buy-in request rejected successfully' });
    } catch (error) {
        console.error('Error rejecting buy-in request:', error);
        return res.status(500).json({ error: 'Internal server error while rejecting buy-in request' });
    }
});

/**
 * POST /api/requests/buy-in/:id/confirm
 * (Requires Auth)
 * Player confirms receipt of chips after Admin approved; creates actual BuyIn record
 */
router.post('/buy-in/:id/confirm', authenticateToken, async (req, res) => {
    try {
        const requestId = req.params.id;
        const userId = req.user.id;

        const request = await get('SELECT * FROM buy_in_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Buy-in request not found' });
        }

        if (request.user_id !== userId) {
            return res.status(403).json({ error: 'Forbidden: You can only confirm your own requests' });
        }

        if (request.status !== 'APPROVED') {
            return res.status(400).json({
                error: `Cannot confirm buy-in request with status '${request.status}'. Must be 'APPROVED'.`
            });
        }

        const now = Date.now();
        await run(
            'UPDATE buy_in_requests SET status = "CONFIRMED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Find or create Player record in table
        const username = req.user.username;
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND name = ? AND is_deleted = 0',
            [request.table_id, username]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                [playerId, request.table_id, username, now, playerId, now]
            );
            player = { id: playerId };
        } else if (player.status === 'EXITED') {
            await run('UPDATE players SET status = "ACTIVE", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        // Insert actual BuyIn record
        const buyInId = crypto.randomUUID();
        await run(
            `INSERT INTO buy_ins (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'Online Buy-In Request', ?, ?, ?, 1, 0)`,
            [buyInId, request.table_id, player.id, request.amount, now, buyInId, now]
        );

        return res.status(200).json({
            message: 'Buy-in confirmed and recorded successfully',
            buyInId
        });
    } catch (error) {
        console.error('Error confirming buy-in request:', error);
        return res.status(500).json({ error: 'Internal server error while confirming buy-in request' });
    }
});

/**
 * POST /api/requests/exit
 * (Requires Auth)
 * Player submits an exit/cashout request for a table
 */
router.post('/exit', authenticateToken, async (req, res) => {
    try {
        const { groupId, tableId, amount } = req.body;
        const userId = req.user.id;

        if (!groupId || !tableId || amount == null) {
            return res.status(400).json({ error: 'groupId, tableId, and amount are required' });
        }

        const numAmount = Number(amount);
        if (isNaN(numAmount) || numAmount < 0) {
            return res.status(400).json({ error: 'amount must be a non-negative number' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        const table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        const requestId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO exit_requests (id, group_id, table_id, user_id, amount, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
            [requestId, groupId, tableId, userId, numAmount, now, now]
        );

        return res.status(201).json({
            message: 'Exit request submitted successfully',
            requestId,
            status: 'PENDING'
        });
    } catch (error) {
        console.error('Error submitting exit request:', error);
        return res.status(500).json({ error: 'Internal server error while submitting exit request' });
    }
});

/**
 * POST /api/requests/exit/:id/approve
 * (Requires Auth + role='ADMIN')
 * Admin approves an exit request
 */
router.post('/exit/:id/approve', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM exit_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Exit request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Exit request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE exit_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        return res.status(200).json({ message: 'Exit request approved successfully' });
    } catch (error) {
        console.error('Error approving exit request:', error);
        return res.status(500).json({ error: 'Internal server error while approving exit request' });
    }
});

/**
 * POST /api/requests/exit/:id/reject
 * (Requires Auth + role='ADMIN')
 * Admin rejects an exit request
 */
router.post('/exit/:id/reject', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const requestId = req.params.id;

        const request = await get('SELECT * FROM exit_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Exit request not found' });
        }

        if (request.status !== 'PENDING') {
            return res.status(400).json({ error: `Exit request is already ${request.status}` });
        }

        const now = Date.now();
        await run(
            'UPDATE exit_requests SET status = "REJECTED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        return res.status(200).json({ message: 'Exit request rejected successfully' });
    } catch (error) {
        console.error('Error rejecting exit request:', error);
        return res.status(500).json({ error: 'Internal server error while rejecting exit request' });
    }
});

/**
 * POST /api/requests/exit/:id/confirm
 * (Requires Auth)
 * Player confirms payout receipt; creates actual Exit record
 */
router.post('/exit/:id/confirm', authenticateToken, async (req, res) => {
    try {
        const requestId = req.params.id;
        const userId = req.user.id;

        const request = await get('SELECT * FROM exit_requests WHERE id = ?', [requestId]);
        if (!request) {
            return res.status(404).json({ error: 'Exit request not found' });
        }

        if (request.user_id !== userId) {
            return res.status(403).json({ error: 'Forbidden: You can only confirm your own requests' });
        }

        if (request.status !== 'APPROVED') {
            return res.status(400).json({
                error: `Cannot confirm exit request with status '${request.status}'. Must be 'APPROVED'.`
            });
        }

        const now = Date.now();
        await run(
            'UPDATE exit_requests SET status = "CONFIRMED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Find or create Player record in table
        const username = req.user.username;
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND name = ? AND is_deleted = 0',
            [request.table_id, username]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, 'EXITED', ?, ?, ?, 1, 0)`,
                [playerId, request.table_id, username, now, playerId, now]
            );
            player = { id: playerId };
        } else {
            await run('UPDATE players SET status = "EXITED", updated_at = ? WHERE id = ?', [now, player.id]);
        }

        // Insert actual Exit record
        const exitId = crypto.randomUUID();
        await run(
            `INSERT INTO exit_records (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'Online Exit Request', ?, ?, ?, 1, 0)`,
            [exitId, request.table_id, player.id, request.amount, now, exitId, now]
        );

        return res.status(200).json({
            message: 'Exit confirmed and recorded successfully',
            exitId
        });
    } catch (error) {
        console.error('Error confirming exit request:', error);
        return res.status(500).json({ error: 'Internal server error while confirming exit request' });
    }
});

/**
 * GET /api/requests/pending
 * (Requires Auth + role='ADMIN')
 * Return all pending requests (Join, BuyIn, Exit) for a group or all admin groups
 */
router.get('/pending', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { groupId } = req.query;

        let joinRequests;
        let buyInRequests;
        let exitRequests;

        if (groupId) {
            joinRequests = await all(
                `SELECT jr.*, u.username, g.name as group_name
                 FROM join_requests jr
                 JOIN users u ON jr.user_id = u.id
                 JOIN groups g ON jr.group_id = g.id
                 WHERE jr.group_id = ? AND jr.status = 'PENDING'
                 ORDER BY jr.created_at DESC`,
                [groupId]
            );

            buyInRequests = await all(
                `SELECT br.*, u.username, g.name as group_name, t.name as table_name
                 FROM buy_in_requests br
                 JOIN users u ON br.user_id = u.id
                 JOIN groups g ON br.group_id = g.id
                 JOIN tables t ON br.table_id = t.id
                 WHERE br.group_id = ? AND br.status = 'PENDING'
                 ORDER BY br.created_at DESC`,
                [groupId]
            );

            exitRequests = await all(
                `SELECT er.*, u.username, g.name as group_name, t.name as table_name
                 FROM exit_requests er
                 JOIN users u ON er.user_id = u.id
                 JOIN groups g ON er.group_id = g.id
                 JOIN tables t ON er.table_id = t.id
                 WHERE er.group_id = ? AND er.status = 'PENDING'
                 ORDER BY er.created_at DESC`,
                [groupId]
            );
        } else {
            joinRequests = await all(
                `SELECT jr.*, u.username, g.name as group_name
                 FROM join_requests jr
                 JOIN users u ON jr.user_id = u.id
                 JOIN groups g ON jr.group_id = g.id
                 WHERE jr.status = 'PENDING'
                 ORDER BY jr.created_at DESC`
            );

            buyInRequests = await all(
                `SELECT br.*, u.username, g.name as group_name, t.name as table_name
                 FROM buy_in_requests br
                 JOIN users u ON br.user_id = u.id
                 JOIN groups g ON br.group_id = g.id
                 JOIN tables t ON br.table_id = t.id
                 WHERE br.status = 'PENDING'
                 ORDER BY br.created_at DESC`
            );

            exitRequests = await all(
                `SELECT er.*, u.username, g.name as group_name, t.name as table_name
                 FROM exit_requests er
                 JOIN users u ON er.user_id = u.id
                 JOIN groups g ON er.group_id = g.id
                 JOIN tables t ON er.table_id = t.id
                 WHERE er.status = 'PENDING'
                 ORDER BY er.created_at DESC`
            );
        }

        return res.status(200).json({
            joinRequests,
            buyInRequests,
            exitRequests
        });
    } catch (error) {
        console.error('Error fetching pending requests:', error);
        return res.status(500).json({ error: 'Internal server error while fetching pending requests' });
    }
});

/**
 * GET /api/requests/my
 * (Requires Auth)
 * Return all requests by the authenticated user in a group or across all groups
 */
router.get('/my', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const { groupId } = req.query;

        let joinRequests;
        let buyInRequests;
        let exitRequests;

        if (groupId) {
            joinRequests = await all(
                `SELECT jr.*, g.name as group_name
                 FROM join_requests jr
                 JOIN groups g ON jr.group_id = g.id
                 WHERE jr.user_id = ? AND jr.group_id = ?
                 ORDER BY jr.created_at DESC`,
                [userId, groupId]
            );

            buyInRequests = await all(
                `SELECT br.*, g.name as group_name, t.name as table_name
                 FROM buy_in_requests br
                 JOIN groups g ON br.group_id = g.id
                 JOIN tables t ON br.table_id = t.id
                 WHERE br.user_id = ? AND br.group_id = ?
                 ORDER BY br.created_at DESC`,
                [userId, groupId]
            );

            exitRequests = await all(
                `SELECT er.*, g.name as group_name, t.name as table_name
                 FROM exit_requests er
                 JOIN groups g ON er.group_id = g.id
                 JOIN tables t ON er.table_id = t.id
                 WHERE er.user_id = ? AND er.group_id = ?
                 ORDER BY er.created_at DESC`,
                [userId, groupId]
            );
        } else {
            joinRequests = await all(
                `SELECT jr.*, g.name as group_name
                 FROM join_requests jr
                 JOIN groups g ON jr.group_id = g.id
                 WHERE jr.user_id = ?
                 ORDER BY jr.created_at DESC`,
                [userId]
            );

            buyInRequests = await all(
                `SELECT br.*, g.name as group_name, t.name as table_name
                 FROM buy_in_requests br
                 JOIN groups g ON br.group_id = g.id
                 JOIN tables t ON br.table_id = t.id
                 WHERE br.user_id = ?
                 ORDER BY br.created_at DESC`,
                [userId]
            );

            exitRequests = await all(
                `SELECT er.*, g.name as group_name, t.name as table_name
                 FROM exit_requests er
                 JOIN groups g ON er.group_id = g.id
                 JOIN tables t ON er.table_id = t.id
                 WHERE er.user_id = ?
                 ORDER BY er.created_at DESC`,
                [userId]
            );
        }

        return res.status(200).json({
            joinRequests,
            buyInRequests,
            exitRequests
        });
    } catch (error) {
        console.error('Error fetching user requests:', error);
        return res.status(500).json({ error: 'Internal server error while fetching requests' });
    }
});

/**
 * POST /api/requests/tables/:id/buy-in-direct
 * (Requires Auth + role='ADMIN')
 * Directly create a BuyIn record for a user (bypassing the request system)
 */
router.post('/tables/:id/buy-in-direct', authenticateToken, requireAdmin, async (req, res) => {
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
 * POST /api/requests/tables/:id/exit-direct
 * (Requires Auth + role='ADMIN')
 * Directly create an Exit record for a user (bypassing the request system)
 */
router.post('/tables/:id/exit-direct', authenticateToken, requireAdmin, async (req, res) => {
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
