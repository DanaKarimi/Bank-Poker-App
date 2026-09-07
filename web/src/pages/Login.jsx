import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, User, LogIn, UserPlus, Sparkles, AlertCircle, CheckCircle } from 'lucide-react';
import { PokerAvatar, AvatarPickerModal } from '../components/AvatarSystem';

const Login = () => {
  const [activeTab, setActiveTab] = useState('guest'); // 'guest' | 'login' | 'register'
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [avatarId, setAvatarId] = useState('avatar_1');
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);

  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { user, login, register, guestJoin } = useAuth();
  const navigate = useNavigate();

  // If already logged in, redirect to dashboard
  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setIsSubmitting(true);

    try {
      if (activeTab === 'guest') {
        if (!displayName.trim()) {
          setError('Please enter your display name.');
          setIsSubmitting(false);
          return;
        }
        await guestJoin(displayName.trim(), avatarId);
        navigate('/dashboard');
      } else if (activeTab === 'login') {
        if (!username.trim() || !password.trim()) {
          setError('Please enter both username and password.');
          setIsSubmitting(false);
          return;
        }
        await login(username.trim(), password);
        navigate('/dashboard');
      } else if (activeTab === 'register') {
        if (!username.trim() || !password.trim()) {
          setError('Please enter both username and password.');
          setIsSubmitting(false);
          return;
        }
        await register(
          username.trim(),
          password,
          displayName.trim() || username.trim(),
          avatarId
        );
        setSuccessMsg('Account created successfully! Logging you in...');
        await login(username.trim(), password);
        navigate('/dashboard');
      }
    } catch (err) {
      console.error('Auth error:', err);
      const serverMsg =
        err.response?.data?.error ||
        (activeTab === 'guest'
          ? 'Guest join failed.'
          : activeTab === 'register'
          ? 'Registration failed.'
          : 'Invalid credentials.');
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
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-felt-dark border-2 border-gold-accent mb-3 shadow-lg">
            <span className="text-3xl text-gold-accent select-none">♠</span>
          </div>
          <h1 className="text-3xl font-extrabold text-gold-accent tracking-widest uppercase">
            BankPoker
          </h1>
          <p className="text-xs text-cream-text/75 mt-1 font-medium">
            Fast, secure poker ledger & live tables
          </p>
        </div>

        {/* Tab Selector */}
        <div className="grid grid-cols-3 gap-1 bg-felt-dark p-1 rounded-xl border border-gold-accent/30 mb-6">
          <button
            type="button"
            onClick={() => {
              setActiveTab('guest');
              setError('');
            }}
            className={`py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition cursor-pointer ${
              activeTab === 'guest'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            Quick Guest
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab('login');
              setError('');
            }}
            className={`py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition cursor-pointer ${
              activeTab === 'login'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab('register');
              setError('');
            }}
            className={`py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition cursor-pointer ${
              activeTab === 'register'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            Register
          </button>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}
        {successMsg && (
          <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-lg flex items-center gap-2 text-green-200 text-xs">
            <CheckCircle className="w-4 h-4 shrink-0 text-green-400" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {activeTab === 'guest' && (
            <>
              <p className="text-xs text-cream-text/70 text-center mb-2">
                Join with 1-click without a password. You can upgrade to a permanent account at any time!
              </p>

              {/* Avatar Selector */}
              <div className="flex items-center justify-center gap-3 p-3 bg-felt-dark/60 rounded-xl border border-gold-accent/20">
                <div
                  onClick={() => setShowAvatarPicker(true)}
                  className="cursor-pointer hover:scale-105 transition"
                  title="Choose Avatar"
                >
                  <PokerAvatar avatarId={avatarId} name={displayName || 'Guest'} size={52} />
                </div>
                <button
                  type="button"
                  onClick={() => setShowAvatarPicker(true)}
                  className="text-xs text-gold-accent hover:underline font-semibold cursor-pointer"
                >
                  Change Avatar (24 styles)
                </button>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Your Table Name
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="e.g. Mike McDermott"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {activeTab === 'login' && (
            <>
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Username
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Enter username"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
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
                    <Lock className="w-4 h-4" />
                  </div>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {activeTab === 'register' && (
            <>
              {/* Avatar Selector */}
              <div className="flex items-center justify-center gap-3 p-3 bg-felt-dark/60 rounded-xl border border-gold-accent/20">
                <div
                  onClick={() => setShowAvatarPicker(true)}
                  className="cursor-pointer hover:scale-105 transition"
                  title="Choose Avatar"
                >
                  <PokerAvatar avatarId={avatarId} name={displayName || username} size={52} />
                </div>
                <button
                  type="button"
                  onClick={() => setShowAvatarPicker(true)}
                  className="text-xs text-gold-accent hover:underline font-semibold cursor-pointer"
                >
                  Choose Avatar (24 styles)
                </button>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Display Name
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Your table name"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Unique Username
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gold-accent/70">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="e.g. ace_player"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
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
                    <Lock className="w-4 h-4" />
                  </div>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-10 pr-4 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent transition"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider rounded-xl shadow-lg hover:opacity-95 active:scale-[0.98] transition flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer text-sm mt-2"
          >
            {isSubmitting ? (
              <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
            ) : activeTab === 'guest' ? (
              <>
                <Sparkles className="w-4 h-4" />
                <span>Join Now as Guest</span>
              </>
            ) : activeTab === 'register' ? (
              <>
                <UserPlus className="w-4 h-4" />
                <span>Create Permanent Account</span>
              </>
            ) : (
              <>
                <LogIn className="w-4 h-4" />
                <span>Sign In</span>
              </>
            )}
          </button>
        </form>
      </div>

      {showAvatarPicker && (
        <AvatarPickerModal
          selectedAvatarId={avatarId}
          onSelect={(id) => setAvatarId(id)}
          onClose={() => setShowAvatarPicker(false)}
        />
      )}
    </div>
  );
};

export default Login;
