const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const webpush = require('web-push');
const { run, get, all } = require('../database/db');
const { emitToUser } = require('../socket');

const DEFAULT_PREFS = {
    table_created: true,
    member_joined: true,
    claim: false,
    request_to_me: true,
    settlement: true
};

let fcmInitialized = false;
let firebaseAdmin = null;

// Attempt optional Firebase Admin initialization for FCM
try {
    const serviceAccountPath = path.resolve(__dirname, '../../fcm-service-account.json');
    if (fs.existsSync(serviceAccountPath)) {
        firebaseAdmin = require('firebase-admin');
        const serviceAccount = require(serviceAccountPath);
        firebaseAdmin.initializeApp({
            credential: firebaseAdmin.credential.cert(serviceAccount)
        });
        fcmInitialized = true;
        console.log('[FCM] Firebase Admin initialized with service account.');
    }
} catch (err) {
    console.warn('[FCM] Optional Firebase Admin initialization skipped:', err.message);
}

/**
 * Get notification preferences for a user
 */
async function getUserPrefs(userId) {
    try {
        const row = await get('SELECT notif_prefs FROM user_settings WHERE user_id = ?', [userId]);
        if (row && row.notif_prefs) {
            return { ...DEFAULT_PREFS, ...JSON.parse(row.notif_prefs) };
        }
    } catch (e) {
        console.error('Error fetching user preferences:', e);
    }
    return { ...DEFAULT_PREFS };
}

/**
 * Set notification preferences for a user
 */
async function setUserPrefs(userId, prefs) {
    const merged = { ...DEFAULT_PREFS, ...prefs };
    const now = Date.now();
    await run(
        `INSERT INTO user_settings (user_id, notif_prefs, updated_at)
         VALUES (?, ?, ?)
         ON CONFLICT(user_id) DO UPDATE SET notif_prefs = excluded.notif_prefs, updated_at = excluded.updated_at`,
        [userId, JSON.stringify(merged), now]
    );
    return merged;
}

/**
 * Send a notification to a specific user honoring anti-spam (60s collapse), WS, and push
 */
async function sendNotification(userId, { type, title, message, targetId, targetType, data = {} }) {
    try {
        if (!userId) return null;

        // Check user preferences
        const prefs = await getUserPrefs(userId);
        if (prefs[type] === false) {
            // User disabled this notification type
            return null;
        }

        const now = Date.now();
        const payloadObj = {
            title,
            message,
            targetId: targetId || null,
            targetType: targetType || null,
            ...data
        };

        // Anti-spam rule: same type + target within 60s collapses into one row with count++
        let notificationItem = null;
        if (targetId) {
            const recentRow = await get(
                `SELECT * FROM notifications 
                 WHERE user_id = ? AND type = ? AND created_at >= ?
                 ORDER BY created_at DESC LIMIT 1`,
                [userId, type, now - 60000]
            );

            if (recentRow) {
                try {
                    const parsedPayload = JSON.parse(recentRow.payload);
                    if (parsedPayload.targetId === targetId) {
                        // Collapse into this notification
                        const newCount = (recentRow.count || 1) + 1;
                        await run(
                            `UPDATE notifications 
                             SET count = ?, read = 0, updated_at = ? 
                             WHERE id = ?`,
                            [newCount, now, recentRow.id]
                        );

                        notificationItem = {
                            id: recentRow.id,
                            userId,
                            type,
                            payload: payloadObj,
                            count: newCount,
                            read: 0,
                            createdAt: recentRow.created_at,
                            updatedAt: now
                        };
                    }
                } catch (pe) {
                    // Ignore parse error and proceed to insert new
                }
            }
        }

        if (!notificationItem) {
            const notifId = crypto.randomUUID();
            await run(
                `INSERT INTO notifications (id, user_id, type, payload, count, read, created_at, updated_at)
                 VALUES (?, ?, ?, ?, 1, 0, ?, ?)`,
                [notifId, userId, type, JSON.stringify(payloadObj), now, now]
            );

            notificationItem = {
                id: notifId,
                userId,
                type,
                payload: payloadObj,
                count: 1,
                read: 0,
                createdAt: now,
                updatedAt: now
            };
        }

        // 1. Deliver live via Socket.IO
        emitToUser(userId, 'notification', notificationItem);

        // 2. Web Push delivery
        dispatchWebPush(userId, notificationItem).catch(err => {
            console.error('[WebPush] Dispatch error:', err.message);
        });

        // 3. FCM Push delivery (Android)
        dispatchFcm(userId, notificationItem).catch(err => {
            console.error('[FCM] Dispatch error:', err.message);
        });

        return notificationItem;
    } catch (error) {
        console.error('Error sending notification:', error);
        return null;
    }
}

