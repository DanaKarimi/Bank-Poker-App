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
} from 'lucide-react';
import { getPlayers, sendJoinRequest } from '../api';
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
  const [loadingPlayers, setLoadingPlayers] = useState(false);
  const [isJoining, setIsJoining] = useState(false);
  const [joinMessage, setJoinMessage] = useState('');
  const [error, setError] = useState('');

  const fetchTablePlayers = async () => {
    if (!table?.id) return;
    setLoadingPlayers(true);
    try {
      const response = await getPlayers(table.id);
      setPlayers(response.data?.players || []);
    } catch (err) {
      console.error('Failed to fetch table players:', err);
    } finally {
      setLoadingPlayers(false);
    }
  };

  useEffect(() => {
    if (isOpen && table?.id) {
      setJoinMessage('');
      setError('');
      fetchTablePlayers();
    }
  }, [isOpen, table?.id]);

  if (!isOpen || !table) return null;

  // Check if current user is an active player at this table
  const isPlayerAtTable = players.some(
    (p) => (p.user_id === user?.id || p.name === user?.username) && p.status === 'ACTIVE'
  );

  // Check if user has a pending join request for this table
  const pendingJoinReq = (myRequests.joinRequests || []).find(
    (jr) => jr.table_id === table.id && jr.status === 'PENDING'
  );

  // Filter buy-in & exit requests for this table
  const tableBuyIns = (myRequests.buyInRequests || []).filter((r) => r.table_id === table.id);
  const tableExits = (myRequests.exitRequests || []).filter((r) => r.table_id === table.id);

  const handleJoinTable = async () => {
    setIsJoining(true);
    setError('');
    setJoinMessage('');
    try {
      await sendJoinRequest(table.id, table.group_id);
      setJoinMessage('Join request sent to table host! Once approved, you can request chips.');
      if (onJoinSuccess) onJoinSuccess();
    } catch (err) {
      console.error('Failed to send join request:', err);
      setError(err.response?.data?.error || 'Failed to submit join request.');
    } finally {
      setIsJoining(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-lg p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-150">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition"
        >
          <X className="w-5 h-5" />
        </button>

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
            <p className="text-xs text-cream-text/60">Table Details & Player Seat Management</p>
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

        {/* Table Details Grid */}
        <div className="bg-felt-dark/90 border border-gold-accent/30 rounded-xl p-4 mb-4 space-y-2 text-xs">
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

        {/* Actions Area */}
        <div className="mb-5">
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

        {/* Players List at Table */}
        <div className="mb-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gold-accent flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5" />
              <span>Players At Table ({players.length})</span>
            </h3>
          </div>

          {loadingPlayers ? (
            <div className="text-center py-4 text-xs text-cream-text/50">Loading players...</div>
          ) : players.length === 0 ? (
            <div className="p-3 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
              No players currently seated at this table.
            </div>
          ) : (
            <div className="space-y-1.5 max-h-32 overflow-y-auto">
              {players.map((p) => (
                <div
                  key={p.id}
                  className="px-3 py-2 bg-felt-dark rounded-lg flex items-center justify-between text-xs border border-gold-accent/20"
                >
                  <span className="font-semibold text-cream-text">
                    {p.name || p.username} {p.name === user?.username ? '(You)' : ''}
                  </span>
                  <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                    {p.status || 'ACTIVE'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Close Button */}
        <div className="pt-3 border-t border-gold-accent/20 text-center">
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
