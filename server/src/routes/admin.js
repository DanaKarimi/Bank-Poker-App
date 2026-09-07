const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, get, all } = require('../database/db');
const { authenticateToken, requireSuperAdmin } = require('../middleware/auth');

// All admin routes strictly require SUPER_ADMIN
router.use(authenticateToken, requireSuperAdmin);

/**
 * GET /api/admin/overview
 * High-level platform statistics
 */
router.get('/overview', async (req, res) => {
    try {
        const totalUsersRow = await get('SELECT COUNT(*) as cnt FROM users');
        const guestUsersRow = await get('SELECT COUNT(*) as cnt FROM users WHERE password_hash IS NULL');
        const fullUsersRow = await get('SELECT COUNT(*) as cnt FROM users WHERE password_hash IS NOT NULL');
        const groupsRow = await get('SELECT COUNT(*) as cnt FROM groups WHERE is_deleted = 0');
        const tablesRow = await get('SELECT COUNT(*) as cnt FROM tables WHERE is_deleted = 0');
        const activeTablesRow = await get("SELECT COUNT(*) as cnt FROM tables WHERE is_deleted = 0 AND status = 'ACTIVE'");
        const quickTablesRow = await get('SELECT COUNT(*) as cnt FROM tables WHERE group_id IS NULL AND is_deleted = 0');
        const activePlayersRow = await get("SELECT COUNT(*) as cnt FROM players WHERE is_deleted = 0 AND status = 'ACTIVE'");

        res.json({
            stats: {
                totalUsers: totalUsersRow?.cnt || 0,
                guestUsers: guestUsersRow?.cnt || 0,
                fullUsers: fullUsersRow?.cnt || 0,
                totalGroups: groupsRow?.cnt || 0,
                totalTables: tablesRow?.cnt || 0,
                activeTables: activeTablesRow?.cnt || 0,
                quickTables: quickTablesRow?.cnt || 0,
                activePlayers: activePlayersRow?.cnt || 0
            }
        });
    } catch (err) {
        console.error('Error fetching admin overview:', err);
        res.status(500).json({ error: 'Failed to fetch overview metrics' });
    }
});

/**
 * GET /api/admin/users
 * List all users
 */
router.get('/users', async (req, res) => {
    try {
        const rows = await all(
            `SELECT u.id, u.username, u.display_name, u.avatar_id, u.role, 
                    (u.password_hash IS NULL) as is_guest, u.created_at, u.updated_at,
                    (SELECT COUNT(*) FROM group_members WHERE user_id = u.id) as group_count
             FROM users u
             ORDER BY u.created_at DESC`
        );

        res.json({
            users: rows.map(r => ({
                id: r.id,
                username: r.username,
                display_name: r.display_name || r.username,
                avatar_id: r.avatar_id || 'avatar_1',
                role: r.role,
                is_guest: Boolean(r.is_guest),
                group_count: r.group_count,
                created_at: r.created_at,
                updated_at: r.updated_at
            }))
        });
    } catch (err) {
        console.error('Error listing users for admin:', err);
        res.status(500).json({ error: 'Failed to list users' });
    }
});

/**
 * DELETE /api/admin/users/:id
 * Delete user with safe cascades (cannot delete self)
 */
