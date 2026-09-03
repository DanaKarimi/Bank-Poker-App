import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api, { getGroupByInvite, getGroupPlayersList, claimPlayer, joinNewPlayer } from '../api';
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
} from 'lucide-react';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  const fetchGroups = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/groups/my-groups');
      setGroups(response.data.groups || []);
    } catch (err) {
      console.error('Failed to fetch groups:', err);
      setError('Unable to load your groups. Please check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const openJoinModal = () => {
    setJoinStep('A');
    setInviteCode('');
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

  // STEP A: Validate invite code and branch
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
      // 1. Call GET /api/groups/by-invite/:code
      const response = await getGroupByInvite(cleanCode);
      const groupData = response.data;
      setInspectedGroup(groupData);

      // Branch logic:
      // If group has NO unclaimed players (native online or fully claimed): join directly
      if (!groupData.hasUnclaimedPlayers) {
        // Join group directly
        const joinRes = await api.post('/api/groups/join', {
          invite_code: cleanCode,
        });
        setJoinSuccess(joinRes.data?.message || 'Joined group successfully! Entering...');
        setTimeout(() => {
          setIsJoinModalOpen(false);
          navigate(`/group/${groupData.groupId}`);
        }, 800);
      } else {
        // Group has unclaimed players -> Go to STEP B (Question / Re-claim option)
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

  return (
    <div className="min-h-screen bg-felt-green text-cream-text flex flex-col">
      {/* Top Navigation Bar */}
      <header className="bg-felt-dark/95 border-b border-gold-accent/40 sticky top-0 z-30 shadow-lg backdrop-blur-md">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl text-gold-accent select-none">♠</span>
            <span className="font-extrabold text-xl tracking-wider text-gold-accent uppercase">
              BankPoker
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 bg-felt-card px-3 py-1.5 rounded-xl border border-gold-accent/30 text-sm">
              <span className="text-cream-text/70">Player:</span>
              <span className="font-bold text-gold-accent">{user?.username}</span>
            </div>

            <button
              onClick={logout}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-red-900/60 hover:bg-red-800 text-red-200 hover:text-white rounded-xl border border-red-500/40 text-sm font-semibold transition active:scale-95 cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-6xl w-full mx-auto px-4 py-8 flex-1">
        {/* Welcome & Action Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 bg-felt-card/80 p-6 rounded-2xl border border-gold-accent/50 shadow-xl">
          <div>
            <h1 className="text-2xl sm:text-3xl font-black text-gold-accent tracking-wide flex items-center gap-2">
              Welcome back, {user?.username}
            </h1>
            <p className="text-sm text-cream-text/75 mt-1">
              Select a poker group below to view your balance, buy-ins, and performance history.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchGroups}
              disabled={loading}
              title="Refresh groups"
              className="p-3 bg-felt-dark hover:bg-felt-dark/80 text-gold-accent rounded-xl border border-gold-accent/40 shadow transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
            </button>

            <button
              onClick={openJoinModal}
              className="px-5 py-3 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider text-sm rounded-xl shadow-lg hover:opacity-95 active:scale-95 transition flex items-center gap-2 cursor-pointer"
            >
              <Plus className="w-5 h-5" />
              <span>Join Group</span>
            </button>
          </div>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="mb-6 p-4 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-3 text-red-200">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Groups Grid */}
        {loading && groups.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-12 h-12 border-4 border-gold-accent border-t-transparent rounded-full animate-spin"></div>
            <p className="mt-4 text-gold-accent font-medium tracking-wide">Loading your poker groups...</p>
          </div>
        ) : groups.length === 0 ? (
          <div className="bg-felt-card/50 border border-dashed border-gold-accent/40 rounded-2xl p-12 text-center">
            <Users className="w-16 h-16 text-gold-accent/40 mx-auto mb-3" />
            <h3 className="text-xl font-bold text-cream-text">No Groups Joined Yet</h3>
            <p className="text-sm text-cream-text/60 max-w-md mx-auto mt-1 mb-6">
              You haven't joined any poker groups yet. Ask your group admin for an invite code or join an existing group.
            </p>
            <button
              onClick={openJoinModal}
              className="px-6 py-2.5 bg-gold-accent text-black font-bold rounded-xl shadow hover:bg-gold-light transition cursor-pointer"
            >
              Join with Invite Code
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {groups.map((group) => (
              <div
                key={group.id}
                className="bg-felt-card/90 border-2 border-gold-accent/60 rounded-2xl p-6 shadow-xl hover:border-gold-accent hover:shadow-2xl transition duration-200 flex flex-col justify-between group"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-2">
                      <span className="text-gold-accent text-xl">♣</span>
                      <h3 className="font-extrabold text-xl text-cream-text group-hover:text-gold-light transition line-clamp-1">
                        {group.name}
                      </h3>
                    </div>

                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider border ${
                        group.mode === 'ONLINE'
                          ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/50'
                          : 'bg-felt-dark text-cream-text/60 border-gold-accent/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          group.mode === 'ONLINE' ? 'bg-emerald-400 animate-pulse' : 'bg-cream-text/40'
                        }`}
                      />
                      <span>{group.mode || 'OFFLINE'}</span>
                    </span>
                  </div>

                  {group.invite_code && (
                    <div className="flex items-center gap-1.5 text-xs text-cream-text/70 bg-felt-dark/80 px-3 py-1.5 rounded-lg border border-gold-accent/20 w-fit mb-4">
                      <Key className="w-3.5 h-3.5 text-gold-accent" />
                      <span>Code:</span>
                      <span className="font-mono font-bold text-gold-accent">{group.invite_code}</span>
                    </div>
                  )}
                </div>

                <div className="pt-4 border-t border-gold-accent/20 mt-4">
                  <Link
                    to={`/group/${group.id}`}
                    state={{ group }}
                    className="w-full py-2.5 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent font-bold uppercase tracking-wider text-xs rounded-xl border border-gold-accent/50 shadow flex items-center justify-center gap-1.5 transition active:scale-95"
                  >
                    <span>{group.mode === 'ONLINE' ? 'Enter Table & Requests' : 'View Stats'}</span>
                    <ChevronRight className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Multi-step Join Group Modal */}
      {isJoinModalOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
            {/* Close button */}
            <button
              onClick={closeJoinModal}
              disabled={isJoining}
              className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition disabled:opacity-40 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Modal Header */}
            <div className="flex items-center gap-2 mb-4">
              <Key className="w-6 h-6 text-gold-accent" />
              <h2 className="text-xl font-bold text-gold-accent">
                {joinStep === 'A' && 'Join Poker Group'}
                {joinStep === 'B' && 'Identity Claim'}
                {joinStep === 'C' && 'Select Your Name'}
                {joinStep === 'D' && 'New Player Profile'}
              </h2>
            </div>

            {/* Error message */}
            {joinError && (
              <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
                <span>{joinError}</span>
              </div>
            )}

            {/* Success message */}
            {joinSuccess && (
              <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-lg flex items-center gap-2 text-green-200 text-xs animate-pulse">
                <CheckCircle2 className="w-4 h-4 shrink-0 text-green-400" />
                <span>{joinSuccess}</span>
              </div>
            )}

            {/* STEP A: Invite Code Input */}
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

            {/* STEP B: Question (Converted Group) */}
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
                      If you played before this group went online, claim your previous player identity to restore your balance and game history.
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

            {/* STEP C: Unclaimed Player List */}
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

            {/* STEP D: New Player Profile Form */}
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
    </div>
  );
};

export default Dashboard;
