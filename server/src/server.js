const express = require('express');
const http = require('http');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const dotenv = require('dotenv');

// Load environment variables from server/.env
dotenv.config({ path: path.resolve(__dirname, '../.env') });

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

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 3000;

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
