const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_bankpoker_key_change_in_production';

let io = null;

function initSocket(httpServer) {
    io = new Server(httpServer, {
        cors: {
            origin: '*',
            methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
        },
        transports: ['polling', 'websocket'] // automatic polling fallback
    });

    // JWT authentication middleware (soft rollout mode: allow connection even if unauthenticated)
    io.use((socket, next) => {
        next();
    });

    io.on('connection', (socket) => {
        // Extract token from handshake auth or query
        const token = socket.handshake.auth?.token || socket.handshake.query?.token;
        if (token) {
            try {
                const decoded = jwt.verify(token, JWT_SECRET);
                socket.data = socket.data || {};
                socket.data.user = decoded;
                socket.user = decoded; // backward compatibility
            } catch (err) {
                console.warn('WARNING: Socket connected without valid auth token');
                socket.data = socket.data || {};
                socket.data.user = null;
                socket.user = null;
            }
        } else {
            console.warn('WARNING: Socket connected without valid auth token');
            socket.data = socket.data || {};
            socket.data.user = null;
            socket.user = null;
        }

        const currentUser = socket.data?.user || socket.user;
        if (currentUser && currentUser.id) {
            socket.join(`user:${currentUser.id}`);
        }

        socket.on('join_group', (groupId) => {
            if (groupId) {
                socket.join(`group:${groupId}`);
            }
        });

        socket.on('leave_group', (groupId) => {
            if (groupId) {
                socket.leave(`group:${groupId}`);
            }
        });

        socket.on('join_table', (tableId) => {
            if (tableId) {
                socket.join(`table:${tableId}`);
            }
        });

        socket.on('leave_table', (tableId) => {
            if (tableId) {
                socket.leave(`table:${tableId}`);
            }
        });

        socket.on('disconnect', () => {
            // connection cleaned up automatically by socket.io
        });
    });

    return io;
}

function getIO() {
    return io;
}

function emitToGroup(groupId, event, data) {
    if (io && groupId) {
        io.to(`group:${groupId}`).emit(event, data);
    }
}

function emitToTable(tableId, event, data) {
    if (io && tableId) {
        io.to(`table:${tableId}`).emit(event, data);
    }
}

function emitToUser(userId, event, data) {
    if (io && userId) {
        io.to(`user:${userId}`).emit(event, data);
    }
}

function emitGlobal(event, data) {
    if (io) {
        io.emit(event, data);
    }
}

module.exports = {
    initSocket,
    getIO,
    emitToGroup,
    emitToTable,
    emitToUser,
    emitGlobal
};
