import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api';
import { ArrowLeft, RefreshCw, Trophy, TrendingUp, TrendingDown, DollarSign, Send, Download, AlertCircle } from 'lucide-react';

const GroupStats = () => {
  const { id: groupId } = useParams();
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchStats = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get(`/api/groups/${groupId}/my-stats`);
      setStats(response.data);
    } catch (err) {
      console.error('Failed to fetch personal stats:', err);
      setError(err.response?.data?.error || 'Unable to load group stats. Please check connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (groupId) {
      fetchStats();
    }
  }, [groupId]);

  const isPositiveBalance = (stats?.currentBalance || 0) > 0;
  const isNegativeBalance = (stats?.currentBalance || 0) < 0;

  return (
    <div className="min-h-screen bg-felt-green text-cream-text flex flex-col">
      {/* Top Header */}
      <header className="bg-felt-dark/95 border-b border-gold-accent/40 sticky top-0 z-30 shadow-lg backdrop-blur-md">
        <div className="max-w-4xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 text-gold-accent hover:text-gold-light font-bold text-sm transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Dashboard</span>
          </Link>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchStats}
              disabled={loading}
              title="Refresh stats"
              className="p-2 bg-felt-card hover:bg-felt-card/80 text-gold-accent rounded-xl border border-gold-accent/40 transition active:scale-95 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            <span className="text-xl text-gold-accent select-none">♠</span>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-4xl w-full mx-auto px-4 py-8 flex-1">
        {/* Error Notification */}
        {error && (
          <div className="mb-6 p-4 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-3 text-red-200">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {loading && !stats ? (
          <div className="flex flex-col items-center justify-center py-24">
            <div className="w-12 h-12 border-4 border-gold-accent border-t-transparent rounded-full animate-spin"></div>
            <p className="mt-4 text-gold-accent font-semibold tracking-wide">Calculating your group balance & stats...</p>
          </div>
        ) : stats ? (
          <div className="space-y-6">
            {/* Header Title Card */}
            <div className="bg-felt-card/90 border border-gold-accent/50 rounded-2xl p-6 shadow-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <span className="text-xs uppercase tracking-widest text-gold-accent font-bold">
                  Group Performance
                </span>
                <h1 className="text-2xl sm:text-3xl font-black text-cream-text tracking-wide mt-0.5">
                  {stats.groupName || 'Poker Group'}
                </h1>
              </div>
              <div className="bg-felt-dark px-4 py-2 rounded-xl border border-gold-accent/30 self-start sm:self-auto">
                <span className="text-xs text-cream-text/60">Player: </span>
                <span className="font-bold text-gold-accent">{stats.username || user?.username}</span>
              </div>
            </div>

            {/* Primary Balance Hero Card */}
            <div className="bg-felt-card border-2 border-gold-accent rounded-3xl p-8 shadow-2xl relative overflow-hidden text-center">
              {/* Background accents */}
              <div className="absolute top-2 right-4 text-8xl text-gold-accent/5 select-none pointer-events-none">♠</div>
              <div className="absolute bottom-2 left-4 text-8xl text-gold-accent/5 select-none pointer-events-none">♦</div>

              <div className="relative z-10">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-felt-dark/80 border border-gold-accent/40 text-gold-accent text-xs font-bold uppercase tracking-wider mb-3">
                  <Trophy className="w-3.5 h-3.5 text-gold-accent" />
                  <span>Net Ledger Balance</span>
                </div>

                <div className="my-2">
                  <span
                    className={`text-5xl sm:text-6xl font-black tracking-tight font-mono ${
                      isPositiveBalance
                        ? 'text-green-400'
                        : isNegativeBalance
                        ? 'text-red-400'
                        : 'text-gold-accent'
                    }`}
                  >
                    {stats.currentBalance > 0 ? `+${stats.currentBalance}` : `${stats.currentBalance}`}
                  </span>
                </div>

                <p className="text-xs sm:text-sm text-cream-text/70 max-w-md mx-auto mt-2">
                  {isPositiveBalance
                    ? 'You are currently owed money from the group.'
                    : isNegativeBalance
                    ? 'You currently owe money to the group.'
                    : 'Your balance in this group is fully settled.'}
                </p>
              </div>
            </div>

            {/* Stats Breakdown Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Total Buy-Ins */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Total Buy-Ins</span>
                  <TrendingDown className="w-4 h-4 text-red-400" />
                </div>
                <div className="text-2xl font-bold font-mono text-cream-text">
                  {stats.totalBuyIns}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Total chips bought across tables</p>
              </div>

              {/* Total Exits */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Total Exits</span>
                  <TrendingUp className="w-4 h-4 text-green-400" />
                </div>
                <div className="text-2xl font-bold font-mono text-cream-text">
                  {stats.totalExits}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Total chips cashed out</p>
              </div>

              {/* Net Game Result */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Game Net Result</span>
                  <DollarSign className="w-4 h-4 text-gold-accent" />
                </div>
                <div
                  className={`text-2xl font-bold font-mono ${
                    stats.netGameBalance > 0
                      ? 'text-green-400'
                      : stats.netGameBalance < 0
                      ? 'text-red-400'
                      : 'text-cream-text'
                  }`}
                >
                  {stats.netGameBalance > 0 ? `+${stats.netGameBalance}` : stats.netGameBalance}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Exits minus Buy-ins</p>
              </div>

              {/* Payments Sent */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Payments Sent</span>
                  <Send className="w-4 h-4 text-gold-accent" />
                </div>
                <div className="text-2xl font-bold font-mono text-cream-text">
                  {stats.paymentsSent}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Settlements you paid out</p>
              </div>

              {/* Payments Received */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Payments Received</span>
                  <Download className="w-4 h-4 text-gold-accent" />
                </div>
                <div className="text-2xl font-bold font-mono text-cream-text">
                  {stats.paymentsReceived}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Settlements received from others</p>
              </div>

              {/* Tables Played */}
              <div className="bg-felt-card/80 border border-gold-accent/30 rounded-2xl p-5 shadow">
                <div className="flex items-center justify-between text-cream-text/70 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Tables Played</span>
                  <span className="text-gold-accent text-sm font-bold">♠</span>
                </div>
                <div className="text-2xl font-bold font-mono text-cream-text">
                  {stats.tablesPlayed}
                </div>
                <p className="text-xs text-cream-text/50 mt-1">Recorded sessions in this group</p>
              </div>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
};

export default GroupStats;
