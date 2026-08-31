import axios from 'axios';

// Base API instance connecting to backend server
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

const api = axios.create({
  baseURL: API_BASE_URL,
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

// Interceptor to handle global response errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    return Promise.reject(error);
  }
);

// --- Request System APIs ---

// Join requests
export const sendJoinRequest = (groupId) => api.post('/api/requests/join', { groupId });
export const getMyRequests = (groupId) => api.get('/api/requests/my', { params: { groupId } });

// Buy-in requests
export const sendBuyInRequest = (groupId, tableId, amount) =>
  api.post('/api/requests/buy-in', { groupId, tableId, amount: Number(amount) });

// Exit requests
export const sendExitRequest = (groupId, tableId, amount) =>
  api.post('/api/requests/exit', { groupId, tableId, amount: Number(amount) });

// Confirm receipt
export const confirmBuyInReceipt = (requestId) => api.post(`/api/requests/buy-in/${requestId}/confirm`);
export const confirmExitReceipt = (requestId) => api.post(`/api/requests/exit/${requestId}/confirm`);

// Group and Table helpers
export const getMyGroups = () => api.get('/api/groups/my-groups');
export const getGroupStats = (groupId) => api.get(`/api/groups/${groupId}/my-stats`);
export const getTables = (groupId) => api.get(`/api/groups/${groupId}/tables`);
export const getGroupTables = async (groupId) => {
  try {
    const response = await api.get(`/api/groups/${groupId}/tables`);
    return response.data?.tables || [];
  } catch (err) {
    console.error('Failed to fetch group tables:', err);
    return [];
  }
};

export default api;
