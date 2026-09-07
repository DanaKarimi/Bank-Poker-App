const express = require('express');
const router = express.Router();
const { get, all } = require('../database/db');

/**
 * GET /api/lookup/:code
 * Smart single code input that auto-detects whether code is a group invite code or table code
 */
router.get('/:code', async (req, res) => {
    try {
        const rawCode = req.params.code;
        if (!rawCode || !rawCode.trim()) {
            return res.status(400).json({ error: 'Code is required' });
        }

        const cleanCode = rawCode.trim().toUpperCase();

        // 1. Check if code matches a Group invite_code
        const group = await get(
            `SELECT g.*, u.username as owner_username, u.display_name as owner_display_name, u.avatar_id as owner_avatar_id
             FROM groups g
             LEFT JOIN users u ON g.owner_user_id = u.id
             WHERE UPPER(TRIM(g.invite_code)) = ? AND g.is_deleted = 0`,
            [cleanCode]
        );

        if (group) {
            const memberCountRow = await get('SELECT COUNT(*) as count FROM group_members WHERE group_id = ?', [group.id]);
            const tableCountRow = await get('SELECT COUNT(*) as count FROM tables WHERE group_id = ? AND is_deleted = 0', [group.id]);

            return res.json({
                type: 'GROUP',
                group: {
                    id: group.id,
                    name: group.name,
                    invite_code: group.invite_code,
                    owner_user_id: group.owner_user_id,
                    owner: group.owner_user_id ? {
                        id: group.owner_user_id,
                        username: group.owner_username,
                        display_name: group.owner_display_name || group.owner_username,
                        avatar_id: group.owner_avatar_id || 'avatar_1'
                    } : null,
                    member_count: memberCountRow?.count || 0,
                    table_count: tableCountRow?.count || 0,
                    created_at: group.created_at
                }
            });
        }

        // 2. Check if code matches a Table code (Quick table or Group table)
        const table = await get(
            `SELECT t.*, g.name as group_name, u.username as creator_username, u.display_name as creator_display_name, u.avatar_id as creator_avatar_id
             FROM tables t
             LEFT JOIN groups g ON t.group_id = g.id
             LEFT JOIN users u ON t.creator_user_id = u.id
             WHERE UPPER(TRIM(t.code)) = ? AND t.is_deleted = 0`,
            [cleanCode]
        );

        if (table) {
            const playerCountRow = await get('SELECT COUNT(*) as count FROM players WHERE table_id = ? AND is_deleted = 0', [table.id]);

            return res.json({
                type: 'TABLE',
                table: {
                    id: table.id,
                    groupId: table.group_id,
                    groupName: table.group_name || null,
                    name: table.name,
                    code: table.code,
                    status: table.status,
                    isQuickTable: !table.group_id,
                    chip_value: table.chip_value,
                    has_entry_fee: Boolean(table.has_entry_fee),
                    entry_fee: table.entry_fee,
                    player_count: playerCountRow?.count || 0,
                    creator: table.creator_user_id ? {
                        id: table.creator_user_id,
                        username: table.creator_username,
                        display_name: table.creator_display_name || table.creator_username,
                        avatar_id: table.creator_avatar_id || 'avatar_1'
                    } : null,
                    created_at: table.created_at,
                    published_at: table.published_at
                }
            });
        }

        return res.status(404).json({
            error: `No group or table found with code "${cleanCode}"`
        });
    } catch (err) {
        console.error('Error during code lookup:', err);
        res.status(500).json({ error: 'Internal server error during lookup' });
    }
});

module.exports = router;