router.delete('/users/:id', async (req, res) => {
    try {
        const targetUserId = req.params.id;
        if (targetUserId === req.user.id) {
            return res.status(400).json({ error: 'You cannot delete your own super admin account' });
        }

        const user = await get('SELECT id, username FROM users WHERE id = ?', [targetUserId]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Safe cascades
        await run('UPDATE players SET user_id = NULL WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM group_members WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM notifications WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM user_settings WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM fcm_tokens WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM push_subscriptions WHERE user_id = ?', [targetUserId]);
        await run('DELETE FROM users WHERE id = ?', [targetUserId]);

        res.json({ message: `User ${user.username} deleted successfully` });
    } catch (err) {
        console.error('Error deleting user:', err);
        res.status(500).json({ error: 'Failed to delete user' });
    }
});

/**
 * PUT /api/admin/users/:id/role
 * Update user role (USER, ADMIN, SUPER_ADMIN)
 */
router.put('/users/:id/role', async (req, res) => {
    try {
        const targetUserId = req.params.id;
        const { role } = req.body;
        const validRoles = ['USER', 'ADMIN', 'SUPER_ADMIN'];
        if (!validRoles.includes(role)) {
            return res.status(400).json({ error: `Invalid role. Must be one of: ${validRoles.join(', ')}` });
        }

        const user = await get('SELECT id, username FROM users WHERE id = ?', [targetUserId]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        await run('UPDATE users SET role = ?, updated_at = ? WHERE id = ?', [role, Date.now(), targetUserId]);
        res.json({ message: `Role for ${user.username} updated to ${role}`, role });
    } catch (err) {
        console.error('Error updating user role:', err);
        res.status(500).json({ error: 'Failed to update user role' });
    }
});

/**
 * GET /api/admin/groups
 * List all groups
 */
router.get('/groups', async (req, res) => {
    try {
        const rows = await all(
            `SELECT g.*, u.username as owner_username, u.display_name as owner_display_name, u.avatar_id as owner_avatar_id,
                    (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) as member_count,
                    (SELECT COUNT(*) FROM tables WHERE group_id = g.id AND is_deleted = 0) as table_count
             FROM groups g
             LEFT JOIN users u ON g.owner_user_id = u.id
             WHERE g.is_deleted = 0
             ORDER BY g.created_at DESC`
        );

        res.json({
            groups: rows.map(r => ({
                id: r.id,
                name: r.name,
                invite_code: r.invite_code,
                owner_user_id: r.owner_user_id,
                owner: r.owner_user_id ? {
                    id: r.owner_user_id,
                    username: r.owner_username,
                    display_name: r.owner_display_name || r.owner_username,
                    avatar_id: r.owner_avatar_id || 'avatar_1'
                } : null,
                member_count: r.member_count,
                table_count: r.table_count,
                created_at: r.created_at,
                updated_at: r.updated_at
            }))
        });
    } catch (err) {
        console.error('Error listing groups for admin:', err);
        res.status(500).json({ error: 'Failed to list groups' });
    }
});

/**
 * DELETE /api/admin/groups/:id
 * Delete group with safe cascades
 */
router.delete('/groups/:id', async (req, res) => {
    try {
        const groupId = req.params.id;
        const group = await get('SELECT id, name FROM groups WHERE id = ?', [groupId]);
        if (!group) {
            return res.status(404).json({ error: 'Group not found' });
        }

        // Cascades
        await run('DELETE FROM group_members WHERE group_id = ?', [groupId]);
        await run('DELETE FROM payments WHERE group_id = ?', [groupId]);
        await run('DELETE FROM settlement_records WHERE group_id = ?', [groupId]);
        await run('DELETE FROM entry_fee_records WHERE group_id = ?', [groupId]);
        await run('DELETE FROM synced_balances WHERE group_id = ?', [groupId]);
        
        // Find tables to cascade delete
        const tables = await all('SELECT id FROM tables WHERE group_id = ?', [groupId]);
        for (const t of tables) {
            await run('DELETE FROM buy_ins WHERE table_id = ?', [t.id]);
            await run('DELETE FROM exit_records WHERE table_id = ?', [t.id]);
            await run('DELETE FROM requests WHERE table_id = ?', [t.id]);
            await run('DELETE FROM players WHERE table_id = ?', [t.id]);
        }
        await run('DELETE FROM tables WHERE group_id = ?', [groupId]);
        await run('DELETE FROM groups WHERE id = ?', [groupId]);

        res.json({ message: `Group "${group.name}" deleted successfully` });
    } catch (err) {
        console.error('Error deleting group:', err);
        res.status(500).json({ error: 'Failed to delete group' });
    }
});

/**
 * GET /api/admin/tables
 * List all tables (both Group tables and Quick tables)
 */
router.get('/tables', async (req, res) => {
    try {
        const rows = await all(
            `SELECT t.*, g.name as group_name, u.username as creator_username, u.display_name as creator_display_name, u.avatar_id as creator_avatar_id,
                    (SELECT COUNT(*) FROM players WHERE table_id = t.id AND is_deleted = 0) as player_count
             FROM tables t
             LEFT JOIN groups g ON t.group_id = g.id
             LEFT JOIN users u ON t.creator_user_id = u.id
             WHERE t.is_deleted = 0
             ORDER BY t.created_at DESC`
        );

        res.json({
            tables: rows.map(r => ({
                id: r.id,
                groupId: r.group_id,
                groupName: r.group_name || null,
                isQuickTable: !r.group_id,
                name: r.name,
                code: r.code,
                status: r.status,
                chip_value: r.chip_value,
                has_entry_fee: Boolean(r.has_entry_fee),
                entry_fee: r.entry_fee,
                player_count: r.player_count,
                creator: r.creator_user_id ? {
                    id: r.creator_user_id,
                    username: r.creator_username,
                    display_name: r.creator_display_name || r.creator_username,
                    avatar_id: r.creator_avatar_id || 'avatar_1'
                } : null,
                created_at: r.created_at,
                closed_at: r.closed_at,
                published_at: r.published_at
            }))
        });
    } catch (err) {
        console.error('Error listing tables for admin:', err);
        res.status(500).json({ error: 'Failed to list tables' });
    }
});

/**
 * DELETE /api/admin/tables/:id
 * Delete table with safe cascades
 */
router.delete('/tables/:id', async (req, res) => {
    try {
        const tableId = req.params.id;
        const table = await get('SELECT id, name FROM tables WHERE id = ?', [tableId]);
        if (!table) {
            return res.status(404).json({ error: 'Table not found' });
        }

        await run('DELETE FROM buy_ins WHERE table_id = ?', [tableId]);
        await run('DELETE FROM exit_records WHERE table_id = ?', [tableId]);
        await run('DELETE FROM requests WHERE table_id = ?', [tableId]);
        await run('DELETE FROM players WHERE table_id = ?', [tableId]);
        await run('DELETE FROM tables WHERE id = ?', [tableId]);

        res.json({ message: `Table "${table.name}" deleted successfully` });
    } catch (err) {
        console.error('Error deleting table:', err);
        res.status(500).json({ error: 'Failed to delete table' });
    }
});

/**
 * GET /api/admin/tables/:tableId/players
 * List players with buy-ins, exits, and balances for super admin inspection
 */
router.get('/tables/:tableId/players', async (req, res) => {
    try {
        const { tableId } = req.params;
        const players = await all(
            `SELECT p.*, u.username as linked_username, u.display_name as linked_display_name, u.avatar_id as linked_avatar_id,
                    COALESCE((SELECT SUM(amount) FROM buy_ins WHERE player_id = p.id AND is_deleted = 0), 0) as total_buy_ins,
                    COALESCE((SELECT SUM(amount) FROM exit_records WHERE player_id = p.id AND is_deleted = 0), 0) as total_exits
             FROM players p
             LEFT JOIN users u ON p.user_id = u.id
             WHERE p.table_id = ? AND p.is_deleted = 0
             ORDER BY p.created_at ASC`,
            [tableId]
        );

        res.json({
            players: players.map(p => ({
                id: p.id,
                table_id: p.table_id,
                user_id: p.user_id,
                name: p.name,
                status: p.status,
                user_linked_at: p.user_linked_at,
                entry_fee_paid: Boolean(p.entry_fee_paid),
                totalBuyIns: p.total_buy_ins,
                totalExits: p.total_exits,
                balance: p.total_exits - p.total_buy_ins,
                linked_user: p.user_id ? {
                    id: p.user_id,
                    username: p.linked_username,
                    display_name: p.linked_display_name || p.linked_username,
                    avatar_id: p.linked_avatar_id || 'avatar_1'
                } : null
            }))
        });
    } catch (err) {
        console.error('Error listing table players for admin:', err);
        res.status(500).json({ error: 'Failed to list table players' });
    }
});

/**
 * PUT /api/admin/tables/:tableId/players/:playerId
 * Super admin edit player: rename, unlink, or balance adjustment
 */
router.put('/tables/:tableId/players/:playerId', async (req, res) => {
    try {
        const { tableId, playerId } = req.params;
        const { name, user_id, balanceAdjustment } = req.body;

        const player = await get('SELECT * FROM players WHERE id = ? AND table_id = ?', [playerId, tableId]);
        if (!player) {
            return res.status(404).json({ error: 'Player not found' });
        }

        const now = Date.now();
        const newName = (name && name.trim()) ? name.trim() : player.name;
        const newUserId = user_id !== undefined ? user_id : player.user_id;

        await run(
            `UPDATE players 
             SET name = ?, user_id = ?, updated_at = ? 
             WHERE id = ?`,
            [newName, newUserId, now, playerId]
        );

        // If balance adjustment requested (e.g. +50 or -50)
        if (balanceAdjustment && !isNaN(Number(balanceAdjustment))) {
            const adj = Number(balanceAdjustment);
            if (adj > 0) {
                // Add exit to increase balance
                const exitId = crypto.randomUUID();
                await run(
                    `INSERT INTO exit_records (id, table_id, player_id, amount, note, actor_user_id, created_at, updated_at)
                     VALUES (?, ?, ?, ?, 'Admin balance adjustment', ?, ?, ?)`,
                    [exitId, tableId, playerId, adj, req.user.id, now, now]
                );
            } else if (adj < 0) {
                // Add buy-in to decrease balance
                const buyInId = crypto.randomUUID();
                await run(
                    `INSERT INTO buy_ins (id, table_id, player_id, amount, note, actor_user_id, created_at, updated_at)
                     VALUES (?, ?, ?, ?, 'Admin balance adjustment', ?, ?, ?)`,
                    [buyInId, tableId, playerId, Math.abs(adj), req.user.id, now, now]
                );
            }
        }

        res.json({ message: 'Player updated successfully' });
    } catch (err) {
        console.error('Error updating player by admin:', err);
        res.status(500).json({ error: 'Failed to update player' });
    }
});

module.exports = router;
