import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getTableDetail,
  getTableStatus,
  getTableActivity,
  getPlayers,
  getMyRequests,
  sendJoinRequest,
  sendBuyInRequest,
  sendExitRequest,
  confirmBuyInReceipt,
  confirmExitReceipt,
  publishTable,
  deleteTablePlayer,
  addTablePlayer,
  directBuyIn,
  directExit,
} from '../api';
import StatusBadge from '../components/StatusBadge';
import RequestCard from '../components/RequestCard';
import BuyInModal from '../components/BuyInModal';
import ExitModal from '../components/ExitModal';
import { UserBadge } from '../components/AvatarSystem';
import GroupCodeChip from '../components/GroupCodeChip';
import { getSocket, joinTable, leaveTable, joinGroup, leaveGroup } from '../socket';
import {
  ArrowLeft,
  RefreshCw,
  PlusCircle,
  MinusCircle,
  UserPlus,
  Users,
  Clock,
  CheckCircle,
  AlertCircle,
  ShieldCheck,
  TrendingUp,
  TrendingDown,
  Layers,
  Coins,
  DollarSign,
  AlertTriangle,
  History,
  Share2,
  Trash2,
} from 'lucide-react';
import NotificationsDropdown from '../components/NotificationsDropdown';

const TableDetail = () => {
  const { groupId, tableId: pTableId, id } = useParams();
  const tableId = pTableId || id;
  const navigate = useNavigate();
  const { user } = useAuth();

  // State
  const [table, setTable] = useState(null);
  const [players, setPlayers] = useState([]);
  const [activity, setActivity] = useState({ buyIns: [], exits: [] });
  const [myRequests, setMyRequests] = useState({ joinRequests: [], buyInRequests: [], exitRequests: [] });

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'activity' | 'players' | 'requests'

  // Modal states
  const [isBuyInModalOpen, setIsBuyInModalOpen] = useState(false);
  const [isExitModalOpen, setIsExitModalOpen] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [playerToDelete, setPlayerToDelete] = useState(null);
  const [isDeletingPlayer, setIsDeletingPlayer] = useState(false);
  const [isAddPlayerModalOpen, setIsAddPlayerModalOpen] = useState(false);
  const [newPlayerName, setNewPlayerName] = useState('');
  const [isAddingPlayer, setIsAddingPlayer] = useState(false);
  const [selectedPlayerForBuyIn, setSelectedPlayerForBuyIn] = useState(null);
  const [selectedPlayerForExit, setSelectedPlayerForExit] = useState(null);

  const previousStatusRef = useRef(null);

  // Fetch all table data
  const fetchTableData = async (isBackground = false) => {
    if (!tableId) return;
    if (!isBackground) setLoading(true);

    try {
      const [tableRes, statusRes, playersRes, activityRes, requestsRes] = await Promise.allSettled([
        getTableDetail(tableId),
        getTableStatus(tableId),
        getPlayers(tableId),
        getTableActivity(tableId),
        getMyRequests(groupId, tableId),
      ]);

      let currentTableObj = null;
      if (tableRes.status === 'fulfilled' && tableRes.value.data?.table) {
        currentTableObj = tableRes.value.data.table;
      }

      // Sync status override if available
      if (statusRes.status === 'fulfilled' && statusRes.value.data) {
        const sData = statusRes.value.data;
        if (currentTableObj) {
          currentTableObj.status = sData.status || (sData.isActive ? 'ACTIVE' : 'CLOSED');
          currentTableObj.isActive = sData.isActive;
        }

        // Check if table just transitioned to CLOSED
        if (previousStatusRef.current === 'ACTIVE' && sData.status === 'CLOSED') {
          setError('Notice: This table was just closed by the host. New transactions are disabled.');
        }
        previousStatusRef.current = sData.status;
      }

      if (currentTableObj) {
        setTable(currentTableObj);
      }

      if (playersRes.status === 'fulfilled') {
        const rawPlayers = playersRes.value.data?.players || [];
        const playerMap = new Map();
        rawPlayers.forEach((p) => {
          if (p.id) playerMap.set(p.id, p);
        });
        setPlayers(Array.from(playerMap.values()));
      }

      if (activityRes.status === 'fulfilled') {
        const rawBuyIns = activityRes.value.data?.buyIns || [];
        const rawExits = activityRes.value.data?.exits || [];
        
        const buyInMap = new Map();
        rawBuyIns.forEach((b) => {
          const key = b.id || `${b.player_id || b.playerId}-${b.amount}-${b.created_at || b.timestamp}`;
          buyInMap.set(key, b);
        });

        const exitMap = new Map();
        rawExits.forEach((e) => {
          const key = e.id || `${e.player_id || e.playerId}-${e.amount}-${e.created_at || e.timestamp}`;
          exitMap.set(key, e);
        });

        setActivity({
          buyIns: Array.from(buyInMap.values()),
          exits: Array.from(exitMap.values()),
        });
      }

      if (requestsRes.status === 'fulfilled') {
        setMyRequests(requestsRes.value.data || { joinRequests: [], buyInRequests: [], exitRequests: [] });
      }
    } catch (err) {
      console.error('Failed to fetch table details:', err);
      if (!isBackground) setError('Failed to load table details.');
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    fetchTableData();

    // 1. Join Socket.IO rooms for table and group
    joinTable(tableId);
    if (groupId) {
      joinGroup(groupId);
    }
    const socket = getSocket();

    const handleRefresh = () => {
      fetchTableData(true);
    };

    const handleTableClosed = (payload) => {
      if (!payload?.tableId || String(payload.tableId) === String(tableId)) {
        setTable((prev) => (prev ? { ...prev, status: 'CLOSED', isActive: false } : null));
        fetchTableData(true);
      }
    };

    socket.on('table_closed', handleTableClosed);
    socket.on('table_updated', handleRefresh);
    socket.on('table_published', handleRefresh);
    socket.on('buyin_recorded', handleRefresh);
    socket.on('exit_recorded', handleRefresh);
    socket.on('player_added', handleRefresh);
    socket.on('player_deleted', handleRefresh);
    socket.on('request_created', handleRefresh);
    socket.on('request_resolved', handleRefresh);

    // 2. High-frequency API polling fallback (3.5 seconds) ensuring realtime parity
    const interval = setInterval(() => {
      fetchTableData(true);
    }, 3500);

    return () => {
      clearInterval(interval);
      leaveTable(tableId);
      if (groupId) {
        leaveGroup(groupId);
      }
      socket.off('table_closed', handleTableClosed);
      socket.off('table_updated', handleRefresh);
      socket.off('table_published', handleRefresh);
      socket.off('buyin_recorded', handleRefresh);
      socket.off('exit_recorded', handleRefresh);
      socket.off('player_added', handleRefresh);
      socket.off('player_deleted', handleRefresh);
      socket.off('request_created', handleRefresh);
      socket.off('request_resolved', handleRefresh);
    };
  }, [groupId, tableId]);

  // Derived user status at this table
  const isClosed = table?.status === 'CLOSED' || table?.isActive === false;

  const myPlayer = players.find(
    (p) =>
      (p.user_id === user?.id ||
        p.userId === user?.id ||
        p.name?.toLowerCase() === user?.username?.toLowerCase() ||
        p.username?.toLowerCase() === user?.username?.toLowerCase()) &&
      p.status === 'ACTIVE'
  );

  const isPlayerSeated = !!myPlayer;

  const pendingJoinReq = (myRequests.joinRequests || []).find(
    (jr) => (jr.table_id === tableId || jr.tableId === tableId) && jr.status === 'PENDING'
  );

  // Table specific requests
  const tableBuyInRequests = (myRequests.buyInRequests || []).filter(
    (r) => r.table_id === tableId || r.tableId === tableId
  );
  const tableExitRequests = (myRequests.exitRequests || []).filter(
    (r) => r.table_id === tableId || r.tableId === tableId
  );
  const totalTablePendingRequests =
    tableBuyInRequests.filter((r) => r.status === 'PENDING' || r.status === 'APPROVED').length +
    tableExitRequests.filter((r) => r.status === 'PENDING' || r.status === 'APPROVED').length;

  // Combine, strictly deduplicate by ID, and sort all activity transactions
  const txMap = new Map();
  (activity.buyIns || []).forEach((b) => {
    const key = b.id ? `buyin-${b.id}` : `buyin-${b.player_id || b.playerId}-${b.amount}-${b.created_at || b.timestamp}`;
    txMap.set(key, { ...b, txType: 'buy-in' });
  });
  (activity.exits || []).forEach((e) => {
    const key = e.id ? `exit-${e.id}` : `exit-${e.player_id || e.playerId}-${e.amount}-${e.created_at || e.timestamp}`;
    txMap.set(key, { ...e, txType: 'exit' });
  });

  const allTransactions = Array.from(txMap.values()).sort((a, b) => {
    const timeA = a.timestamp || a.created_at || a.createdAt || 0;
    const timeB = b.timestamp || b.created_at || b.createdAt || 0;
    return timeB - timeA;
  });

  // Server is the ONLY balance authority. Clients MUST render server-computed balances; NEVER recompute client-side.
  const myTableBuyIns = myPlayer?.totalBuyIns ?? myPlayer?.total_buy_ins ?? 0;
  const myTableExits = myPlayer?.totalExits ?? myPlayer?.total_exits ?? 0;
  const myTableNetBalance = myPlayer?.balance ?? 0;

  // Actions
  const handleJoinTable = async () => {
    if (isClosed) return;
    setIsJoining(true);
    setError('');
    try {
      await sendJoinRequest(tableId, groupId);
      setSuccessMessage('Join request sent to table host! Awaiting approval.');
      fetchTableData(true);
    } catch (err) {
      console.error('Failed to send join request:', err);
      setError(err.response?.data?.error || 'Failed to submit join request.');
    } finally {
      setIsJoining(false);
    }
  };

  const handleOpenBuyInModal = (p = null) => {
    if (isClosed) return;
    setSelectedPlayerForBuyIn(p || (isPlayerSeated ? myPlayer : null));
    setIsBuyInModalOpen(true);
  };

  const handleOpenExitModal = (p = null) => {
    if (isClosed) return;
    setSelectedPlayerForExit(p || (isPlayerSeated ? myPlayer : null));
    setIsExitModalOpen(true);
  };

  const handleAddPlayerSubmit = async (e) => {
    e.preventDefault();
    const cleanName = newPlayerName.trim();
    if (!cleanName) {
      setError('Please enter a player name');
      return;
    }
    setIsAddingPlayer(true);
    setError('');
    try {
      await addTablePlayer(tableId, { name: cleanName });
      setSuccessMessage(`Player "${cleanName}" added to table.`);
      setIsAddPlayerModalOpen(false);
      setNewPlayerName('');
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to add player:', err);
      setError(err.response?.data?.error || 'Failed to add player to table.');
    } finally {
      setIsAddingPlayer(false);
    }
  };

  const handleBuyInSubmit = async (payload, legacyAmount, legacyNote) => {
    try {
      const pId = typeof payload === 'object' ? payload.playerId : null;
      const targetName = typeof payload === 'object' ? payload.name : (myPlayer?.name || user?.username);
      const amt = Number(typeof payload === 'object' ? payload.amount : payload);
      const nt = typeof payload === 'object' ? payload.note : legacyNote;

      const isHostOrAdmin = user?.role === 'SUPER_ADMIN' || table?.host_id === user?.id || table?.isHost;
      if (isHostOrAdmin || targetName !== user?.username) {
        await directBuyIn(tableId, {
          playerId: pId,
          name: targetName,
          amount: amt,
          note: nt,
        });
        setSuccessMessage(`Buy-in of ${amt.toLocaleString()} chips recorded for ${targetName}!`);
      } else {
        await sendBuyInRequest(groupId, tableId, amt, nt);
        setSuccessMessage(`Buy-in request for ${amt.toLocaleString()} chips submitted successfully!`);
      }
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to submit buy-in:', err);
      setError(err.response?.data?.error || 'Failed to record buy-in.');
    }
  };

  const handleExitSubmit = async (payload, legacyAmount, legacyNote) => {
    try {
      const pId = typeof payload === 'object' ? payload.playerId : null;
      const targetName = typeof payload === 'object' ? payload.name : (myPlayer?.name || user?.username);
      const amt = Number(typeof payload === 'object' ? payload.amount : payload);
      const nt = typeof payload === 'object' ? payload.note : legacyNote;

      const isHostOrAdmin = user?.role === 'SUPER_ADMIN' || table?.host_id === user?.id || table?.isHost;
      if (isHostOrAdmin || targetName !== user?.username) {
        await directExit(tableId, {
          playerId: pId,
          name: targetName,
          amount: amt,
          note: nt,
        });
        setSuccessMessage(`Exit of ${amt.toLocaleString()} chips recorded for ${targetName}!`);
      } else {
        await sendExitRequest(groupId, tableId, amt, nt);
        setSuccessMessage(`Exit cashout request for ${amt.toLocaleString()} chips submitted successfully!`);
      }
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to submit exit:', err);
      setError(err.response?.data?.error || 'Failed to record exit.');
    }
  };

  const handleConfirmReceipt = async (request, type) => {
    try {
      if (type === 'buy-in') {
        await confirmBuyInReceipt(request.id);
        setSuccessMessage('Buy-in receipt confirmed! Chips recorded.');
      } else {
        await confirmExitReceipt(request.id);
        setSuccessMessage('Exit receipt confirmed! Cashout recorded.');
      }
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to confirm receipt:', err);
      setError(err.response?.data?.error || 'Failed to confirm receipt.');
    }
  };

  const handlePublishTable = async () => {
    setIsPublishing(true);
    try {
      const res = await publishTable(tableId);
      if (res.data?.code) {
        setTable((prev) => ({
          ...prev,
          code: res.data.code,
          published_at: res.data.published_at || Date.now(),
        }));
        setSuccessMessage(`Table published! Share code: ${res.data.code}`);
        setTimeout(() => setSuccessMessage(''), 4000);
      }
    } catch (err) {
      console.error('Failed to publish table:', err);
      setError(err.response?.data?.error || 'Failed to publish table code.');
    } finally {
      setIsPublishing(false);
    }
  };

  const handleDeletePlayerClick = (p) => {
    const buyIns = Number(p.totalBuyIns || p.total_buy_ins || p.buy_ins || 0);
    if (buyIns > 0) {
      setError('Cannot remove player who has already bought in. Settle their stack with an Exit transaction first.');
      setTimeout(() => setError(''), 6000);
      return;
    }
    setPlayerToDelete(p);
  };

  const handleConfirmDeletePlayer = async () => {
    if (!playerToDelete) return;
    setIsDeletingPlayer(true);
    try {
      await deleteTablePlayer(tableId, playerToDelete.id);
      setSuccessMessage(`Player "${playerToDelete.name || playerToDelete.username}" removed from table.`);
      setPlayerToDelete(null);
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to delete player:', err);
      setError(err.response?.data?.error || 'Failed to remove player from table.');
    } finally {
      setIsDeletingPlayer(false);
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

  if (loading && !table) {
    return (
      <div className="min-h-screen bg-felt-dark flex items-center justify-center p-4">
        <div className="text-center space-y-3">
          <div className="w-12 h-12 border-4 border-gold-accent border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-cream-text/70 text-sm font-semibold">Loading table details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-felt-dark text-cream-text flex flex-col items-center py-6 px-4 sm:px-6 pb-32">
      <div className="w-full max-w-4xl space-y-6">
        {/* Navigation & Header */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate(groupId ? `/group/${groupId}` : '/')}
            className="inline-flex items-center gap-2 px-3 py-1.5 bg-felt-card/80 hover:bg-felt-card border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition shadow-sm cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>{groupId ? 'Back to Group Tables' : 'Back to Dashboard'}</span>
          </button>

          <div className="flex items-center gap-2">
            <NotificationsDropdown />
            <button
              onClick={() => fetchTableData()}
              className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition cursor-pointer"
              title="Refresh Table Data"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Table Hero Card */}
        <div className="bg-felt-card border-2 border-gold-accent rounded-2xl p-6 shadow-2xl relative overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div
                className={`p-3.5 rounded-2xl border ${
                  isClosed
                    ? 'bg-zinc-800 text-zinc-400 border-zinc-700'
                    : 'bg-felt-dark text-gold-accent border-gold-accent/50'
                }`}
              >
                <Layers className="w-7 h-7" />
              </div>
              <div>
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h1 className="text-2xl font-black tracking-tight text-cream-text">
                    {table?.name || `Table ${tableId}`}
                  </h1>
                  <StatusBadge status={table?.status} />
                  {(table?.entry_fee || table?.entryFee) > 0 && (
                    (myPlayer?.entry_fee_paid === 1 || myPlayer?.entryFeePaid === true || table?.myEntryFeePaid === true || table?.my_entry_fee_paid === 1) ? (
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide bg-emerald-950 text-emerald-300 border border-emerald-500/60 shadow-sm">
                        Entry Fee Paid ✓
                      </span>
                    ) : (
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide bg-red-950 text-red-300 border border-red-500/60 shadow-sm">
                        UNPAID
                      </span>
                    )
                  )}
                </div>
                <p className="text-xs text-cream-text/60 mt-0.5">
                  Live Table Session & Transaction Ledger
                </p>

                {/* Table Code / Publish Button */}
                <div className="flex items-center gap-2 mt-2">
                  {table?.code ? (
                    <GroupCodeChip code={table.code} label="Table Code" />
                  ) : !isClosed ? (
                    <button
                      onClick={handlePublishTable}
                      disabled={isPublishing}
                      className="px-3 py-1.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent hover:opacity-95 active:scale-95 text-black font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                    >
                      <Share2 className="w-3.5 h-3.5" />
                      <span>{isPublishing ? 'Publishing...' : 'Publish Table Code'}</span>
                    </button>
                  ) : null}
                </div>
              </div>
            </div>

            {/* Quick Table Metrics */}
            <div className="flex items-center gap-2">
              <div className="px-3.5 py-2 bg-felt-dark rounded-xl border border-gold-accent/30 text-center text-xs">
                <div className="text-[10px] text-cream-text/50 uppercase">Chip Value</div>
                <div className="font-bold text-gold-accent">
                  {table?.chip_value || table?.chipValue
                    ? `$${table.chip_value || table.chipValue}`
                    : '$1'}
                </div>
              </div>

              {(table?.has_entry_fee || table?.hasEntryFee) && (
                <div className="px-3.5 py-2 bg-felt-dark rounded-xl border border-amber-500/30 text-center text-xs">
                  <div className="text-[10px] text-amber-400 uppercase">Entry Fee</div>
                  <div className="font-bold text-amber-400">
                    ${table?.entry_fee || table?.entryFee}
                  </div>
                </div>
              )}

              <div className="px-3.5 py-2 bg-felt-dark rounded-xl border border-gold-accent/30 text-center text-xs">
                <div className="text-[10px] text-cream-text/50 uppercase">Players Seated</div>
                <div className="font-bold text-cream-text">{players.length}</div>
              </div>
            </div>
          </div>

          {/* Closed Alert Banner */}
          {isClosed && (
            <div className="mt-5 p-4 bg-red-950/90 border-2 border-red-500 rounded-xl flex items-center justify-center gap-2.5 text-red-200 text-sm font-black tracking-wider uppercase shadow-lg">
              <AlertTriangle className="w-5 h-5 text-red-400 shrink-0" />
              <span>TABLE CLOSED • HISTORY LOCKED</span>
            </div>
          )}
        </div>

        {/* Global Feedback Banners */}
        {successMessage && (
          <div className="p-3.5 bg-emerald-950/90 border border-emerald-500 rounded-xl flex items-center gap-2 text-emerald-200 text-xs shadow-lg animate-in fade-in">
            <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {error && (
          <div className="p-3.5 bg-red-950/90 border border-red-500 rounded-xl flex items-center gap-2 text-red-200 text-xs shadow-lg animate-in fade-in">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* User Session Bar (if seated at this table) */}
        {isPlayerSeated && (
          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-5 shadow-lg">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold uppercase tracking-wider text-gold-accent flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4" />
                <span>Your Table Balance & Session Result</span>
              </span>
              <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                Seated
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-center">
              <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                <div className="text-[11px] text-cream-text/60 uppercase">Table Buy-Ins</div>
                <div className="text-lg font-bold font-mono text-emerald-400">
                  ${myTableBuyIns.toLocaleString()}
                </div>
              </div>

              <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                <div className="text-[11px] text-cream-text/60 uppercase">Table Exits</div>
                <div className="text-lg font-bold font-mono text-amber-400">
                  ${myTableExits.toLocaleString()}
                </div>
              </div>

              <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                <div className="text-[11px] text-cream-text/60 uppercase">Net Table Balance</div>
                <div
                  className={`text-lg font-extrabold font-mono ${
                    myTableNetBalance >= 0 ? 'text-emerald-400' : 'text-red-400'
                  }`}
                >
                  {myTableNetBalance >= 0
                    ? `+$${myTableNetBalance.toLocaleString()}`
                    : `-$${Math.abs(myTableNetBalance).toLocaleString()}`}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Action Controls for Table (Fixed Bottom) */}
        {!isClosed && (
          <div className="fixed bottom-0 left-0 right-0 p-4 bg-felt-dark/90 backdrop-blur-md border-t border-gold-accent/30 z-40 pb-safe shadow-[0_-10px_30px_-10px_rgba(0,0,0,0.5)]">
            <div className="max-w-4xl mx-auto">
              {!isPlayerSeated ? (
                <div className="text-center space-y-2">
                  {pendingJoinReq ? (
                    <div className="py-3 px-4 bg-amber-950/80 border border-amber-500/50 rounded-xl text-amber-300 text-xs font-bold flex items-center justify-center gap-2">
                      <Clock className="w-4 h-4 animate-spin" />
                      <span>Join Request Pending Host Approval</span>
                    </div>
                  ) : (
                    <button
                      onClick={handleJoinTable}
                      disabled={isJoining}
                      className="w-full max-w-sm mx-auto py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg hover:opacity-95 transition active:scale-95 flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                      <UserPlus className="w-4 h-4" />
                      <span>{isJoining ? 'Submitting Join Request...' : 'Request to Join Table'}</span>
                    </button>
                  )}
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    onClick={handleOpenBuyInModal}
                    className="py-3.5 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-500 hover:to-green-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
                  >
                    <PlusCircle className="w-5 h-5" />
                    <span>Request Buy-In (Chips)</span>
                  </button>

                  <button
                    onClick={handleOpenExitModal}
                    className="py-3.5 bg-gradient-to-r from-amber-500 to-yellow-600 hover:from-amber-400 hover:to-yellow-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
                  >
                    <MinusCircle className="w-5 h-5" />
                    <span>Request Exit / Cashout</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tabs Bar */}
        <div className="flex items-center gap-2 border-b border-gold-accent/20 pb-2 text-xs">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2 rounded-xl font-bold transition flex items-center gap-1.5 ${
              activeTab === 'overview'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <ShieldCheck className="w-4 h-4" />
            <span>Overview & My Requests ({totalTablePendingRequests})</span>
          </button>

          <button
            onClick={() => setActiveTab('activity')}
            className={`px-4 py-2 rounded-xl font-bold transition flex items-center gap-1.5 ${
              activeTab === 'activity'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <History className="w-4 h-4" />
            <span>Activity History ({allTransactions.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('players')}
            className={`px-4 py-2 rounded-xl font-bold transition flex items-center gap-1.5 ${
              activeTab === 'players'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Seated Players ({players.length})</span>
          </button>
        </div>

        {/* TAB 1: OVERVIEW & MY TABLE REQUESTS */}
        {activeTab === 'overview' && (
          <div className="space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gold-accent flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              <span>My Pending & Recent Requests for this Table</span>
            </h3>

            {tableBuyInRequests.length === 0 && tableExitRequests.length === 0 ? (
              <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No active requests for this table.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {tableBuyInRequests.map((req) => (
                  <RequestCard
                    key={req.id}
                    request={req}
                    type="buy-in"
                    onConfirmReceipt={(r) => handleConfirmReceipt(r, 'buy-in')}
                  />
                ))}
                {tableExitRequests.map((req) => (
                  <RequestCard
                    key={req.id}
                    request={req}
                    type="exit"
                    onConfirmReceipt={(r) => handleConfirmReceipt(r, 'exit')}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: ACTIVITY HISTORY */}
        {activeTab === 'activity' && (
          <div className="bg-felt-card border border-gold-accent/30 rounded-2xl p-5 space-y-3 shadow-lg">
            <div className="flex items-center justify-between text-xs text-cream-text/60 mb-1">
              <span>All direct and request-approved transactions</span>
              <span>{allTransactions.length} records</span>
            </div>

            {allTransactions.length === 0 ? (
              <div className="p-8 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No buy-ins or exits recorded at this table yet.
              </div>
            ) : (
              <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {allTransactions.map((tx) => {
                  const isBuyIn = tx.txType === 'buy-in';
                  return (
                    <div
                      key={tx.id}
                      className="p-3.5 bg-felt-dark rounded-xl border border-gold-accent/20 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`p-2.5 rounded-xl ${
                            isBuyIn
                              ? 'bg-emerald-950 text-emerald-400 border border-emerald-500/30'
                              : 'bg-amber-950 text-amber-400 border border-amber-500/30'
                          }`}
                        >
                          {isBuyIn ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                        </div>
                        <div>
                          <div className="font-bold text-cream-text flex items-center gap-1.5 text-sm">
                            <span>{tx.playerName || 'Player'}</span>
                            {tx.playerName === user?.username && (
                              <span className="text-[10px] text-gold-accent font-normal">(You)</span>
                            )}
                          </div>
                          <div className="text-[11px] text-cream-text/50 flex items-center gap-1.5 mt-0.5">
                            <span>{formatDate(tx.timestamp || tx.created_at || tx.createdAt)}</span>
                            {tx.note && <span>• {tx.note}</span>}
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`font-mono font-extrabold text-base ${
                            isBuyIn ? 'text-emerald-400' : 'text-amber-400'
                          }`}
                        >
                          {isBuyIn ? `+$${Number(tx.amount).toLocaleString()}` : `-$${Number(tx.amount).toLocaleString()}`}
                        </div>
                        <span
                          className={`text-[9px] font-extrabold uppercase px-2 py-0.5 rounded ${
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

        {/* TAB 3: SEATED PLAYERS */}
        {activeTab === 'players' && (
          <div className="bg-felt-card border border-gold-accent/30 rounded-2xl p-5 space-y-3 shadow-lg">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-cream-text/60 mb-2">
              <div>
                <span className="font-bold text-cream-text">Current players seated at this table</span>
                <span className="ml-2 px-2 py-0.5 rounded-full bg-felt-dark border border-gold-accent/20 text-gold-accent font-semibold">{players.length} players</span>
              </div>
              {!isClosed && (
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setNewPlayerName('');
                      setIsAddPlayerModalOpen(true);
                    }}
                    className="px-3 py-1.5 bg-gradient-to-r from-gold-accent to-yellow-500 hover:opacity-95 text-black font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition active:scale-95 flex items-center gap-1.5 cursor-pointer"
                  >
                    <UserPlus className="w-3.5 h-3.5" />
                    <span>+ Add Player</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleOpenBuyInModal(null)}
                    className="px-3 py-1.5 bg-emerald-700 hover:bg-emerald-600 text-white font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition active:scale-95 flex items-center gap-1.5 cursor-pointer"
                  >
                    <PlusCircle className="w-3.5 h-3.5" />
                    <span>Record Buy-In</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleOpenExitModal(null)}
                    className="px-3 py-1.5 bg-amber-600 hover:bg-amber-500 text-black font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition active:scale-95 flex items-center gap-1.5 cursor-pointer"
                  >
                    <MinusCircle className="w-3.5 h-3.5" />
                    <span>Record Exit</span>
                  </button>
                </div>
              )}
            </div>

            {players.length === 0 ? (
              <div className="p-8 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No players currently seated at this table.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-[500px] overflow-y-auto">
                {players.map((p) => (
                  <div
                    key={p.id}
                    className="p-3.5 bg-felt-dark rounded-xl border border-gold-accent/20 flex flex-col gap-2.5 text-xs"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <UserBadge
                          avatarId={p.avatar_id || p.avatar}
                          name={p.name || p.display_name || p.username}
                          username={p.username}
                          size="md"
                        />
                        {(p.name === user?.username || p.username === user?.username) && (
                          <span className="text-[10px] text-gold-accent font-semibold px-1.5 py-0.5 rounded bg-gold-accent/10 border border-gold-accent/20">You</span>
                        )}
                      </div>

                      <div className="flex items-center gap-2">
                        {(table?.entry_fee || table?.entryFee) > 0 && (
                          (p.entry_fee_paid === 1 || p.entryFeePaid === true) ? (
                            <span className="px-2 py-0.5 text-[9px] font-extrabold uppercase rounded-full bg-emerald-950 text-emerald-300 border border-emerald-500/40">
                              Fee Paid ✓
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 text-[9px] font-extrabold uppercase rounded-full bg-red-950 text-red-300 border border-red-500/40">
                              Fee Unpaid
                            </span>
                          )
                        )}
                        <span className="px-2.5 py-1 text-[10px] font-extrabold rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                          {p.status || 'ACTIVE'}
                        </span>
                        {!isClosed && (
                          <button
                            onClick={() => handleDeletePlayerClick(p)}
                            className="p-1.5 rounded-lg bg-red-950/60 hover:bg-red-900 border border-red-500/40 text-red-400 hover:text-white transition cursor-pointer"
                            title="Remove player from table"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>

                    {!isClosed && (
                      <div className="flex items-center justify-end gap-1.5 pt-2 border-t border-gold-accent/10">
                        <button
                          type="button"
                          onClick={() => handleOpenBuyInModal(p)}
                          className="px-2 py-1 bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-500/40 text-emerald-300 text-[10px] font-bold rounded-lg transition active:scale-95 flex items-center gap-1 cursor-pointer"
                        >
                          <PlusCircle className="w-3 h-3" />
                          <span>+ Buy-In</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => handleOpenBuyInModal(p)}
                          className="px-2 py-1 bg-blue-950/80 hover:bg-blue-900 border border-blue-500/40 text-blue-300 text-[10px] font-bold rounded-lg transition active:scale-95 flex items-center gap-1 cursor-pointer"
                        >
                          <RefreshCw className="w-3 h-3" />
                          <span>Rebuy</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => handleOpenExitModal(p)}
                          className="px-2 py-1 bg-amber-950/80 hover:bg-amber-900 border border-amber-500/40 text-amber-300 text-[10px] font-bold rounded-lg transition active:scale-95 flex items-center gap-1 cursor-pointer"
                        >
                          <MinusCircle className="w-3 h-3" />
                          <span>- Exit</span>
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Add Player Modal */}
      {isAddPlayerModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-sm p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <h3 className="text-base font-black text-gold-accent uppercase tracking-wide mb-2 flex items-center gap-2">
              <UserPlus className="w-5 h-5 text-gold-accent" />
              <span>Add Player to Table</span>
            </h3>
            <p className="text-xs text-cream-text/70 mb-4 leading-relaxed">
              Manually add a player to this table by name.
            </p>
            <form onSubmit={handleAddPlayerSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-cream-text/70 uppercase mb-1">
                  Player Name
                </label>
                <input
                  type="text"
                  value={newPlayerName}
                  onChange={(e) => setNewPlayerName(e.target.value)}
                  placeholder="Enter player name..."
                  autoFocus
                  className="w-full px-3 py-2 bg-felt-dark border border-gold-accent/30 rounded-xl text-cream-text text-sm focus:outline-none focus:border-gold-accent"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setIsAddPlayerModalOpen(false);
                    setNewPlayerName('');
                  }}
                  disabled={isAddingPlayer}
                  className="px-4 py-2 bg-felt-dark border border-gold-accent/30 text-cream-text/70 rounded-xl text-xs font-bold hover:text-cream-text cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isAddingPlayer || !newPlayerName.trim()}
                  className="px-4 py-2 bg-gradient-to-r from-gold-accent to-yellow-500 hover:from-yellow-400 hover:to-gold-accent text-black rounded-xl text-xs font-black uppercase tracking-wider shadow-lg transition active:scale-95 disabled:opacity-50 cursor-pointer"
                >
                  {isAddingPlayer ? 'Adding...' : 'Add Player'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Player Modal */}
      {playerToDelete && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-red-500/80 rounded-2xl w-full max-w-sm p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <h3 className="text-base font-black text-red-400 uppercase tracking-wide mb-2 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-red-400" />
              <span>Remove Player</span>
            </h3>
            <p className="text-xs text-cream-text/80 mb-5 leading-relaxed">
              Are you sure you want to remove <strong>{playerToDelete.name || playerToDelete.username}</strong> from this table? This player has 0 buy-ins.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setPlayerToDelete(null)}
                disabled={isDeletingPlayer}
                className="px-4 py-2 bg-felt-dark border border-gold-accent/30 text-cream-text/70 rounded-xl text-xs font-bold hover:text-cream-text cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDeletePlayer}
                disabled={isDeletingPlayer}
                className="px-4 py-2 bg-red-800 hover:bg-red-700 text-white rounded-xl text-xs font-black uppercase tracking-wider shadow-lg transition active:scale-95 disabled:opacity-50 cursor-pointer"
              >
                {isDeletingPlayer ? 'Removing...' : 'Confirm Remove'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Buy-In Modal */}
      <BuyInModal
        isOpen={isBuyInModalOpen}
        onClose={() => {
          setIsBuyInModalOpen(false);
          setSelectedPlayerForBuyIn(null);
        }}
        players={players}
        initialPlayer={selectedPlayerForBuyIn}
        playerName={selectedPlayerForBuyIn?.name || myPlayer?.name || user?.username || 'Player'}
        currentBalance={selectedPlayerForBuyIn ? (selectedPlayerForBuyIn.balance ?? 0) : myTableNetBalance}
        onSubmit={handleBuyInSubmit}
      />

      {/* Exit Modal */}
      <ExitModal
        isOpen={isExitModalOpen}
        onClose={() => {
          setIsExitModalOpen(false);
          setSelectedPlayerForExit(null);
        }}
        players={players}
        initialPlayer={selectedPlayerForExit}
        playerName={selectedPlayerForExit?.name || myPlayer?.name || user?.username || 'Player'}
        currentBalance={selectedPlayerForExit ? (selectedPlayerForExit.balance ?? 0) : myTableNetBalance}
        onSubmit={handleExitSubmit}
      />
    </div>
  );
};

export default TableDetail;
