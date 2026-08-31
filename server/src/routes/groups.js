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

        if (!invite_code) {
            return res.status(400).json({ error: 'invite_code is required' });
        }

        const group = await get(
            'SELECT * FROM groups WHERE invite_code = ? AND is_deleted = 0',
            [invite_code.trim().toUpperCase()]
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

        // Calculations
        const totalPayments = paymentsSent + paymentsReceived;
        const netGameBalance = totalExits - totalBuyIns;
        const currentBalance = netGameBalance + paymentsSent - paymentsReceived;
        const balance = (totalBuyIns - totalExits) + (paymentsReceived - paymentsSent);

        // 6. Recent Transactions (BuyIns, Exits, Payments)
        const recentBuyIns = await all(
            `SELECT b.id, 'BUY_IN' as type, b.amount, b.note, b.created_at as timestamp, t.name as table_name
             FROM buy_ins b
             JOIN players p ON b.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND b.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY b.created_at DESC
             LIMIT 10`,
            [groupId, username]
        );

        const recentExits = await all(
            `SELECT e.id, 'EXIT' as type, e.amount, e.note, e.created_at as timestamp, t.name as table_name
             FROM exit_records e
             JOIN players p ON e.player_id = p.id
             JOIN tables t ON p.table_id = t.id
             WHERE t.group_id = ? AND p.name = ? AND e.is_deleted = 0 AND p.is_deleted = 0 AND t.is_deleted = 0
             ORDER BY e.created_at DESC
             LIMIT 10`,
            [groupId, username]
        );

        const recentPayments = await all(
            `SELECT p.id,
                    CASE WHEN p.from_player = ? THEN 'PAYMENT_SENT' ELSE 'PAYMENT_RECEIVED' END as type,
                    p.amount,
                    p.from_player || ' -> ' || p.to_player as note,
                    p.created_at as timestamp,
                    'Settlement Payment' as table_name
             FROM payments p
             WHERE p.group_id = ? AND (p.from_player = ? OR p.to_player = ?) AND p.is_deleted = 0
             ORDER BY p.created_at DESC
             LIMIT 10`,
            [username, groupId, username, username]
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
            totalBuyIns,
            totalExits,
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

module.exports = router;
