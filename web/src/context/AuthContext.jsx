import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore authentication state from localStorage on initial load
  useEffect(() => {
    try {
      const storedToken = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      }
    } catch (err) {
      console.error('Failed to parse stored user data:', err);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Log in user with username and password
   */
  const login = async (username, password) => {
    const response = await api.post('/api/auth/login', { username, password });
    const { token: receivedToken, user: receivedUser } = response.data;

    localStorage.setItem('token', receivedToken);
    localStorage.setItem('user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);

    return receivedUser;
  };

  /**
   * Register a new user
   */
  const register = async (username, password, displayName = null, avatarId = null) => {
    const response = await api.post('/api/auth/register', {
      username,
      password,
      display_name: displayName,
      avatar_id: avatarId
    });
    return response.data;
  };

  /**
   * Quick 1-tap Guest join
   */
  const guestJoin = async (displayName, avatarId = null) => {
    const response = await api.post('/api/auth/guest', {
      display_name: displayName,
      avatar_id: avatarId
    });
    const { token: receivedToken, user: receivedUser } = response.data;

    localStorage.setItem('token', receivedToken);
    localStorage.setItem('user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);

    return receivedUser;
  };

  /**
   * Permanently activate a guest account
   */
  const activateAccount = async (username, password) => {
    const response = await api.post('/api/auth/activate', { username, password });
    const { token: receivedToken, user: receivedUser } = response.data;

    localStorage.setItem('token', receivedToken);
    localStorage.setItem('user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);

    return receivedUser;
  };

  /**
   * Update profile (display name, avatar, optional username)
   */
  const updateProfile = async (displayName, avatarId, username = null) => {
    const response = await api.put('/api/auth/profile', {
      display_name: displayName,
      avatar_id: avatarId,
      username
    });
    const updatedUser = response.data;

    localStorage.setItem('user', JSON.stringify(updatedUser));
    setUser(updatedUser);

    return updatedUser;
  };

  /**
   * Refresh current user profile from server
   */
  const fetchCurrentUser = async () => {
    try {
      const response = await api.get('/api/auth/me');
      const currentUser = response.data;
      localStorage.setItem('user', JSON.stringify(currentUser));
      setUser(currentUser);
      return currentUser;
    } catch (err) {
      console.error('Failed to fetch current user:', err);
    }
  };

  /**
   * Log out and clear state and storage
   */
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        guestJoin,
        activateAccount,
        updateProfile,
        fetchCurrentUser,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
