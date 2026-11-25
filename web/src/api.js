import axios from 'axios';

// Base API instance connecting to backend server (empty string for same-origin production deployment)
const API_URL = import.meta.env.VITE_API_URL || '';

const api = axios.create({
  baseURL: API_URL,
  timeout: 12000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach JWT token from localStorage to Authorization header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Auto-retry transient network/timeout/502-504 errors on idempotent GET requests
const RETRY_DELAYS_MS = [400, 900];
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error?.config;
    const method = (config?.method || '').toLowerCase();
    const status = error?.response?.status;
    const isRetryable =
      method === 'get' &&
      (!error.response ||
        error.code === 'ECONNABORTED' ||
        error.code === 'ERR_NETWORK' ||
        status === 502 ||
        status === 503 ||
        status === 504);

    if (config && isRetryable) {
      const attempt = config.__retryCount || 0;
      if (attempt < RETRY_DELAYS_MS.length) {
        config.__retryCount = attempt + 1;
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAYS_MS[attempt]));
        return api(config);
      }
    }
    return Promise.reject(error);
  }
);

// --- Request System APIs ---

// Join table requests
export const sendJoinRequest = (tableId, groupId = null) => {
  if (typeof tableId === 'object') {
    return api.post('/api/requests/join', tableId);
  }
  return api.post('/api/requests/join', { tableId, groupId });
};

export const getMyRequests = (groupId, tableId = null) =>
  api.get('/api/requests/my', { params: { groupId, tableId } });

export const getMyJoinRequests = (groupId) =>
  api.get('/api/requests/my', { params: { groupId } });

// Buy-in requests
export const sendBuyInRequest = (groupId, tableId, amount, note = '') =>
  api.post('/api/requests/buy-in', { groupId, tableId, amount: Number(amount), note });

// Exit requests
export const sendExitRequest = (groupId, tableId, amount, note = '') =>
  api.post('/api/requests/exit', { groupId, tableId, amount: Number(amount), note });

// Confirm receipt
export const confirmBuyInReceipt = (requestId) => api.post(`/api/requests/buy-in/${requestId}/confirm`);
export const confirmExitReceipt = (requestId) => api.post(`/api/requests/exit/${requestId}/confirm`);

// Group and Table helpers
export const getMyGroups = () => api.get('/api/groups/my-groups');
export const getGroupStats = (groupId) => api.get(`/api/groups/${groupId}/my-stats`);
export const getGroupBalances = (groupId) => api.get(`/api/groups/${groupId}/balances`);
export const getGroupSettlement = (groupId) => api.get(`/api/groups/${groupId}/settlement`);
export const getGroupSettlementPlan = (groupId) => api.get(`/api/groups/${groupId}/settlement`);
export const toggleSettlementPaid = (groupId, recordId, paid) => api.post(`/api/groups/${groupId}/settlement/${recordId}/toggle-paid`, { paid });
export const regenerateSettlementPlan = (groupId) => api.post(`/api/groups/${groupId}/settlement/regenerate`);
export const getGroupStatsDetails = (groupId) => api.get(`/api/groups/${groupId}/stats`);
export const getGroupLeaderboard = (groupId) => api.get(`/api/groups/${groupId}/leaderboard`);
export const getGroupByInvite = (code) => api.get(`/api/groups/by-invite/${encodeURIComponent(code)}`);
export const getGroupPlayersList = (groupId) => api.get(`/api/groups/${groupId}/players-list`);
export const recordGroupPayment = (groupId, data) => api.post(`/api/groups/${groupId}/payments`, data);
export const getGroupPayments = (groupId) => api.get(`/api/groups/${groupId}/payments`);
export const updateGroupPayment = (groupId, paymentId, data) => api.put(`/api/groups/${groupId}/payments/${paymentId}`, data);
export const deleteGroupPayment = (groupId, paymentId) => api.delete(`/api/groups/${groupId}/payments/${paymentId}`);
export const claimPlayer = (groupId, data) => api.post(`/api/groups/${groupId}/claim-player`, data);
export const joinNewPlayer = (groupId, data) => api.post(`/api/groups/${groupId}/join-new-player`, data);
export const getTables = (groupId) => api.get(`/api/groups/${groupId}/tables`);
export const getTableDetail = (tableId) => api.get(`/api/tables/${tableId}`);
export const getTableStatus = (tableId) => api.get(`/api/tables/${tableId}/status`);
export const closeTable = (tableId) => api.post(`/api/tables/${tableId}/close`);
export const getTableBuyIns = (tableId) => api.get(`/api/tables/${tableId}/buy-ins`);
export const getTableExits = (tableId) => api.get(`/api/tables/${tableId}/exits`);
export const getTableActivity = async (tableId) => {
  try {
    const [buyInsRes, exitsRes] = await Promise.allSettled([
      api.get(`/api/tables/${tableId}/buy-ins`),
      api.get(`/api/tables/${tableId}/exits`),
    ]);
    const buyIns = buyInsRes.status === 'fulfilled' ? (buyInsRes.value.data?.buyIns || []) : [];
    const exits = exitsRes.status === 'fulfilled' ? (exitsRes.value.data?.exits || []) : [];
    return { data: { buyIns, exits } };
  } catch (err) {
    return { data: { buyIns: [], exits: [] } };
  }
};
export const getPlayers = (tableId) => api.get(`/api/tables/${tableId}/players`);
export const directBuyIn = (tableId, data) => api.post(`/api/tables/${tableId}/buy-in-direct`, data);
export const directExit = (tableId, data) => api.post(`/api/tables/${tableId}/exit-direct`, data);
export const updateTableExit = (tableId, exitId, data) => api.put(`/api/tables/${tableId}/exits/${exitId}`, data);
export const updateTableBuyIn = (tableId, buyInId, data) => api.put(`/api/tables/${tableId}/buy-ins/${buyInId}`, data);
export const createQuickTable = (data) => api.post('/api/tables/quick', data);
export const publishTable = (tableId) => api.post(`/api/tables/${tableId}/publish`);
export const deleteTablePlayer = (tableId, playerId) => api.delete(`/api/tables/${tableId}/players/${playerId}`);
export const createGroup = (data) => api.post('/api/groups/create', data);
export const createTable = (data) => api.post('/api/tables/create', data);
export const addTablePlayer = (tableId, data) => api.post(`/api/tables/${tableId}/players`, data);
export const getTableByCode = (code) => api.get(`/api/tables/by-code/${encodeURIComponent(code)}`);
export const getActiveTables = () => api.get('/api/tables/active');

