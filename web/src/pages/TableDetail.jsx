import React, { useState, useEffect, useRef, useMemo } from 'react';
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
  closeTable,
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
  Plus,
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
  X,
  Search,
  BarChart3,
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
  const [activeTab, setActiveTab] = useState('players'); // 'players' | 'history' | 'stats'
  const [searchQuery, setSearchQuery] = useState('');

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
  const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);
  const [isClosingTable, setIsClosingTable] = useState(false);

  const previousStatusRef = useRef(null);

  // Fetch all table data
  const fetchTableData = async (isBackground = false) => {
    if (!tableId) return;
    if (!isBackground) setLoading(true);

    try {
      const effectiveGroupId = groupId || table?.groupId;
      const requestsPromise = effectiveGroupId
        ? getMyRequests(effectiveGroupId, tableId)
        : Promise.resolve({ data: { joinRequests: [], buyInRequests: [], exitRequests: [] } });

      const [tableRes, statusRes, playersRes, activityRes, requestsRes] = await Promise.allSettled([
        getTableDetail(tableId),
        getTableStatus(tableId),
        getPlayers(tableId),
        getTableActivity(tableId),
        requestsPromise,
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
    socket.on('entry_fee_updated', handleRefresh);

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
      socket.off('entry_fee_updated', handleRefresh);
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

  const isHostOrAdmin =
    user?.role === 'SUPER_ADMIN' ||
    table?.host_id === user?.id ||
    table?.hostId === user?.id ||
    table?.creator_user_id === user?.id ||
    table?.creatorUserId === user?.id ||
    table?.isHost ||
    table?.isQuickTable ||
    !(groupId || table?.groupId);

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

  // Table totals (matches Android TableSummaryBar calculation)
  const totalBuyIns = useMemo(() => {
    return (activity.buyIns || []).reduce((sum, b) => sum + (Number(b.amount) || 0), 0);
  }, [activity.buyIns]);

  const totalExits = useMemo(() => {
    return (activity.exits || []).reduce((sum, e) => sum + (Number(e.amount) || 0), 0);
  }, [activity.exits]);

  const remainingBalance = totalBuyIns - totalExits;

  // Helper player balance & profit/loss functions
  const getPlayerBuyIns = (pId) => {
    return (activity.buyIns || [])
      .filter((b) => b.player_id === pId || b.playerId === pId)
      .reduce((sum, b) => sum + (Number(b.amount) || 0), 0);
  };

  const getPlayerExits = (pId) => {
    return (activity.exits || [])
      .filter((e) => e.player_id === pId || e.playerId === pId)
      .reduce((sum, e) => sum + (Number(e.amount) || 0), 0);
  };

  const getPlayerBalance = (p) => {
    const buy = getPlayerBuyIns(p.id);
    const exit = getPlayerExits(p.id);
    if (buy > 0 || exit > 0) {
      return buy - exit;
    }
    return p.balance ?? 0;
  };

  const getPlayerNetResult = (p) => {
    const buy = getPlayerBuyIns(p.id);
    const exit = getPlayerExits(p.id);
    return exit - buy;
  };

  // Deduplicated & ranked players
  const sortedPlayersWithRank = useMemo(() => {
    const playerMap = new Map();
    players.forEach((p) => {
      if (p.id) playerMap.set(p.id, p);
    });
    const unique = Array.from(playerMap.values());
    unique.sort((a, b) => getPlayerBalance(b) - getPlayerBalance(a));
    return unique.map((p, idx) => ({ player: p, rank: idx + 1 }));
  }, [players, activity.buyIns, activity.exits]);

  const filteredPlayers = useMemo(() => {
    if (!searchQuery.trim()) return sortedPlayersWithRank;
    const q = searchQuery.toLowerCase().trim();
    return sortedPlayersWithRank.filter(({ player }) =>
      (player.name || player.username || '').toLowerCase().includes(q)
    );
  }, [sortedPlayersWithRank, searchQuery]);

  // Player results for Stats tab
  const playerResults = useMemo(() => {
    return sortedPlayersWithRank.map(({ player }) => {
      const b = getPlayerBuyIns(player.id);
      const e = getPlayerExits(player.id);
      return {
        id: player.id,
        name: player.name || player.username || 'Player',
        status: player.status,
        buyIns: b,
        exits: e,
        netResult: e - b,
      };
    });
  }, [sortedPlayersWithRank, activity.buyIns, activity.exits]);

  // Settlements calculation (Creditors & Debtors)
  const settlements = useMemo(() => {
    const debtors = playerResults
      .filter((r) => r.netResult < 0)
      .map((r) => ({ ...r, remaining: Math.abs(r.netResult) }))
      .sort((a, b) => b.remaining - a.remaining);
    const creditors = playerResults
      .filter((r) => r.netResult > 0)
      .map((r) => ({ ...r, remaining: r.netResult }))
      .sort((a, b) => b.remaining - a.remaining);

    const result = [];
    let d = 0;
    let c = 0;

    while (d < debtors.length && c < creditors.length) {
      const debtor = debtors[d];
      const creditor = creditors[c];
      const amt = Math.min(debtor.remaining, creditor.remaining);

      if (amt > 0) {
        result.push({
          from: debtor.name,
          to: creditor.name,
          amount: amt,
        });
        debtor.remaining -= amt;
        creditor.remaining -= amt;
      }

      if (debtor.remaining <= 0) d++;
      if (creditor.remaining <= 0) c++;
    }

    return result;
  }, [playerResults]);

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
      await addTablePlayer(tableId, { name: cleanName, playerName: cleanName });
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
      const targetName = typeof payload === 'object' ? (payload.name || payload.playerName) : (myPlayer?.name || user?.username);
      const amt = Number(typeof payload === 'object' ? payload.amount : payload);
      const nt = typeof payload === 'object' ? payload.note : legacyNote;

      const effectiveGroupId = groupId || table?.groupId;

      if (isHostOrAdmin || !effectiveGroupId || targetName !== user?.username) {
        await directBuyIn(tableId, {
          playerId: pId,
          name: targetName,
          playerName: targetName,
          amount: amt,
          note: nt,
        });
        setSuccessMessage(`Buy-in of ${amt.toLocaleString()} chips recorded for ${targetName}!`);
      } else {
        await sendBuyInRequest(effectiveGroupId, tableId, amt, nt);
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
      const targetName = typeof payload === 'object' ? (payload.name || payload.playerName) : (myPlayer?.name || user?.username);
      const amt = Number(typeof payload === 'object' ? payload.amount : payload);
      const nt = typeof payload === 'object' ? payload.note : legacyNote;

      const effectiveGroupId = groupId || table?.groupId;

      if (isHostOrAdmin || !effectiveGroupId || targetName !== user?.username) {
        await directExit(tableId, {
          playerId: pId,
          name: targetName,
          playerName: targetName,
          amount: amt,
          note: nt,
        });
        setSuccessMessage(`Exit of ${amt.toLocaleString()} chips recorded for ${targetName}!`);
      } else {
        await sendExitRequest(effectiveGroupId, tableId, amt, nt);
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
    const buyIns = getPlayerBuyIns(p.id);
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

  const handleCloseTableConfirm = async () => {
    setIsClosingTable(true);
    try {
      await closeTable(tableId);
      setSuccessMessage('Table closed successfully. History is now locked.');
      setIsCloseModalOpen(false);
      fetchTableData(true);
      setTimeout(() => setSuccessMessage(''), 4000);
    } catch (err) {
      console.error('Failed to close table:', err);
      setError(err.response?.data?.error || 'Failed to close table.');
    } finally {
      setIsClosingTable(false);
    }
  };

  const handleShareResults = () => {
    const lines = [
      `🎰 ${table?.name || 'BankPoker Table'} Results 🎰`,
      `Total Buy-Ins: $${totalBuyIns.toLocaleString()}`,
      `Total Exits: $${totalExits.toLocaleString()}`,
      `Remaining Chips: $${remainingBalance.toLocaleString()}`,
      '',
      '--- PLAYER STANDINGS ---',
      ...playerResults.map(
        (r, i) =>
          `#${i + 1} ${r.name}: Buy-in $${r.buyIns.toLocaleString()} | Exit $${r.exits.toLocaleString()} | Net: ${
            r.netResult >= 0 ? '+' : ''
          }$${r.netResult.toLocaleString()}`
      ),
      '',
      ...(settlements.length > 0
        ? [
            '--- SUGGESTED SETTLEMENTS ---',
            ...settlements.map((s) => `• ${s.from} pays ${s.to}: $${s.amount.toLocaleString()}`),
          ]
        : ['No settlements needed.']),
    ];

    const shareText = lines.join('\n');
    if (navigator.clipboard) {
      navigator.clipboard.writeText(shareText);
      setSuccessMessage('Table results copied to clipboard!');
      setTimeout(() => setSuccessMessage(''), 4000);
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
    <div className="min-h-screen bg-felt-dark text-cream-text flex flex-col items-center py-6 px-4 sm:px-6 pb-28">
      <div className="w-full max-w-4xl space-y-5">
        {/* Top Bar (Matches Android TopAppBar: Back arrow, Table Name + LIVE badge, Actions) */}
        <div className="flex items-center justify-between gap-2 pb-1">
          <div className="flex items-center gap-3 min-w-0">
            <button
              onClick={() => navigate(groupId ? `/group/${groupId}` : '/')}
              className="p-2 bg-felt-card/80 hover:bg-felt-card border border-gold-accent/40 rounded-xl text-gold-accent transition cursor-pointer shrink-0"
              title="Back"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div className="flex items-center gap-2.5 truncate">
              <h1 className="text-xl sm:text-2xl font-black text-cream-text tracking-tight truncate">
                {table?.name || `Table ${tableId}`}
              </h1>
              {!isClosed ? (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-red-600 text-white animate-pulse shrink-0">
                  LIVE
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-zinc-800 text-zinc-400 border border-zinc-700 shrink-0">
                  CLOSED
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <NotificationsDropdown />
            <button
              onClick={() => fetchTableData()}
              className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent transition cursor-pointer"
              title="Refresh from Server"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
            <button
              onClick={handleShareResults}
              className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent transition cursor-pointer"
              title="Share Results"
            >
              <Share2 className="w-4 h-4" />
            </button>
            {!isClosed && isHostOrAdmin && (
              <button
                onClick={() => setIsCloseModalOpen(true)}
                className="px-3 py-1.5 bg-red-950/60 hover:bg-red-900 border border-red-500/50 rounded-xl text-red-300 hover:text-white text-xs font-bold transition cursor-pointer"
              >
                Close
              </button>
            )}
          </div>
        </div>

        {/* Closed Banner (Full Width Red/Orange Banner matching Android) */}
        {isClosed && (
          <div className="w-full py-3.5 px-4 bg-red-950/50 border border-red-500/60 rounded-2xl flex items-center justify-center gap-2.5 text-red-400 text-xs sm:text-sm font-black tracking-wider uppercase shadow-md animate-in fade-in">
            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
            <span>TABLE CLOSED • HISTORY LOCKED</span>
          </div>
        )}

        {/* Global Feedback Alerts */}
        {successMessage && (
          <div className="p-3 bg-emerald-950/90 border border-emerald-500 rounded-xl flex items-center gap-2 text-emerald-200 text-xs shadow-lg animate-in fade-in">
            <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}
        {error && (
          <div className="p-3 bg-red-950/90 border border-red-500 rounded-xl flex items-center gap-2 text-red-200 text-xs shadow-lg animate-in fade-in">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Table Summary Bar (Exact Match to Android TableSummaryBar Hero Card) */}
        <div className="bg-felt-card border-[1.5px] border-gold-accent/70 rounded-[20px] p-5 sm:p-6 shadow-2xl relative overflow-hidden">
          <div className="flex items-center justify-center gap-2 mb-4">
            <span className="text-gold-accent text-lg font-bold">♠</span>
            <h3 className="text-xs sm:text-sm font-bold text-cream-text uppercase tracking-[3px]">
              TABLE SUMMARY
            </h3>
          </div>

          <div className="grid grid-cols-3 gap-2 text-center divide-x divide-gold-accent/20">
            <div className="px-2">
              <div className="text-[10px] font-bold text-cream-text/60 tracking-wider uppercase mb-1">
                BUY-INS
              </div>
              <div className="text-lg sm:text-2xl font-bold font-mono text-win-green">
                ${totalBuyIns.toLocaleString()}
              </div>
            </div>
            <div className="px-2">
              <div className="text-[10px] font-bold text-cream-text/60 tracking-wider uppercase mb-1">
                EXITS
              </div>
              <div className="text-lg sm:text-2xl font-bold font-mono text-amber-400">
                ${totalExits.toLocaleString()}
              </div>
            </div>
            <div className="px-2">
              <div className="text-[10px] font-bold text-cream-text/60 tracking-wider uppercase mb-1">
                REMAINING
              </div>
              <div
                className={`text-lg sm:text-2xl font-bold font-mono ${
                  remainingBalance < 0
                    ? 'text-lose-red'
                    : remainingBalance === 0
                    ? 'text-cream-text'
                    : 'text-win-green'
                }`}
              >
                ${remainingBalance.toLocaleString()}
              </div>
            </div>
          </div>

          {/* Table Code / Publish Bar */}
          <div className="flex items-center justify-center gap-3 mt-4 pt-4 border-t border-gold-accent/20">
            {table?.code ? (
              <GroupCodeChip code={table.code} label="Table Code" />
            ) : !isClosed ? (
              <button
                onClick={handlePublishTable}
                disabled={isPublishing}
                className="px-4 py-1.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 text-gold-accent font-bold text-xs uppercase tracking-wider rounded-xl transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                <Share2 className="w-3.5 h-3.5 text-gold-accent" />
                <span>{isPublishing ? 'Publishing...' : 'Publish / Share Table'}</span>
              </button>
            ) : null}

            {(table?.chip_value || table?.chipValue) && (
              <span className="px-3 py-1 bg-felt-dark rounded-xl border border-gold-accent/30 text-[11px] font-bold text-gold-accent">
                Chip: ${table.chip_value || table.chipValue}
              </span>
            )}
            {(table?.has_entry_fee || table?.hasEntryFee) && (
              <span className="px-3 py-1 bg-felt-dark rounded-xl border border-amber-500/40 text-[11px] font-bold text-amber-400">
                Entry Fee: ${table.entry_fee || table.entryFee}
              </span>
            )}
          </div>
        </div>

        {/* Tab Navigation (Matching Android HorizontalPagerTabs: PLAYERS | HISTORY | STATS) */}
        <div className="grid grid-cols-3 gap-2 p-1.5 bg-felt-card/80 border border-gold-accent/30 rounded-2xl shadow-md">
          <button
            type="button"
            onClick={() => setActiveTab('players')}
            className={`py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition cursor-pointer ${
              activeTab === 'players'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            PLAYERS ({players.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('history')}
            className={`py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition cursor-pointer ${
              activeTab === 'history'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            HISTORY ({allTransactions.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('stats')}
            className={`py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition cursor-pointer ${
              activeTab === 'stats'
                ? 'bg-gold-accent text-black shadow'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            STATS
          </button>
        </div>

        {/* ================= TAB 1: PLAYERS ================= */}
        {activeTab === 'players' && (
          <div className="space-y-4">
            {/* Action Row Below Tabs (Matching Android: Record Buy-In + Record Exit side-by-side) */}
            {!isClosed && (
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => handleOpenBuyInModal(null)}
                  className="py-3 px-4 bg-gold-accent hover:brightness-105 active:scale-[0.98] text-black font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Plus className="w-4 h-4 stroke-[3]" />
                  <span>Record Buy-In</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleOpenExitModal(null)}
                  className="py-3 px-4 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/60 active:scale-[0.98] text-gold-accent font-extrabold text-xs uppercase tracking-wider rounded-xl shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                >
                  <MinusCircle className="w-4 h-4" />
                  <span>Record Exit</span>
                </button>
              </div>
            )}

            {/* Pending Requests Notice Card (if any pending requests) */}
            {totalTablePendingRequests > 0 && (
              <div className="p-4 bg-amber-950/40 border border-amber-500/50 rounded-2xl space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-amber-300 uppercase tracking-wider flex items-center gap-1.5">
                    <Clock className="w-4 h-4" />
                    <span>Pending Requests ({totalTablePendingRequests})</span>
                  </span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
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
              </div>
            )}

            {/* Player Search Bar (Matches Android OutlinedTextField) */}
            {players.length > 0 && (
              <div className="relative">
                <Search className="w-4 h-4 text-gold-accent absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search players..."
                  className="w-full pl-10 pr-10 py-2.5 bg-felt-card border border-gold-accent/40 rounded-xl text-cream-text text-xs focus:outline-none focus:border-gold-accent placeholder:text-cream-text/40 transition"
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gold-accent hover:text-gold-light"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>
            )}

            {/* Empty State */}
            {players.length === 0 ? (
              <div className="p-12 bg-felt-card/60 rounded-2xl text-center space-y-3 border border-gold-accent/20">
                <div className="text-4xl text-gold-accent/40">♠</div>
                <div className="font-bold text-cream-text text-sm">No players yet</div>
                <div className="text-xs text-cream-text/50">Tap + to add players</div>
                {!isClosed && (
                  <button
                    type="button"
                    onClick={() => {
                      setNewPlayerName('');
                      setIsAddPlayerModalOpen(true);
                    }}
                    className="mt-2 px-5 py-2.5 bg-gold-accent hover:brightness-105 text-black font-extrabold text-xs uppercase tracking-wider rounded-xl shadow transition cursor-pointer"
                  >
                    Add Player
                  </button>
                )}
              </div>
            ) : filteredPlayers.length === 0 ? (
              <div className="p-8 bg-felt-card/60 rounded-2xl text-center text-xs text-cream-text/60 border border-gold-accent/20">
                No players found matching "{searchQuery}".
              </div>
            ) : (
              /* Player Cards List (Matches Android PlayerCard layout) */
              <div className="space-y-3">
                {filteredPlayers.map(({ player: p, rank }) => {
                  const balance = getPlayerBalance(p);
                  const finalResult = getPlayerNetResult(p);
                  const isExited = p.status === 'EXITED';
                  const playerBuyIns = getPlayerBuyIns(p.id);

                  // Rank border color (1: Gold, 2: Silver, 3: Bronze, else Gold/40)
                  const rankBorder =
                    rank === 1
                      ? 'border-gold-accent shadow-gold-accent/10 shadow-lg'
                      : rank === 2
                      ? 'border-[#c0c0c0]'
                      : rank === 3
                      ? 'border-[#cd7f32]'
                      : 'border-gold-accent/40';

                  const rankPillBg =
                    rank === 1
                      ? 'bg-gold-accent text-black'
                      : rank === 2
                      ? 'bg-[#c0c0c0] text-black'
                      : rank === 3
                      ? 'bg-[#cd7f32] text-black'
                      : 'bg-gold-accent/15 text-gold-accent';

                  return (
                    <div
                      key={p.id}
                      className={`bg-felt-card border-2 ${rankBorder} rounded-[20px] p-4 sm:p-5 transition shadow-md`}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          {/* Rank badge */}
                          <span
                            className={`px-2 py-0.5 rounded-lg text-[10px] font-bold shrink-0 ${rankPillBg}`}
                          >
                            {rank === 1
                              ? '🥇 #1'
                              : rank === 2
                              ? '🥈 #2'
                              : rank === 3
                              ? '🥉 #3'
                              : `#${rank}`}
                          </span>

                          <UserBadge
                            avatarId={p.avatar_id || p.avatar}
                            name={p.name || p.display_name || p.username}
                            username={p.username}
                            size="md"
                          />

                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5 truncate">
                              <span className="font-bold text-cream-text text-sm sm:text-base truncate">
                                {p.name || p.username}
                              </span>
                              {(p.name === user?.username || p.username === user?.username) && (
                                <span className="px-1.5 py-0.5 bg-gold-accent/20 border border-gold-accent/40 text-gold-accent text-[9px] font-bold rounded">
                                  You
                                </span>
                              )}
                            </div>

                            {/* Status badge / Entry fee status */}
                            {(table?.has_entry_fee || table?.hasEntryFee) && !isExited ? (
                              <div className="text-[11px] mt-0.5">
                                <span className="text-cream-text/60">Entry Fee: </span>
                                <span
                                  className={
                                    p.entry_fee_paid === 1 || p.entryFeePaid === true
                                      ? 'text-win-green font-bold'
                                      : 'text-lose-red font-bold'
                                  }
                                >
                                  {p.entry_fee_paid === 1 || p.entryFeePaid === true ? 'Paid' : 'Unpaid'}
                                </span>
                              </div>
                            ) : (
                              <div className="mt-0.5">
                                <StatusBadge status={p.status || 'ACTIVE'} />
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Balance display & Delete button */}
                        <div className="flex items-center gap-3 shrink-0">
                          {!isExited && (
                            <div className="text-right">
                              <div
                                className={`text-base sm:text-xl font-bold font-mono ${
                                  balance >= 0 ? 'text-win-green' : 'text-lose-red'
                                }`}
                              >
                                {balance >= 0 ? `+${balance}` : balance}
                              </div>
                              <div className="text-[10px] text-cream-text/50 uppercase">chips</div>
                            </div>
                          )}

                          {!isClosed && playerBuyIns === 0 && (
                            <button
                              type="button"
                              onClick={() => handleDeletePlayerClick(p)}
                              className="p-1.5 text-lose-red/80 hover:text-lose-red hover:bg-red-950/40 rounded-lg transition cursor-pointer"
                              title="Remove Player (0 buy-ins)"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </div>

                      {/* EXITED Status Result Banner & Rebuy Button */}
                      {isExited && (
                        <div className="mt-3 space-y-2">
                          <div
                            className={`p-2.5 rounded-lg text-xs font-bold text-center ${
                              finalResult > 0
                                ? 'bg-win-green/15 text-win-green border border-win-green/30'
                                : finalResult < 0
                                ? 'bg-lose-red/15 text-lose-red border border-lose-red/30'
                                : 'bg-felt-dark text-cream-text border border-gold-accent/20'
                            }`}
                          >
                            {finalResult > 0
                              ? `Creditor: +$${finalResult.toLocaleString()}`
                              : finalResult < 0
                              ? `Debtor: -$${Math.abs(finalResult).toLocaleString()}`
                              : 'Break-even'}
                          </div>

                          {!isClosed && (
                            <button
                              type="button"
                              onClick={() => handleOpenBuyInModal(p)}
                              className="w-full py-2.5 bg-gold-accent hover:brightness-105 active:scale-[0.99] text-black font-extrabold text-xs uppercase tracking-wider rounded-xl transition flex items-center justify-center gap-1.5 cursor-pointer shadow"
                            >
                              <Plus className="w-3.5 h-3.5 stroke-[3]" />
                              <span>Rebuy / Buy-In</span>
                            </button>
                          )}
                        </div>
                      )}

                      {/* PLAYING Status Action Buttons */}
                      {!isExited && !isClosed && (
                        <div className="mt-3 grid grid-cols-2 gap-2 pt-2 border-t border-gold-accent/10">
                          <button
                            type="button"
                            onClick={() => handleOpenBuyInModal(p)}
                            className="py-2.5 bg-gold-accent hover:brightness-105 active:scale-[0.98] text-black font-extrabold text-xs uppercase tracking-wider rounded-xl transition flex items-center justify-center gap-1 cursor-pointer shadow"
                          >
                            <Plus className="w-3.5 h-3.5 stroke-[3]" />
                            <span>Buy-In</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleOpenExitModal(p)}
                            className="py-2.5 bg-transparent hover:bg-gold-accent/10 border border-gold-accent text-gold-accent font-extrabold text-xs uppercase tracking-wider rounded-xl transition flex items-center justify-center gap-1 cursor-pointer active:scale-[0.98]"
                          >
                            <MinusCircle className="w-3.5 h-3.5" />
                            <span>Exit</span>
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ================= TAB 2: HISTORY ================= */}
        {activeTab === 'history' && (
          <div className="bg-felt-card border border-gold-accent/30 rounded-2xl p-5 space-y-3 shadow-lg">
            <div className="flex items-center justify-between text-xs text-cream-text/60 mb-2">
              <span>All recorded buy-ins and exits</span>
              <span>{allTransactions.length} records</span>
            </div>

            {allTransactions.length === 0 ? (
              <div className="p-8 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                No transactions recorded at this table yet.
              </div>
            ) : (
              <div className="space-y-2 max-h-[550px] overflow-y-auto pr-1">
                {allTransactions.map((tx) => {
                  const isBuyIn = tx.txType === 'buy-in';
                  return (
                    <div
                      key={tx.id}
                      className="p-3.5 bg-felt-dark rounded-xl border border-gold-accent/20 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`p-2 rounded-xl ${
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
                            isBuyIn ? 'text-win-green' : 'text-amber-400'
                          }`}
                        >
                          {isBuyIn
                            ? `+$${Number(tx.amount).toLocaleString()}`
                            : `-$${Number(tx.amount).toLocaleString()}`}
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

        {/* ================= TAB 3: STATS / RESULTS ================= */}
        {activeTab === 'stats' && (
          <div className="space-y-4">
            {/* Player Results Table */}
            <div className="bg-felt-card border border-gold-accent/30 rounded-2xl p-5 shadow-lg space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold uppercase tracking-wider text-gold-accent flex items-center gap-1.5">
                  <BarChart3 className="w-4 h-4" />
                  <span>Player Results & Net Position</span>
                </h3>
                <button
                  type="button"
                  onClick={handleShareResults}
                  className="px-3 py-1.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition flex items-center gap-1 cursor-pointer"
                >
                  <Share2 className="w-3.5 h-3.5" />
                  <span>Share Results</span>
                </button>
              </div>

              {playerResults.length === 0 ? (
                <div className="p-8 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                  No player results yet.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead>
                      <tr className="border-b border-gold-accent/20 text-cream-text/60 uppercase text-[10px]">
                        <th className="py-2.5 px-3">Player</th>
                        <th className="py-2.5 px-3 text-right">Buy-Ins</th>
                        <th className="py-2.5 px-3 text-right">Exits</th>
                        <th className="py-2.5 px-3 text-right">Net Result</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gold-accent/10">
                      {playerResults.map((r) => (
                        <tr key={r.id} className="hover:bg-felt-dark/40 transition">
                          <td className="py-2.5 px-3 font-bold text-cream-text flex items-center gap-1.5">
                            <span>{r.name}</span>
                            {r.name === user?.username && (
                              <span className="text-[9px] text-gold-accent">(You)</span>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-cream-text/80">
                            ${r.buyIns.toLocaleString()}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-cream-text/80">
                            ${r.exits.toLocaleString()}
                          </td>
                          <td
                            className={`py-2.5 px-3 text-right font-mono font-extrabold ${
                              r.netResult > 0
                                ? 'text-win-green'
                                : r.netResult < 0
                                ? 'text-lose-red'
                                : 'text-cream-text/60'
                            }`}
                          >
                            {r.netResult > 0 ? `+$${r.netResult.toLocaleString()}` : `$${r.netResult.toLocaleString()}`}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Suggested Settlements Section */}
            <div className="bg-felt-card border border-gold-accent/30 rounded-2xl p-5 shadow-lg space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-gold-accent flex items-center gap-1.5">
                <Coins className="w-4 h-4" />
                <span>Suggested Settlements</span>
              </h3>

              {settlements.length === 0 ? (
                <div className="p-6 bg-felt-dark/60 rounded-xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                  All balances are settled or no exits recorded yet.
                </div>
              ) : (
                <div className="space-y-2">
                  {settlements.map((s, idx) => (
                    <div
                      key={idx}
                      className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-lose-red">{s.from}</span>
                        <span className="text-cream-text/50">pays</span>
                        <span className="font-bold text-win-green">{s.to}</span>
                      </div>
                      <div className="font-mono font-black text-sm text-gold-accent">
                        ${s.amount.toLocaleString()}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Floating Action Button (+) for Add Player (Matches Android lines 881-896) */}
      {!isClosed && (
        <button
          type="button"
          onClick={() => {
            setNewPlayerName('');
            setIsAddPlayerModalOpen(true);
          }}
          className="fixed bottom-6 right-6 w-14 h-14 bg-gradient-to-tr from-yellow-600 via-gold-accent to-yellow-400 hover:opacity-95 text-black rounded-full shadow-2xl border-2 border-gold-light flex items-center justify-center active:scale-95 transition-all z-40 cursor-pointer"
          title="Add Player"
        >
          <Plus className="w-7 h-7 stroke-[2.5]" />
        </button>
      )}

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

      {/* Close Table Confirmation Modal */}
      {isCloseModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-red-500 rounded-2xl w-full max-w-sm p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <h3 className="text-base font-black text-red-400 uppercase tracking-wide mb-2 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-red-400" />
              <span>Close Table Session</span>
            </h3>
            <p className="text-xs text-cream-text/80 mb-5 leading-relaxed">
              Are you sure you want to close this table? All transaction history will be locked and no further buy-ins or exits will be allowed.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setIsCloseModalOpen(false)}
                disabled={isClosingTable}
                className="px-4 py-2 bg-felt-dark border border-gold-accent/30 text-cream-text/70 rounded-xl text-xs font-bold hover:text-cream-text cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleCloseTableConfirm}
                disabled={isClosingTable}
                className="px-4 py-2 bg-red-700 hover:bg-red-600 text-white rounded-xl text-xs font-black uppercase tracking-wider shadow-lg transition active:scale-95 disabled:opacity-50 cursor-pointer"
              >
                {isClosingTable ? 'Closing...' : 'Confirm Close Table'}
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
        playerName={selectedPlayerForBuyIn?.name || (isPlayerSeated ? myPlayer?.name : '') || ''}
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
        playerName={selectedPlayerForExit?.name || (isPlayerSeated ? myPlayer?.name : '') || ''}
        currentBalance={selectedPlayerForExit ? (selectedPlayerForExit.balance ?? 0) : myTableNetBalance}
        onSubmit={handleExitSubmit}
      />
    </div>
  );
};

export default TableDetail;
