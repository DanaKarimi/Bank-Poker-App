import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api, {
  getGroupByInvite,
  getGroupPlayersList,
  claimPlayer,
  joinNewPlayer,
  createQuickTable,
  createGroup,
  getActiveTables
} from '../api';
import {
  Users,
  Plus,
  LogOut,
  RefreshCw,
  ChevronRight,
  ChevronLeft,
  Key,
  AlertCircle,
  CheckCircle2,
  X,
  UserCheck,
  UserPlus,
  ArrowRight,
  Zap,
  ShieldCheck,
  ClipboardPaste,
  Pin
} from 'lucide-react';
import { UserBadge } from '../components/AvatarSystem';
import GroupCodeChip from '../components/GroupCodeChip';
import ProfileModal from '../components/ProfileModal';
import NotificationsDropdown from '../components/NotificationsDropdown';
import { getSocket } from '../socket';

const CHIP_PRESETS = [50, 100, 200, 500];

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [groups, setGroups] = useState([]);
  const [activeTables, setActiveTables] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Profile modal and smart lookup state
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [smartCode, setSmartCode] = useState('');
  const [smartLoading, setSmartLoading] = useState(false);
  const [smartError, setSmartError] = useState('');

  // Join Group Multi-step Modal State: 'A' (code) | 'B' (question) | 'C' (claim) | 'D' (new)
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [joinStep, setJoinStep] = useState('A');
  const [inviteCode, setInviteCode] = useState('');
  const [joinError, setJoinError] = useState('');
  const [joinSuccess, setJoinSuccess] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [inspectedGroup, setInspectedGroup] = useState(null);
  const [unclaimedPlayers, setUnclaimedPlayers] = useState([]);
  const [isLoadingPlayers, setIsLoadingPlayers] = useState(false);
  const [newPlayerName, setNewPlayerName] = useState('');

  // Create Group Modal State
  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [createGroupLoading, setCreateGroupLoading] = useState(false);
  const [createGroupError, setCreateGroupError] = useState('');

  // Quick Table State
  const [isQuickModalOpen, setIsQuickModalOpen] = useState(false);
  const [quickTableName, setQuickTableName] = useState('');
  const [quickChipPreset, setQuickChipPreset] = useState(100);
  const [quickChipCustom, setQuickChipCustom] = useState('');
  const [quickHasEntryFee, setQuickHasEntryFee] = useState(false);
  const [quickEntryFeeAmount, setQuickEntryFeeAmount] = useState('');
  const [quickPlayerNames, setQuickPlayerNames] = useState('');
  const [quickLoading, setQuickLoading] = useState(false);
  const [quickError, setQuickError] = useState('');

  // Action Menu Bottom Sheet / Modal for FAB (+)
  const [isActionMenuOpen, setIsActionMenuOpen] = useState(false);

  // Fetch Dashboard Data (Groups + Active Tables)
  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [groupRes, activeRes] = await Promise.all([
        api.get('/api/groups/my-groups').catch((err) => {
          console.error('Failed to fetch groups:', err);
          return { data: { groups: [] } };
        }),
        getActiveTables().catch((err) => {
          console.error('Failed to fetch active tables:', err);
          return { data: { tables: [] } };
        })
      ]);

      setGroups(groupRes.data?.groups || []);
      setActiveTables(activeRes.data?.tables || []);
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
      setError('Unable to load dashboard data. Please check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();

    const socket = getSocket();
    const handleRefresh = () => {
      fetchDashboardData();
    };

    socket.on('table_created', handleRefresh);
    socket.on('table_closed', handleRefresh);
    socket.on('table_updated', handleRefresh);
    socket.on('group_updated', handleRefresh);

    const interval = setInterval(fetchDashboardData, 8000);

    return () => {
      clearInterval(interval);
      socket.off('table_created', handleRefresh);
      socket.off('table_closed', handleRefresh);
      socket.off('table_updated', handleRefresh);
      socket.off('group_updated', handleRefresh);
    };
  }, []);

  // Smart Lookup Handler
  const handleSmartLookup = async (e) => {
    if (e) e.preventDefault();
    const clean = smartCode.trim().toUpperCase();
    if (clean.length < 4) {
      setSmartError('Please enter a valid 6-character code.');
      return;
    }
    setSmartLoading(true);
    setSmartError('');
    try {
      const res = await api.get(`/api/lookup/${clean}`);
      const data = res.data;
      if (data.type === 'GROUP') {
        openJoinModalWithCode(clean);
      } else if (data.type === 'TABLE') {
        navigate(`/table/${data.id}`);
      } else {
        navigate(`/group/${data.id}`);
      }
    } catch (err) {
      console.error('Lookup error:', err);
      setSmartError(err.response?.data?.error || 'Code not found. Please verify code.');
    } finally {
      setSmartLoading(false);
    }
  };

  const handlePasteCode = async () => {
    try {
      if (navigator.clipboard?.readText) {
        const text = await navigator.clipboard.readText();
        const clean = text?.trim().toUpperCase();
        if (clean && clean.length >= 4 && clean.length <= 8) {
          setSmartCode(clean);
        }
      }
    } catch (err) {
      console.warn('Clipboard read permission denied or unavailable:', err);
    }
  };

  // Create Group Handler
  const handleCreateGroup = async (e) => {
    e.preventDefault();
    const trimmed = newGroupName.trim();
    if (!trimmed) return;
    setCreateGroupLoading(true);
    setCreateGroupError('');
    try {
      const res = await createGroup({ name: trimmed });
      setIsCreateGroupOpen(false);
      setNewGroupName('');
      fetchDashboardData();
      if (res.data?.group?.id) {
        navigate(`/group/${res.data.group.id}`);
      }
    } catch (err) {
      console.error('Failed to create group:', err);
      setCreateGroupError(err.response?.data?.error || 'Failed to create group.');
    } finally {
      setCreateGroupLoading(false);
    }
  };

  // Create Quick Table Handler
  const handleCreateQuickTable = async (e) => {
    e.preventDefault();
    setQuickLoading(true);
    setQuickError('');
    try {
      const parsedPlayers = quickPlayerNames
        .split(/[,\n]+/)
        .map((n) => n.trim())
        .filter(Boolean);

      const effectiveChipValue = quickChipCustom
        ? Number(quickChipCustom)
        : Number(quickChipPreset) || 100;

      const numEntryFee = quickHasEntryFee && quickEntryFeeAmount
        ? Number(quickEntryFeeAmount)
        : null;

      const res = await createQuickTable({
        name: quickTableName.trim() || 'Quick Table',
        chipValue: effectiveChipValue,
        default_buy_in: effectiveChipValue,
        entryFee: numEntryFee,
        playerNames: parsedPlayers,
      });

      setIsQuickModalOpen(false);
      setQuickTableName('');
      setQuickChipCustom('');
      setQuickHasEntryFee(false);
      setQuickEntryFeeAmount('');
      setQuickPlayerNames('');
      fetchDashboardData();

      if (res.data?.table?.id) {
        navigate(`/table/${res.data.table.id}`);
      }
    } catch (err) {
      console.error('Failed to create quick table:', err);
      setQuickError(err.response?.data?.error || 'Failed to create quick table.');
    } finally {
      setQuickLoading(false);
    }
  };

  // Open Join Modal with pre-filled code
  const openJoinModalWithCode = (code = '') => {
    setJoinStep('A');
    setInviteCode(code);
    setJoinError('');
    setJoinSuccess('');
    setInspectedGroup(null);
    setUnclaimedPlayers([]);
    setNewPlayerName(user?.username || '');
    setIsJoinModalOpen(true);
  };

  const closeJoinModal = () => {
    if (isJoining) return;
    setIsJoinModalOpen(false);
    setJoinStep('A');
    setJoinError('');
    setJoinSuccess('');
  };

  // STEP A: Validate invite code
  const handleValidateInvite = async (e) => {
    e.preventDefault();
    setJoinError('');
    setJoinSuccess('');

    const cleanCode = inviteCode.trim().toUpperCase();
    if (!cleanCode) {
      setJoinError('Please enter an invite code.');
      return;
    }

    setIsJoining(true);
    try {
      const response = await getGroupByInvite(cleanCode);
      const groupData = response.data;
      setInspectedGroup(groupData);

      if (!groupData.hasUnclaimedPlayers) {
        const joinRes = await api.post('/api/groups/join', {
          invite_code: cleanCode,
        });
        setJoinSuccess(joinRes.data?.message || 'Joined group successfully! Entering...');
        setTimeout(() => {
          setIsJoinModalOpen(false);
          navigate(`/group/${groupData.groupId}`);
        }, 800);
      } else {
        setJoinStep('B');
      }
    } catch (err) {
      console.error('Validate invite error:', err);
      setJoinError(err.response?.data?.error || 'Group not found with that invite code.');
    } finally {
      setIsJoining(false);
    }
  };

  // STEP B -> STEP C: Fetch unclaimed players
  const handleSelectExisting = async () => {
    setJoinError('');
    setIsLoadingPlayers(true);
    setJoinStep('C');
    try {
      const res = await getGroupPlayersList(inspectedGroup.groupId);
      const allPlayers = res.data?.players || [];
      const unclaimed = allPlayers.filter((p) => !p.isClaimed);
      setUnclaimedPlayers(unclaimed);
    } catch (err) {
      console.error('Failed to load unclaimed players:', err);
      setJoinError('Failed to load unclaimed player roster.');
    } finally {
      setIsLoadingPlayers(false);
    }
  };

  // STEP C: Claim selected player identity
  const handleClaimPlayer = async (player) => {
    if (isJoining) return;
    setIsJoining(true);
    setJoinError('');
    setJoinSuccess('');

    try {
      await claimPlayer(inspectedGroup.groupId, {
        playerId: player.id,
        playerName: player.name,
      });
      setJoinSuccess(`Welcome back! You claimed "${player.name}". Entering group...`);
      setTimeout(() => {
        setIsJoinModalOpen(false);
        navigate(`/group/${inspectedGroup.groupId}`);
      }, 800);
    } catch (err) {
      console.error('Claim player error:', err);
      setJoinError(err.response?.data?.error || 'Failed to claim player identity.');
      setIsJoining(false);
    }
  };

  // STEP D: Create new player profile
  const handleCreateNewPlayer = async (e) => {
    e.preventDefault();
    if (isJoining) return;

    const trimmedName = newPlayerName.trim();
    if (!trimmedName) {
      setJoinError('Please enter your in-game name.');
      return;
    }

    setIsJoining(true);
    setJoinError('');
    setJoinSuccess('');

    try {
      await joinNewPlayer(inspectedGroup.groupId, {
        playerName: trimmedName,
      });
      setJoinSuccess(`Joined as "${trimmedName}"! Entering group...`);
      setTimeout(() => {
        setIsJoinModalOpen(false);
        navigate(`/group/${inspectedGroup.groupId}`);
      }, 800);
    } catch (err) {
      console.error('Join new player error:', err);
      setJoinError(err.response?.data?.error || 'Failed to create new player identity.');
      setIsJoining(false);
    }
  };

  const quickTables = activeTables.filter((t) => t.isQuickTable);

  return (
    <div className="min-h-screen bg-felt-green text-cream-text flex flex-col relative pb-20">
      {/* Top Navigation Bar */}
      <header className="bg-felt-dark/95 border-b border-gold-accent/40 sticky top-0 z-30 shadow-lg backdrop-blur-md">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl text-gold-accent select-none">♠</span>
            <span className="font-extrabold text-xl tracking-wider text-gold-accent uppercase">
              Bank Poker
            </span>
          </div>

          <div className="flex items-center gap-2.5">
            {user?.role === 'SUPER_ADMIN' && (
              <Link
                to="/admin"
                className="flex items-center gap-1.5 px-3 py-1.5 bg-yellow-950/70 hover:bg-yellow-900 border border-gold-accent/60 text-gold-accent rounded-xl text-xs font-black transition shadow"
                title="Super Admin Control Plane"
              >
                <ShieldCheck className="w-4 h-4 text-gold-accent" />
                <span className="hidden sm:inline">ADMIN</span>
              </Link>
            )}

            <NotificationsDropdown />

            <div
              onClick={() => setIsProfileOpen(true)}
              className="flex items-center bg-felt-card hover:bg-felt-card/80 p-1 sm:px-3 sm:py-1.5 rounded-xl border border-gold-accent/40 cursor-pointer transition select-none shadow"
              title="Click to manage profile and settings"
            >
              <UserBadge
                displayName={user?.display_name || user?.username}
                username={user?.username}
                avatarId={user?.avatar_id}
                role={user?.role}
                isGuest={user?.is_guest}
                size={32}
              />
            </div>

            <button
              onClick={logout}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-red-950/70 hover:bg-red-900 text-red-200 hover:text-white rounded-xl border border-red-500/40 text-xs font-bold transition active:scale-95 cursor-pointer"
              title="Sign Out"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-6xl w-full mx-auto px-4 py-6 flex-1 space-y-6">
        {/* Error Notification */}
        {error && (
          <div className="p-4 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-3 text-red-200">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        {/* SMART CODE INPUT CARD */}
        <div className="bg-felt-card/90 border-2 border-gold-accent/60 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center justify-between gap-2 mb-3">
            <div className="flex items-center gap-2">
              <Pin className="w-4 h-4 text-gold-accent" />
              <h2 className="text-xs font-black uppercase tracking-wider text-gold-accent">
                JOIN BY CODE
              </h2>
            </div>

            <button
              type="button"
              onClick={handlePasteCode}
              className="flex items-center gap-1 px-2.5 py-1 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 text-gold-accent text-[10px] font-bold uppercase rounded-lg transition active:scale-95 cursor-pointer"
              title="Paste from clipboard"
            >
              <ClipboardPaste className="w-3 h-3 text-gold-accent" />
              <span>PASTE</span>
            </button>
          </div>

          <form onSubmit={handleSmartLookup} className="flex items-center gap-2">
            <input
              type="text"
              value={smartCode}
              onChange={(e) => {
                setSmartCode(e.target.value.toUpperCase().slice(0, 8));
                if (smartError) setSmartError('');
              }}
              placeholder="ENTER 6-CHAR CODE"
              maxLength={8}
              className="flex-1 px-4 py-3 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text font-mono font-black tracking-widest text-sm text-center placeholder-cream-text/30 focus:outline-none focus:border-gold-accent transition"
            />
            <button
              type="submit"
              disabled={smartLoading || !smartCode.trim()}
              className="px-5 py-3 bg-gold-accent hover:bg-gold-light text-black font-black uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 disabled:opacity-50 flex items-center justify-center shrink-0 cursor-pointer"
            >
              {smartLoading ? (
                <div className="w-4 h-4 border-2 border-black border-t-transparent rounded-full animate-spin" />
              ) : (
                <ArrowRight className="w-4 h-4" />
              )}
            </button>
          </form>

          {smartError && (
            <p className="text-xs text-red-400 font-semibold mt-2">{smartError}</p>
          )}

          <p className="text-xs text-cream-text/60 mt-2">
            Enter 6-char group invite or table code to jump straight in
          </p>
        </div>

        {/* 1. LIVE NOW: ACTIVE TABLES HORIZONTAL SECTION */}
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-red-950/80 border border-red-500/80 text-red-300 rounded-full text-xs font-black tracking-wider uppercase shadow-sm">
                <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
                LIVE NOW
              </span>
              <span className="text-xs text-cream-text/60 font-semibold">
                ({activeTables.length} Active {activeTables.length === 1 ? 'Table' : 'Tables'})
              </span>
            </div>

            <button
              onClick={fetchDashboardData}
              disabled={loading}
              title="Refresh tables"
              className="p-1.5 text-gold-accent hover:text-gold-light transition cursor-pointer"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          {activeTables.length === 0 ? (
            <div className="p-5 bg-felt-card/40 border border-dashed border-gold-accent/30 rounded-2xl text-center">
              <p className="text-xs text-cream-text/60">
                No live tables right now. Start an instant quick table or join a group table.
              </p>
            </div>
          ) : (
            <div className="flex gap-4 overflow-x-auto pb-3 pt-1 scrollbar-thin">
              {activeTables.map((table) => (
                <div
                  key={table.id}
                  className="min-w-[260px] max-w-[280px] bg-felt-card/90 border border-gold-accent/50 rounded-2xl p-4 shadow-lg flex flex-col justify-between shrink-0 hover:border-gold-accent transition duration-150"
                >
                  <div>
                    <div className="flex items-center justify-between gap-1 mb-2">
                      <span className="text-[10px] font-mono font-bold tracking-wider text-gold-accent/80 uppercase">
                        {table.gameType || "NL Hold'em"}
                      </span>
                      <span className="flex items-center gap-1 text-[10px] font-bold text-red-400">
                        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                        ACTIVE
                      </span>
                    </div>

                    <h4 className="font-extrabold text-base text-cream-text truncate">
                      {table.name}
                    </h4>

                    <p className="text-xs text-cream-text/60 mt-0.5 truncate flex items-center gap-1">
                      {table.isQuickTable ? (
                        <>
                          <Zap className="w-3 h-3 text-gold-accent" />
                          <span>Quick Table</span>
                        </>
                      ) : (
                        <>
                          <span className="text-gold-accent">♣</span>
                          <span>{table.groupName || 'Group Table'}</span>
                        </>
                      )}
                    </p>

                    <div className="flex items-center gap-3 mt-3 text-xs text-cream-text/80">
                      <div className="flex items-center gap-1">
                        <Users className="w-3.5 h-3.5 text-gold-accent" />
                        <span>{table.playerCount} Players</span>
                      </div>
                      {table.chipValue && (
                        <div className="font-mono text-gold-light text-[11px] font-bold">
                          ${Number(table.chipValue).toLocaleString()}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="mt-4 pt-3 border-t border-gold-accent/20 flex items-center justify-between gap-2">
                    {table.code ? (
                      <span className="font-mono text-[11px] font-bold text-gold-accent/80 bg-felt-dark px-2 py-1 rounded-lg border border-gold-accent/30">
                        {table.code}
                      </span>
                    ) : <span />}

                    <Link
                      to={`/table/${table.id}`}
                      className="px-3 py-1.5 bg-gold-accent hover:bg-gold-light text-black font-black uppercase text-[11px] rounded-xl shadow transition flex items-center gap-1 active:scale-95"
                    >
                      <span>Join</span>
                      <ChevronRight className="w-3 h-3" />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* 2. POKER GROUPS SECTION */}
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-base text-gold-accent select-none">♣</span>
              <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent">
                POKER GROUPS
              </h2>
              <span className="px-2 py-0.5 bg-gold-accent/20 border border-gold-accent/40 rounded-full text-[10px] font-bold text-gold-accent">
                {groups.length}
              </span>
            </div>

            <button
              type="button"
              onClick={() => {
                setNewGroupName('');
                setCreateGroupError('');
                setIsCreateGroupOpen(true);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 text-gold-accent text-xs font-bold uppercase rounded-xl shadow transition active:scale-95 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5 text-gold-accent" />
              <span>New Group</span>
            </button>
          </div>

          {loading && groups.length === 0 ? (
            <div className="py-12 text-center text-gold-accent">
              <div className="w-8 h-8 border-3 border-gold-accent border-t-transparent rounded-full animate-spin mx-auto mb-2" />
              <span className="text-xs font-semibold">Loading groups...</span>
            </div>
          ) : groups.length === 0 ? (
            <div className="bg-felt-card/40 border border-dashed border-gold-accent/30 rounded-2xl p-8 text-center space-y-3">
              <Users className="w-12 h-12 text-gold-accent/40 mx-auto" />
              <h3 className="text-base font-bold text-cream-text">No Groups Joined Yet</h3>
              <p className="text-xs text-cream-text/60 max-w-sm mx-auto">
                Create your own poker group to host games and track balances, or join with an invite code.
              </p>
              <div className="flex items-center justify-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateGroupOpen(true)}
                  className="px-4 py-2 bg-gold-accent text-black font-bold text-xs rounded-xl shadow hover:bg-gold-light transition cursor-pointer"
                >
                  Create Group
                </button>
                <button
                  type="button"
                  onClick={() => openJoinModalWithCode()}
                  className="px-4 py-2 bg-felt-dark border border-gold-accent/40 text-gold-accent font-bold text-xs rounded-xl shadow hover:bg-gold-accent/10 transition cursor-pointer"
                >
                  Join with Code
                </button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {groups.map((group) => {
                const isOwner = group.owner_user_id === user?.id || group.owner_id === user?.id || group.role === 'OWNER';
                return (
                  <div
                    key={group.id}
                    className="bg-felt-card/90 border-2 border-gold-accent/60 rounded-2xl p-5 shadow-xl hover:border-gold-accent transition duration-150 flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-2 mb-2">
                        <div className="flex items-center gap-2">
                          <span className="text-gold-accent text-lg">♣</span>
                          <h3 className="font-black text-lg text-cream-text line-clamp-1">
                            {group.name}
                          </h3>
                        </div>

                        {isOwner && (
                          <span className="px-2 py-0.5 bg-gold-accent/20 border border-gold-accent/60 text-gold-accent text-[9px] font-black tracking-wider uppercase rounded-md shrink-0">
                            ADMIN
                          </span>
                        )}
                      </div>

                      {group.invite_code && (
                        <div className="my-3">
                          <GroupCodeChip code={group.invite_code} groupName={group.name} />
                        </div>
                      )}

                      <div className="flex items-center gap-2 text-xs text-cream-text/70 mt-2">
                        <Users className="w-3.5 h-3.5 text-gold-accent" />
                        <span>
                          {group.member_count || group.memberCount || 1} {group.member_count === 1 ? 'member' : 'members'}
                        </span>
                      </div>
                    </div>

                    {/* HARD RULE: NO balance display on group card */}
                    <div className="pt-4 border-t border-gold-accent/20 mt-4">
                      <Link
                        to={`/group/${group.id}`}
                        state={{ group }}
                        className="w-full py-2.5 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent font-bold uppercase tracking-wider text-xs rounded-xl border border-gold-accent/50 shadow flex items-center justify-center gap-1.5 transition active:scale-95"
                      >
                        <span>Enter Group</span>
                        <ChevronRight className="w-4 h-4" />
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* 3. QUICK TABLES SECTION */}
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-gold-accent" />
              <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent">
                QUICK TABLES
              </h2>
              <span className="px-2 py-0.5 bg-gold-accent/20 border border-gold-accent/40 rounded-full text-[10px] font-bold text-gold-accent">
                {quickTables.length}
              </span>
            </div>

            <button
              type="button"
              onClick={() => {
                setQuickTableName('');
                setQuickChipPreset(100);
                setQuickChipCustom('');
                setQuickHasEntryFee(false);
                setQuickEntryFeeAmount('');
                setQuickPlayerNames('');
                setQuickError('');
                setIsQuickModalOpen(true);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 text-gold-accent text-xs font-bold uppercase rounded-xl shadow transition active:scale-95 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5 text-gold-accent" />
              <span>Instant Table</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Quick table cards */}
            {quickTables.map((table) => (
              <div
                key={table.id}
                className="bg-felt-card/90 border border-gold-accent/50 rounded-2xl p-5 shadow-lg flex flex-col justify-between hover:border-gold-accent transition"
              >
                <div>
                  <div className="flex items-center justify-between gap-1 mb-2">
                    <span className="text-[10px] font-mono font-bold tracking-wider text-gold-accent uppercase">
                      Quick Game
                    </span>
                    <span className="flex items-center gap-1 text-[10px] font-bold text-red-400">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                      LIVE
                    </span>
                  </div>

                  <h3 className="font-extrabold text-base text-cream-text truncate">
                    {table.name}
                  </h3>

                  {table.code && (
                    <div className="my-2.5">
                      <GroupCodeChip code={table.code} groupName={table.name} />
                    </div>
                  )}

                  <div className="flex items-center gap-3 text-xs text-cream-text/70 mt-2">
                    <div className="flex items-center gap-1">
                      <Users className="w-3.5 h-3.5 text-gold-accent" />
                      <span>{table.playerCount} Seated</span>
                    </div>
                    {table.chipValue && (
                      <span className="font-mono text-gold-light text-[11px] font-bold">
                        Chip: ${Number(table.chipValue).toLocaleString()}
                      </span>
                    )}
                  </div>
                </div>

                <div className="pt-3 border-t border-gold-accent/20 mt-4">
                  <Link
                    to={`/table/${table.id}`}
                    className="w-full py-2 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent font-bold uppercase tracking-wider text-xs rounded-xl border border-gold-accent/40 shadow flex items-center justify-center gap-1 transition active:scale-95"
                  >
                    <span>Enter Table</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ))}

            {/* Start Instant Table Action Card */}
            <div
              onClick={() => {
                setQuickTableName('');
                setQuickChipPreset(100);
                setQuickChipCustom('');
                setQuickHasEntryFee(false);
                setQuickEntryFeeAmount('');
                setQuickPlayerNames('');
                setQuickError('');
                setIsQuickModalOpen(true);
              }}
              className="bg-felt-card/40 border-2 border-dashed border-gold-accent/40 hover:border-gold-accent hover:bg-felt-card/60 rounded-2xl p-6 shadow transition cursor-pointer flex flex-col items-center justify-center text-center group min-h-[160px]"
            >
              <div className="w-10 h-10 rounded-full bg-gold-accent/15 border border-gold-accent/40 flex items-center justify-center mb-2 group-hover:scale-110 transition">
                <Zap className="w-5 h-5 text-gold-accent" />
              </div>
              <h4 className="font-black text-sm text-gold-accent uppercase tracking-wide">
                Start Instant Table
              </h4>
              <p className="text-xs text-cream-text/60 mt-1 max-w-[200px]">
                No group needed. Create and share 6-character code immediately.
              </p>
            </div>
          </div>
        </section>
      </main>

      {/* Floating Action Button (+) */}
      <button
        type="button"
        onClick={() => setIsActionMenuOpen(true)}
        className="fixed bottom-6 right-6 w-14 h-14 bg-gradient-to-tr from-yellow-600 via-gold-accent to-yellow-400 hover:opacity-95 text-black rounded-full shadow-2xl border-2 border-gold-light flex items-center justify-center active:scale-95 transition-all z-40 cursor-pointer"
        title="Quick Actions"
      >
        <Plus className="w-7 h-7 stroke-[2.5]" />
      </button>

      {/* Action Menu Modal (Bottom Sheet style) */}
      {isActionMenuOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-felt-card border-t-2 sm:border-2 border-gold-accent rounded-t-3xl sm:rounded-2xl w-full max-w-sm p-6 shadow-2xl relative animate-in slide-in-from-bottom sm:zoom-in-95 duration-200">
            <button
              onClick={() => setIsActionMenuOpen(false)}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-base font-black text-gold-accent uppercase tracking-wide mb-4">
              Quick Actions
            </h3>

            <div className="space-y-3">
              <button
                type="button"
                onClick={() => {
                  setIsActionMenuOpen(false);
                  setIsCreateGroupOpen(true);
                }}
                className="w-full p-3.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 rounded-xl flex items-center gap-3 text-left transition cursor-pointer active:scale-98"
              >
                <div className="p-2 bg-gold-accent/20 rounded-lg text-gold-accent">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <div className="font-bold text-sm text-cream-text">New Poker Group</div>
                  <div className="text-[11px] text-cream-text/60">Persistent group with balances & history</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsActionMenuOpen(false);
                  setIsQuickModalOpen(true);
                }}
                className="w-full p-3.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 rounded-xl flex items-center gap-3 text-left transition cursor-pointer active:scale-98"
              >
                <div className="p-2 bg-gold-accent/20 rounded-lg text-gold-accent">
                  <Zap className="w-5 h-5" />
                </div>
                <div>
                  <div className="font-bold text-sm text-cream-text">New Quick Table</div>
                  <div className="text-[11px] text-cream-text/60">Standalone session with shareable code</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsActionMenuOpen(false);
                  openJoinModalWithCode();
                }}
                className="w-full p-3.5 bg-felt-dark hover:bg-felt-dark/80 border border-gold-accent/50 rounded-xl flex items-center gap-3 text-left transition cursor-pointer active:scale-98"
              >
                <div className="p-2 bg-gold-accent/20 rounded-lg text-gold-accent">
                  <Key className="w-5 h-5" />
                </div>
                <div>
                  <div className="font-bold text-sm text-cream-text">Join by Code</div>
                  <div className="text-[11px] text-cream-text/60">Enter 6-character group invite code</div>
                </div>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Multi-step Join Group Modal */}
      {isJoinModalOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <button
              onClick={closeJoinModal}
              disabled={isJoining}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition disabled:opacity-40 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-2 mb-4">
              <Key className="w-6 h-6 text-gold-accent" />
              <h2 className="text-xl font-bold text-gold-accent">
                {joinStep === 'A' && 'Join Poker Group'}
                {joinStep === 'B' && 'Identity Claim'}
                {joinStep === 'C' && 'Select Your Name'}
                {joinStep === 'D' && 'New Player Profile'}
              </h2>
            </div>

            {joinError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{joinError}</span>
              </div>
            )}

            {joinSuccess && (
              <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-lg flex items-center gap-2 text-green-200 text-xs animate-pulse">
                <CheckCircle2 className="w-4 h-4 shrink-0 text-green-400" />
                <span>{joinSuccess}</span>
              </div>
            )}

            {/* STEP A */}
            {joinStep === 'A' && (
              <div>
                <p className="text-xs text-cream-text/75 mb-4">
                  Enter the unique invite code provided by your group organizer or table manager.
                </p>

                <form onSubmit={handleValidateInvite} className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                      Invite Code
                    </label>
                    <input
                      type="text"
                      value={inviteCode}
                      onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                      placeholder="e.g. POKER123"
                      className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text font-mono placeholder-cream-text/40 focus:outline-none focus:border-gold-accent uppercase transition"
                      required
                      autoFocus
                    />
                  </div>

                  <div className="flex items-center justify-end gap-3 pt-2">
                    <button
                      type="button"
                      onClick={closeJoinModal}
                      className="px-4 py-2.5 bg-felt-dark hover:bg-felt-dark/80 text-cream-text/80 rounded-xl text-sm font-semibold transition cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isJoining || !inviteCode.trim()}
                      className="px-5 py-2.5 bg-gold-accent text-black font-bold uppercase tracking-wider text-sm rounded-xl shadow hover:bg-gold-light disabled:opacity-50 transition cursor-pointer"
                    >
                      {isJoining ? 'Checking Code...' : 'Join Group'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* STEP B */}
            {joinStep === 'B' && (
              <div className="space-y-4 animate-in fade-in duration-200">
                <div className="p-3 bg-felt-dark/80 border border-gold-accent/30 rounded-xl flex items-center justify-between">
                  <span className="text-xs text-cream-text/70">Group:</span>
                  <span className="font-bold text-gold-accent text-sm">{inspectedGroup?.name}</span>
                </div>

                {inspectedGroup?.userHasPlayer ? (
                  <div className="text-center space-y-1.5 pt-1">
                    <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-gold-accent/15 border border-gold-accent/40 text-gold-accent text-xs font-bold mb-1">
                      <UserCheck className="w-3.5 h-3.5" />
                      <span>Linked as: {inspectedGroup.claimedPlayerName || 'Claimed Player'}</span>
                    </div>
                    <h3 className="text-base font-black text-cream-text">
                      Already in this group
                    </h3>
                    <p className="text-xs text-cream-text/65">
                      You are currently linked to this group. If you claimed the wrong player identity, you can re-claim another one below.
                    </p>
                  </div>
                ) : (
                  <div className="text-center space-y-1.5 pt-1">
                    <h3 className="text-base font-black text-cream-text">
                      Have you played in this group before?
                    </h3>
                    <p className="text-xs text-cream-text/65">
                      Claim your previous player identity to restore your balance and game history.
                    </p>
                  </div>
                )}

                <div className="space-y-3 pt-2">
                  {inspectedGroup?.userHasPlayer ? (
                    <>
                      <button
                        type="button"
                        onClick={async () => {
                          const joinRes = await api.post('/api/groups/join', {
                            invite_code: inviteCode.trim().toUpperCase(),
                          });
                          setJoinSuccess(joinRes.data?.message || 'Entering group...');
                          setTimeout(() => {
                            setIsJoinModalOpen(false);
                            navigate(`/group/${inspectedGroup.groupId}`);
                          }, 500);
                        }}
                        className="w-full p-3.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent hover:opacity-95 text-black font-black uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <UserCheck className="w-4 h-4 text-black shrink-0" />
                        <span>Enter Group</span>
                      </button>

                      <button
                        type="button"
                        onClick={handleSelectExisting}
                        className="w-full p-3.5 bg-[#043327] hover:bg-[#064e3b] border-2 border-gold-accent/60 hover:border-gold-accent text-cream-text font-bold uppercase tracking-wider text-xs rounded-xl shadow-md transition active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <UserPlus className="w-4 h-4 text-gold-accent shrink-0" />
                        <span>Re-claim / Switch Player Identity</span>
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={handleSelectExisting}
                        className="w-full p-3.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent hover:opacity-95 text-black font-black uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <UserCheck className="w-4 h-4 text-black shrink-0" />
                        <span>Yes, I was in this group</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setJoinError('');
                          setJoinStep('D');
                        }}
                        className="w-full p-3.5 bg-[#043327] hover:bg-[#064e3b] border-2 border-gold-accent/60 hover:border-gold-accent text-cream-text font-bold uppercase tracking-wider text-xs rounded-xl shadow-md transition active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <UserPlus className="w-4 h-4 text-gold-accent shrink-0" />
                        <span>No, I'm a new player</span>
                      </button>
                    </>
                  )}
                </div>

                <div className="pt-2 flex justify-start">
                  <button
                    type="button"
                    onClick={() => {
                      setJoinError('');
                      setJoinStep('A');
                    }}
                    className="inline-flex items-center gap-1 text-xs text-cream-text/70 hover:text-gold-accent transition font-semibold cursor-pointer"
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                    <span>Back</span>
                  </button>
                </div>
              </div>
            )}

            {/* STEP C */}
            {joinStep === 'C' && (
              <div className="space-y-4 animate-in fade-in duration-200">
                <p className="text-xs text-cream-text/70">
                  Tap your name below to claim your past games and balances.
                </p>

                {isLoadingPlayers ? (
                  <div className="py-8 text-center text-xs text-cream-text/50">
                    <div className="w-6 h-6 border-2 border-gold-accent border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                    Loading unclaimed roster...
                  </div>
                ) : unclaimedPlayers.length === 0 ? (
                  <div className="py-6 text-center space-y-3 bg-felt-dark/60 rounded-xl p-4 border border-gold-accent/20">
                    <p className="text-xs text-cream-text/60">
                      No unclaimed player identities available in this group.
                    </p>
                    <button
                      type="button"
                      onClick={() => setJoinStep('D')}
                      className="px-4 py-2 bg-gold-accent text-black font-bold text-xs rounded-xl shadow transition cursor-pointer"
                    >
                      Join as New Player
                    </button>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-2.5 max-h-56 overflow-y-auto p-1">
                    {unclaimedPlayers.map((player) => (
                      <button
                        key={player.id}
                        type="button"
                        disabled={isJoining}
                        onClick={() => handleClaimPlayer(player)}
                        className="p-3 bg-[#043327] hover:bg-gold-accent hover:text-black border-2 border-gold-accent/50 hover:border-gold-accent rounded-xl font-bold text-xs text-[#f5f5dc] shadow-md hover:scale-105 active:scale-95 transition-all duration-150 flex flex-col items-center justify-center gap-1 cursor-pointer disabled:opacity-50"
                      >
                        <span className="font-mono text-[9px] uppercase text-gold-accent/70 group-hover:text-black">
                          Player
                        </span>
                        <span className="font-black text-center truncate max-w-full">
                          {player.name}
                        </span>
                      </button>
                    ))}
                  </div>
                )}

                <div className="pt-2 flex justify-start">
                  <button
                    type="button"
                    disabled={isJoining}
                    onClick={() => {
                      setJoinError('');
                      setJoinStep('B');
                    }}
                    className="inline-flex items-center gap-1 text-xs text-cream-text/70 hover:text-gold-accent transition font-semibold cursor-pointer disabled:opacity-50"
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                    <span>Back</span>
                  </button>
                </div>
              </div>
            )}

            {/* STEP D */}
            {joinStep === 'D' && (
              <div className="space-y-4 animate-in fade-in duration-200">
                <p className="text-xs text-cream-text/70">
                  Enter your display name to start with a fresh player profile in this group.
                </p>

                <form onSubmit={handleCreateNewPlayer} className="space-y-4">
                  <div>
                    <label className="block text-[11px] font-bold uppercase tracking-wider text-cream-text/70 mb-1">
                      Your In-Game Name
                    </label>
                    <input
                      type="text"
                      value={newPlayerName}
                      onChange={(e) => setNewPlayerName(e.target.value)}
                      placeholder="Enter your name"
                      className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 focus:border-gold-accent rounded-xl text-cream-text font-bold placeholder-cream-text/40 outline-none transition"
                      required
                      autoFocus
                    />
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <button
                      type="button"
                      disabled={isJoining}
                      onClick={() => {
                        setJoinError('');
                        setJoinStep('B');
                      }}
                      className="inline-flex items-center gap-1 text-xs text-cream-text/70 hover:text-gold-accent transition font-semibold cursor-pointer disabled:opacity-50"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                      <span>Back</span>
                    </button>

                    <button
                      type="submit"
                      disabled={isJoining || !newPlayerName.trim()}
                      className="px-5 py-2.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent hover:opacity-95 active:scale-[0.98] text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg transition disabled:opacity-50 flex items-center justify-center cursor-pointer"
                    >
                      {isJoining ? 'Joining...' : 'Create & Join'}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Group Modal */}
      {isCreateGroupOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            <button
              onClick={() => setIsCreateGroupOpen(false)}
              disabled={createGroupLoading}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition disabled:opacity-40 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 bg-felt-dark text-gold-accent border border-gold-accent/40 rounded-xl">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-xl font-black text-gold-accent uppercase tracking-wide">
                  Create New Group
                </h3>
                <p className="text-xs text-cream-text/60">
                  Enter a name for your poker group. You will become the group host.
                </p>
              </div>
            </div>
            {createGroupError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-xl text-red-200 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{createGroupError}</span>
              </div>
            )}
            <form onSubmit={handleCreateGroup} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                  Group Name
                </label>
                <input
                  type="text"
                  value={newGroupName}
                  onChange={(e) => setNewGroupName(e.target.value)}
                  placeholder="e.g. Friday Night Poker"
                  autoFocus
                  maxLength={50}
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text placeholder-cream-text/40 font-bold text-sm focus:outline-none focus:border-gold-accent"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateGroupOpen(false)}
                  disabled={createGroupLoading}
                  className="px-4 py-2.5 bg-felt-dark border border-gold-accent/30 text-cream-text/80 rounded-xl text-xs font-bold hover:text-cream-text cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createGroupLoading || !newGroupName.trim()}
                  className="px-5 py-2.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 disabled:opacity-50 cursor-pointer"
                >
                  {createGroupLoading ? 'Creating...' : 'Create Group'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Quick Table Creation Modal (mirrors Android CreateQuickTableBottomSheet) */}
      {isQuickModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setIsQuickModalOpen(false)}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 bg-felt-dark text-gold-accent border border-gold-accent/40 rounded-xl">
                <Zap className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-xl font-black text-gold-accent uppercase tracking-wide">
                  Create Quick Table
                </h3>
                <p className="text-xs text-cream-text/60">
                  Instant standalone session with shareable code
                </p>
              </div>
            </div>

            {quickError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-xl text-red-200 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{quickError}</span>
              </div>
            )}

            <form onSubmit={handleCreateQuickTable} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                  Table Name
                </label>
                <input
                  type="text"
                  value={quickTableName}
                  onChange={(e) => setQuickTableName(e.target.value)}
                  placeholder="e.g. Quick Cash Game"
                  maxLength={40}
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text font-bold text-sm focus:outline-none focus:border-gold-accent"
                />
              </div>

              {/* Chip Value Presets */}
              <div>
                <label className="block text-[10px] font-bold text-gold-accent/80 uppercase tracking-wider mb-1.5">
                  DEFAULT BUY-IN / CHIP VALUE
                </label>
                <div className="grid grid-cols-4 gap-2 mb-2">
                  {CHIP_PRESETS.map((preset) => {
                    const isSelected = quickChipPreset === preset && !quickChipCustom;
                    return (
                      <button
                        key={preset}
                        type="button"
                        onClick={() => {
                          setQuickChipPreset(preset);
                          setQuickChipCustom('');
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
                  value={quickChipCustom}
                  onChange={(e) => setQuickChipCustom(e.target.value)}
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
                    checked={quickHasEntryFee}
                    onChange={(e) => setQuickHasEntryFee(e.target.checked)}
                    className="w-4 h-4 accent-[#d4af37] rounded cursor-pointer"
                  />
                </div>

                {quickHasEntryFee && (
                  <div>
                    <input
                      type="number"
                      value={quickEntryFeeAmount}
                      onChange={(e) => setQuickEntryFeeAmount(e.target.value)}
                      placeholder="Entry Fee Amount"
                      className="w-full px-3 py-2 bg-felt-card border border-gold-accent/40 rounded-xl text-cream-text font-mono text-xs focus:outline-none focus:border-gold-accent"
                    />
                  </div>
                )}
              </div>

              {/* Initial Players */}
              <div>
                <label className="block text-xs font-bold text-cream-text/80 uppercase tracking-wider mb-1.5">
                  Initial Player Names (optional)
                </label>
                <input
                  type="text"
                  value={quickPlayerNames}
                  onChange={(e) => setQuickPlayerNames(e.target.value)}
                  placeholder="e.g. Arash, Reza, Ali (comma separated)"
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/40 rounded-xl text-cream-text font-bold text-sm focus:outline-none focus:border-gold-accent"
                />
                <p className="text-[11px] text-cream-text/50 mt-1">
                  Optional. Comma-separated names to auto-seat at the table.
                </p>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsQuickModalOpen(false)}
                  className="px-4 py-2.5 bg-felt-dark border border-gold-accent/30 text-cream-text/80 rounded-xl text-xs font-bold hover:text-cream-text cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={quickLoading}
                  className="px-5 py-2.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-extrabold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 disabled:opacity-50 cursor-pointer"
                >
                  {quickLoading ? 'Creating...' : 'Start Table'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Profile & Identity Modal */}
      {isProfileOpen && <ProfileModal onClose={() => setIsProfileOpen(false)} />}
    </div>
  );
};

export default Dashboard;