export const getGroupTables = async (groupId) => {
  try {
    const response = await api.get(`/api/groups/${groupId}/tables`);
    return response.data?.tables || [];
  } catch (err) {
    console.error('Failed to fetch group tables:', err);
    return [];
  }
};

// --- Notification APIs ---
export const getNotifications = () => api.get('/api/notifications');
export const markNotificationRead = (id) => api.put(`/api/notifications/${id}/read`);
export const markAllNotificationsRead = () => api.put('/api/notifications/read-all');
export const getNotificationSettings = () => api.get('/api/notifications/settings');
export const updateNotificationSettings = (settings) => api.put('/api/notifications/settings', { settings });

// --- Admin APIs ---
export const getAdminOverview = () => api.get('/api/admin/overview');
export const getAdminUsers = () => api.get('/api/admin/users');
export const updateAdminUserRole = (id, role) => api.put(`/api/admin/users/${id}/role`, { role });
export const deleteAdminUser = (id) => api.delete(`/api/admin/users/${id}`);
export const getAdminGroups = () => api.get('/api/admin/groups');
export const deleteAdminGroup = (id) => api.delete(`/api/admin/groups/${id}`);
export const getAdminTables = () => api.get('/api/admin/tables');
export const deleteAdminTable = (id) => api.delete(`/api/admin/tables/${id}`);
export const getAdminTablePlayers = (tableId) => api.get(`/api/admin/tables/${tableId}/players`);
export const updateAdminTablePlayer = (tableId, playerId, data) => api.put(`/api/admin/tables/${tableId}/players/${playerId}`, data);
export const getGroupEntryFees = (groupId) => api.get(`/api/groups/${groupId}/entry-fees`);
export const updateGroupEntryFee = (groupId, feeId, data) => api.put(`/api/groups/${groupId}/entry-fees/${feeId}`, data);
export const deleteGroupEntryFee = (groupId, feeId) => api.delete(`/api/groups/${groupId}/entry-fees/${feeId}`);
export const forgotPassword = (username) => api.post('/api/auth/forgot-password', { username });
export const resetPasswordWithCode = (username, resetCode, newPassword) => api.post('/api/auth/reset-password', { username, resetCode, newPassword });
export const getGroupPasswordResets = (groupId) => api.get(`/api/groups/${groupId}/password-resets`);
export const resetGroupMemberPassword = (groupId, userId, data) => api.post(`/api/groups/${groupId}/members/${userId}/reset-password`, data);
export const getGroupMembers = (groupId) => api.get(`/api/groups/${groupId}/members`);
export const removeGroupMember = (groupId, userId) => api.delete(`/api/groups/${groupId}/members/${userId}`);
export const updateGroupMemberRole = (groupId, userId, role) => api.put(`/api/groups/${groupId}/members/${userId}/role`, { role });
export const getGroupAdminOverview = (groupId) => api.get(`/api/groups/${groupId}/admin/overview`);
export const getGroupAdminTable = (groupId, tableId) => api.get(`/api/groups/${groupId}/admin/tables/${tableId}`);
export const updateGroupAdminTableStatus = (groupId, tableId, data) => api.put(`/api/groups/${groupId}/admin/tables/${tableId}/status`, data);
export const addGroupAdminTablePlayer = (groupId, tableId, data) => api.post(`/api/groups/${groupId}/admin/tables/${tableId}/players`, data);
export const deleteGroupAdminTablePlayer = (groupId, tableId, playerId) => api.delete(`/api/groups/${groupId}/admin/tables/${tableId}/players/${playerId}`);
export const addGroupAdminTableBuyIn = (groupId, tableId, data) => api.post(`/api/groups/${groupId}/admin/tables/${tableId}/buy-ins`, data);
export const updateGroupAdminTableBuyIn = (groupId, tableId, buyInId, data) => api.put(`/api/groups/${groupId}/admin/tables/${tableId}/buy-ins/${buyInId}`, data);
export const deleteGroupAdminTableBuyIn = (groupId, tableId, buyInId) => api.delete(`/api/groups/${groupId}/admin/tables/${tableId}/buy-ins/${buyInId}`);
export const addGroupAdminTableExit = (groupId, tableId, data) => api.post(`/api/groups/${groupId}/admin/tables/${tableId}/exits`, data);
export const updateGroupAdminTableExit = (groupId, tableId, exitId, data) => api.put(`/api/groups/${groupId}/admin/tables/${tableId}/exits/${exitId}`, data);
export const deleteGroupAdminTableExit = (groupId, tableId, exitId) => api.delete(`/api/groups/${groupId}/admin/tables/${tableId}/exits/${exitId}`);
export const getAdminPasswordResets = () => api.get('/api/admin/password-resets');
export const resetAdminUserPassword = (userId, data) => api.post(`/api/admin/users/${userId}/reset-password`, data);
export const getServerHealth = () => api.get('/api/health');

export default api;
