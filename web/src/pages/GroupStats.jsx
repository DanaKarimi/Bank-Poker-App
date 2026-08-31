import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getGroupStats,
  getMyRequests,
  getGroupTables,
  getMyGroups,
} from '../api';
import TableCard from '../components/TableCard';
import RequestCard from '../components/RequestCard';
import {
  ArrowLeft,
  RefreshCw,
  Trophy,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Clock,
  Wifi,
  Layers,
  Shield,
  Plus,
} from 'lucide-react';

const GroupStats = () => {
  const { id: groupId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Group metadata & Data
  const [group, setGroup] = useState(null);
  const [stats, setStats] = useState(null);
  const [tables, setTables] = useState([]);
  const [requests, setRequests] = useState({ joinRequests: [], buyInRequests: [], exitRequests: [] });

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const isOnline = group?.mode === 'ONLINE';

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
      const [statsRes, tablesData, requestsRes] = await Promise.allSettled([
        getGroupStats(groupId),
        getGroupTables(groupId),
        getMyRequests(groupId),
      ]);

      if (statsRes.status === 'fulfilled') {
        setStats(statsRes.value.data?.stats || null);
        if (statsRes.value.data?.group) {
          setGroup(statsRes.value.data.group);
        }
      }

      if (tablesData.status === 'fulfilled') {
        setTables(tablesData.value || []);
      }

      if (requestsRes.status === 'fulfilled') {
        setRequests(requestsRes.value.data || { joinRequests: [], buyInRequests: [], exitRequests: [] });
      }
    } catch (err) {
      console.error('Error fetching group data:', err);
      if (!isBackground) setError('Failed to load group details.');
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroupInfo();
    fetchData();

    // Auto-poll every 12 seconds
    const interval = setInterval(() => {
      fetchData(true);
    }, 12000);

    return () => clearInterval(interval);
  }, [groupId]);

  const handleTableClick = (table) => {
    navigate(`/group/${groupId}/table/${table.id}`);
  };

  const balance = stats?.balance ?? 0;
  const isPositive = balance >= 0;
  const activeTablesCount = tables.filter((t) => t.status === 'ACTIVE' || t.isActive).length;
  const closedTablesCount = tables.filter((t) => t.status === 'CLOSED' || t.isActive === false).length;

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

          <button
            onClick={() => fetchData()}
            className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition"
            title="Refresh Group Data"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {/* Group Hero Card */}
        <div className="bg-felt-card border-2 border-gold-accent rounded-2xl p-6 shadow-2xl relative overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl font-black tracking-tight text-cream-text">
                  {group?.name || 'Poker Club'}
                </h1>
                {isOnline && (
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide bg-emerald-950 text-emerald-300 border border-emerald-500/40 flex items-center gap-1 shadow-sm">
                    <Wifi className="w-3 h-3" />
                    <span>ONLINE</span>
                  </span>
                )}
              </div>
              <p className="text-xs text-cream-text/60 mt-0.5">
                Club Tables & Overall Group Performance
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

        {/* Overall Group Personal Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 shadow-lg flex items-center justify-between">
            <div>
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">Total Group Balance</div>
              <div
                className={`text-xl font-extrabold font-mono mt-0.5 ${
                  isPositive ? 'text-emerald-400' : 'text-red-400'
                }`}
              >
                {isPositive ? `+$${balance.toLocaleString()}` : `-$${Math.abs(balance).toLocaleString()}`}
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
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">Total Group Buy-Ins</div>
              <div className="text-xl font-extrabold font-mono text-emerald-400 mt-0.5">
                ${(stats?.totalBuyIns || 0).toLocaleString()}
              </div>
            </div>
            <div className="p-3 bg-felt-dark text-emerald-400 rounded-xl border border-gold-accent/20">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>

          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 shadow-lg flex items-center justify-between">
            <div>
              <div className="text-[11px] text-cream-text/60 uppercase font-semibold">Total Group Exits</div>
              <div className="text-xl font-extrabold font-mono text-amber-400 mt-0.5">
                ${(stats?.totalExits || 0).toLocaleString()}
              </div>
            </div>
            <div className="p-3 bg-felt-dark text-amber-400 rounded-xl border border-gold-accent/20">
              <TrendingDown className="w-5 h-5" />
            </div>
          </div>
        </div>

        {/* Section: Tables in Group */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gold-accent flex items-center gap-2">
              <Layers className="w-4 h-4" />
              <span>Poker Tables & Rooms ({tables.length})</span>
            </h2>
            <span className="text-xs text-cream-text/50">
              Click any table to view ledger or buy-in
            </span>
          </div>

          {loading && tables.length === 0 ? (
            <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
              Loading active tables...
            </div>
          ) : tables.length === 0 ? (
            <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
              No poker tables have been created in this group yet.
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
    </div>
  );
};

export default GroupStats;
