import React, { useState, useEffect } from 'react';
import {
  X,
  Layers,
  PlusCircle,
  MinusCircle,
  UserPlus,
  Users,
  Clock,
  CheckCircle,
  AlertCircle,
  ShieldCheck,
  History,
  TrendingUp,
  TrendingDown,
  RefreshCw,
} from 'lucide-react';
import { getPlayers, sendJoinRequest, getTableActivity } from '../api';
import { useAuth } from '../context/AuthContext';

const TableDetailModal = ({
  isOpen,
  onClose,
  table,
  myRequests = [],
  onRequestBuyIn,
  onRequestExit,
  onJoinSuccess,
}) => {
  const { user } = useAuth();
  const [players, setPlayers] = useState([]);
  const [activity, setActivity] = useState({ buyIns: [], exits: [] });
  const [loadingPlayers, setLoadingPlayers] = useState(false);
  const [loadingActivity, setLoadingActivity] = useState(false);
  const [isJoining, setIsJoining] = useState(false);
  const [joinMessage, setJoinMessage] = useState('');
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'activity' | 'players'

  const fetchTableData = async () => {
    if (!table?.id) return;
    setLoadingPlayers(true);
    setLoadingActivity(true);
    try {
      const [playersRes, activityRes] = await Promise.allSettled([
        getPlayers(table.id),
        getTableActivity(table.id),
      ]);

      if (playersRes.status === 'fulfilled') {
        setPlayers(playersRes.value.data?.players || []);
      }
      if (activityRes.status === 'fulfilled') {
        setActivity({
          buyIns: activityRes.value.data?.buyIns || [],
          exits: activityRes.value.data?.exits || [],
        });
      }
    } catch (err) {
      console.error('Failed to fetch table data:', err);
    } finally {
      setLoadingPlayers(false);
      setLoadingActivity(false);
    }
  };

  useEffect(() => {
    if (isOpen && table?.id) {
      setJoinMessage('');
      setError('');
      fetchTableData();

      // Poll table data every 8 seconds while open
      const interval = setInterval(fetchTableData, 8000);
      return () => clearInterval(interval);
    }
  }, [isOpen, table?.id]);

  if (!isOpen || !table) return null;

  // Find user's player record at this table
  const myPlayer = players.find(
    (p) =>
      (p.user_id === user?.id ||
        p.userId === user?.id ||
        p.name?.toLowerCase() === user?.username?.toLowerCase() ||
        p.username?.toLowerCase() === user?.username?.toLowerCase()) &&
      p.status === 'ACTIVE'
  );

  const isPlayerAtTable = !!myPlayer;

  // Check if user has a pending join request for this table
  const pendingJoinReq = (myRequests.joinRequests || []).find(
    (jr) => jr.table_id === table.id && jr.status === 'PENDING'
  );

  // Combine and sort all activity transactions (Direct + Request-based)
  const allTransactions = [
    ...(activity.buyIns || []).map((b) => ({ ...b, txType: 'buy-in' })),
    ...(activity.exits || []).map((e) => ({ ...e, txType: 'exit' })),
  ].sort((a, b) => {
    const timeA = a.timestamp || a.created_at || a.createdAt || 0;
    const timeB = b.timestamp || b.created_at || b.createdAt || 0;
    return timeB - timeA;
  });

  // Calculate current user's session metrics from all activity records
  const myBuyIns = (activity.buyIns || []).filter(
    (b) =>
      (myPlayer && (b.player_id === myPlayer.id || b.playerId === myPlayer.id)) ||
      b.playerName?.toLowerCase() === user?.username?.toLowerCase()
  );
  const myExits = (activity.exits || []).filter(
    (e) =>
      (myPlayer && (e.player_id === myPlayer.id || e.playerId === myPlayer.id)) ||
      e.playerName?.toLowerCase() === user?.username?.toLowerCase()
  );

  const myTotalBuyIns = myBuyIns.reduce((sum, b) => sum + (Number(b.amount) || 0), 0);
  const myTotalExits = myExits.reduce((sum, e) => sum + (Number(e.amount) || 0), 0);
  const myNetBalance = myTotalExits - myTotalBuyIns;

  const handleJoinTable = async () => {
    setIsJoining(true);
    setError('');
    setJoinMessage('');
    try {
      await sendJoinRequest(table.id, table.group_id);
      setJoinMessage('Join request sent to table host! Once approved, you can request chips.');
      if (onJoinSuccess) onJoinSuccess();
      fetchTableData();
    } catch (err) {
      console.error('Failed to send join request:', err);
      setError(err.response?.data?.error || 'Failed to submit join request.');
    } finally {
      setIsJoining(false);
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(Number(timestamp));
    return isNaN(date.getTime())
      ? ''
      : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) +
          ' ' +
          date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-lg p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-150">
        {/* Close & Refresh Buttons */}
        <div className="absolute top-4 right-4 flex items-center gap-1.5">
          <button
            onClick={fetchTableData}
            title="Refresh table activity"
            className="text-cream-text/60 hover:text-gold-accent p-1 rounded-lg transition"
          >
            <RefreshCw className={`w-4 h-4 ${loadingActivity ? 'animate-spin text-gold-accent' : ''}`} />
          </button>
          <button
            onClick={onClose}
            className="text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="p-3 bg-felt-dark text-gold-accent border border-gold-accent/40 rounded-xl">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-cream-text">{table.name || `Table ${table.id}`}</h2>
              <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                {table.status || 'ACTIVE'}
              </span>
            </div>
            <p className="text-xs text-cream-text/60">Table Details & Live Activity</p>
          </div>
        </div>

        {/* Status Alerts */}
        {joinMessage && (
          <div className="mb-4 p-3 bg-emerald-950/90 border border-emerald-500 rounded-xl flex items-center gap-2 text-emerald-200 text-xs">
            <CheckCircle className="w-4 h-4 shrink-0 text-emerald-400" />
            <span>{joinMessage}</span>
          </div>
        )}

        {error && (
          <div className="mb-4 p-3 bg-red-950/90 border border-red-500 rounded-xl flex items-center gap-2 text-red-200 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Tabs */}
        <div className="flex items-center gap-1 border-b border-gold-accent/20 mb-4 pb-1 text-xs">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-3 py-1.5 rounded-lg font-bold transition flex items-center gap-1.5 ${
              activeTab === 'overview'
                ? 'bg-gold-accent text-black shadow-md'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Overview</span>
          </button>
          <button
            onClick={() => setActiveTab('activity')}
            className={`px-3 py-1.5 rounded-lg font-bold transition flex items-center gap-1.5 ${
              activeTab === 'activity'
                ? 'bg-gold-accent text-black shadow-md'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <History className="w-3.5 h-3.5" />
            <span>Activity History ({allTransactions.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('players')}
            className={`px-3 py-1.5 rounded-lg font-bold transition flex items-center gap-1.5 ${
              activeTab === 'players'
                ? 'bg-gold-accent text-black shadow-md'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Players ({players.length})</span>
          </button>
        </div>

        {/* TAB 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <div className="space-y-4">
            {/* Table Details Grid */}
            <div className="bg-felt-dark/90 border border-gold-accent/30 rounded-xl p-4 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-cream-text/60">Chip Value</span>
                <span className="font-mono font-bold text-gold-accent">
                  {table.chip_value ? `$${table.chip_value} / chip` : 'Standard 1:1'}
                </span>
              </div>

              {table.has_entry_fee && table.entry_fee > 0 && (
                <div className="flex items-center justify-between border-t border-gold-accent/15 pt-2">
                  <span className="text-cream-text/60">Entry Fee</span>
                  <span className="font-mono font-bold text-amber-400">${table.entry_fee}</span>
                </div>
              )}

              <div className="flex items-center justify-between border-t border-gold-accent/15 pt-2">
                <span className="text-cream-text/60">Your Seat Status</span>
                <span
                  className={`font-bold ${
                    isPlayerAtTable
                      ? 'text-emerald-400 flex items-center gap-1'
                      : pendingJoinReq
                      ? 'text-amber-400 flex items-center gap-1'
                      : 'text-cream-text/70'
                  }`}
                >
                  {isPlayerAtTable ? (
                    <>
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Joined (Active Seat)</span>
                    </>
                  ) : pendingJoinReq ? (
                    <>
                      <Clock className="w-3.5 h-3.5" />
                      <span>Join Pending Approval</span>
                    </>
                  ) : (
                    <span>Not Joined Yet</span>
                  )}
                </span>
              </div>
            </div>

            {/* Personal Session Balance Card (if seated) */}
            {isPlayerAtTable && (
              <div className="p-4 bg-felt-dark border border-gold-accent/40 rounded-xl">
                <div className="text-[11px] font-bold uppercase tracking-wider text-gold-accent/80 mb-2">
                  Your Table Session Summary
                </div>
                <div className="grid grid-cols-3 gap-2 text-center">
                  <div className="p-2.5 bg-black/40 rounded-lg border border-gold-accent/20">
                    <div className="text-[10px] text-cream-text/60 uppercase">Total Buy-Ins</div>
                    <div className="text-sm font-bold font-mono text-emerald-400">
                      ${myTotalBuyIns.toLocaleString()}
                    </div>
                  </div>
                  <div className="p-2.5 bg-black/40 rounded-lg border border-gold-accent/20">
                    <div className="text-[10px] text-cream-text/60 uppercase">Total Exits</div>
                    <div className="text-sm font-bold font-mono text-amber-400">
                      ${myTotalExits.toLocaleString()}
                    </div>
                  </div>
                  <div className="p-2.5 bg-black/40 rounded-lg border border-gold-accent/20">
                    <div className="text-[10px] text-cream-text/60 uppercase">Net Balance</div>
                    <div
                      className={`text-sm font-extrabold font-mono ${
                        myNetBalance >= 0 ? 'text-emerald-400' : 'text-red-400'
                      }`}
                    >
                      {myNetBalance >= 0 ? `+$${myNetBalance.toLocaleString()}` : `-$${Math.abs(myNetBalance).toLocaleString()}`}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Actions Area */}
            <div>
              {!isPlayerAtTable ? (
                /* User NOT yet in table: Show Join Table CTA */
                <div className="p-4 bg-felt-dark border border-gold-accent/40 rounded-xl text-center space-y-3">
                  <p className="text-xs text-cream-text/80">
                    You must join this table and be approved by the admin before you can request buy-in chips.
                  </p>

                  {pendingJoinReq ? (
                    <div className="py-2.5 px-4 bg-amber-950/80 border border-amber-500/50 rounded-xl text-amber-300 text-xs font-bold flex items-center justify-center gap-2">
                      <Clock className="w-4 h-4 animate-spin" />
                      <span>Join Request Pending Host Approval</span>
                    </div>
                  ) : (
                    <button
                      onClick={handleJoinTable}
                      disabled={isJoining}
                      className="w-full py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg hover:opacity-95 transition active:scale-95 flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                      <UserPlus className="w-4 h-4" />
                      <span>{isJoining ? 'Sending Join Request...' : 'Send Request to Join Table'}</span>
                    </button>
                  )}
                </div>
              ) : (
                /* User IS in table: Show Buy-In & Exit Actions */
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    onClick={() => onRequestBuyIn(table)}
                    className="py-3 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-500 hover:to-green-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
                  >
                    <PlusCircle className="w-4 h-4" />
                    <span>Request Buy-In</span>
                  </button>

                  <button
                    onClick={() => onRequestExit(table)}
                    className="py-3 bg-gradient-to-r from-amber-500 to-yellow-600 hover:from-amber-400 hover:to-yellow-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
                  >
                    <MinusCircle className="w-4 h-4" />
                    <span>Request Exit / Cashout</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 2: ACTIVITY HISTORY */}
        {activeTab === 'activity' && (
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs text-cream-text/60">
              <span>All direct and request-approved transactions</span>
              <span>{allTransactions.length} records</span>
            </div>

            {loadingActivity ? (
              <div className="text-center py-6 text-xs text-cream-text/50">Loading activity...</div>
            ) : allTransactions.length === 0 ? (
              <div className="p-6 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No buy-ins or exits recorded at this table yet.
              </div>
            ) : (
              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {allTransactions.map((tx) => {
                  const isBuyIn = tx.txType === 'buy-in';
                  return (
                    <div
                      key={tx.id}
                      className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`p-2 rounded-lg ${
                            isBuyIn
                              ? 'bg-emerald-950/80 text-emerald-400 border border-emerald-500/30'
                              : 'bg-amber-950/80 text-amber-400 border border-amber-500/30'
                          }`}
                        >
                          {isBuyIn ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                        </div>
                        <div>
                          <div className="font-bold text-cream-text flex items-center gap-1.5">
                            <span>{tx.playerName || 'Player'}</span>
                            {tx.playerName === user?.username && (
                              <span className="text-[10px] text-gold-accent font-normal">(You)</span>
                            )}
                          </div>
                          <div className="text-[10px] text-cream-text/50 flex items-center gap-1">
                            <span>{formatDate(tx.timestamp || tx.created_at || tx.createdAt)}</span>
                            {tx.note && <span>• {tx.note}</span>}
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`font-mono font-extrabold text-sm ${
                            isBuyIn ? 'text-emerald-400' : 'text-amber-400'
                          }`}
                        >
                          {isBuyIn ? `+$${tx.amount}` : `-$${tx.amount}`}
                        </div>
                        <span
                          className={`text-[9px] font-extrabold uppercase px-1.5 py-0.5 rounded ${
                            isBuyIn
                              ? 'bg-emerald-950 text-emerald-300 border border-emerald-500/30'
                              : 'bg-amber-950 text-amber-300 border border-amber-500/30'
                          }`}
                        >
                          {isBuyIn ? 'BUY-IN' : 'EXIT'}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: PLAYERS */}
        {activeTab === 'players' && (
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs text-cream-text/60">
              <span>Current seated players at this table</span>
              <span>{players.length} players</span>
            </div>

            {loadingPlayers ? (
              <div className="text-center py-6 text-xs text-cream-text/50">Loading players...</div>
            ) : players.length === 0 ? (
              <div className="p-6 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No players currently seated at this table.
              </div>
            ) : (
              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {players.map((p) => (
                  <div
                    key={p.id}
                    className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-cream-text flex items-center gap-1.5">
                        <span>{p.name || p.username}</span>
                        {p.name === user?.username && (
                          <span className="text-[10px] text-gold-accent font-normal">(You)</span>
                        )}
                      </div>
                      <div className="text-[10px] text-cream-text/50">
                        Joined: {formatDate(p.createdAt || p.created_at)}
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                        {p.status || 'ACTIVE'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Close Button */}
        <div className="pt-4 mt-4 border-t border-gold-accent/20 text-center">
          <button
            onClick={onClose}
            className="text-xs text-cream-text/60 hover:text-cream-text transition font-semibold"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default TableDetailModal;
