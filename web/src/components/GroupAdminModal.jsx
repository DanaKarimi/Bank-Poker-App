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
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="bg-felt-card border-t-2 sm:border-2 border-gold-accent rounded-t-3xl sm:rounded-3xl w-full max-w-3xl max-h-[88dvh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-4 sm:p-5 bg-felt-dark/90 border-b border-gold-accent/30 flex items-center justify-between gap-2 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <Shield className="w-5 h-5 text-gold-accent shrink-0" />
            <div className="min-w-0">
              <h2 className="text-sm sm:text-base font-black text-gold-accent uppercase tracking-wider truncate">
                Group Admin • {groupName || overview?.groupName || 'Club'}
              </h2>
              <p className="text-[11px] text-cream-text/60 truncate">
                Manage members and correct closed tables
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

        {/* Pinned Full-Width 2-Column Segmented Tab Bar */}
        <div className="px-3 sm:px-4 py-2.5 bg-felt-dark/60 border-b border-gold-accent/20 shrink-0">
          <div className="grid grid-cols-2 gap-1.5 p-1 bg-felt-dark/90 rounded-2xl border border-gold-accent/25">
            {[
              { id: 'members', label: `Members (${overview?.members?.length || 0})`, icon: Users },
              { id: 'tables', label: `Tables (${overview?.tables?.length || 0})`, icon: Layers },
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
                  className={`py-2 px-1.5 rounded-xl text-[11px] sm:text-xs font-bold flex items-center justify-center gap-1 min-w-0 cursor-pointer transition ${
                    tab === item.id
                      ? 'bg-gold-accent text-black shadow'
                      : 'text-cream-text/70 hover:text-cream-text'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5 shrink-0" />
                  <span className="truncate">{item.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Banner */}
        {banner.text && (
          <div
            className={`mx-4 mt-3 p-3 rounded-xl text-xs font-semibold flex items-center justify-between gap-2 shrink-0 ${
              banner.type === 'error'
                ? 'bg-red-950/80 border border-red-500/60 text-red-200'
                : 'bg-emerald-950/80 border border-emerald-500/60 text-emerald-200'
            }`}
          >
            <span className="min-w-0 break-words">{banner.text}</span>
            <button type="button" onClick={() => setBanner({ type: '', text: '' })} className="cursor-pointer shrink-0">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Body */}
        <div className="p-3.5 sm:p-5 overflow-y-auto overscroll-contain flex-1 min-h-0 space-y-3">
          {loading && !overview ? (
            <div className="py-10 text-center text-xs text-cream-text/60">Loading admin data...</div>
          ) : tab === 'members' ? (
            <div className="space-y-2.5">
              {(overview?.members || []).map((m) => {
                const unsettled = Math.abs(Number(m.balance) || 0) > 0.005;
                return (
                  <div
                    key={m.userId}
                    className="p-3.5 rounded-2xl bg-felt-dark/70 border border-gold-accent/25 space-y-3"
                  >
                    {/* Row 1: Avatar + Name/Badges/@username + Balance */}
                    <div className="flex items-center justify-between gap-3 min-w-0">
                      <div className="flex items-center gap-3 min-w-0 flex-1">
                        <PokerAvatar avatarId={m.avatarId} name={m.displayName} size={40} />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <span className="text-sm font-bold text-cream-text truncate">{m.displayName}</span>
                            {m.isOwner ? (
                              <span className="px-1.5 py-0.5 rounded bg-gold-accent/20 text-gold-accent text-[9px] font-black uppercase shrink-0">
                                Owner
                              </span>
                            ) : m.role === 'ADMIN' ? (
                              <span className="px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-500/40 text-[9px] font-black uppercase shrink-0">
                                Admin
                              </span>
                            ) : null}
                          </div>
                          <div className="text-[11px] text-cream-text/55 truncate mt-0.5">
                            @{m.username}
                          </div>
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <div
                          className={`text-sm sm:text-base font-mono font-black ${
                            m.balance > 0 ? 'text-emerald-400' : m.balance < 0 ? 'text-rose-400' : 'text-cream-text/75'
                          }`}
                        >
                          {formatSigned(m.balance)}
                        </div>
                      </div>
                    </div>

                    {/* Row 2: Action Buttons */}
                    <div className="flex items-center gap-2 pt-2 border-t border-gold-accent/15">
                      <button
                        type="button"
                        disabled={m.isOwner}
                        onClick={() => handleToggleRole(m)}
                        className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11px] font-bold border transition cursor-pointer disabled:opacity-40 text-center ${
                          m.role === 'ADMIN'
                            ? 'bg-gold-accent/20 text-gold-accent border-gold-accent/60'
                            : 'bg-felt-card text-cream-text/80 border-gold-accent/25 hover:border-gold-accent'
                        }`}
                      >
                        {m.role === 'ADMIN' ? 'Admin' : 'Member'}
                      </button>

                      {!m.isOwner && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(m)}
                          title={unsettled ? `Settle balance (${formatSigned(m.balance)}) to $0 before removing` : 'Remove from group'}
                          className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11px] font-bold border flex items-center justify-center gap-1 cursor-pointer ${
                            unsettled
                              ? 'bg-amber-950/50 text-amber-300 border-amber-500/40'
                              : 'bg-red-950/60 text-red-300 border-red-500/50 hover:bg-red-900'
                          }`}
                        >
                          {unsettled ? <AlertTriangle className="w-3.5 h-3.5 shrink-0" /> : <Trash2 className="w-3.5 h-3.5 shrink-0" />}
                          <span>Remove</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
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
          )}
        </div>
      </div>
    </div>
  );
};

export default GroupAdminModal;
