const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { run, get } = require('../database/db');
const { authenticateToken } = require('../middleware/auth');
const { getUserPrefs } = require('../services/notifications');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_bankpoker_key_change_in_production';

function generateToken(user) {
    return jwt.sign(
        {
            id: user.id,
            username: user.username,
            role: user.role
        },
        JWT_SECRET,
        { expiresIn: '30d' }
    );
}

function formatUser(user) {
    return {
        id: user.id,
        username: user.username,
        display_name: user.display_name || user.username,
        avatar_id: user.avatar_id || 'avatar_1',
        role: user.role || 'USER',
        is_guest: !user.password_hash,
        created_at: user.created_at,
        updated_at: user.updated_at
    };
}

/**
 * Check if user should be SUPER_ADMIN
 */
async function determineRole(username) {
    // 1. Check if SUPER_ADMIN_USERNAME matches
    const superAdminUsername = process.env.SUPER_ADMIN_USERNAME;
    if (superAdminUsername && username.trim().toLowerCase() === superAdminUsername.trim().toLowerCase()) {
        return 'SUPER_ADMIN';
    }

    // 2. First non-guest registered user in the system becomes SUPER_ADMIN
    const fullUserCount = await get("SELECT COUNT(*) as cnt FROM users WHERE password_hash IS NOT NULL");
    if (!fullUserCount || fullUserCount.cnt === 0) {
        return 'SUPER_ADMIN';
    }

    return 'USER';
}

/**
 * POST /api/auth/register
 * Register a new full user account
 */
