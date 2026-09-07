const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/auth');
const { run, get, all } = require('../database/db');
const { getUserPrefs, setUserPrefs } = require('../services/notifications');

/**
 * GET /api/notifications
 * List user notifications with unread count
 */
router.get('/', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const rows = await all(
            `SELECT * FROM notifications 
             WHERE user_id = ? 
             ORDER BY created_at DESC 
             LIMIT 50`,
            [userId]
        );

        const unreadCountRow = await get(
            `SELECT COUNT(*) as count FROM notifications 
             WHERE user_id = ? AND read = 0`,
            [userId]
        );

        const notifications = rows.map(r => ({
            id: r.id,
            userId: r.user_id,
            type: r.type,
            payload: JSON.parse(r.payload || '{}'),
            count: r.count,
            read: Boolean(r.read),
            createdAt: r.created_at,
            updatedAt: r.updated_at
        }));

        res.json({
            notifications,
            unreadCount: unreadCountRow?.count || 0
        });
    } catch (err) {
        console.error('Error fetching notifications:', err);
        res.status(500).json({ error: 'Failed to fetch notifications' });
    }
});

/**
 * PUT /api/notifications/:id/read
 * Mark a notification as read
 */
router.put('/:id/read', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        await run(
            `UPDATE notifications 
             SET read = 1, updated_at = ? 
             WHERE id = ? AND user_id = ?`,
            [Date.now(), id, userId]
        );

        res.json({ message: 'Marked as read' });
    } catch (err) {
        console.error('Error marking notification read:', err);
        res.status(500).json({ error: 'Failed to mark notification as read' });
    }
});

/**
 * PUT /api/notifications/read-all
 * Mark all notifications as read
 */
router.put('/read-all', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        await run(
            `UPDATE notifications 
             SET read = 1, updated_at = ? 
             WHERE user_id = ? AND read = 0`,
            [Date.now(), userId]
        );

        res.json({ message: 'All notifications marked as read' });
    } catch (err) {
        console.error('Error marking all notifications read:', err);
        res.status(500).json({ error: 'Failed to mark all as read' });
    }
});

/**
 * GET /api/notifications/settings
 * Fetch user notification preferences
 */
router.get('/settings', authenticateToken, async (req, res) => {
    try {
        const prefs = await getUserPrefs(req.user.id);
        res.json({ settings: prefs });
    } catch (err) {
        console.error('Error fetching settings:', err);
        res.status(500).json({ error: 'Failed to fetch settings' });
    }
});

/**
 * PUT /api/notifications/settings
 * Update user notification preferences
 */
router.put('/settings', authenticateToken, async (req, res) => {
    try {
        const updated = await setUserPrefs(req.user.id, req.body.settings || req.body);
        res.json({ settings: updated, message: 'Settings saved' });
    } catch (err) {
        console.error('Error saving settings:', err);
        res.status(500).json({ error: 'Failed to save settings' });
    }
});

/**
 * POST /api/notifications/fcm-token
 * Register an Android FCM token
 */
router.post('/fcm-token', authenticateToken, async (req, res) => {
    try {
        const { token, platform = 'android' } = req.body;
        if (!token) return res.status(400).json({ error: 'FCM token required' });

        await run(
            `INSERT INTO fcm_tokens (user_id, token, platform, created_at)
             VALUES (?, ?, ?, ?)
             ON CONFLICT(user_id, token) DO UPDATE SET created_at = excluded.created_at`,
            [req.user.id, token, platform, Date.now()]
        );

        res.json({ message: 'FCM token registered' });
    } catch (err) {
        console.error('Error saving FCM token:', err);
        res.status(500).json({ error: 'Failed to save FCM token' });
    }
});

/**
 * POST /api/notifications/web-subscribe
 * Register a Web Push subscription
 */
router.post('/web-subscribe', authenticateToken, async (req, res) => {
    try {
        const { subscription } = req.body;
        if (!subscription || !subscription.endpoint || !subscription.keys) {
            return res.status(400).json({ error: 'Invalid web-push subscription' });
        }

        await run(
            `INSERT INTO push_subscriptions (user_id, endpoint, keys, created_at)
             VALUES (?, ?, ?, ?)
             ON CONFLICT(endpoint) DO UPDATE SET user_id = excluded.user_id, keys = excluded.keys, created_at = excluded.created_at`,
            [req.user.id, subscription.endpoint, JSON.stringify(subscription.keys), Date.now()]
        );

        res.json({ message: 'Web push subscription saved' });
    } catch (err) {
        console.error('Error saving web push subscription:', err);
        res.status(500).json({ error: 'Failed to save push subscription' });
    }
});

/**
 * GET /api/notifications/vapid-key
 * Get VAPID public key for web push clients
 */
router.get('/vapid-key', async (req, res) => {
    try {
        const row = await get("SELECT value FROM system_settings WHERE key = 'vapid_public_key'");
        res.json({ publicKey: row?.value || null });
    } catch (err) {
        res.status(500).json({ error: 'Failed to get VAPID key' });
    }
});

module.exports = router;
