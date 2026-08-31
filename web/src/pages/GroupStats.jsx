import React, { useState, useEffect, useRef } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getGroupStats,
  getMyRequests,
  getGroupTables,
  getMyGroups,
  sendBuyInRequest,
  sendExitRequest,
  confirmBuyInReceipt,
  confirmExitReceipt,
} from '../api';
import RequestCard from '../components/RequestCard';
import RequestModal from '../components/RequestModal';
import TableDetailModal from '../components/TableDetailModal';
import {
  ArrowLeft,
  RefreshCw,
  Trophy,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Send,
  Download,
  AlertCircle,
  Clock,
  CheckCircle,
  Wifi,
  Shield,
  Layers,
  ChevronRight,
} from 'lucide-react';

const GroupStats = () => {
  const { id: groupId } = useParams();
  const location = useLocation();
  const { user } = useAuth();

  // Group metadata
  const [group, setGroup] = useState(location.state?.group || null);
  const [stats, setStats] = useState(null);
  const [tables, setTables] = useState([]);
  const [requests, setRequests] = useState({ joinRequests: [], buyInRequests: [], exitRequests: [] });

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Table Detail Modal & Request Modal States
  const [selectedTableForDetail, setSelectedTableForDetail] = useState(null);
  const [isTableDetailOpen, setIsTableDetailOpen] = useState(false);
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [modalType, setModalType] = useState('buy-in');
  const [activeTableForRequest, setActiveTableForRequest] = useState(null);

  const isOnline = group?.mode === 'ONLINE';
  const timerRef = useRef(null);

  // 1. Fetch group info if not in location.state
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
      // Parallel fetch
      const [statsRes, requestsRes, tablesList] = await Promise.all([
        getGroupStats(groupId),
        getMyRequests(groupId),
        getGroupTables(groupId),
      ]);

      setStats(statsRes.data);
      setRequests(requestsRes.data || { joinRequests: [], buyInRequests: [], exitRequests: [] });
      setTables(tablesList || []);
    } catch (err) {
      console.error('Failed to fetch data:', err);
      if (!isBackground) {
        setError(err.response?.data?.error || 'Unable to load group stats. Please check connection.');
      }
    } finally {
      if (!isBackground) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    if (!group) {
      fetchGroupInfo();
    }
    if (groupId) {
      fetchData(false);

      // Set up 10-second polling for live status updates & pending requests
      timerRef.current = setInterval(() => {
        fetchData(true);
      }, 10000);
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [groupId]);

  // Handle Opening Table Detail
  const handleOpenTableDetail = (table) => {
    setSelectedTableForDetail(table);
    setIsTableDetailOpen(true);
  };

  // Handle Request Initiated from Table View
  const handleRequestBuyInFromTable = (table) => {
    setActiveTableForRequest(table);
    setModalType('buy-in');
    setIsTableDetailOpen(false);
    setIsRequestModalOpen(true);
  };

  const handleRequestExitFromTable = (table) => {
    setActiveTableForRequest(table);
    setModalType('exit');
    setIsTableDetailOpen(false);
    setIsRequestModalOpen(true);
  };

  // Request Submission
  const handleSubmitRequest = async (amount, tableId) => {
    if (modalType === 'buy-in') {
      await sendBuyInRequest(groupId, tableId, amount);
      setSuccessMessage(`Buy-in request for ${amount.toLocaleString()} chips submitted successfully!`);
    } else {
      await sendExitRequest(groupId, tableId, amount);
      setSuccessMessage(`Exit cashout request for ${amount.toLocaleString()} chips submitted successfully!`);
    }
    // Refresh requests list immediately
    fetchData(true);

    setTimeout(() => {
      setSuccessMessage('');
    }, 4000);
  };

  // Request Confirmation (Player confirming receipt)
  const handleConfirmReceipt = async (request, type) => {
    try {
      if (type === 'buy-in') {
        await confirmBuyInReceipt(request.id);
        setSuccessMessage('Buy-in receipt confirmed! Chips added to your session.');
      } else {
        await confirmExitReceipt(request.id);
        setSuccessMessage('Exit payout receipt confirmed! Your cashout is recorded.');
      }
      fetchData(true);
      setTimeout(() => {
        setSuccessMessage('');
      }, 4000);
    } catch (err) {
      console.error('Failed to confirm receipt:', err);
      setError(err.response?.data?.error || 'Failed to confirm receipt.');
      setTimeout(() => {
        setError('');
      }, 4000);
    }
  };

  const isPositiveBalance = (stats?.currentBalance || 0) > 0;
  const isNegativeBalance = (stats?.currentBalance || 0) < 0;

  const allRequests = [
    ...(requests.buyInRequests || []).map((r) => ({ ...r, _type: 'buy-in' })),
    ...(requests.exitRequests || []).map((r) => ({ ...r, _type: 'exit' })),
    ...(requests.joinRequests || []).map((r) => ({ ...r, _type: 'join' })),
  ].sort((a, b) => (b.created_at || 0) - (a.created_at || 0));

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
            <span>Dashboard</span>
          </Link>

          <div className="flex items-center gap-3">
            <button
              onClick={() => fetchData(false)}
              disabled={loading}
              title="Refresh group stats & requests"
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
        {/* Success Alert Banner */}
        {successMessage && (
          <div className="mb-6 p-4 bg-emerald-950/90 border border-emerald-500 rounded-xl flex items-center gap-3 text-emerald-200 shadow-lg animate-in fade-in slide-in-from-top-2 duration-200">
            <CheckCircle className="w-5 h-5 shrink-0 text-emerald-400" />
            <span className="font-medium text-sm">{successMessage}</span>
          </div>
        )}

        {/* Error Notification */}
        {error && (
          <div className="mb-6 p-4 bg-red-950/90 border border-red-500 rounded-xl flex items-center gap-3 text-red-200 shadow-lg">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span className="font-medium text-sm">{error}</span>
          </div>
        )}

        {loading && !stats ? (
          <div className="flex flex-col items-center justify-center py-24">
            <div className="w-12 h-12 border-4 border-gold-accent border-t-transparent rounded-full animate-spin"></div>
            <p className="mt-4 text-gold-accent font-semibold tracking-wide">
              Loading group information & ledger...
            </p>
          </div>
        ) : stats ? (
          <div className="space-y-6">
            {/* Header Title & Mode Card */}
            <div className="bg-felt-card/90 border border-gold-accent/50 rounded-2xl p-6 shadow-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-extrabold uppercase tracking-wider border ${
                      isOnline
                        ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/50'
                        : 'bg-felt-dark text-cream-text/60 border-gold-accent/20'
                    }`}
                  >
                    <span
                      className={`w-1.5 h-1.5 rounded-full ${
                        isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-cream-text/40'
                      }`}
                    />
                    <span>{group?.mode || 'OFFLINE'} GROUP</span>
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-black text-cream-text tracking-wide">
                  {stats.groupName || group?.name || 'Poker Group'}
                </h1>
              </div>

              <div className="bg-felt-dark px-4 py-2 rounded-xl border border-gold-accent/30 self-start sm:self-auto flex items-center gap-2">
                <Shield className="w-4 h-4 text-gold-accent" />
                <div className="text-xs">
                  <span className="text-cream-text/60">Player: </span>
                  <span className="font-bold text-gold-accent">{stats.username || user?.username}</span>
                </div>
              </div>
            </div>

            {/* Offline Group Notice */}
            {!isOnline && (
              <div className="bg-felt-card/60 border border-dashed border-gold-accent/30 rounded-2xl p-4 text-center text-xs text-cream-text/70">
                <span>♠ This is an offline group. Table entries and balances are managed directly by your admin.</span>
              </div>
            )}

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

            {/* Active Tables Section (Online Groups) */}
            {isOnline && (
              <div className="pt-2">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-bold text-gold-accent flex items-center gap-2">
                    <Layers className="w-5 h-5" />
                    <span>Active Tables</span>
                  </h3>
                  <span className="text-xs font-semibold text-cream-text/60">
                    {tables.length} {tables.length === 1 ? 'Table' : 'Tables'} Available
                  </span>
                </div>

                {tables.length === 0 ? (
                  <div className="bg-felt-card/50 border border-dashed border-gold-accent/30 rounded-2xl p-8 text-center text-cream-text/60">
                    <p className="text-sm font-medium">No active tables in this group right now.</p>
                    <p className="text-xs mt-1 text-cream-text/40">
                      When your admin opens a table, it will appear here so you can request buy-ins.
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {tables.map((table) => (
                      <div
                        key={table.id}
                        onClick={() => handleOpenTableDetail(table)}
                        className="bg-felt-card/90 border-2 border-gold-accent/50 hover:border-gold-accent rounded-2xl p-5 shadow-lg transition duration-200 cursor-pointer flex flex-col justify-between group"
                      >
                        <div>
                          <div className="flex items-start justify-between gap-2 mb-2">
                            <h4 className="font-extrabold text-lg text-cream-text group-hover:text-gold-light transition line-clamp-1">
                              ♣ {table.name || `Table ${table.id}`}
                            </h4>
                            <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                              {table.status || 'ACTIVE'}
                            </span>
                          </div>

                          <div className="flex items-center gap-3 text-xs text-cream-text/70 mb-4">
                            {table.chip_value && (
                              <span className="bg-felt-dark px-2.5 py-1 rounded-lg border border-gold-accent/20 font-mono">
                                ${table.chip_value}/chip
                              </span>
                            )}
                            {table.has_entry_fee && table.entry_fee > 0 && (
                              <span className="bg-felt-dark px-2.5 py-1 rounded-lg border border-amber-500/30 text-amber-300 font-mono">
                                Fee: ${table.entry_fee}
                              </span>
                            )}
                            <span className="bg-felt-dark px-2.5 py-1 rounded-lg border border-emerald-500/30 text-emerald-300 font-semibold">
                              {table.playerCount || 0} {table.playerCount === 1 ? 'Player' : 'Players'}
                            </span>
                          </div>
                        </div>

                        <button
                          type="button"
                          className="w-full py-2 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent font-bold uppercase tracking-wider text-xs rounded-xl border border-gold-accent/40 shadow flex items-center justify-center gap-1.5 transition"
                        >
                          <span>Open Table Actions</span>
                          <ChevronRight className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Online Group Requests Section */}
            {isOnline && (
              <div className="pt-4">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-bold text-gold-accent flex items-center gap-2">
                    <Clock className="w-5 h-5" />
                    <span>My Table Requests</span>
                  </h3>
                  <span className="text-xs text-cream-text/60">Auto-refreshing every 10s</span>
                </div>

                {allRequests.length === 0 ? (
                  <div className="bg-felt-card/50 border border-dashed border-gold-accent/30 rounded-2xl p-8 text-center text-cream-text/60">
                    <p className="text-sm font-medium">No requests submitted yet.</p>
                    <p className="text-xs mt-1 text-cream-text/40">
                      Select an active table above to join and request buy-in chips or exit payouts.
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {allRequests.map((req) => (
                      <RequestCard
                        key={req.id}
                        request={req}
                        type={req._type}
                        onConfirm={(r) => handleConfirmReceipt(r, req._type)}
                      />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        ) : null}
      </main>

      {/* Table Detail Modal (Initiates Join, Buy-in or Exit for the selected table) */}
      <TableDetailModal
        isOpen={isTableDetailOpen}
        onClose={() => setIsTableDetailOpen(false)}
        table={selectedTableForDetail}
        myRequests={requests}
        onRequestBuyIn={handleRequestBuyInFromTable}
        onRequestExit={handleRequestExitFromTable}
        onJoinSuccess={() => fetchData(true)}
      />

      {/* Request Modal */}
      <RequestModal
        isOpen={isRequestModalOpen}
        onClose={() => {
          setIsRequestModalOpen(false);
          setActiveTableForRequest(null);
        }}
        type={modalType}
        tables={tables}
        selectedTable={activeTableForRequest}
        initialTableId={activeTableForRequest?.id}
        onSubmit={handleSubmitRequest}
      />
    </div>
  );
};

export default GroupStats;
