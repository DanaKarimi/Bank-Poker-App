import { io } from 'socket.io-client';

// Determine Socket.IO server URL
const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || import.meta.env.VITE_API_URL || window.location.origin;

let socket = null;
const activeGroupRooms = new Set();
const activeTableRooms = new Set();

/**
 * Initialize and return the singleton Socket.IO client
 */
export const getSocket = () => {
  if (!socket) {
    const token = localStorage.getItem('token') || '';

    socket = io(SOCKET_URL, {
      transports: ['websocket', 'polling'],
      autoConnect: true,
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      timeout: 20000,
      auth: {
        token: token,
      },
      query: {
        token: token,
      },
    });

    socket.on('connect', () => {
      console.log('[Socket] Connected to server:', socket.id);
      // Automatically rejoin any active group/table rooms upon reconnection
      activeGroupRooms.forEach((groupId) => {
        socket.emit('join_group', groupId);
      });
      activeTableRooms.forEach((tableId) => {
        socket.emit('join_table', tableId);
      });
    });

    socket.on('disconnect', (reason) => {
      console.log('[Socket] Disconnected from server:', reason);
    });

    socket.on('connect_error', (error) => {
      console.warn('[Socket] Connection error:', error.message);
    });
  }

  return socket;
};

/**
 * Update authentication token for the socket connection (e.g. after login/logout)
 */
export const updateSocketAuth = (newToken) => {
  const token = newToken || '';
  const s = getSocket();
  s.auth = { token };
  s.io.opts.query = { token };

  if (s.connected) {
    s.disconnect().connect();
  }
};

/**
 * Join a group room to receive realtime group-level events
 */
export const joinGroup = (groupId) => {
  if (!groupId) return;
  activeGroupRooms.add(groupId);
  const s = getSocket();
  if (s.connected) {
    s.emit('join_group', groupId);
  }
};

/**
 * Leave a group room
 */
export const leaveGroup = (groupId) => {
  if (!groupId) return;
  activeGroupRooms.delete(groupId);
  const s = getSocket();
  if (s.connected) {
    s.emit('leave_group', groupId);
  }
};

/**
 * Join a table room to receive realtime table-level events
 */
export const joinTable = (tableId) => {
  if (!tableId) return;
  activeTableRooms.add(tableId);
  const s = getSocket();
  if (s.connected) {
    s.emit('join_table', tableId);
  }
};

/**
 * Leave a table room
 */
export const leaveTable = (tableId) => {
  if (!tableId) return;
  activeTableRooms.delete(tableId);
  const s = getSocket();
  if (s.connected) {
    s.emit('leave_table', tableId);
  }
};

export default getSocket;