router.post('/register', async (req, res) => {
    try {
        const { username, password, display_name, avatar_id } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        const trimmedUsername = username.trim();
        if (trimmedUsername.length < 3) {
            return res.status(400).json({ error: 'Username must be at least 3 characters long' });
        }

        if (password.length < 4) {
            return res.status(400).json({ error: 'Password must be at least 4 characters long' });
        }

        const existingUser = await get('SELECT id FROM users WHERE LOWER(username) = LOWER(?)', [trimmedUsername]);
        if (existingUser) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        const role = await determineRole(trimmedUsername);
        const passwordHash = await bcrypt.hash(password, 10);
        const userId = crypto.randomUUID();
        const now = Date.now();
        const displayName = (display_name && display_name.trim()) || trimmedUsername;
        const avatarId = avatar_id || 'avatar_1';

        await run(
            `INSERT INTO users (id, username, display_name, password_hash, avatar_id, role, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [userId, trimmedUsername, displayName, passwordHash, avatarId, role, now, now]
        );

        const newUser = {
            id: userId,
            username: trimmedUsername,
            display_name: displayName,
            password_hash: passwordHash,
            avatar_id: avatarId,
            role,
            created_at: now,
            updated_at: now
        };

        const token = generateToken(newUser);

        return res.status(201).json({
            message: 'User registered successfully',
            token,
            user: formatUser(newUser)
        });
    } catch (error) {
        console.error('Error during registration:', error);
        return res.status(500).json({ error: 'Internal server error during registration' });
    }
});

/**
 * POST /api/auth/guest
 * Guest join: display name only -> creates user with username guest_<random>, random avatar, password_hash NULL
 */
router.post('/guest', async (req, res) => {
    try {
        const { display_name } = req.body;
        const displayName = (display_name && display_name.trim()) ? display_name.trim() : 'Guest Player';

        // Generate unique guest username
        let guestUsername;
        let isUnique = false;
        while (!isUnique) {
            guestUsername = 'guest_' + crypto.randomBytes(4).toString('hex');
            const existing = await get('SELECT id FROM users WHERE LOWER(username) = LOWER(?)', [guestUsername]);
            if (!existing) isUnique = true;
        }

        // Random avatar from 1 to 24
        const randomNum = Math.floor(Math.random() * 24) + 1;
        const avatarId = `avatar_${randomNum}`;

        const userId = crypto.randomUUID();
        const now = Date.now();
        const role = 'USER';

        await run(
            `INSERT INTO users (id, username, display_name, password_hash, avatar_id, role, created_at, updated_at)
             VALUES (?, ?, ?, NULL, ?, ?, ?, ?)`,
            [userId, guestUsername, displayName, avatarId, role, now, now]
        );

        const guestUser = {
            id: userId,
            username: guestUsername,
            display_name: displayName,
            password_hash: null,
            avatar_id: avatarId,
            role,
            created_at: now,
            updated_at: now
        };

        const token = generateToken(guestUser);

        return res.status(201).json({
            message: 'Guest session created',
            token,
            user: formatUser(guestUser)
        });
    } catch (error) {
        console.error('Error creating guest user:', error);
        return res.status(500).json({ error: 'Failed to create guest session' });
    }
});

/**
 * POST /api/auth/login
 * Log in with username and password
 */
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        const user = await get('SELECT * FROM users WHERE LOWER(username) = LOWER(?)', [username.trim()]);
        if (!user || !user.password_hash) {
            return res.status(401).json({ error: 'Invalid username or password' });
        }

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({ error: 'Invalid username or password' });
        }

        const token = generateToken(user);

        return res.status(200).json({
            token,
            user: formatUser(user)
        });
    } catch (error) {
        console.error('Error during login:', error);
        return res.status(500).json({ error: 'Internal server error during login' });
    }
});

/**
 * POST /api/auth/activate
 * Activate guest account into a permanent account with password, username, and avatar
 */
router.post('/activate', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const currentUser = await get('SELECT * FROM users WHERE id = ?', [userId]);
        if (!currentUser) {
            return res.status(404).json({ error: 'User not found' });
        }

        const { username, password, display_name, avatar_id } = req.body;

        if (!password || password.length < 4) {
            return res.status(400).json({ error: 'Password must be at least 4 characters long' });
        }

        let newUsername = currentUser.username;
        if (username && username.trim()) {
            const trimmed = username.trim();
            if (trimmed.length < 3) {
                return res.status(400).json({ error: 'Username must be at least 3 characters long' });
            }
            if (trimmed.toLowerCase() !== currentUser.username.toLowerCase()) {
                const existing = await get('SELECT id FROM users WHERE LOWER(username) = LOWER(?) AND id != ?', [trimmed, userId]);
                if (existing) {
                    return res.status(400).json({ error: 'Username already taken' });
                }
                newUsername = trimmed;
            }
        }

        const newDisplayName = (display_name && display_name.trim()) ? display_name.trim() : (currentUser.display_name || newUsername);
        const newAvatarId = avatar_id || currentUser.avatar_id || 'avatar_1';
        const passwordHash = await bcrypt.hash(password, 10);
        const now = Date.now();

        // Determine if this user becomes super admin
        let newRole = currentUser.role;
        if (newRole !== 'SUPER_ADMIN') {
            newRole = await determineRole(newUsername);
        }

        await run(
            `UPDATE users 
             SET username = ?, display_name = ?, password_hash = ?, avatar_id = ?, role = ?, updated_at = ?
             WHERE id = ?`,
            [newUsername, newDisplayName, passwordHash, newAvatarId, newRole, now, userId]
        );

        const updatedUser = {
            id: userId,
            username: newUsername,
            display_name: newDisplayName,
            password_hash: passwordHash,
            avatar_id: newAvatarId,
            role: newRole,
            created_at: currentUser.created_at,
            updated_at: now
        };

        const token = generateToken(updatedUser);

        return res.json({
            message: 'Account activated successfully',
            token,
            user: formatUser(updatedUser)
        });
    } catch (err) {
        console.error('Error activating account:', err);
        return res.status(500).json({ error: 'Failed to activate account' });
    }
});

/**
 * PUT /api/auth/profile
 * Update profile: display_name, username (uniqueness enforced), avatar_id
 */
router.put('/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const currentUser = await get('SELECT * FROM users WHERE id = ?', [userId]);
        if (!currentUser) {
            return res.status(404).json({ error: 'User not found' });
        }

        const { display_name, username, avatar_id } = req.body;
        let newUsername = currentUser.username;

        if (username && username.trim()) {
            const trimmed = username.trim();
            if (trimmed.length < 3) {
                return res.status(400).json({ error: 'Username must be at least 3 characters long' });
            }
            if (trimmed.toLowerCase() !== currentUser.username.toLowerCase()) {
                const existing = await get('SELECT id FROM users WHERE LOWER(username) = LOWER(?) AND id != ?', [trimmed, userId]);
                if (existing) {
                    return res.status(400).json({ error: 'Username already taken' });
                }
                newUsername = trimmed;
            }
        }

        const newDisplayName = (display_name && display_name.trim()) ? display_name.trim() : (currentUser.display_name || newUsername);
        const newAvatarId = avatar_id || currentUser.avatar_id || 'avatar_1';
        const now = Date.now();

        await run(
            `UPDATE users 
             SET username = ?, display_name = ?, avatar_id = ?, updated_at = ?
             WHERE id = ?`,
            [newUsername, newDisplayName, newAvatarId, now, userId]
        );

        const updatedUser = {
            id: userId,
            username: newUsername,
            display_name: newDisplayName,
            password_hash: currentUser.password_hash,
            avatar_id: newAvatarId,
            role: currentUser.role,
            created_at: currentUser.created_at,
            updated_at: now
        };

        const token = generateToken(updatedUser);

        return res.json({
            message: 'Profile updated successfully',
            token,
            user: formatUser(updatedUser)
        });
    } catch (err) {
        console.error('Error updating profile:', err);
        return res.status(500).json({ error: 'Failed to update profile' });
    }
});

/**
 * GET /api/auth/me
 * Fetch current user profile and notification preferences
 */
router.get('/me', authenticateToken, async (req, res) => {
    try {
        const user = await get('SELECT * FROM users WHERE id = ?', [req.user.id]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        const notifPrefs = await getUserPrefs(user.id);

        return res.status(200).json({
            user: formatUser(user),
            settings: {
                notif_prefs: notifPrefs
            }
        });
    } catch (error) {
        console.error('Error fetching user profile:', error);
        return res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
