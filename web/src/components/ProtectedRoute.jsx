import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route wrapper that redirects unauthenticated users to the Login page
 */
const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-felt-green text-cream-text">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-gold-accent border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gold-accent font-semibold tracking-wider">Loading BankPoker...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
