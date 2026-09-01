const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

/**
 * POST /api/requests/join
 * (Requires Auth)
 * Player requests to join a specific table in an online group
 */
router.post('/join', authenticateToken, async (req, res) => {
    try {
        const { tableId, groupId: rawGroupId } = req.body;
        const userId = req.user.id;
        const username = req.user.username;

        let table = null;
        let groupId = rawGroupId;

        if (tableId) {
            table = await get('SELECT * FROM tables WHERE id = ? AND is_deleted = 0', [tableId]);
            if (!table) {
                return res.status(404).json({ error: 'Table not found' });
            }
            if (table.status === 'CLOSED' || table.is_active === 0) {
                return res.status(400).json({ error: 'Table is closed' });
            }
            groupId = table.group_id;
        }

        if (!groupId) {
            return res.status(400).json({ error: 'tableId or groupId is required' });
        }

        const group = await get('SELECT * FROM groups WHERE id = ? AND is_deleted = 0', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // If tableId is specified, check if already a player in this table
        if (tableId) {
            const existingPlayer = await get(
                `SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0 AND status = 'ACTIVE'`,
                [tableId, userId, username]
            );
            if (existingPlayer) {
                return res.status(400).json({ error: 'You are already a player at this table' });
            }

            // Check if pending request exists for this table
            const pendingRequest = await get(
                `SELECT * FROM join_requests WHERE user_id = ? AND table_id = ? AND status = 'PENDING'`,
                [userId, tableId]
            );
            if (pendingRequest) {
                return res.status(400).json({
                    error: 'A pending join request already exists for this table',
                    requestId: pendingRequest.id
                });
            }
        } else {
            // Check if pending request exists for group
            const pendingRequest = await get(
                `SELECT * FROM join_requests WHERE user_id = ? AND group_id = ? AND status = 'PENDING'`,
                [userId, groupId]
            );
            if (pendingRequest) {
                return res.status(400).json({
                    error: 'A pending join request already exists for this group',
                    requestId: pendingRequest.id
                });
            }
        }

        const requestId = crypto.randomUUID();
        const now = Date.now();

        await run(
            `INSERT INTO join_requests (id, group_id, table_id, user_id, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, 'PENDING', ?, ?)`,
            [requestId, groupId, tableId || null, userId, now, now]
        );

        // Ensure user is in group_members
        await run(
            'INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at) VALUES (?, ?, ?)',
            [userId, groupId, now]
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
 * Admin approves a join request and creates a Player record in the database
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

        const targetUser = await get('SELECT * FROM users WHERE id = ?', [request.user_id]);
        if (!targetUser) {
            return res.status(404).json({ error: 'Requesting user not found' });
        }

        const now = Date.now();

        console.log(`[Requests] Approving join request: ${requestId} for user: ${request.user_id}`);

        // Update join request status
        await run(
            'UPDATE join_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Add user to group_members
        await run(
            'INSERT OR IGNORE INTO group_members (user_id, group_id, joined_at) VALUES (?, ?, ?)',
            [request.user_id, request.group_id, now]
        );

        let playerId = null;

        // If join request was for a table, CREATE Player record in database
        if (request.table_id) {
            console.log(`[Requests] Creating player record for table: ${request.table_id}, user: ${request.user_id}, name: ${targetUser.username}`);

            let player = await get(
                'SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0',
                [request.table_id, request.user_id, targetUser.username]
            );

            if (!player) {
                playerId = crypto.randomUUID();
                await run(
                    `INSERT INTO players (id, table_id, user_id, name, status, created_at, entry_fee_paid, server_id, updated_at, is_synced, is_deleted)
                     VALUES (?, ?, ?, ?, 'ACTIVE', ?, 0, ?, ?, 1, 0)`,
                    [playerId, request.table_id, request.user_id, targetUser.username, now, playerId, now]
                );
                console.log(`[Requests] Inserted new player ${playerId} into table ${request.table_id}`);
            } else {
                playerId = player.id;
                await run(
                    'UPDATE players SET status = "ACTIVE", user_id = ?, updated_at = ? WHERE id = ?',
                    [request.user_id, now, player.id]
                );
                console.log(`[Requests] Activated existing player ${playerId} in table ${request.table_id}`);
            }
        }

        return res.status(200).json({
            message: 'Player added to table',
            playerId,
            requestId
        });
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
        const username = req.user.username;

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
        if (table.status === 'CLOSED' || table.is_active === 0) {
            return res.status(400).json({ error: 'Table is closed' });
        }

        // Verify player has joined table (or has active player record)
        const player = await get(
            `SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0 AND status = 'ACTIVE'`,
            [tableId, userId, username]
        );

        if (!player) {
            return res.status(400).json({
                error: 'You must join this table before requesting a buy-in.'
            });
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
 * Admin approves a buy-in request and immediately creates a BuyIn record
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

        const targetUser = await get('SELECT * FROM users WHERE id = ?', [request.user_id]);
        if (!targetUser) {
            return res.status(404).json({ error: 'Requesting user not found' });
        }

        const now = Date.now();
        await run(
            'UPDATE buy_in_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Find or create Player record in table
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0',
            [request.table_id, request.user_id, targetUser.username]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 1, 0)`,
                [playerId, request.table_id, request.user_id, targetUser.username, now, playerId, now]
            );
            player = { id: playerId };
            console.log(`[Requests] Created fallback player record: ${playerId}`);
        } else if (player.status === 'EXITED') {
            await run('UPDATE players SET status = "ACTIVE", updated_at = ? WHERE id = ?', [now, player.id]);
            console.log(`[Requests] Reactivated player: ${player.id}`);
        }

        console.log(`[Requests] Creating BuyIn record for player: ${player.id}, table: ${request.table_id}, amount: ${request.amount}`);

        // Insert actual BuyIn record
        const buyInId = crypto.randomUUID();
        await run(
            `INSERT INTO buy_ins (id, table_id, player_id, amount, note, created_at, server_id, updated_at, is_synced, is_deleted)
             VALUES (?, ?, ?, ?, 'Online Buy-In Request', ?, ?, ?, 1, 0)`,
            [buyInId, request.table_id, player.id, request.amount, now, buyInId, now]
        );

        return res.status(200).json({ message: 'Buy-in request approved and recorded successfully', buyInId });
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
 * POST /api/requests/exit
 * (Requires Auth)
 * Player submits an exit/cashout request for a table
 */
router.post('/exit', authenticateToken, async (req, res) => {
    try {
        const { groupId, tableId, amount } = req.body;
        const userId = req.user.id;
        const username = req.user.username;

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
        if (table.status === 'CLOSED' || table.is_active === 0) {
            return res.status(400).json({ error: 'Table is closed' });
        }

        // Verify player exists in table
        const player = await get(
            `SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0 AND status = 'ACTIVE'`,
            [tableId, userId, username]
        );

        if (!player) {
            return res.status(400).json({
                error: 'You must be an active player at this table to request an exit.'
            });
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
 * Admin approves an exit request and immediately creates an Exit record
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

        const targetUser = await get('SELECT * FROM users WHERE id = ?', [request.user_id]);
        if (!targetUser) {
            return res.status(404).json({ error: 'Requesting user not found' });
        }

        const now = Date.now();
        await run(
            'UPDATE exit_requests SET status = "APPROVED", updated_at = ? WHERE id = ?',
            [now, requestId]
        );

        // Find or create Player record in table
        let player = await get(
            'SELECT * FROM players WHERE table_id = ? AND (user_id = ? OR name = ?) AND is_deleted = 0',
            [request.table_id, request.user_id, targetUser.username]
        );

        if (!player) {
            const playerId = crypto.randomUUID();
            await run(
                `INSERT INTO players (id, table_id, user_id, name, status, created_at, server_id, updated_at, is_synced, is_deleted)
                 VALUES (?, ?, ?, ?, 'EXITED', ?, ?, ?, 1, 0)`,
                [playerId, request.table_id, request.user_id, targetUser.username, now, playerId, now]
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

        return res.status(200).json({ message: 'Exit request approved and recorded successfully', exitId });
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
 * GET /api/requests/pending
 * (Requires Auth + role='ADMIN')
 * Return all pending requests (Join, BuyIn, Exit) for a group or table
 */
router.get('/pending', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { groupId, tableId } = req.query;

        let joinQuery = `
            SELECT jr.*, u.username, g.name as group_name, t.name as table_name
            FROM join_requests jr
            JOIN users u ON jr.user_id = u.id
            JOIN groups g ON jr.group_id = g.id
            LEFT JOIN tables t ON jr.table_id = t.id
            WHERE jr.status = 'PENDING'
        `;
        let buyInQuery = `
            SELECT br.*, u.username, g.name as group_name, t.name as table_name
            FROM buy_in_requests br
            JOIN users u ON br.user_id = u.id
            JOIN groups g ON br.group_id = g.id
            JOIN tables t ON br.table_id = t.id
            WHERE br.status = 'PENDING'
        `;
        let exitQuery = `
            SELECT er.*, u.username, g.name as group_name, t.name as table_name
            FROM exit_requests er
            JOIN users u ON er.user_id = u.id
            JOIN groups g ON er.group_id = g.id
            JOIN tables t ON er.table_id = t.id
            WHERE er.status = 'PENDING'
        `;

        const params = [];

        if (tableId) {
            joinQuery += ' AND jr.table_id = ?';
            buyInQuery += ' AND br.table_id = ?';
            exitQuery += ' AND er.table_id = ?';
            params.push(tableId);
        } else if (groupId) {
            joinQuery += ' AND jr.group_id = ?';
            buyInQuery += ' AND br.group_id = ?';
            exitQuery += ' AND er.group_id = ?';
            params.push(groupId);
        }

        joinQuery += ' ORDER BY jr.created_at DESC';
        buyInQuery += ' ORDER BY br.created_at DESC';
        exitQuery += ' ORDER BY er.created_at DESC';

        const [joinRequests, buyInRequests, exitRequests] = await Promise.all([
            all(joinQuery, params),
            all(buyInQuery, params),
            all(exitQuery, params)
        ]);

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
 * Return all requests by the authenticated user
 */
router.get('/my', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const { groupId, tableId } = req.query;

        let joinQuery = `
            SELECT jr.*, g.name as group_name, t.name as table_name
            FROM join_requests jr
            JOIN groups g ON jr.group_id = g.id
            LEFT JOIN tables t ON jr.table_id = t.id
            WHERE jr.user_id = ?
        `;
        let buyInQuery = `
            SELECT br.*, g.name as group_name, t.name as table_name
            FROM buy_in_requests br
            JOIN groups g ON br.group_id = g.id
            JOIN tables t ON br.table_id = t.id
            WHERE br.user_id = ?
        `;
        let exitQuery = `
            SELECT er.*, g.name as group_name, t.name as table_name
            FROM exit_requests er
            JOIN groups g ON er.group_id = g.id
            JOIN tables t ON er.table_id = t.id
            WHERE er.user_id = ?
        `;

        const params = [userId];

        if (tableId) {
            joinQuery += ' AND jr.table_id = ?';
            buyInQuery += ' AND br.table_id = ?';
            exitQuery += ' AND er.table_id = ?';
            params.push(tableId);
        } else if (groupId) {
            joinQuery += ' AND jr.group_id = ?';
            buyInQuery += ' AND br.group_id = ?';
            exitQuery += ' AND er.group_id = ?';
            params.push(groupId);
        }

        joinQuery += ' ORDER BY jr.created_at DESC';
        buyInQuery += ' ORDER BY br.created_at DESC';
        exitQuery += ' ORDER BY er.created_at DESC';

        const [joinRequests, buyInRequests, exitRequests] = await Promise.all([
            all(joinQuery, params),
            all(buyInQuery, params),
            all(exitQuery, params)
        ]);

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

module.exports = router;