/**
 * Dispatch Web Push to user's push subscriptions
 */
async function dispatchWebPush(userId, notificationItem) {
    try {
        const pubKeyRow = await get("SELECT value FROM system_settings WHERE key = 'vapid_public_key'");
        const privKeyRow = await get("SELECT value FROM system_settings WHERE key = 'vapid_private_key'");
        if (!pubKeyRow || !privKeyRow) return;

        webpush.setVapidDetails(
            'mailto:support@bankjoker.ir',
            pubKeyRow.value,
            privKeyRow.value
        );

        const subs = await all('SELECT endpoint, keys FROM push_subscriptions WHERE user_id = ?', [userId]);
        if (!subs || subs.length === 0) return;

        const payloadString = JSON.stringify({
            title: notificationItem.payload.title || 'BankPoker',
            body: notificationItem.payload.message || 'You have a new update.',
            icon: '/icon-192.png',
            badge: '/icon-192.png',
            data: {
                notificationId: notificationItem.id,
                type: notificationItem.type,
                targetId: notificationItem.payload.targetId,
                targetType: notificationItem.payload.targetType
            }
        });

        for (const sub of subs) {
            try {
                const subObj = {
                    endpoint: sub.endpoint,
                    keys: JSON.parse(sub.keys)
                };
                await webpush.sendNotification(subObj, payloadString);
            } catch (sendErr) {
                // If 404 or 410 (Gone), delete stale subscription
                if (sendErr.statusCode === 404 || sendErr.statusCode === 410) {
                    await run('DELETE FROM push_subscriptions WHERE endpoint = ?', [sub.endpoint]);
                }
            }
        }
    } catch (err) {
        // silent fail on push errors
    }
}

/**
 * Dispatch FCM push to Android tokens
 */
async function dispatchFcm(userId, notificationItem) {
    if (!fcmInitialized || !firebaseAdmin) return;
    try {
        const tokens = await all('SELECT token FROM fcm_tokens WHERE user_id = ?', [userId]);
        if (!tokens || tokens.length === 0) return;

        const registrationTokens = tokens.map(t => t.token);
        const message = {
            notification: {
                title: notificationItem.payload.title || 'BankPoker',
                body: notificationItem.payload.message || 'You have an update'
            },
            data: {
                notificationId: String(notificationItem.id),
                type: String(notificationItem.type),
                targetId: String(notificationItem.payload.targetId || '')
            },
            tokens: registrationTokens
        };

        const response = await firebaseAdmin.messaging().sendEachForMulticast(message);
        // Clean up invalid tokens
        if (response.failureCount > 0) {
            response.responses.forEach(async (resp, idx) => {
                if (!resp.success) {
                    const error = resp.error;
                    if (error?.code === 'messaging/invalid-registration-token' ||
                        error?.code === 'messaging/registration-token-not-registered') {
                        await run('DELETE FROM fcm_tokens WHERE token = ?', [registrationTokens[idx]]);
                    }
                }
            });
        }
    } catch (err) {
        // silent fail on push errors
    }
}

module.exports = {
    DEFAULT_PREFS,
    getUserPrefs,
    setUserPrefs,
    sendNotification
};
