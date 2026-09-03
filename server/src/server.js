const express = require('express');
const cors = require('cors');
const path = require('path');
const dotenv = require('dotenv');

// Load environment variables from server/.env
dotenv.config({ path: path.resolve(__dirname, '../.env') });

const { initDb } = require('./database/db');
const authRoutes = require('./routes/auth');
const groupRoutes = require('./routes/groups');
const requestRoutes = require('./routes/requests');
const tableRoutes = require('./routes/tables');
const syncRoutes = require('./routes/sync');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Health Check Endpoint
app.get('/api/health', (req, res) => {
    res.status(200).json({
        status: 'ok',
        timestamp: Date.now()
    });
});

const fs = require('fs');

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/requests', requestRoutes);
app.use('/api/tables', tableRoutes);
app.use('/api/sync', syncRoutes);

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
        app.listen(PORT, () => {
            console.log(`BankPoker server is running on http://localhost:${PORT}`);
            console.log(`Health check: http://localhost:${PORT}/api/health`);
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
};

startServer();

module.exports = app;
