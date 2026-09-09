import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getGroupStats,
  getMyRequests,
  getGroupTables,
  getMyGroups,
  getGroupBalances,
  getGroupSettlementPlan,
  getGroupStatsDetails,
  createTable,
  getGroupPlayersList,
} from '../api';
import TableCard from '../components/TableCard';
import RequestCard from '../components/RequestCard';
import BalancesTab from '../components/BalancesTab';
import StatsTab from '../components/StatsTab';
import NotificationsDropdown from '../components/NotificationsDropdown';
import { getSocket, joinGroup, leaveGroup } from '../socket';
import {
  ArrowLeft,
  RefreshCw,
  Trophy,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Clock,
  Layers,
  Shield,
  Plus,
  Users,
  BarChart3,
  X,
  Check,
  AlertCircle
} from 'lucide-react';

const CHIP_PRESETS = [5, 10, 25, 50, 100];

const GroupStats = () => {
  const { id: groupId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Group metadata & Data
  const [group, setGroup] = useState(null);
  const [stats, setStats] = useState(null);
  const [tables, setTables] = useState([]);
  const [requests, setRequests] = useState({ joinRequests: [], buyInRequests: [], exitRequests: [] });
  const [balances, setBalances] = useState([]);
  const [settlementPlan, setSettlementPlan] = useState([]);
  const [groupStatsDetails, setGroupStatsDetails] = useState(null);

  // Tab State: 'tables' | 'balances' | 'stats'
  const [activeTab, setActiveTab] = useState('tables');

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Create Table Modal State (mirrors Android CreateTableBottomSheet)
  const [isCreateTableOpen, setIsCreateTableOpen] = useState(false);
  const [newTableName, setNewTableName] = useState('');
  const [newChipPreset, setNewChipPreset] = useState(null);
  const [newChipCustom, setNewChipCustom] = useState('');
  const [newHasEntryFee, setNewHasEntryFee] = useState(false);
  const [newEntryFeeAmount, setNewEntryFeeAmount] = useState('');
  const [groupPlayers, setGroupPlayers] = useState([]);
  const [selectedPlayerIds, setSelectedPlayerIds] = useState(new Set());
  const [manualPlayerNames, setManualPlayerNames] = useState('');
  const [createTableLoading, setCreateTableLoading] = useState(false);
  const [createTableError, setCreateTableError] = useState('');

  // 1. Fetch group info
  const fetchGroupInfo = async () => {
    try {
      const response = await getMyGroups();
      const userGroups = response.data?.groups || [];
      const found = userGroups.find((g) => g.id === groupId);
      if (found) {
        setGroup(found);
      }
    } catch (err) {
      console.error('Failed to fetch group info:', err);
    }
  };

  // 2. Fetch stats, tables & requests
  const fetchData = async (isBackground = false) => {
    if (!isBackground) {
      setLoading(true);
      setError('');
    }

    try {
      const [
        statsRes,
        tablesData,
        requestsRes,
        balancesRes,
        settlementRes,
        groupStatsRes,
      ] = await Promise.allSettled([
        getGroupStats(groupId),
        getGroupTables(groupId),
        getMyRequests(groupId),
        getGroupBalances(groupId),
        getGroupSettlementPlan(groupId),
        getGroupStatsDetails(groupId),
      ]);

      if (statsRes.status === 'fulfilled') {
        const rawStats = statsRes.value.data?.stats || statsRes.value.data || null;
        if (rawStats && Array.isArray(rawStats.recentTransactions)) {
          const txMap = new Map();
          rawStats.recentTransactions.forEach((tx) => {
            const key = tx.id || `${tx.type}-${tx.timestamp}-${tx.amount}`;
            txMap.set(key, tx);
          });
          rawStats.recentTransactions = Array.from(txMap.values());
        }
        setStats(rawStats);
        if (statsRes.value.data?.group) {
          setGroup(statsRes.value.data.group);
        }
      }

      if (tablesData.status === 'fulfilled') {
        const rawTables = tablesData.value || [];
        const tMap = new Map();
        rawTables.forEach((t) => {
          if (t.id) tMap.set(t.id, t);
        });
        setTables(Array.from(tMap.values()));
      }

      if (requestsRes.status === 'fulfilled') {
        setRequests(requestsRes.value.data || { joinRequests: [], buyInRequests: [], exitRequests: [] });
      }

      if (balancesRes.status === 'fulfilled') {
        const rawBalances = balancesRes.value.data?.balances || [];
        const idMap = new Map();
        const nameMap = new Map();
        const deduplicated = [];

        rawBalances.forEach((b) => {
          if (!b) return;
          const playerId = b.playerId || b.player_id || b.id || b.userId || b.user_id;
          const normalizedName = (b.playerName || b.username || b.name || '').trim().toLowerCase();

          let existing = null;
          if (playerId && idMap.has(String(playerId))) {
            existing = idMap.get(String(playerId));
          } else if (normalizedName && nameMap.has(normalizedName)) {
            existing = nameMap.get(normalizedName);
          }

          if (existing) {
            Object.assign(existing, b);
            if (playerId) idMap.set(String(playerId), existing);
            if (normalizedName) nameMap.set(normalizedName, existing);
          } else {
            const entry = { ...b };
            deduplicated.push(entry);
            if (playerId) idMap.set(String(playerId), entry);
            if (normalizedName) nameMap.set(normalizedName, entry);
          }
        });

        setBalances(deduplicated);
      }

      if (settlementRes.status === 'fulfilled') {
        setSettlementPlan(settlementRes.value.data?.settlement || []);
      }

      if (groupStatsRes.status === 'fulfilled') {
        setGroupStatsDetails(groupStatsRes.value.data?.stats || groupStatsRes.value.data || null);
      }
    } catch (err) {
      console.error('Failed to load group details:', err);
      if (!isBackground) {
        setError('Failed to load group data. Please try again.');
      }
    } finally {
      if (!isBackground) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchGroupInfo();
    fetchData();

    // Join WebSocket room for this group
    joinGroup(groupId);

    const socket = getSocket();

    const handleRefresh = () => {
      fetchData(true);
    };

    const handleTableClosed = (payload) => {
      if (payload && payload.tableId) {
        setTables((prev) =>
          prev.map((t) => (t.id === payload.tableId ? { ...t, status: 'CLOSED', isActive: false } : t))
        );
      }
      fetchData(true);
    };

    socket.on('table_created', handleRefresh);
    socket.on('table_closed', handleTableClosed);
    socket.on('table_updated', handleRefresh);
    socket.on('table_published', handleRefresh);
    socket.on('buyin_recorded', handleRefresh);
    socket.on('exit_recorded', handleRefresh);
    socket.on('payment_created', handleRefresh);
    socket.on('request_created', handleRefresh);
    socket.on('request_resolved', handleRefresh);
    socket.on('player_added', handleRefresh);
    socket.on('player_deleted', handleRefresh);
    socket.on('group_updated', handleRefresh);
    socket.on('claim_done', handleRefresh);
    socket.on('settlement_done', handleRefresh);
    socket.on('entry_fee_updated', handleRefresh);

    const interval = setInterval(() => {
      fetchData(true);
    }, 3500);

    return () => {
      clearInterval(interval);
      leaveGroup(groupId);
      socket.off('table_created', handleRefresh);
      socket.off('table_closed', handleTableClosed);
      socket.off('table_updated', handleRefresh);
      socket.off('table_published', handleRefresh);
      socket.off('buyin_recorded', handleRefresh);
      socket.off('exit_recorded', handleRefresh);
      socket.off('payment_created', handleRefresh);
      socket.off('request_created', handleRefresh);
      socket.off('request_resolved', handleRefresh);
      socket.off('player_added', handleRefresh);
      socket.off('player_deleted', handleRefresh);
      socket.off('group_updated', handleRefresh);
      socket.off('claim_done', handleRefresh);
      socket.off('settlement_done', handleRefresh);
      socket.off('entry_fee_updated', handleRefresh);
    };
  }, [groupId]);

  const handleTableClick = (table) => {
    navigate(`/group/${groupId}/table/${table.id}`);
  };

  const handleOpenCreateTable = async () => {
    setNewTableName('');
    setNewChipPreset(null);
    setNewChipCustom('');
    setNewHasEntryFee(false);
    setNewEntryFeeAmount('');
    setSelectedPlayerIds(new Set());
    setManualPlayerNames('');
    setCreateTableError('');
    setIsCreateTableOpen(true);

    try {
      const res = await getGroupPlayersList(groupId);
      setGroupPlayers(res.data?.players || []);
    } catch (err) {
      console.warn('Failed to load group players list:', err);
    }
  };

  const togglePlayerSelection = (playerId) => {
    setSelectedPlayerIds((prev) => {
      const next = new Set(prev);
      if (next.has(playerId)) {
        next.delete(playerId);
      } else {
        next.add(playerId);
      }
      return next;
    });
  };

  const handleCreateTableSubmit = async (e) => {
    e.preventDefault();
    const trimmedName = newTableName.trim();
    if (!trimmedName) {
      setCreateTableError('Table name is required');
      return;
    }
    setCreateTableLoading(true);
    setCreateTableError('');

    try {
      const effectiveChipValue = newChipCustom
        ? Number(newChipCustom)
        : newChipPreset
        ? Number(newChipPreset)
        : null;
      const numEntryFee = newHasEntryFee && newEntryFeeAmount
        ? Number(newEntryFeeAmount)
        : null;
      const extraNames = manualPlayerNames
        .split(/[,\n]+/)
        .map((n) => n.trim())
        .filter(Boolean);

      const res = await createTable({
        groupId,
        name: trimmedName,
        chipValue: effectiveChipValue,
        default_buy_in: effectiveChipValue,
        entryFee: numEntryFee,
        memberPlayerIds: Array.from(selectedPlayerIds),
        newPlayerNames: extraNames,
      });

      setIsCreateTableOpen(false);
      fetchData(true);
      const createdId = res.data?.tableId || res.data?.table?.id || res.data?.id;
      if (createdId) {
        navigate(`/group/${groupId}/table/${createdId}`);
      }
    } catch (err) {
      console.error('Failed to create group table:', err);
      setCreateTableError(err.response?.data?.error || 'Failed to create table.');
    } finally {
      setCreateTableLoading(false);
    }
  };

  // Server is the ONLY balance authority
  const myPlayerInBalances = balances.find((b) =>
    (user && (b.userId === user.id || b.user_id === user.id)) ||
    b.isMe ||
    (user && (b.username || b.name)?.toLowerCase() === user.username?.toLowerCase())
  );
  const myBalance = myPlayerInBalances !== undefined
    ? (myPlayerInBalances.balance ?? 0)
    : (stats?.myBalance ?? stats?.balance ?? stats?.currentBalance ?? 0);
  const myBuyIns = myPlayerInBalances?.totalBuyIns ?? stats?.myBuyIns ?? stats?.userTotalBuyIns ?? stats?.totalBuyIns ?? 0;
  const myExits = myPlayerInBalances?.totalExits ?? stats?.myExits ?? stats?.userTotalExits ?? stats?.totalExits ?? 0;
  const isPositive = myBalance >= 0;
  const activeTablesCount = tables.filter((t) => t.status === 'ACTIVE' || t.isActive).length;

  return (
    <div className="min-h-screen bg-felt-dark text-cream-text flex flex-col items-center py-6 px-4 sm:px-6">
      <div className="w-full max-w-4xl space-y-6">
        {/* Navigation & Header */}
        <div className="flex items-center justify-between">
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 px-3.5 py-2 bg-felt-card/80 hover:bg-felt-card border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Dashboard</span>
          </Link>

          <div className="flex items-center gap-2">
            <NotificationsDropdown />
            <button
              onClick={() => fetchData()}
              className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition cursor-pointer"
              title="Refresh Group Data"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Group Hero Card */}
        <div className="bg-felt-card border-2 border-gold-accent rounded-2xl p-6 shadow-2xl relative overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl font-black tracking-tight text-cream-text">
                  {group?.name || 'Poker Club'}
                </h1>
              </div>
              <p className="text-xs text-cream-text/60 mt-0.5">
                Club Tables & Personal Performance
              </p>
            </div>

            <div className="flex items-center gap-2">
              <div className="px-3 py-2 bg-felt-dark rounded-xl border border-gold-accent/30 text-center text-xs">
                <div className="text-[10px] text-cream-text/50 uppercase">Active Tables</div>
                <div className="font-bold text-gold-accent">{activeTablesCount}</div>
              </div>
              <div className="px-3 py-2 bg-felt-dark rounded-xl border border-gold-accent/30 text-center text-xs">
                <div className="text-[10px] text-cream-text/50 uppercase">Total Tables</div>
                <div className="font-bold text-cream-text">{tables.length}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Personal Stats Cards (Logged-in Player's Own Stats) */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 shadow-lg flex items-center justify-between">
            <div>
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">MY BALANCE</div>
              <div
                className={`text-xl font-extrabold font-mono mt-0.5 ${
                  isPositive ? 'text-emerald-400' : 'text-red-400'
                }`}
              >
                {isPositive ? `+$${myBalance.toLocaleString()}` : `-$${Math.abs(myBalance).toLocaleString()}`}
              </div>
            </div>
            <div
              className={`p-3 rounded-xl ${
                isPositive ? 'bg-emerald-950 text-emerald-400' : 'bg-red-950 text-red-400'
              } border border-gold-accent/20`}
            >
              <Trophy className="w-5 h-5" />
            </div>
          </div>

          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 shadow-lg flex items-center justify-between">
            <div>
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">MY BUY-INS</div>
              <div className="text-xl font-extrabold font-mono text-emerald-400 mt-0.5">
                ${myBuyIns.toLocaleString()}
              </div>
            </div>
            <div className="p-3 bg-felt-dark text-emerald-400 rounded-xl border border-gold-accent/20">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>

          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 shadow-lg flex items-center justify-between">
            <div>
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">MY EXITS</div>
              <div className="text-xl font-extrabold font-mono text-amber-400 mt-0.5">
                ${myExits.toLocaleString()}
              </div>
            </div>
            <div className="p-3 bg-felt-dark text-amber-400 rounded-xl border border-gold-accent/20">
              <TrendingDown className="w-5 h-5" />
            </div>
          </div>
        </div>

        {/* Navigation Tabs (Like Android App) */}
        <div className="flex items-center gap-2 border-b border-gold-accent/20 pb-3">
          <button
            onClick={() => setActiveTab('tables')}
            className={`px-4 py-2 rounded-xl font-bold text-xs transition flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'tables'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <Layers className="w-4 h-4" />
            <span>Tables ({tables.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('balances')}
            className={`px-4 py-2 rounded-xl font-bold text-xs transition flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'balances'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Balances ({balances.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('stats')}
            className={`px-4 py-2 rounded-xl font-bold text-xs transition flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'stats'
                ? 'bg-gold-accent text-black shadow-md'
                : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
            }`}
          >
            <BarChart3 className="w-4 h-4" />
            <span>Stats & Settlement</span>
          </button>
        </div>

        {/* TAB 1: TABLES */}
        {activeTab === 'tables' && (
          <div className="space-y-6 animate-in fade-in duration-200">
            {/* Section: Tables in Group */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-bold uppercase tracking-wider text-gold-accent flex items-center gap-2">
                  <Layers className="w-4 h-4" />
                  <span>Poker Tables & Rooms ({tables.length})</span>
                </h2>

                <button
                  type="button"
                  onClick={handleOpenCreateTable}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/50 text-gold-accent text-xs font-bold uppercase rounded-xl shadow transition active:scale-95 cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5 text-gold-accent" />
                  <span>New Table</span>
                </button>
              </div>

              {loading && tables.length === 0 ? (
                <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
                  Loading active tables...
                </div>
              ) : tables.length === 0 ? (
                <div className="p-8 bg-felt-card rounded-2xl text-center space-y-3 border border-gold-accent/20">
                  <p className="text-xs text-cream-text/60">
                    No poker tables have been created in this group yet.
                  </p>
                  <button
                    type="button"
                    onClick={handleOpenCreateTable}
                    className="px-4 py-2 bg-gold-accent text-black font-bold text-xs rounded-xl shadow hover:bg-gold-light transition cursor-pointer"
                  >
                    + Create First Table
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {tables.map((table) => (
                    <TableCard
                      key={table.id}
                      table={table}
                      onClick={() => handleTableClick(table)}
                    />
                  ))}
                </div>
              )}
            </div>

            {/* Section: My Join Requests */}
            {(requests.joinRequests || []).length > 0 && (
              <div className="space-y-3 pt-2">
                <h2 className="text-sm font-bold uppercase tracking-wider text-gold-accent flex items-center gap-2">
                  <Clock className="w-4 h-4" />
                  <span>My Table Join Requests ({(requests.joinRequests || []).length})</span>
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {requests.joinRequests.map((req) => (
                    <RequestCard key={req.id} request={req} type="join" />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: BALANCES */}
        {activeTab === 'balances' && (
          <BalancesTab balances={balances} groupBalances={balances} loading={loading} />
        )}

        {/* TAB 3: STATS & SETTLEMENT */}
        {activeTab === 'stats' && (
          <StatsTab
            groupId={groupId}
            stats={groupStatsDetails}
            settlement={settlementPlan}
            balances={balances}
            loading={loading}
            onRefresh={() => fetchData(true)}
          />
        )}
      </div>

      {/* CREATE TABLE MODAL (Checklist + Manual + Entry Fee) */}
      {isCreateTableOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-[28px] w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            {/* Drag Handle */}
            <div className="w-10 h-1 bg-gold-accent/60 rounded-full mx-auto mb-3" />

            <div className="flex items-center justify-between pb-3 mb-4 border-b border-gold-accent/30">
              <div className="flex items-center gap-2">
                <span className="text-gold-accent text-sm font-bold">♠</span>
                <h3 className="text-sm font-bold text-cream-text uppercase tracking-[2px]">
                  NEW GROUP TABLE
                </h3>
              </div>
              <button
                onClick={() => setIsCreateTableOpen(false)}
                disabled={createTableLoading}
                className="text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition disabled:opacity-40 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {createTableError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-xl text-red-200 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{createTableError}</span>
              </div>
            )}

            <form onSubmit={handleCreateTableSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                  Table Name
                </label>
                <input
                  type="text"
                  value={newTableName}
                  onChange={(e) => setNewTableName(e.target.value)}
                  placeholder="e.g. Friday Game #1"
                  autoFocus
                  maxLength={40}
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text font-bold text-sm focus:outline-none focus:border-gold-accent"
                />
              </div>

              {/* Chip Presets */}
              <div>
                <label className="block text-[10px] font-bold text-gold-accent/80 uppercase tracking-wider mb-1.5">
                  CHIP VALUE
                </label>
                <div className="grid grid-cols-5 gap-1.5 mb-2">
                  {CHIP_PRESETS.map((preset) => {
                    const isSelected = newChipPreset === preset && !newChipCustom;
                    return (
                      <button
                        key={preset}
                        type="button"
                        onClick={() => {
                          if (newChipPreset === preset && !newChipCustom) {
                            setNewChipPreset(null);
                          } else {
                            setNewChipPreset(preset);
                            setNewChipCustom('');
                          }
                        }}
                        className={`py-2 px-1 rounded-xl text-xs font-black transition border cursor-pointer ${
                          isSelected
                            ? 'bg-gold-accent text-black border-gold-accent shadow'
                            : 'bg-felt-dark text-cream-text border-gold-accent/30 hover:border-gold-accent/60'
                        }`}
                      >
                        ${preset}
                      </button>
                    );
                  })}
                </div>

                <input
                  type="number"
                  value={newChipCustom}
                  onChange={(e) => setNewChipCustom(e.target.value)}
                  placeholder="Custom Chip Value (optional)"
                  className="w-full px-4 py-2 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text font-mono text-xs focus:outline-none focus:border-gold-accent"
                />
              </div>

              {/* Entry Fee Toggle & Input */}
              <div className="p-3.5 bg-felt-dark/80 border border-gold-accent/30 rounded-xl space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold text-cream-text block">Entry Fee</span>
                    <span className="text-[11px] text-cream-text/60">Require entry fee for this game</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={newHasEntryFee}
                    onChange={(e) => setNewHasEntryFee(e.target.checked)}
                    className="w-4 h-4 accent-[#d4af37] rounded cursor-pointer"
                  />
                </div>

                {newHasEntryFee && (
                  <div>
                    <input
                      type="number"
                      value={newEntryFeeAmount}
                      onChange={(e) => setNewEntryFeeAmount(e.target.value)}
                      placeholder="Entry Fee Amount"
                      className="w-full px-3 py-2 bg-felt-card border border-gold-accent/40 rounded-xl text-cream-text font-mono text-xs focus:outline-none focus:border-gold-accent"
                    />
                  </div>
                )}
              </div>

              {/* Member Checklist (Select group members to auto-seat) */}
              {groupPlayers.length > 0 && (
                <div>
                  <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                    Select Group Members to Seat ({selectedPlayerIds.size} selected)
                  </label>
                  <div className="max-h-36 overflow-y-auto bg-felt-dark/90 border border-gold-accent/30 rounded-xl p-2 space-y-1">
                    {groupPlayers.map((p) => {
                      const isSelected = selectedPlayerIds.has(p.id);
                      return (
                        <div
                          key={p.id}
                          onClick={() => togglePlayerSelection(p.id)}
                          className={`flex items-center justify-between px-3 py-1.5 rounded-lg cursor-pointer transition text-xs ${
                            isSelected
                              ? 'bg-gold-accent/20 border border-gold-accent/50 text-gold-accent font-bold'
                              : 'hover:bg-felt-card text-cream-text/80'
                          }`}
                        >
                          <span className="truncate">{p.name}</span>
                          <div className={`w-4 h-4 rounded border flex items-center justify-center ${
                            isSelected ? 'bg-gold-accent border-gold-accent text-black' : 'border-gold-accent/40'
                          }`}>
                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Manual Player Names */}
              <div>
                <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                  Additional Player Names (optional)
                </label>
                <input
                  type="text"
                  value={manualPlayerNames}
                  onChange={(e) => setManualPlayerNames(e.target.value)}
                  placeholder="e.g. Guest1, Guest2 (comma separated)"
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text font-bold text-sm focus:outline-none focus:border-gold-accent"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={createTableLoading || !newTableName.trim()}
                  className="w-full py-3.5 bg-gradient-to-r from-gold-accent via-[#f3d068] to-gold-accent text-black font-extrabold uppercase tracking-wider text-sm rounded-xl shadow-lg transition active:scale-[0.98] disabled:opacity-40 flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span className="text-black font-bold text-base">♠</span>
                  <span>{createTableLoading ? 'CREATING...' : 'CREATE TABLE'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default GroupStats;
