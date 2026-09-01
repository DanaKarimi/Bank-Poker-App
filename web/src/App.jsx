import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import GroupStats from './pages/GroupStats';
import TableDetail from './pages/TableDetail';
import ClaimPlayer from './pages/ClaimPlayer';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Login & Register Route */}
          <Route path="/" element={<Login />} />

          {/* Protected Dashboard Route */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          {/* Protected Claim Player Identity Route */}
          <Route
            path="/group/:groupId/claim"
            element={
              <ProtectedRoute>
                <ClaimPlayer />
              </ProtectedRoute>
            }
          />

          {/* Protected Group Stats & Tables Route */}
          <Route
            path="/group/:id"
            element={
              <ProtectedRoute>
                <GroupStats />
              </ProtectedRoute>
            }
          />

          {/* Protected Specific Table Detail Route */}
          <Route
            path="/group/:groupId/table/:tableId"
            element={
              <ProtectedRoute>
                <TableDetail />
              </ProtectedRoute>
            }
          />

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
