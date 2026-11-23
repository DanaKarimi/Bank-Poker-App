import React, { useState, useEffect } from 'react';
import {
  Shield,
  Users,
  Layers,
  CreditCard,
  X,
  KeyRound,
  Trash2,
  Copy,
  AlertTriangle,
  ArrowLeft,
  Plus,
  Edit2,
  Check,
  RefreshCw,
} from 'lucide-react';
import { PokerAvatar } from './AvatarSystem';
import {
  getGroupAdminOverview,
  getGroupAdminTable,
  updateGroupMemberRole,
  removeGroupMember,
  resetGroupMemberPassword,
  updateGroupAdminTableStatus,
  addGroupAdminTablePlayer,
  deleteGroupAdminTablePlayer,
  addGroupAdminTableBuyIn,
  updateGroupAdminTableBuyIn,
  deleteGroupAdminTableBuyIn,
  addGroupAdminTableExit,
  updateGroupAdminTableExit,
  deleteGroupAdminTableExit,
  deleteGroupPayment,
} from '../api';

const formatSigned = (amount) => {
  const n = Number(amount) || 0;
  if (n > 0) return `+$${n.toLocaleString('en-US')}`;
  if (n < 0) return `-$${Math.abs(n).toLocaleString('en-US')}`;
  return '$0';
};

