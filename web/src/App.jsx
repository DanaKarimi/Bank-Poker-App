import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import GroupStats from './pages/GroupStats';
import TableDetail from './pages/TableDetail';

const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));

const PageLoader = () => (
  <div className="min-h-screen bg-felt-dark flex flex-col items-center justify-center p-6 text-center">
    <div className="w-12 h-12 rounded-2xl bg-felt-card border border-gold-accent/50 flex items-center justify-center text-2xl text-gold-accent mb-3 shadow-lg">
      ♠
    </div>
    <div className="text-xs font-extrabold uppercase tracking-widest text-gold-accent animate-pulse">
      Loading BankPoker...
    </div>
  </div>
);

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<PageLoader />}>
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
            <Route
              path="/table/:tableId"
              element={
                <ProtectedRoute>
                  <TableDetail />
                </ProtectedRoute>
              }
            />
            {/* Protected Super Admin Route */}
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
