import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  ShieldAlert,
  Users,
  Layers,
  Activity,
  Trash2,
  RefreshCw,
  Search,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Clock,
  Key,
  ShieldCheck,
  Zap,
  Sliders,
  X,
  Plus,
  Minus
} from 'lucide-react';
import { UserBadge, PokerAvatar } from '../components/AvatarSystem';
import GroupCodeChip from '../components/GroupCodeChip';
import {
  getAdminOverview,
  getAdminUsers,
  updateAdminUserRole,
  deleteAdminUser,
  getAdminGroups,
  deleteAdminGroup,
  getAdminTables,
  deleteAdminTable,
  getAdminTablePlayers,
  updateAdminTablePlayer,
  getServerHealth
} from '../api';

export const AdminDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';

  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'users' | 'groups' | 'tables'
  const [stats, setStats] = useState(null);
  const [health, setHealth] = useState(null);
  const [users, setUsers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [tables, setTables] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Search & filter states
  const [userSearch, setUserSearch] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState('ALL');
  const [tableFilter, setTableFilter] = useState('ALL'); // 'ALL' | 'ACTIVE' | 'CLOSED' | 'QUICK'

  // Confirm delete modal state
  const [deleteTarget, setDeleteTarget] = useState(null); // { type: 'user'|'group'|'table', id, name }
  const [isDeleting, setIsDeleting] = useState(false);

  // Table inspection modal state
  const [inspectedTable, setInspectedTable] = useState(null);
  const [tablePlayers, setTablePlayers] = useState([]);
  const [loadingPlayers, setLoadingPlayers] = useState(false);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [adjAmount, setAdjAmount] = useState('');
  const [editingName, setEditingName] = useState('');
  const [isUpdatingPlayer, setIsUpdatingPlayer] = useState(false);

  const fetchOverviewData = async () => {
    try {
      const [overviewRes, healthRes] = await Promise.allSettled([
        getAdminOverview(),
        getServerHealth()
      ]);
      if (overviewRes.status === 'fulfilled') {
        setStats(overviewRes.value.data?.stats || null);
      }
      if (healthRes.status === 'fulfilled') {
        setHealth(healthRes.value.data || null);
      }
    } catch (err) {
      console.error('Failed to fetch overview:', err);
    }
  };

  const fetchUsersData = async () => {
    try {
      const res = await getAdminUsers();
      setUsers(res.data?.users || []);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('Failed to fetch users list');
    }
  };

  const fetchGroupsData = async () => {
    try {
      const res = await getAdminGroups();
      setGroups(res.data?.groups || []);
    } catch (err) {
      console.error('Failed to fetch groups:', err);
      setError('Failed to fetch groups list');
    }
  };

  const fetchTablesData = async () => {
    try {
      const res = await getAdminTables();
      setTables(res.data?.tables || []);
    } catch (err) {
      console.error('Failed to fetch tables:', err);
      setError('Failed to fetch tables list');
    }
  };

  const reloadAll = async () => {
    setLoading(true);
    setError('');
    try {
      await Promise.allSettled([
        fetchOverviewData(),
        fetchUsersData(),
        fetchGroupsData(),
        fetchTablesData()
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isSuperAdmin) {
      reloadAll();
    }
  }, [isSuperAdmin]);

  const handleRoleChange = async (userId, newRole) => {
    try {
      await updateAdminUserRole(userId, newRole);
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u));
      setSuccess(`Role updated to ${newRole}`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      console.error('Role update error:', err);
      setError(err.response?.data?.error || 'Failed to update role');
      setTimeout(() => setError(''), 4000);
    }
  };

  const handleExecuteDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    setError('');
    try {
      if (deleteTarget.type === 'user') {
        await deleteAdminUser(deleteTarget.id);
        setUsers(prev => prev.filter(u => u.id !== deleteTarget.id));
        setSuccess(`User "${deleteTarget.name}" deleted successfully.`);
      } else if (deleteTarget.type === 'group') {
        await deleteAdminGroup(deleteTarget.id);
        setGroups(prev => prev.filter(g => g.id !== deleteTarget.id));
        setSuccess(`Group "${deleteTarget.name}" deleted successfully.`);
      } else if (deleteTarget.type === 'table') {
        await deleteAdminTable(deleteTarget.id);
        setTables(prev => prev.filter(t => t.id !== deleteTarget.id));
        setSuccess(`Table "${deleteTarget.name}" deleted successfully.`);
      }
      setDeleteTarget(null);
      fetchOverviewData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      console.error('Delete error:', err);
      setError(err.response?.data?.error || 'Failed to delete target item.');
      setTimeout(() => setError(''), 4000);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleOpenTableInspection = async (table) => {
    setInspectedTable(table);
    setLoadingPlayers(true);
    setSelectedPlayer(null);
    setAdjAmount('');
    setEditingName('');
    try {
      const res = await getAdminTablePlayers(table.id);
      setTablePlayers(res.data?.players || []);
    } catch (err) {
      console.error('Failed to load table players:', err);
    } finally {
      setLoadingPlayers(false);
    }
  };

  const handleUpdatePlayerSubmit = async (e) => {
    e.preventDefault();
    if (!selectedPlayer || !inspectedTable) return;
    setIsUpdatingPlayer(true);
    try {
      const payload = {};
      if (editingName.trim()) payload.name = editingName.trim();
      if (adjAmount && !isNaN(Number(adjAmount))) {
        payload.balanceAdjustment = Number(adjAmount);
      }
      await updateAdminTablePlayer(inspectedTable.id, selectedPlayer.id, payload);
      setSuccess('Player adjusted successfully');
      const res = await getAdminTablePlayers(inspectedTable.id);
      setTablePlayers(res.data?.players || []);
      setSelectedPlayer(null);
      setAdjAmount('');
      setEditingName('');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      console.error('Failed to update player:', err);
      setError(err.response?.data?.error || 'Failed to adjust player');
      setTimeout(() => setError(''), 4000);
    } finally {
      setIsUpdatingPlayer(false);
    }
  };

  if (!isSuperAdmin) {
    return (
      <div className="min-h-screen bg-felt-dark text-cream-text flex items-center justify-center p-4">
        <div className="bg-felt-card border-2 border-red-500/50 rounded-2xl max-w-md w-full p-6 text-center space-y-4 shadow-2xl">
          <ShieldAlert className="w-16 h-16 text-red-500 mx-auto" />
          <h2 className="text-xl font-bold text-red-200">Access Restricted</h2>
          <p className="text-xs text-cream-text/70">
            This administration control plane is restricted to SUPER_ADMIN accounts only.
          </p>
          <Link
            to="/dashboard"
            className="inline-block px-5 py-2.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/40 rounded-xl text-gold-accent font-bold text-xs"
          >
            Return to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const filteredUsers = users.filter(u => {
    const matchesSearch =
      u.username.toLowerCase().includes(userSearch.toLowerCase()) ||
      (u.display_name && u.display_name.toLowerCase().includes(userSearch.toLowerCase()));
    const matchesRole =
      userRoleFilter === 'ALL'
        ? true
        : userRoleFilter === 'GUEST'
        ? u.is_guest
        : u.role === userRoleFilter;
    return matchesSearch && matchesRole;
  });

  const filteredTables = tables.filter(t => {
    if (tableFilter === 'ACTIVE') return t.status === 'ACTIVE';
    if (tableFilter === 'CLOSED') return t.status === 'CLOSED';
    if (tableFilter === 'QUICK') return t.isQuickTable;
    return true;
  });

  return (
    <div className="min-h-screen bg-felt-green text-cream-text flex flex-col">
      {/* Admin Top Navbar */}
      <header className="bg-felt-dark/95 border-b border-gold-accent/40 sticky top-0 z-30 shadow-lg backdrop-blur-md">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link
              to="/dashboard"
              className="p-1.5 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/30 rounded-xl text-gold-accent transition"
              title="Return to user app"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-6 h-6 text-gold-accent" />
              <span className="font-extrabold text-lg tracking-wider text-gold-accent uppercase">
                BankPoker Super Admin
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 bg-emerald-950/70 border border-emerald-500/40 rounded-full text-emerald-300 text-xs font-semibold">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>{health?.status === 'ok' ? 'Server Online' : 'Checking Health...'}</span>
            </div>

            <button
              onClick={reloadAll}
              className="p-2 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition cursor-pointer"
              title="Refresh All Admin Data"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-6xl w-full mx-auto px-4 py-6 flex-1 space-y-6">
        {error && (
          <div className="p-3 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-2 text-red-200 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}
        {success && (
          <div className="p-3 bg-green-950/80 border border-green-500 rounded-xl flex items-center gap-2 text-green-200 text-xs">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-green-400" />
            <span>{success}</span>
          </div>
        )}

        {/* Tab Navigation */}
        <div className="flex gap-2 border-b border-gold-accent/20 pb-3 overflow-x-auto">
          {[
            { id: 'overview', label: 'Platform Overview', icon: Activity },
            { id: 'users', label: `Users (${users.length})`, icon: Users },
            { id: 'groups', label: `Groups (${groups.length})`, icon: Key },
            { id: 'tables', label: `Tables (${tables.length})`, icon: Layers },
          ].map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer whitespace-nowrap ${
                activeTab === id
                  ? 'bg-gold-accent text-black shadow-lg font-black'
                  : 'bg-felt-card text-cream-text/70 hover:text-cream-text border border-gold-accent/30'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{label}</span>
            </button>
          ))}
        </div>

        {/* TAB 1: OVERVIEW */}
        {activeTab === 'overview' && stats && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow">
                <div className="text-xs text-cream-text/60 font-semibold mb-1">Total Users</div>
                <div className="text-2xl font-black text-gold-accent">{stats.totalUsers}</div>
                <div className="text-[10px] text-cream-text/50 mt-1">
                  {stats.fullUsers} permanent · {stats.guestUsers} guests
                </div>
              </div>

              <div className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow">
                <div className="text-xs text-cream-text/60 font-semibold mb-1">Active Tables</div>
                <div className="text-2xl font-black text-emerald-400">{stats.activeTables}</div>
                <div className="text-[10px] text-cream-text/50 mt-1">
                  out of {stats.totalTables} total tables
                </div>
              </div>

              <div className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow">
                <div className="text-xs text-cream-text/60 font-semibold mb-1">Quick Tables</div>
                <div className="text-2xl font-black text-yellow-300">{stats.quickTables}</div>
                <div className="text-[10px] text-cream-text/50 mt-1">
                  Direct instant tables
                </div>
              </div>

              <div className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow">
                <div className="text-xs text-cream-text/60 font-semibold mb-1">Total Groups</div>
                <div className="text-2xl font-black text-purple-300">{stats.totalGroups}</div>
                <div className="text-[10px] text-cream-text/50 mt-1">
                  {stats.activePlayers} seated players
                </div>
              </div>
            </div>

            <div className="p-5 bg-felt-card border border-gold-accent/40 rounded-2xl shadow space-y-3">
              <h3 className="font-bold text-sm text-gold-accent uppercase tracking-wider flex items-center gap-2">
                <Activity className="w-4 h-4" />
                <span>Backend Diagnostics & Environment</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                  <span className="text-cream-text/50 block">Status:</span>
                  <span className="font-bold text-emerald-400">{health?.status || 'Active'}</span>
                </div>
                <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                  <span className="text-cream-text/50 block">Server Timestamp:</span>
                  <span className="font-mono text-[11px] text-cream-text">
                    {health?.timestamp ? new Date(health.timestamp).toLocaleString() : 'N/A'}
                  </span>
                </div>
                <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/20">
                  <span className="text-cream-text/50 block">Public Host / Domain:</span>
                  <span className="font-mono text-[11px] text-gold-accent">
                    {window.location.host}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: USERS */}
        {activeTab === 'users' && (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-cream-text/40 absolute left-3.5 top-3" />
                <input
                  type="text"
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  placeholder="Search user by username or display name..."
                  className="w-full pl-10 pr-4 py-2 bg-felt-card border border-gold-accent/40 rounded-xl text-cream-text placeholder-cream-text/40 text-xs focus:outline-none focus:border-gold-accent"
                />
              </div>

              <select
                value={userRoleFilter}
                onChange={(e) => setUserRoleFilter(e.target.value)}
                className="px-3 py-2 bg-felt-card border border-gold-accent/40 rounded-xl text-cream-text text-xs focus:outline-none focus:border-gold-accent cursor-pointer"
              >
                <option value="ALL">All Roles</option>
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
                <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                <option value="GUEST">GUEST Only</option>
              </select>
            </div>

            <div className="bg-felt-card border border-gold-accent/40 rounded-2xl overflow-hidden shadow">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-felt-dark/80 text-gold-accent uppercase font-black text-[10px] tracking-wider border-b border-gold-accent/30">
                    <tr>
                      <th className="p-3.5">User</th>
                      <th className="p-3.5">Role</th>
                      <th className="p-3.5">Type</th>
                      <th className="p-3.5">Groups</th>
                      <th className="p-3.5">Joined</th>
                      <th className="p-3.5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gold-accent/15">
                    {filteredUsers.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="p-8 text-center text-cream-text/50">
                          No users matched your criteria.
                        </td>
                      </tr>
                    ) : (
                      filteredUsers.map((u) => (
                        <tr key={u.id} className="hover:bg-felt-dark/40 transition">
                          <td className="p-3.5">
                            <UserBadge
                              displayName={u.display_name}
                              username={u.username}
                              avatarId={u.avatar_id}
                              role={u.role}
                              isGuest={u.is_guest}
                              size={32}
                            />
                          </td>
                          <td className="p-3.5">
                            <select
                              value={u.role}
                              onChange={(e) => handleRoleChange(u.id, e.target.value)}
                              disabled={u.id === user.id}
                              className="px-2 py-1 bg-felt-dark border border-gold-accent/40 rounded-lg text-cream-text font-semibold text-[11px] cursor-pointer focus:outline-none disabled:opacity-50"
                            >
                              <option value="USER">USER</option>
                              <option value="ADMIN">ADMIN</option>
                              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                            </select>
                          </td>
                          <td className="p-3.5">
                            {u.is_guest ? (
                              <span className="px-2 py-0.5 rounded-full bg-yellow-900/60 border border-yellow-500/40 text-yellow-300 text-[10px] font-bold">
                                Guest
                              </span>
                            ) : (
                              <span className="px-2 py-0.5 rounded-full bg-emerald-900/60 border border-emerald-500/40 text-emerald-300 text-[10px] font-bold">
                                Permanent
                              </span>
                            )}
                          </td>
                          <td className="p-3.5 font-mono">{u.group_count || 0}</td>
                          <td className="p-3.5 text-cream-text/60">
                            {u.created_at ? new Date(u.created_at).toLocaleDateString() : 'N/A'}
                          </td>
                          <td className="p-3.5 text-right">
                            {u.id !== user.id ? (
                              <button
                                onClick={() => setDeleteTarget({ type: 'user', id: u.id, name: u.username })}
                                className="p-1.5 text-red-400 hover:text-red-200 hover:bg-red-900/40 rounded-lg transition cursor-pointer"
                                title="Delete user"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            ) : (
                              <span className="text-[10px] text-cream-text/40 italic">Self</span>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: GROUPS */}
        {activeTab === 'groups' && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {groups.map((g) => (
                <div
                  key={g.id}
                  className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow space-y-3 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="font-black text-sm text-cream-text truncate">{g.name}</h4>
                      {g.invite_code && (
                        <GroupCodeChip code={g.invite_code} label="" />
                      )}
                    </div>

                    <div className="mt-2 text-[11px] text-cream-text/70 space-y-1">
                      <div>Owner: <span className="text-gold-accent font-semibold">@{g.owner?.username || 'N/A'}</span></div>
                      <div>Members: <span className="font-semibold text-cream-text">{g.member_count}</span> · Tables: <span className="font-semibold text-cream-text">{g.table_count}</span></div>
                      <div className="text-cream-text/40 text-[10px]">
                        Created: {g.created_at ? new Date(g.created_at).toLocaleDateString() : 'N/A'}
                      </div>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-gold-accent/20 flex items-center justify-between">
                    <Link
                      to={`/group/${g.id}`}
                      className="text-xs font-bold text-gold-accent hover:underline"
                    >
                      Open Group View →
                    </Link>

                    <button
                      onClick={() => setDeleteTarget({ type: 'group', id: g.id, name: g.name })}
                      className="p-1.5 text-red-400 hover:text-red-200 hover:bg-red-900/40 rounded-lg transition cursor-pointer"
                      title="Delete group"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 4: TABLES */}
        {activeTab === 'tables' && (
          <div className="space-y-4">
            <div className="flex gap-2">
              {['ALL', 'ACTIVE', 'CLOSED', 'QUICK'].map((f) => (
                <button
                  key={f}
                  onClick={() => setTableFilter(f)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                    tableFilter === f
                      ? 'bg-gold-accent text-black font-black'
                      : 'bg-felt-card text-cream-text/70 hover:text-cream-text border border-gold-accent/30'
                  }`}
                >
                  {f} Tables
                </button>
              ))}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredTables.map((t) => (
                <div
                  key={t.id}
                  className="p-4 bg-felt-card border border-gold-accent/40 rounded-2xl shadow space-y-3 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="font-black text-sm text-cream-text truncate">{t.name}</h4>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase ${
                          t.status === 'ACTIVE'
                            ? 'bg-emerald-950 text-emerald-300 border border-emerald-500/40'
                            : 'bg-gray-900 text-gray-400 border border-gray-700'
                        }`}
                      >
                        {t.status}
                      </span>
                    </div>

                    <div className="mt-2 text-[11px] text-cream-text/70 space-y-1">
                      {t.code && (
                        <div className="flex items-center gap-1">
                          <span className="text-cream-text/50">Code:</span>
                          <GroupCodeChip code={t.code} label="" />
                        </div>
                      )}
                      <div>
                        Group: {t.isQuickTable ? (
                          <span className="text-yellow-300 font-semibold">⚡ Quick Table (No Group)</span>
                        ) : (
                          <span className="text-cream-text font-semibold">{t.groupName || 'Group Table'}</span>
                        )}
                      </div>
                      <div>Seated Players: <span className="font-semibold text-cream-text">{t.player_count}</span></div>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-gold-accent/20 flex items-center justify-between gap-2">
                    <button
                      onClick={() => handleOpenTableInspection(t)}
                      className="px-3 py-1.5 bg-felt-dark hover:bg-felt-dark/80 text-gold-accent border border-gold-accent/40 rounded-lg text-xs font-bold transition cursor-pointer"
                    >
                      Audit Ledger
                    </button>

                    <button
                      onClick={() => setDeleteTarget({ type: 'table', id: t.id, name: t.name })}
                      className="p-1.5 text-red-400 hover:text-red-200 hover:bg-red-900/40 rounded-lg transition cursor-pointer"
                      title="Delete table"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* CONFIRM DELETE MODAL */}
      {deleteTarget && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-red-500 rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <div className="text-center space-y-2">
              <Trash2 className="w-10 h-10 text-red-500 mx-auto" />
              <h3 className="text-lg font-bold text-cream-text">
                Delete {deleteTarget.type.toUpperCase()}?
              </h3>
              <p className="text-xs text-cream-text/70">
                Are you sure you want to permanently delete <strong className="text-red-300">{deleteTarget.name}</strong>? All associated records will be cascade cleaned.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                disabled={isDeleting}
                className="flex-1 py-2 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/40 rounded-xl text-xs font-bold transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleExecuteDelete}
                disabled={isDeleting}
                className="flex-1 py-2 bg-red-700 hover:bg-red-600 text-white rounded-xl text-xs font-bold transition cursor-pointer disabled:opacity-50"
              >
                {isDeleting ? 'Deleting...' : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* AUDIT TABLE PLAYERS MODAL */}
      {inspectedTable && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-gold-accent/20">
              <div>
                <h3 className="font-black text-base text-gold-accent uppercase tracking-wider">
                  Audit: {inspectedTable.name}
                </h3>
                <p className="text-[11px] text-cream-text/60">
                  Inspect buy-ins, exits, and manual balance override for players
                </p>
              </div>
              <button
                onClick={() => setInspectedTable(null)}
                className="text-cream-text/50 hover:text-cream-text p-1 transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 pr-1">
              {loadingPlayers ? (
                <div className="py-10 text-center text-xs text-cream-text/60">
                  Loading ledger...
                </div>
              ) : tablePlayers.length === 0 ? (
                <div className="py-10 text-center text-xs text-cream-text/50">
                  No players seated at this table.
                </div>
              ) : (
                tablePlayers.map((p) => (
                  <div
                    key={p.id}
                    className={`p-3.5 bg-felt-dark rounded-xl border transition ${
                      selectedPlayer?.id === p.id
                        ? 'border-gold-accent bg-felt-dark/90'
                        : 'border-gold-accent/20 hover:border-gold-accent/50'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <PokerAvatar avatarId={p.linked_user?.avatar_id || 'avatar_1'} name={p.name} size={36} />
                        <div>
                          <div className="font-bold text-xs text-cream-text">{p.name}</div>
                          <div className="text-[10px] text-cream-text/50">
                            {p.linked_user ? `@${p.linked_user.username}` : 'Unlinked local seat'}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-4 text-right">
                        <div className="text-[11px]">
                          <div className="text-cream-text/50 text-[10px]">Buy-in / Exit</div>
                          <div className="font-mono text-cream-text">
                            {Number(p.totalBuyIns).toLocaleString()} / {Number(p.totalExits).toLocaleString()}
                          </div>
                        </div>

                        <div className="text-right min-w-[70px]">
                          <div className="text-cream-text/50 text-[10px]">Balance</div>
                          <div
                            className={`font-mono font-bold text-xs ${
                              p.balance > 0
                                ? 'text-emerald-400'
                                : p.balance < 0
                                ? 'text-red-400'
                                : 'text-cream-text'
                            }`}
                          >
                            {p.balance > 0 ? `+${p.balance.toLocaleString()}` : p.balance.toLocaleString()}
                          </div>
                        </div>

                        <button
                          onClick={() => {
                            setSelectedPlayer(p);
                            setEditingName(p.name);
                            setAdjAmount('');
                          }}
                          className="px-2.5 py-1 bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 rounded-lg text-gold-accent font-bold text-[10px] cursor-pointer transition"
                        >
                          Adjust
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            {selectedPlayer && (
              <form
                onSubmit={handleUpdatePlayerSubmit}
                className="p-4 bg-felt-dark/95 border border-gold-accent/50 rounded-xl space-y-3 animate-in fade-in"
              >
                <div className="flex items-center justify-between text-xs font-bold text-gold-accent">
                  <span>Adjusting Player: {selectedPlayer.name}</span>
                  <button
                    type="button"
                    onClick={() => setSelectedPlayer(null)}
                    className="text-cream-text/50 hover:text-cream-text text-[11px]"
                  >
                    Cancel
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[10px] font-bold text-cream-text/70 mb-1">
                      Rename Seat
                    </label>
                    <input
                      type="text"
                      value={editingName}
                      onChange={(e) => setEditingName(e.target.value)}
                      placeholder="Player display name"
                      className="w-full px-3 py-1.5 bg-felt-card border border-gold-accent/40 rounded-lg text-cream-text text-xs focus:outline-none focus:border-gold-accent"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-cream-text/70 mb-1">
                      Balance Adjustment (+/- Chips)
                    </label>
                    <input
                      type="number"
                      value={adjAmount}
                      onChange={(e) => setAdjAmount(e.target.value)}
                      placeholder="e.g. 50000 or -25000"
                      className="w-full px-3 py-1.5 bg-felt-card border border-gold-accent/40 rounded-lg text-cream-text text-xs focus:outline-none focus:border-gold-accent"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isUpdatingPlayer}
                  className="w-full py-2 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider text-xs rounded-lg shadow hover:opacity-95 transition disabled:opacity-50 cursor-pointer"
                >
                  {isUpdatingPlayer ? 'Saving Changes...' : 'Apply Adjustments'}
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
