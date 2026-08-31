const express = require('express');
const router = express.Router();
const { run, get, all } = require('../database/db');
const { authenticateToken } = require('../middleware/auth');

/**
 * POST /api/groups/join
 * Join a group using its unique invite_code
 */
router.post('/join', authenticateToken, async (req, res) => {
    try {
        const { invite_code } = req.body;

        if (!invite_code) {
            return res.status(400).json({ error: 'invite_code is required' });
        }

        const group = await get(
            'SELECT * FROM groups WHERE invite_code = ? AND is_deleted = 0',
            [invite_code.trim()]
        );

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
 * List all groups that the authenticated user is a member of
 */
router.get('/my-groups', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;

        const groups = await all(
            `SELECT g.id, g.name, g.invite_code, g.server_id, g.updated_at, gm.joined_at
             FROM groups g
             INNER JOIN group_members gm ON g.id = gm.group_id
             WHERE gm.user_id = ? AND g.is_deleted = 0
             ORDER BY g.name ASC`,
            [userId]
        );

        return res.status(200).json({ groups });
    } catch (error) {
        console.error('Error fetching user groups:', error);
        return res.status(500).json({ error: 'Internal server error while fetching groups' });
    }
});

/**
 * GET /api/groups/:id/my-stats
 * Calculate and return personal stats for the authenticated user in the specified group
 */
router.get('/:id/my-stats', authenticateToken, async (req, res) => {
    try {
        const groupId = req.params.id;
        const username = req.user.username;

        const group = await get(
            'SELECT * FROM groups WHERE id = ? AND is_deleted = 0',
            [groupId]
        );

        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // 1. Tables count
        const tablesResult = await get(
            `SELECT COUNT(DISTINCT t.id) as tables_count
             FROM tables t
             JOIN players p ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND t.is_deleted = 0 AND p.is_deleted = 0`,
            [groupId, username]
        );
        const tablesPlayed = tablesResult ? tablesResult.tables_count : 0;

        // 2. Total Buy-ins
        const buyInsResult = await get(
            `SELECT COALESCE(SUM(b.amount), 0) as total_buy_ins
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, username]
        );
        const totalBuyIns = buyInsResult ? Number(buyInsResult.total_buy_ins) : 0;

        // 3. Total Exits
        const exitsResult = await get(
            `SELECT COALESCE(SUM(e.amount), 0) as total_exits
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0`,
            [groupId, username]
        );
        const totalExits = exitsResult ? Number(exitsResult.total_exits) : 0;

        // 4. Payments sent (from_player = username)
        const paymentsSentResult = await get(
            `SELECT COALESCE(SUM(amount), 0) as payments_sent
             FROM payments
             WHERE group_id = ? AND from_player = ? AND is_deleted = 0`,
            [groupId, username]
        );
        const paymentsSent = paymentsSentResult ? Number(paymentsSentResult.payments_sent) : 0;

        // 5. Payments received (to_player = username)
        const paymentsReceivedResult = await get(
            `SELECT COALESCE(SUM(amount), 0) as payments_received
             FROM payments
             WHERE group_id = ? AND to_player = ? AND is_deleted = 0`,
            [groupId, username]
        );
        const paymentsReceived = paymentsReceivedResult ? Number(paymentsReceivedResult.payments_received) : 0;

        // Balance calculations
        const netGameBalance = totalExits - totalBuyIns;
        const currentBalance = netGameBalance + paymentsSent - paymentsReceived;

        return res.status(200).json({
            groupId: group.id,
            groupName: group.name,
            username,
            tablesPlayed,
            totalBuyIns,
            totalExits,
            netGameBalance,
            paymentsSent,
            paymentsReceived,
            currentBalance
        });
    } catch (error) {
        console.error('Error fetching group stats:', error);
        return res.status(500).json({ error: 'Internal server error while fetching group stats' });
    }
});

module.exports = router;
