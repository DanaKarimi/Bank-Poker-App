import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getGroupPlayersList, claimPlayer, joinNewPlayer } from '../api';
import { ArrowLeft, UserCheck, UserPlus, AlertCircle, CheckCircle2, Shield } from 'lucide-react';

const ClaimPlayer = () => {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [groupName, setGroupName] = useState('Poker Club');
  const [players, setPlayers] = useState([]);
  const [newPlayerName, setNewPlayerName] = useState(user?.username || '');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchPlayers();
  }, [groupId]);

  const fetchPlayers = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await getGroupPlayersList(groupId);
      setGroupName(response.data?.groupName || 'Poker Group');
      setPlayers(response.data?.players || []);
    } catch (err) {
      console.error('Failed to fetch players list:', err);
      setError(err.response?.data?.error || 'Failed to load group player roster.');
    } finally {
      setLoading(false);
    }
  };

  const handleClaim = async (player) => {
    if (submitting) return;
    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await claimPlayer(groupId, {
        playerId: player.id,
        playerName: player.name,
      });
      setSuccess(`Welcome back! You claimed "${player.name}". Redirecting...`);
      setTimeout(() => {
        navigate(`/group/${groupId}`);
      }, 1200);
    } catch (err) {
      console.error('Failed to claim player:', err);
      setError(err.response?.data?.error || 'Failed to claim player identity.');
      setSubmitting(false);
    }
  };

  const handleJoinNew = async (e) => {
    e.preventDefault();
    if (submitting) return;

    const trimmedName = newPlayerName.trim();
    if (!trimmedName) {
      setError('Please enter a player name.');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await joinNewPlayer(groupId, {
        playerName: trimmedName,
      });
      setSuccess(`Joined group as "${trimmedName}"! Redirecting...`);
      setTimeout(() => {
        navigate(`/group/${groupId}`);
      }, 1200);
    } catch (err) {
      console.error('Failed to join as new player:', err);
      setError(err.response?.data?.error || 'Failed to join group.');
      setSubmitting(false);
    }
  };

  const unclaimedPlayers = players.filter((p) => !p.isClaimed);

  return (
    <div className="min-h-screen bg-felt-dark text-cream-text flex flex-col items-center justify-center py-8 px-4 sm:px-6">
      <div className="w-full max-w-lg space-y-6">
        {/* Navigation / Header */}
        <div className="flex items-center justify-between">
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 px-3.5 py-2 bg-felt-card/80 hover:bg-felt-card border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Dashboard</span>
          </Link>
        </div>

        {/* Hero Card */}
        <div className="bg-felt-card border-2 border-gold-accent rounded-2xl p-6 shadow-2xl relative text-center">
          <div className="w-12 h-12 rounded-2xl bg-felt-dark border border-gold-accent/40 flex items-center justify-center text-xl text-gold-accent mx-auto mb-3 shadow-inner">
            ♠
          </div>
          <h1 className="text-2xl font-black tracking-tight text-cream-text">
            Join {groupName}
          </h1>
          <p className="text-xs text-cream-text/70 mt-1">
            Are you already in this group, or joining as a new player?
          </p>

          {/* Feedback messages */}
          {error && (
            <div className="mt-4 p-3 bg-red-950/80 border border-red-500 rounded-xl text-xs text-red-200 flex items-center gap-2 text-left">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="mt-4 p-3 bg-emerald-950/80 border border-emerald-500 rounded-xl text-xs text-emerald-200 flex items-center gap-2 text-left animate-pulse">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
              <span>{success}</span>
            </div>
          )}
        </div>

        {loading ? (
          <div className="p-8 bg-felt-card border border-gold-accent/20 rounded-2xl text-center text-xs text-cream-text/50">
            Loading roster...
          </div>
        ) : (
          <div className="space-y-6">
            {/* OPTION A: Claim existing player identity */}
            {unclaimedPlayers.length > 0 && (
              <div className="bg-felt-card border border-gold-accent/50 rounded-2xl p-5 shadow-xl space-y-3">
                <div className="flex items-center gap-2">
                  <UserCheck className="w-4 h-4 text-gold-accent" />
                  <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent">
                    Select Your Name (Existing Player)
                  </h2>
                </div>
                <p className="text-xs text-cream-text/60">
                  Tap your name below to claim your past game history and balance.
                </p>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 pt-1">
                  {unclaimedPlayers.map((player) => (
                    <button
                      key={player.id}
                      type="button"
                      disabled={submitting}
                      onClick={() => handleClaim(player)}
                      className="p-3 bg-[#043327] hover:bg-gold-accent hover:text-black border-2 border-gold-accent/50 hover:border-gold-accent rounded-xl font-bold text-sm text-[#f5f5dc] shadow-md hover:scale-105 active:scale-95 transition-all duration-150 flex flex-col items-center justify-center gap-1 cursor-pointer disabled:opacity-50"
                    >
                      <span className="font-mono text-xs uppercase text-gold-accent/70 group-hover:text-black">
                        Player
                      </span>
                      <span className="font-black text-center truncate max-w-full">
                        {player.name}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Divider if unclaimed players exist */}
            {unclaimedPlayers.length > 0 && (
              <div className="flex items-center gap-3 px-4">
                <div className="flex-1 h-px bg-gold-accent/30" />
                <span className="text-xs font-bold uppercase tracking-widest text-gold-accent/60">
                  OR
                </span>
                <div className="flex-1 h-px bg-gold-accent/30" />
              </div>
            )}

            {/* OPTION B: Join as a New Player */}
            <div className="bg-felt-card border border-gold-accent/50 rounded-2xl p-5 shadow-xl space-y-3">
              <div className="flex items-center gap-2">
                <UserPlus className="w-4 h-4 text-gold-accent" />
                <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent">
                  I'm a New Player
                </h2>
              </div>
              <p className="text-xs text-cream-text/60">
                Join with a fresh player profile in this group.
              </p>

              <form onSubmit={handleJoinNew} className="space-y-3 pt-1">
                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-cream-text/70 mb-1">
                    Your In-Game Name
                  </label>
                  <input
                    type="text"
                    value={newPlayerName}
                    onChange={(e) => setNewPlayerName(e.target.value)}
                    placeholder="Enter your name"
                    className="w-full px-4 py-2.5 bg-[#043327] border border-gold-accent/50 focus:border-gold-accent rounded-xl text-cream-text font-bold placeholder-cream-text/40 outline-none transition"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={submitting || !newPlayerName.trim()}
                  className="w-full py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent hover:opacity-95 active:scale-[0.98] text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg transition disabled:opacity-50 flex items-center justify-center cursor-pointer"
                >
                  {submitting ? 'Creating Profile...' : 'Create New Player & Enter'}
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ClaimPlayer;
