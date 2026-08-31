const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { run, get } = require('../database/db');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_bankpoker_key_change_in_production';

/**
 * POST /api/auth/register
 * Register a new user
 */
router.post('/register', async (req, res) => {
    try {
        const { username, password, role } = req.body;

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

        // Check if username is already taken
        const existingUser = await get('SELECT id FROM users WHERE LOWER(username) = LOWER(?)', [trimmedUsername]);
        if (existingUser) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        const passwordHash = await bcrypt.hash(password, 10);
        const userId = crypto.randomUUID();
        const userRole = (role && role.toUpperCase() === 'ADMIN') ? 'ADMIN' : 'PLAYER';
        const createdAt = Date.now();

        await run(
            'INSERT INTO users (id, username, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)',
            [userId, trimmedUsername, passwordHash, userRole, createdAt]
        );

        return res.status(201).json({
            message: 'User registered successfully',
            user: {
                id: userId,
                username: trimmedUsername,
                role: userRole,
                created_at: createdAt
            }
        });
    } catch (error) {
        console.error('Error during registration:', error);
        return res.status(500).json({ error: 'Internal server error during registration' });
    }
});

/**
 * POST /api/auth/login
 * Log in and obtain a JWT token
 */
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        const user = await get('SELECT * FROM users WHERE LOWER(username) = LOWER(?)', [username.trim()]);
        if (!user) {
            return res.status(401).json({ error: 'Invalid username or password' });
        }

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({ error: 'Invalid username or password' });
        }

        // Generate JWT token valid for 30 days
        const token = jwt.sign(
            {
                id: user.id,
                username: user.username,
                role: user.role
            },
            JWT_SECRET,
            { expiresIn: '30d' }
        );

        return res.status(200).json({
            token,
            user: {
                id: user.id,
                username: user.username,
                role: user.role
            }
        });
    } catch (error) {
        console.error('Error during login:', error);
        return res.status(500).json({ error: 'Internal server error during login' });
    }
});

module.exports = router;