const GroupAdminModal = ({ groupId, groupName, onClose, onDataChanged }) => {
  const [tab, setTab] = useState('members'); // 'members' | 'tables' | 'payments'
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [banner, setBanner] = useState({ type: '', text: '' });

  // Member actions
  const [resetTarget, setResetTarget] = useState(null);
  const [tempPass, setTempPass] = useState('');
  const [resetResult, setResetResult] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);

  // Table editor sub-view
  const [editingTableId, setEditingTableId] = useState(null);
  const [tableDetail, setTableDetail] = useState(null);
  const [loadingTable, setLoadingTable] = useState(false);
  const [newPlayerName, setNewPlayerName] = useState('');
  const [txPlayerId, setTxPlayerId] = useState('');
  const [txType, setTxType] = useState('buyin'); // 'buyin' | 'exit'
  const [txAmount, setTxAmount] = useState('');
  const [txNote, setTxNote] = useState('');
  const [editingTx, setEditingTx] = useState(null); // { type: 'buyin'|'exit', id, amount, note }

  const showMsg = (type, text) => {
    setBanner({ type, text });
    if (type === 'success') {
      setTimeout(() => setBanner((prev) => (prev.text === text ? { type: '', text: '' } : prev)), 4000);
    }
  };

  const loadOverview = async () => {
    setLoading(true);
    try {
      const res = await getGroupAdminOverview(groupId);
      setOverview(res.data);
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to load Group Admin overview');
    } finally {
      setLoading(false);
    }
  };

  const loadTableDetail = async (tId) => {
    setLoadingTable(true);
    try {
      const res = await getGroupAdminTable(groupId, tId);
      setTableDetail(res.data);
      if (!txPlayerId && res.data?.players?.length > 0) {
        setTxPlayerId(res.data.players[0].id);
      }
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to load table detail');
    } finally {
      setLoadingTable(false);
    }
  };

  useEffect(() => {
    loadOverview();
  }, [groupId]);

  const notifyChanged = () => {
    loadOverview();
    if (editingTableId) loadTableDetail(editingTableId);
    if (onDataChanged) onDataChanged();
  };

  const handleToggleRole = async (m) => {
    const nextRole = m.role === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    try {
      await updateGroupMemberRole(groupId, m.userId, nextRole);
      showMsg('success', `${m.displayName} is now ${nextRole}`);
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to update role');
    }
  };

  const handleRemoveMember = async (m) => {
    if (Math.abs(Number(m.balance) || 0) > 0.005) {
      showMsg(
        'error',
        `Cannot remove ${m.displayName}: balance is ${formatSigned(m.balance)}. Settle to $0 first.`
      );
      return;
    }
    if (!window.confirm(`Remove ${m.displayName} (@${m.username}) from this group? Historical table records will remain intact.`)) {
      return;
    }
    try {
      await removeGroupMember(groupId, m.userId);
      showMsg('success', `${m.displayName} removed from group`);
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.message || err.response?.data?.error || 'Failed to remove member');
    }
  };

  const handleResetPassword = async (mode) => {
    if (!resetTarget) return;
    try {
      const payload = mode === 'direct' ? { newPassword: tempPass } : { generateCode: true };
      const res = await resetGroupMemberPassword(groupId, resetTarget.userId, payload);
      setResetResult(res.data);
      setTempPass('');
      notifyChanged();
    } catch (err) {
      setResetResult({ error: err.response?.data?.error || 'Failed to reset password' });
    }
  };

  const handleTableStatus = async (tableId, nextStatus) => {
    try {
      await updateGroupAdminTableStatus(groupId, tableId, { status: nextStatus });
      showMsg('success', `Table marked ${nextStatus}`);
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to update table status');
    }
  };

  const handleDeleteTable = async (t) => {
    if (!window.confirm(`Soft-delete table "${t.name}"?`)) return;
    try {
      await updateGroupAdminTableStatus(groupId, t.id, { isDeleted: true });
      if (editingTableId === t.id) {
        setEditingTableId(null);
        setTableDetail(null);
      }
      showMsg('success', `Table "${t.name}" deleted`);
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to delete table');
    }
  };

  const handleAddTablePlayer = async (e) => {
    e.preventDefault();
    if (!newPlayerName.trim() || !editingTableId) return;
    try {
      await addGroupAdminTablePlayer(groupId, editingTableId, { name: newPlayerName.trim() });
      setNewPlayerName('');
      showMsg('success', 'Player added to table');
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to add player');
    }
  };

  const handleAddTx = async (e) => {
    e.preventDefault();
    if (!txPlayerId || txAmount === '' || !editingTableId) return;
    try {
      const payload = { playerId: txPlayerId, amount: Number(txAmount), note: txNote };
      if (txType === 'buyin') {
        await addGroupAdminTableBuyIn(groupId, editingTableId, payload);
      } else {
        await addGroupAdminTableExit(groupId, editingTableId, payload);
      }
      setTxAmount('');
      setTxNote('');
      showMsg('success', `${txType === 'buyin' ? 'Buy-in' : 'Exit'} added`);
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to add transaction');
    }
  };

  const handleSaveTxEdit = async () => {
    if (!editingTx || !editingTableId) return;
    try {
      const payload = { amount: Number(editingTx.amount), note: editingTx.note };
      if (editingTx.type === 'buyin') {
        await updateGroupAdminTableBuyIn(groupId, editingTableId, editingTx.id, payload);
      } else {
        await updateGroupAdminTableExit(groupId, editingTableId, editingTx.id, payload);
      }
      setEditingTx(null);
      showMsg('success', 'Transaction updated');
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to update transaction');
    }
  };

  const handleDeleteTx = async (type, id) => {
    if (!editingTableId || !window.confirm('Delete this transaction?')) return;
    try {
      if (type === 'buyin') {
        await deleteGroupAdminTableBuyIn(groupId, editingTableId, id);
      } else {
        await deleteGroupAdminTableExit(groupId, editingTableId, id);
      }
      showMsg('success', 'Transaction deleted');
      notifyChanged();
    } catch (err) {
      showMsg('error', err.response?.data?.error || 'Failed to delete transaction');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-3 sm:p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-3xl w-full max-w-3xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-4 sm:p-5 bg-felt-dark/90 border-b border-gold-accent/30 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <Shield className="w-5 h-5 text-gold-accent shrink-0" />
            <div className="min-w-0">
              <h2 className="text-sm sm:text-base font-black text-gold-accent uppercase tracking-wider truncate">
                Group Admin • {groupName || overview?.groupName || 'Club'}
              </h2>
              <p className="text-[11px] text-cream-text/60">
                Manage members, correct closed tables, and resolve resets
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={loadOverview}
              className="p-1.5 rounded-lg text-gold-accent hover:bg-felt-card cursor-pointer"
              title="Refresh"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg text-cream-text/70 hover:text-cream-text hover:bg-felt-card cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Sub-Tabs */}
        <div className="px-4 pt-3 pb-2 bg-felt-dark/50 border-b border-gold-accent/20 flex items-center gap-2 overflow-x-auto">
          {[
            { id: 'members', label: `Members (${overview?.members?.length || 0})`, icon: Users },
            { id: 'tables', label: `Tables & Corrections (${overview?.tables?.length || 0})`, icon: Layers },
            { id: 'payments', label: `Payments & Resets (${overview?.passwordResets?.length || 0})`, icon: CreditCard },
          ].map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setTab(item.id);
                  setEditingTableId(null);
                  setTableDetail(null);
                }}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 shrink-0 whitespace-nowrap cursor-pointer transition ${
                  tab === item.id
                    ? 'bg-gold-accent text-black shadow'
                    : 'bg-felt-card/80 text-cream-text/70 hover:text-cream-text'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

        {/* Banner */}
        {banner.text && (
          <div
            className={`mx-4 mt-3 p-3 rounded-xl text-xs font-semibold flex items-center justify-between gap-2 ${
              banner.type === 'error'
                ? 'bg-red-950/80 border border-red-500/60 text-red-200'
                : 'bg-emerald-950/80 border border-emerald-500/60 text-emerald-200'
            }`}
          >
            <span>{banner.text}</span>
            <button type="button" onClick={() => setBanner({ type: '', text: '' })} className="cursor-pointer">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Body */}
        <div className="p-4 sm:p-5 overflow-y-auto flex-1 space-y-3">
          {loading && !overview ? (
            <div className="py-10 text-center text-xs text-cream-text/60">Loading admin data...</div>
          ) : tab === 'members' ? (
            <div className="space-y-2.5">
              {(overview?.members || []).map((m) => {
                const unsettled = Math.abs(Number(m.balance) || 0) > 0.005;
                return (
                  <div
                    key={m.userId}
                    className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 flex flex-wrap items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <PokerAvatar avatarId={m.avatarId} name={m.displayName} size={38} />
                      <div className="min-w-0">
                        <div className="text-sm font-bold text-cream-text truncate flex items-center gap-1.5">
                          <span>{m.displayName}</span>
                          {m.isOwner && (
                            <span className="px-1.5 py-0.5 rounded bg-gold-accent/20 text-gold-accent text-[9px] font-black uppercase">
                              Owner
                            </span>
                          )}
                        </div>
                        <div className="text-[11px] text-cream-text/55 flex items-center gap-2">
                          <span>@{m.username}</span>
                          <span>•</span>
                          <span
                            className={`font-mono font-bold ${
                              m.balance > 0 ? 'text-emerald-400' : m.balance < 0 ? 'text-red-400' : 'text-cream-text/70'
                            }`}
                          >
                            {formatSigned(m.balance)}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        disabled={m.isOwner}
                        onClick={() => handleToggleRole(m)}
                        className={`px-2.5 py-1.5 rounded-lg text-[11px] font-bold border transition cursor-pointer disabled:opacity-40 ${
                          m.role === 'ADMIN'
                            ? 'bg-gold-accent/20 text-gold-accent border-gold-accent/60'
                            : 'bg-felt-card text-cream-text/80 border-gold-accent/25 hover:border-gold-accent'
                        }`}
                      >
                        {m.role === 'ADMIN' ? 'Admin' : 'Member'}
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setResetTarget(m);
                          setTempPass('');
                          setResetResult(null);
                          setCopiedCode(false);
                        }}
                        className="px-2.5 py-1.5 rounded-lg text-[11px] font-bold bg-felt-card text-gold-accent border border-gold-accent/40 hover:border-gold-accent flex items-center gap-1 cursor-pointer"
                      >
                        <KeyRound className="w-3.5 h-3.5" />
                        <span>Reset PW</span>
                      </button>

                      {!m.isOwner && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(m)}
                          title={unsettled ? `Settle balance (${formatSigned(m.balance)}) to $0 before removing` : 'Remove from group'}
                          className={`px-2.5 py-1.5 rounded-lg text-[11px] font-bold border flex items-center gap-1 cursor-pointer ${
                            unsettled
                              ? 'bg-amber-950/50 text-amber-300 border-amber-500/40'
                              : 'bg-red-950/60 text-red-300 border-red-500/50 hover:bg-red-900'
                          }`}
                        >
                          {unsettled ? <AlertTriangle className="w-3.5 h-3.5" /> : <Trash2 className="w-3.5 h-3.5" />}
                          <span>Remove</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}

              {/* Inline Reset Password Box */}
              {resetTarget && (
                <div className="p-4 rounded-2xl bg-felt-dark border-2 border-gold-accent/70 space-y-3 mt-3">
                  <div className="flex items-center justify-between">
                    <div className="text-xs font-bold text-gold-accent uppercase">
                      Reset Password: {resetTarget.displayName} (@{resetTarget.username})
                    </div>
                    <button type="button" onClick={() => setResetTarget(null)} className="text-cream-text/60 cursor-pointer">
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  {resetResult?.error && (
                    <div className="text-xs text-red-300 bg-red-950/60 p-2 rounded-lg">{resetResult.error}</div>
                  )}
                  {resetResult?.resetCode && (
                    <div className="p-3 bg-emerald-950/60 border border-emerald-500/50 rounded-xl flex items-center justify-between">
                      <div>
                        <div className="text-[10px] text-emerald-300 uppercase font-bold">6-Char Reset Code (24h)</div>
                        <div className="text-lg font-mono font-black tracking-[0.25em] text-gold-accent">
                          {resetResult.resetCode}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          navigator.clipboard?.writeText(resetResult.resetCode);
                          setCopiedCode(true);
                          setTimeout(() => setCopiedCode(false), 2000);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-felt-card border border-gold-accent/50 text-gold-accent text-xs font-bold flex items-center gap-1 cursor-pointer"
                      >
                        <Copy className="w-3.5 h-3.5" />
                        <span>{copiedCode ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  )}
                  {resetResult?.mode === 'direct' && (
                    <div className="text-xs text-emerald-300 bg-emerald-950/60 p-2 rounded-lg">{resetResult.message}</div>
                  )}
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => handleResetPassword('code')}
                      className="px-3.5 py-2 rounded-xl bg-gold-accent text-black text-xs font-extrabold cursor-pointer"
                    >
                      Generate 6-Char Code
                    </button>
                    <div className="flex flex-1 min-w-[200px] gap-1.5">
                      <input
                        type="text"
                        value={tempPass}
                        onChange={(e) => setTempPass(e.target.value)}
                        placeholder="Or set temp password (6+ chars)"
                        className="flex-1 min-w-0 px-3 py-1.5 bg-felt-card border border-gold-accent/40 rounded-xl text-xs text-cream-text"
                      />
                      <button
                        type="button"
                        disabled={tempPass.length < 6}
                        onClick={() => handleResetPassword('direct')}
                        className="px-3 py-1.5 rounded-xl border border-gold-accent text-gold-accent text-xs font-bold disabled:opacity-40 cursor-pointer"
                      >
                        Set
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : tab === 'tables' ? (
            editingTableId ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setEditingTableId(null);
                      setTableDetail(null);
                    }}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-gold-accent hover:underline cursor-pointer"
                  >
                    <ArrowLeft className="w-4 h-4" />
                    <span>Back to Tables</span>
                  </button>
                  {tableDetail?.table && (
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-cream-text">{tableDetail.table.name}</span>
                      <button
                        type="button"
                        onClick={() =>
                          handleTableStatus(
                            tableDetail.table.id,
                            tableDetail.table.status === 'CLOSED' ? 'ACTIVE' : 'CLOSED'
                          )
                        }
                        className="px-2.5 py-1 rounded-lg bg-felt-dark border border-gold-accent/50 text-gold-accent text-[11px] font-bold cursor-pointer"
                      >
                        {tableDetail.table.status === 'CLOSED' ? 'Reopen Table' : 'Close Table'}
                      </button>
                    </div>
                  )}
                </div>

                {loadingTable || !tableDetail ? (
                  <div className="py-8 text-center text-xs text-cream-text/60">Loading table records...</div>
                ) : (
                  <>
                    {/* Players */}
                    <div className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 space-y-2.5">
                      <div className="text-xs font-extrabold uppercase text-gold-accent">
                        Players ({tableDetail.players.length})
                      </div>
                      <form onSubmit={handleAddTablePlayer} className="flex gap-2">
                        <input
                          type="text"
                          value={newPlayerName}
                          onChange={(e) => setNewPlayerName(e.target.value)}
                          placeholder="Add player name..."
                          className="flex-1 min-w-0 px-3 py-1.5 rounded-xl bg-felt-card border border-gold-accent/30 text-xs text-cream-text"
                        />
                        <button
                          type="submit"
                          className="px-3 py-1.5 rounded-xl bg-gold-accent text-black text-xs font-bold flex items-center gap-1 cursor-pointer"
                        >
                          <Plus className="w-3.5 h-3.5" />
                          <span>Add</span>
                        </button>
                      </form>
                      <div className="flex flex-wrap gap-1.5">
                        {tableDetail.players.map((p) => (
                          <span
                            key={p.id}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-felt-card border border-gold-accent/30 text-xs text-cream-text"
                          >
                            <span>{p.name}</span>
                            <button
                              type="button"
                              onClick={async () => {
                                if (!window.confirm(`Remove ${p.name} and their transactions on this table?`)) return;
                                await deleteGroupAdminTablePlayer(groupId, editingTableId, p.id);
                                notifyChanged();
                              }}
                              className="text-red-400 hover:text-red-200 cursor-pointer"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Add Buy-In / Exit */}
                    <form onSubmit={handleAddTx} className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 space-y-2.5">
                      <div className="text-xs font-extrabold uppercase text-gold-accent">
                        Add Correction Transaction (Allowed on Closed Tables)
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-4 gap-2">
                        <select
                          value={txPlayerId}
                          onChange={(e) => setTxPlayerId(e.target.value)}
                          className="px-2.5 py-1.5 rounded-xl bg-felt-card border border-gold-accent/30 text-xs text-cream-text"
                        >
                          {tableDetail.players.map((p) => (
                            <option key={p.id} value={p.id}>{p.name}</option>
                          ))}
                        </select>
                        <select
                          value={txType}
                          onChange={(e) => setTxType(e.target.value)}
                          className="px-2.5 py-1.5 rounded-xl bg-felt-card border border-gold-accent/30 text-xs text-cream-text"
                        >
                          <option value="buyin">Buy-In</option>
                          <option value="exit">Exit</option>
                        </select>
                        <input
                          type="number"
                          min="0"
                          value={txAmount}
                          onChange={(e) => setTxAmount(e.target.value)}
                          placeholder="Amount"
                          className="px-2.5 py-1.5 rounded-xl bg-felt-card border border-gold-accent/30 text-xs text-cream-text"
                        />
                        <button
                          type="submit"
                          className="px-3 py-1.5 rounded-xl bg-gold-accent text-black text-xs font-extrabold cursor-pointer"
                        >
                          Add Record
                        </button>
                      </div>
                    </form>

                    {/* Buy-Ins & Exits Lists */}
                    {[
                      { title: 'Buy-Ins', type: 'buyin', list: tableDetail.buyIns, color: 'text-emerald-400' },
                      { title: 'Exits', type: 'exit', list: tableDetail.exits, color: 'text-amber-400' },
                    ].map((section) => (
                      <div key={section.type} className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 space-y-2">
                        <div className="text-xs font-extrabold uppercase text-gold-accent">
                          {section.title} ({section.list.length})
                        </div>
                        {section.list.length === 0 ? (
                          <div className="text-[11px] text-cream-text/50">None recorded.</div>
                        ) : (
                          section.list.map((tx) => {
                            const isEditing = editingTx?.type === section.type && editingTx?.id === tx.id;
                            return (
                              <div
                                key={tx.id}
                                className="p-2.5 rounded-xl bg-felt-card/70 border border-gold-accent/15 flex items-center justify-between gap-2 text-xs"
                              >
                                <div className="min-w-0 flex-1">
                                  <span className="font-bold text-cream-text">{tx.playerName}</span>
                                  {tx.note && <span className="text-cream-text/50 ml-1.5">({tx.note})</span>}
                                </div>
                                {isEditing ? (
                                  <div className="flex items-center gap-1.5">
                                    <input
                                      type="number"
                                      min="0"
                                      value={editingTx.amount}
                                      onChange={(e) => setEditingTx({ ...editingTx, amount: e.target.value })}
                                      className="w-24 px-2 py-1 rounded bg-felt-dark border border-gold-accent text-gold-accent font-mono text-xs"
                                    />
                                    <button
                                      type="button"
                                      onClick={handleSaveTxEdit}
                                      className="p-1.5 rounded bg-emerald-600 text-white cursor-pointer"
                                    >
                                      <Check className="w-3.5 h-3.5" />
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => setEditingTx(null)}
                                      className="p-1.5 rounded bg-zinc-700 text-white cursor-pointer"
                                    >
                                      <X className="w-3.5 h-3.5" />
                                    </button>
                                  </div>
                                ) : (
                                  <div className="flex items-center gap-2 shrink-0">
                                    <span className={`font-mono font-black ${section.color}`}>
                                      ${Number(tx.amount).toLocaleString('en-US')}
                                    </span>
                                    <button
                                      type="button"
                                      onClick={() =>
                                        setEditingTx({ type: section.type, id: tx.id, amount: tx.amount, note: tx.note || '' })
                                      }
                                      className="p-1 text-cream-text/70 hover:text-gold-accent cursor-pointer"
                                    >
                                      <Edit2 className="w-3.5 h-3.5" />
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => handleDeleteTx(section.type, tx.id)}
                                      className="p-1 text-red-400 hover:text-red-200 cursor-pointer"
                                    >
                                      <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                  </div>
                                )}
                              </div>
                            );
                          })
                        )}
                      </div>
                    ))}
                  </>
                )}
              </div>
            ) : (
              <div className="space-y-2.5">
                {(overview?.tables || []).map((t) => (
                  <div
                    key={t.id}
                    className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 flex flex-wrap items-center justify-between gap-3"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-cream-text truncate">{t.name}</span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase ${
                            t.status === 'CLOSED'
                              ? 'bg-zinc-800 text-zinc-300 border border-zinc-600'
                              : 'bg-emerald-950 text-emerald-300 border border-emerald-500/50'
                          }`}
                        >
                          {t.status}
                        </span>
                      </div>
                      <div className="text-[11px] text-cream-text/60 mt-0.5">
                        {t.playerCount} players • Buy-Ins ${t.totalBuyIns.toLocaleString('en-US')} • Exits ${t.totalExits.toLocaleString('en-US')}
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingTableId(t.id);
                          loadTableDetail(t.id);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-gold-accent text-black text-xs font-extrabold cursor-pointer"
                      >
                        Edit Table
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteTable(t)}
                        className="p-1.5 rounded-lg bg-red-950/60 text-red-300 border border-red-500/40 hover:bg-red-900 cursor-pointer"
                        title="Delete Table"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )
          ) : (
            <div className="space-y-4">
              {/* Pending Resets */}
              <div className="space-y-2">
                <div className="text-xs font-extrabold uppercase text-gold-accent">
                  Pending Password Resets ({overview?.passwordResets?.length || 0})
                </div>
                {(overview?.passwordResets || []).length === 0 ? (
                  <div className="text-xs text-cream-text/50">No active password reset requests.</div>
                ) : (
                  overview.passwordResets.map((r) => (
                    <div
                      key={r.id}
                      className="p-3 rounded-xl bg-felt-dark/70 border border-amber-500/35 flex items-center justify-between gap-2 text-xs"
                    >
                      <div>
                        <div className="font-bold text-cream-text">{r.displayName} (@{r.username})</div>
                        <div className="text-[11px] text-gold-accent font-mono">
                          {r.resetCode ? `Code: ${r.resetCode}` : 'Pending code'}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={async () => {
                          const res = await resetGroupMemberPassword(groupId, r.userId, { generateCode: true });
                          showMsg('success', `Reset code for ${r.username}: ${res.data.resetCode}`);
                          notifyChanged();
                        }}
                        className="px-3 py-1.5 rounded-lg bg-gold-accent text-black font-bold text-xs cursor-pointer"
                      >
                        Generate Code
                      </button>
                    </div>
                  ))
                )}
              </div>

              {/* Recent Payments */}
              <div className="space-y-2">
                <div className="text-xs font-extrabold uppercase text-gold-accent">
                  Recent Payments ({overview?.payments?.length || 0})
                </div>
                {(overview?.payments || []).length === 0 ? (
                  <div className="text-xs text-cream-text/50">No payments recorded.</div>
                ) : (
                  overview.payments.map((pm) => (
                    <div
                      key={pm.id}
                      className="p-3 rounded-xl bg-felt-dark/70 border border-gold-accent/20 flex items-center justify-between gap-2 text-xs"
                    >
                      <div>
                        <span className="font-bold text-cream-text">{pm.fromPlayer}</span>
                        <span className="text-gold-accent mx-1.5">→</span>
                        <span className="font-bold text-cream-text">{pm.toPlayer}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-black text-gold-accent">
                          ${Number(pm.amount).toLocaleString('en-US')}
                        </span>
                        <button
                          type="button"
                          onClick={async () => {
                            if (!window.confirm('Delete this payment?')) return;
                            await deleteGroupPayment(groupId, pm.id);
                            showMsg('success', 'Payment deleted');
                            notifyChanged();
                          }}
                          className="p-1 text-red-400 hover:text-red-200 cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default GroupAdminModal;
