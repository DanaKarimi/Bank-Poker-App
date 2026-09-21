const express = require('express');
const http = require('http');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const dotenv = require('dotenv');

// Load environment variables from server/.env
dotenv.config({ path: path.resolve(__dirname, '../.env') });

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_bankpoker_key_change_in_production';
if (!process.env.JWT_SECRET) {
    console.warn('WARNING: JWT_SECRET not set, using insecure default. Set it in production.');
}

const { initDb } = require('./database/db');
const { initSocket } = require('./socket');
const authRoutes = require('./routes/auth');
const groupRoutes = require('./routes/groups');
const requestRoutes = require('./routes/requests');
const tableRoutes = require('./routes/tables');
const syncRoutes = require('./routes/sync');
const lookupRoutes = require('./routes/lookup');
const notificationRoutes = require('./routes/notifications');
const adminRoutes = require('./routes/admin');

const { rateLimit } = require('express-rate-limit');

const app = express();

// Trust reverse proxy (e.g. Nginx) so req.ip reflects real client IP from X-Forwarded-For
const trustProxyHops = process.env.TRUST_PROXY_HOPS !== undefined ? Number(process.env.TRUST_PROXY_HOPS) : 1;
app.set('trust proxy', isNaN(trustProxyHops) ? 1 : trustProxyHops);

const server = http.createServer(app);
const PORT = process.env.PORT || 3000;

// Rate limiting configurations
function maskIp(ip) {
    if (!ip || typeof ip !== 'string') return 'unknown';
    const cleanIp = ip.replace(/^::ffff:/, '');
    if (cleanIp.includes('.')) {
        const parts = cleanIp.split('.');
        if (parts.length === 4) {
            return `${parts[0]}.${parts[1]}.*.*`;
        }
    }
    if (cleanIp.includes(':')) {
        const parts = cleanIp.split(':');
        return `${parts.slice(0, 2).join(':')}:*:*`;
    }
    return '***';
}

const globalApiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    limit: 1000, // 1000 requests per 15 minutes per real IP
    standardHeaders: true, // draft-6 / draft-7 RateLimit-* headers
    legacyHeaders: false,
    handler: (req, res, next, options) => {
        const retryAfterSeconds = req.rateLimit?.resetTime
            ? Math.max(1, Math.ceil((req.rateLimit.resetTime.getTime() - Date.now()) / 1000))
            : Math.ceil(options.windowMs / 1000);
        console.warn(`WARNING: Rate limit exceeded (global) for IP: ${maskIp(req.ip)} on ${req.originalUrl}. Retry after: ${retryAfterSeconds}s`);
        res.setHeader('Retry-After', String(retryAfterSeconds));
        res.status(options.statusCode).json({
            error: 'rate_limited',
            message: 'Too many requests. Please wait a moment and try again.',
            retryAfterSeconds
        });
    }
});

function isLanOrLoopbackIp(ip) {
    if (!ip || typeof ip !== 'string') return false;
    const cleanIp = ip.replace(/^::ffff:/, '').trim().toLowerCase();

    // Loopback IPv6 / name
    if (cleanIp === '::1' || cleanIp === 'localhost') return true;

    // Check IPv4
    if (cleanIp.includes('.')) {
        const parts = cleanIp.split('.').map(Number);
        if (parts.length === 4 && parts.every(p => !isNaN(p) && p >= 0 && p <= 255)) {
            // 127.0.0.0/8 (Loopback)
            if (parts[0] === 127) return true;
            // 10.0.0.0/8 (RFC1918)
            if (parts[0] === 10) return true;
            // 172.16.0.0/12 (RFC1918: 172.16.0.0 - 172.31.255.255)
            if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
            // 192.168.0.0/16 (RFC1918)
            if (parts[0] === 192 && parts[1] === 168) return true;
            // 169.254.0.0/16 (Link-local)
            if (parts[0] === 169 && parts[1] === 254) return true;
        }
        return false;
    }

    // Check IPv6
    if (cleanIp.includes(':')) {
        // Link-local: fe80::/10 (fe80 - febf)
        if (/^fe[89ab]/i.test(cleanIp)) return true;
        // Unique Local Address (ULA): fc00::/7 (fc00 - fdff)
        if (/^f[cd]/i.test(cleanIp)) return true;
    }

    return false;
}

const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    limit: 30, // 30 attempts per 15 minutes per real IP
    standardHeaders: true,
    legacyHeaders: false,
    skip: (req) => {
        const allowLan = process.env.RATE_LIMIT_ALLOW_LAN !== 'false';
        return allowLan && isLanOrLoopbackIp(req.ip);
    },
    handler: (req, res, next, options) => {
        const retryAfterSeconds = req.rateLimit?.resetTime
            ? Math.max(1, Math.ceil((req.rateLimit.resetTime.getTime() - Date.now()) / 1000))
            : Math.ceil(options.windowMs / 1000);
        console.warn(`WARNING: Rate limit exceeded (auth) for IP: ${maskIp(req.ip)} on ${req.originalUrl}. Retry after: ${retryAfterSeconds}s`);
        res.setHeader('Retry-After', String(retryAfterSeconds));
        res.status(options.statusCode).json({
            error: 'rate_limited',
            message: 'Too many attempts. Please wait a moment and try again.',
            retryAfterSeconds
        });
    }
});

// Initialize Socket.IO with HTTP server
initSocket(server);

// Middleware
app.use(cors());
app.use(express.json());

// Health Check Endpoint (Keep untouched)
app.get('/api/health', (req, res) => {
    res.status(200).json({
        status: 'ok',
        timestamp: Date.now()
    });
});

// Rate Limiters (applied to API HTTP endpoints only; socket.io transport and health check excluded)
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/register', authLimiter);
app.use('/api/auth/guest', authLimiter);
app.use('/api', globalApiLimiter);

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/requests', requestRoutes);
app.use('/api/tables', tableRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/lookup', lookupRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/admin', adminRoutes);

// Serve Web static files if web/dist exists (single-port production deployment)
const distPath = process.env.CLIENT_DIST_PATH
    ? path.resolve(process.env.CLIENT_DIST_PATH)
    : path.join(__dirname, '../../web/dist');
if (fs.existsSync(distPath)) {
    app.use(express.static(distPath));
    // SPA fallback for client-side routing (Express 5 compatible)
    app.use((req, res) => {
        res.sendFile(path.join(distPath, 'index.html'));
    });
} else {
    // 404 Handler for API / missing static build
    app.use((req, res) => {
        res.status(404).json({ error: 'Endpoint not found' });
    });
}

// Global Error Handler
app.use((err, req, res, next) => {
    console.error('Unhandled server error:', err);
    res.status(500).json({ error: 'Internal server error' });
});

// Start Server after Database Initialization
const startServer = async () => {
    try {
        await initDb();
        server.listen(PORT, () => {
            console.log(`BankPoker server is running on http://localhost:${PORT}`);
            console.log(`Health check: http://localhost:${PORT}/api/health`);
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
};

startServer();

module.exports = { app, server };
