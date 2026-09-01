import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api';
import { Users, Plus, LogOut, RefreshCw, ChevronRight, Key, AlertCircle, CheckCircle, X } from 'lucide-react';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Join Group Modal State
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [inviteCode, setInviteCode] = useState('');
  const [joinError, setJoinError] = useState('');
  const [joinSuccess, setJoinSuccess] = useState('');
  const [isJoining, setIsJoining] = useState(false);

  const fetchGroups = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/groups/my-groups');
      setGroups(response.data.groups || []);
    } catch (err) {
      console.error('Failed to fetch groups:', err);
      setError('Unable to load your groups. Please check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleJoinGroup = async (e) => {
    e.preventDefault();
    setJoinError('');
    setJoinSuccess('');

    if (!inviteCode.trim()) {
      setJoinError('Please enter an invite code.');
      return;
    }

    setIsJoining(true);
    try {
      const response = await api.post('/api/groups/join', {
        invite_code: inviteCode.trim(),
      });
      const joinedGroup = response.data?.group;
      const groupId = joinedGroup?.id;

      setJoinSuccess(response.data.message || 'Joined group successfully!');
      setInviteCode('');
      
      setTimeout(() => {
        setIsJoinModalOpen(false);
        setJoinSuccess('');
        if (groupId) {
          navigate(`/group/${groupId}/claim`);
        } else {
          fetchGroups();
        }
      }, 600);
    } catch (err) {
      console.error('Join group error:', err);
      setJoinError(err.response?.data?.error || 'Failed to join group. Check the invite code.');
    } finally {
      setIsJoining(false);
    }
  };

  return (
    <div className="min-h-screen bg-felt-green text-cream-text flex flex-col">
      {/* Top Navigation Bar */}
      <header className="bg-felt-dark/95 border-b border-gold-accent/40 sticky top-0 z-30 shadow-lg backdrop-blur-md">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl text-gold-accent select-none">♠</span>
            <span className="font-extrabold text-xl tracking-wider text-gold-accent uppercase">
              BankPoker
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 bg-felt-card px-3 py-1.5 rounded-xl border border-gold-accent/30 text-sm">
              <span className="text-cream-text/70">Player:</span>
              <span className="font-bold text-gold-accent">{user?.username}</span>
            </div>

            <button
              onClick={logout}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-red-900/60 hover:bg-red-800 text-red-200 hover:text-white rounded-xl border border-red-500/40 text-sm font-semibold transition active:scale-95"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-6xl w-full mx-auto px-4 py-8 flex-1">
        {/* Welcome & Action Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 bg-felt-card/80 p-6 rounded-2xl border border-gold-accent/50 shadow-xl">
          <div>
            <h1 className="text-2xl sm:text-3xl font-black text-gold-accent tracking-wide flex items-center gap-2">
              Welcome back, {user?.username}
            </h1>
            <p className="text-sm text-cream-text/75 mt-1">
              Select a poker group below to view your balance, buy-ins, and performance history.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchGroups}
              disabled={loading}
              title="Refresh groups"
              className="p-3 bg-felt-dark hover:bg-felt-dark/80 text-gold-accent rounded-xl border border-gold-accent/40 shadow transition active:scale-95 disabled:opacity-50"
            >
              <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
            </button>

            <button
              onClick={() => {
                setIsJoinModalOpen(true);
                setJoinError('');
                setJoinSuccess('');
              }}
              className="px-5 py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider text-sm rounded-xl shadow-lg hover:opacity-95 active:scale-95 transition flex items-center gap-2"
            >
              <Plus className="w-5 h-5" />
              <span>Join Group</span>
            </button>
          </div>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="mb-6 p-4 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-3 text-red-200">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Groups Grid */}
        {loading && groups.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-12 h-12 border-4 border-gold-accent border-t-transparent rounded-full animate-spin"></div>
            <p className="mt-4 text-gold-accent font-medium tracking-wide">Loading your poker groups...</p>
          </div>
        ) : groups.length === 0 ? (
          <div className="bg-felt-card/50 border border-dashed border-gold-accent/40 rounded-2xl p-12 text-center">
            <Users className="w-16 h-16 text-gold-accent/40 mx-auto mb-3" />
            <h3 className="text-xl font-bold text-cream-text">No Groups Joined Yet</h3>
            <p className="text-sm text-cream-text/60 max-w-md mx-auto mt-1 mb-6">
              You haven't joined any poker groups yet. Ask your group admin for an invite code or join an existing group.
            </p>
            <button
              onClick={() => setIsJoinModalOpen(true)}
              className="px-6 py-2.5 bg-gold-accent text-black font-bold rounded-xl shadow hover:bg-gold-light transition"
            >
              Join with Invite Code
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {groups.map((group) => (
              <div
                key={group.id}
                className="bg-felt-card/90 border-2 border-gold-accent/60 rounded-2xl p-6 shadow-xl hover:border-gold-accent hover:shadow-2xl transition duration-200 flex flex-col justify-between group"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-2">
                      <span className="text-gold-accent text-xl">♣</span>
                      <h3 className="font-extrabold text-xl text-cream-text group-hover:text-gold-light transition line-clamp-1">
                        {group.name}
                      </h3>
                    </div>

                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider border ${
                        group.mode === 'ONLINE'
                          ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/50'
                          : 'bg-felt-dark text-cream-text/60 border-gold-accent/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          group.mode === 'ONLINE' ? 'bg-emerald-400 animate-pulse' : 'bg-cream-text/40'
                        }`}
                      />
                      <span>{group.mode || 'OFFLINE'}</span>
                    </span>
                  </div>

                  {group.invite_code && (
                    <div className="flex items-center gap-1.5 text-xs text-cream-text/70 bg-felt-dark/80 px-3 py-1.5 rounded-lg border border-gold-accent/20 w-fit mb-4">
                      <Key className="w-3.5 h-3.5 text-gold-accent" />
                      <span>Code:</span>
                      <span className="font-mono font-bold text-gold-accent">{group.invite_code}</span>
                    </div>
                  )}
                </div>

                <div className="pt-4 border-t border-gold-accent/20 mt-4">
                  <Link
                    to={`/group/${group.id}`}
                    state={{ group }}
                    className="w-full py-2.5 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent font-bold uppercase tracking-wider text-xs rounded-xl border border-gold-accent/50 shadow flex items-center justify-center gap-1.5 transition active:scale-95"
                  >
                    <span>{group.mode === 'ONLINE' ? 'Enter Table & Requests' : 'View Stats'}</span>
                    <ChevronRight className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Join Group Modal */}
      {isJoinModalOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <button
              onClick={() => setIsJoinModalOpen(false)}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-2 mb-4">
              <Key className="w-6 h-6 text-gold-accent" />
              <h2 className="text-xl font-bold text-gold-accent">Join Poker Group</h2>
            </div>

            <p className="text-xs text-cream-text/75 mb-4">
              Enter the unique invite code provided by your group organizer or table manager.
            </p>

            {joinError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{joinError}</span>
              </div>
            )}

            {joinSuccess && (
              <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-lg flex items-center gap-2 text-green-200 text-xs">
                <CheckCircle className="w-4 h-4 shrink-0 text-green-400" />
                <span>{joinSuccess}</span>
              </div>
            )}

            <form onSubmit={handleJoinGroup} className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Invite Code
                </label>
                <input
                  type="text"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                  placeholder="e.g. POKER123"
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text font-mono placeholder-cream-text/40 focus:outline-none focus:border-gold-accent uppercase transition"
                  required
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsJoinModalOpen(false)}
                  className="px-4 py-2.5 bg-felt-dark hover:bg-felt-dark/80 text-cream-text/80 rounded-xl text-sm font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isJoining}
                  className="px-5 py-2.5 bg-gold-accent text-black font-bold uppercase tracking-wider text-sm rounded-xl shadow hover:bg-gold-light disabled:opacity-50 transition"
                >
                  {isJoining ? 'Joining...' : 'Join Group'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
