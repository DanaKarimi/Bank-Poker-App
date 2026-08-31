import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, User, LogIn, UserPlus, AlertCircle, CheckCircle } from 'lucide-react';

const Login = () => {
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { user, login, register } = useAuth();
  const navigate = useNavigate();

  // If already logged in, redirect to dashboard
  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    if (!username.trim() || !password.trim()) {
      setError('Please enter both username and password.');
      return;
    }

    setIsSubmitting(true);
    try {
      if (isRegisterMode) {
        // Register flow
        await register(username.trim(), password);
        setSuccessMsg('Account created successfully! Logging you in...');
        // Auto-login after registration
        await login(username.trim(), password);
        navigate('/dashboard');
      } else {
        // Login flow
        await login(username.trim(), password);
        navigate('/dashboard');
      }
    } catch (err) {
      console.error('Auth error:', err);
      const serverMsg = err.response?.data?.error || (isRegisterMode ? 'Registration failed.' : 'Invalid credentials.');
      setError(serverMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden bg-felt-green">
      {/* Subtle Suit Background Accents */}
      <div className="absolute -top-10 -left-10 text-9xl text-black opacity-10 select-none pointer-events-none">♠</div>
      <div className="absolute -bottom-10 -right-10 text-9xl text-black opacity-10 select-none pointer-events-none">♣</div>
      <div className="absolute top-1/4 -right-12 text-8xl text-red-900 opacity-10 select-none pointer-events-none">♥</div>
      <div className="absolute bottom-1/4 -left-12 text-8xl text-red-900 opacity-10 select-none pointer-events-none">♦</div>

      {/* Main Card */}
      <div className="w-full max-w-md bg-felt-card/90 backdrop-blur-sm border-2 border-gold-accent rounded-2xl shadow-2xl p-8 z-10">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-felt-dark border-2 border-gold-accent mb-3 shadow-lg">
            <span className="text-3xl text-gold-accent select-none">♠</span>
          </div>
          <h1 className="text-3xl font-extrabold text-gold-accent tracking-widest uppercase">
            BankPoker
          </h1>
          <p className="text-sm text-cream-text/75 mt-1 font-medium">
            {isRegisterMode ? 'Create a player account' : 'Sign in to your player account'}
          </p>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}
        {successMsg && (
          <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-lg flex items-center gap-2 text-green-200 text-sm">
            <CheckCircle className="w-5 h-5 shrink-0 text-green-400" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
              Username
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                <User className="w-5 h-5" />
              </div>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                className="w-full pl-10 pr-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 focus:outline-none focus:border-gold-accent focus:ring-1 focus:ring-gold-accent transition"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
              Password
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                <Lock className="w-5 h-5" />
              </div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 focus:outline-none focus:border-gold-accent focus:ring-1 focus:ring-gold-accent transition"
                required
              />
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider rounded-xl shadow-lg hover:opacity-95 active:scale-[0.98] transition flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {isSubmitting ? (
              <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
            ) : isRegisterMode ? (
              <>
                <UserPlus className="w-5 h-5" />
                <span>Register</span>
              </>
            ) : (
              <>
                <LogIn className="w-5 h-5" />
                <span>Login</span>
              </>
            )}
          </button>
        </form>

        {/* Mode Toggle */}
        <div className="mt-6 pt-5 border-t border-gold-accent/20 text-center">
          <p className="text-sm text-cream-text/80">
            {isRegisterMode ? 'Already have an account?' : "Don't have an account yet?"}
            {' '}
            <button
              type="button"
              onClick={() => {
                setIsRegisterMode(!isRegisterMode);
                setError('');
                setSuccessMsg('');
              }}
              className="text-gold-accent font-bold underline hover:text-gold-light transition ml-1"
            >
              {isRegisterMode ? 'Sign In' : 'Create One'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
