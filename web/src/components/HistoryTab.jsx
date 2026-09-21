import React, { useState, useEffect } from 'react';
import {
  getGroupPayments,
  updateGroupPayment,
  deleteGroupPayment,
  getGroupEntryFees,
  updateGroupEntryFee,
  deleteGroupEntryFee
} from '../api';
import { getSocket } from '../socket';
import {
  Receipt,
  DollarSign,
  Check,
  X,
  Edit2,
  Trash2,
  Clock,
  AlertCircle,
  ShieldAlert
} from 'lucide-react';
import { formatTimestamp } from '../utils/formatters';

const HistoryTab = ({ groupId, isAdmin = false, onRefreshBalances }) => {
  const [subTab, setSubTab] = useState('payments'); // 'payments' | 'entry_fees'
  const [payments, setPayments] = useState([]);
  const [entryFees, setEntryFees] = useState([]);
  const [canManageEntryFees, setCanManageEntryFees] = useState(isAdmin);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals state
  const [editingPayment, setEditingPayment] = useState(null);
  const [editPaymentAmount, setEditPaymentAmount] = useState('');
  const [editPaymentFrom, setEditPaymentFrom] = useState('');
  const [editPaymentTo, setEditPaymentTo] = useState('');
  const [deletingPayment, setDeletingPayment] = useState(null);

  const [editingEntryFee, setEditingEntryFee] = useState(null);
  const [editEntryFeeAmount, setEditEntryFeeAmount] = useState('');
  const [editEntryFeePaid, setEditEntryFeePaid] = useState(false);
  const [deletingEntryFee, setDeletingEntryFee] = useState(null);

  const [actionLoading, setActionLoading] = useState(false);

  // Fetch Payments
  const fetchPayments = async () => {
    try {
      const res = await getGroupPayments(groupId);
      const list = res.data?.payments || [];
      setPayments(list);
    } catch (err) {
      console.error('Failed to fetch payments:', err);
    }
  };

  // Fetch Entry Fees
  const fetchEntryFees = async () => {
    try {
      const res = await getGroupEntryFees(groupId);
      const list = res.data?.entryFees || [];
      setEntryFees(list);
      if (res.data?.canManage !== undefined || res.data?.isAdmin !== undefined) {
        setCanManageEntryFees(Boolean(res.data.canManage || res.data.isAdmin || isAdmin));
      }
    } catch (err) {
      console.error('Failed to fetch entry fees:', err);
    }
  };

  const loadAll = async () => {
    setLoading(true);
    setError('');
    try {
      await Promise.all([fetchPayments(), fetchEntryFees()]);
    } catch (err) {
      setError('Failed to load history data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();

    const socket = getSocket();
    const handlePaymentsUpdate = () => {
      fetchPayments();
      if (onRefreshBalances) onRefreshBalances();
    };
    const handleEntryFeeUpdate = () => {
      fetchEntryFees();
      if (onRefreshBalances) onRefreshBalances();
    };

    socket.on('payment_created', handlePaymentsUpdate);
    socket.on('payment_updated', handlePaymentsUpdate);
    socket.on('payment_deleted', handlePaymentsUpdate);
    socket.on('entry_fee_updated', handleEntryFeeUpdate);

    return () => {
      socket.off('payment_created', handlePaymentsUpdate);
      socket.off('payment_updated', handlePaymentsUpdate);
      socket.off('payment_deleted', handlePaymentsUpdate);
      socket.off('entry_fee_updated', handleEntryFeeUpdate);
    };
  }, [groupId]);

  // Payment Actions
  const handleOpenEditPayment = (p) => {
    setEditingPayment(p);
    setEditPaymentAmount(p.amount.toString());
    setEditPaymentFrom(p.fromPlayer || p.from_player || '');
    setEditPaymentTo(p.toPlayer || p.to_player || '');
  };

  const handleSaveEditPayment = async (e) => {
    e.preventDefault();
    if (!editingPayment) return;
    const numAmount = Number(editPaymentAmount);
    if (!numAmount || numAmount <= 0) {
      alert('Payment amount must be a positive number');
      return;
    }
    if (!editPaymentFrom.trim() || !editPaymentTo.trim()) {
      alert('From and To player names are required');
      return;
    }
    setActionLoading(true);
    try {
      await updateGroupPayment(groupId, editingPayment.id, {
        amount: numAmount,
        fromPlayer: editPaymentFrom.trim(),
        toPlayer: editPaymentTo.trim()
      });
      setEditingPayment(null);
      await fetchPayments();
      if (onRefreshBalances) onRefreshBalances();
    } catch (err) {
      console.error('Failed to update payment:', err);
      alert(err.response?.data?.error || 'Failed to update payment');
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmDeletePayment = async () => {
    if (!deletingPayment) return;
    setActionLoading(true);
    try {
      await deleteGroupPayment(groupId, deletingPayment.id);
      setDeletingPayment(null);
      await fetchPayments();
      if (onRefreshBalances) onRefreshBalances();
    } catch (err) {
      console.error('Failed to delete payment:', err);
      alert(err.response?.data?.error || 'Failed to delete payment');
    } finally {
      setActionLoading(false);
    }
  };

  // Entry Fee Actions
  const handleToggleEntryFeePaid = async (ef) => {
    if (!canManageEntryFees) {
      alert('Admin privileges required to modify entry fees');
      return;
    }
    try {
      await updateGroupEntryFee(groupId, ef.id, {
        paid: !ef.paid,
        amount: ef.amount
      });
      // Optimistic update
      setEntryFees((prev) =>
        prev.map((item) => (item.id === ef.id ? { ...item, paid: !ef.paid } : item))
      );
      if (onRefreshBalances) onRefreshBalances();
    } catch (err) {
      console.error('Failed to toggle entry fee:', err);
      alert(err.response?.data?.error || 'Failed to update entry fee');
      fetchEntryFees();
    }
  };

  const handleOpenEditEntryFee = (ef) => {
    setEditingEntryFee(ef);
    setEditEntryFeeAmount(ef.amount.toString());
    setEditEntryFeePaid(Boolean(ef.paid));
  };

  const handleSaveEditEntryFee = async (e) => {
    e.preventDefault();
    if (!editingEntryFee) return;
    const numAmount = Number(editEntryFeeAmount);
    if (isNaN(numAmount) || numAmount < 0) {
      alert('Amount must be a valid non-negative number');
      return;
    }
    setActionLoading(true);
    try {
      await updateGroupEntryFee(groupId, editingEntryFee.id, {
        amount: numAmount,
        paid: editEntryFeePaid
      });
      setEditingEntryFee(null);
      await fetchEntryFees();
      if (onRefreshBalances) onRefreshBalances();
    } catch (err) {
      console.error('Failed to update entry fee:', err);
      alert(err.response?.data?.error || 'Failed to update entry fee');
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmDeleteEntryFee = async () => {
    if (!deletingEntryFee) return;
    setActionLoading(true);
    try {
      await deleteGroupEntryFee(groupId, deletingEntryFee.id);
      setDeletingEntryFee(null);
      await fetchEntryFees();
      if (onRefreshBalances) onRefreshBalances();
    } catch (err) {
      console.error('Failed to delete entry fee:', err);
      alert(err.response?.data?.error || 'Failed to delete entry fee');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Sub-Tabs Header (PAYMENTS | ENTRY FEES) */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-2 p-1 bg-zinc-900/80 border border-gold-accent/30 rounded-2xl">
          <button
            type="button"
            onClick={() => setSubTab('payments')}
            className={`px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition cursor-pointer ${
              subTab === 'payments'
                ? 'bg-gold-accent text-black shadow-md'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            Payments ({payments.length})
          </button>
          <button
            type="button"
            onClick={() => setSubTab('entry_fees')}
            className={`px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition cursor-pointer ${
              subTab === 'entry_fees'
                ? 'bg-gold-accent text-black shadow-md'
                : 'text-cream-text/70 hover:text-cream-text'
            }`}
          >
            Entry Fees ({entryFees.length})
          </button>
        </div>

        {canManageEntryFees && (
          <span className="text-[11px] text-cream-text/50 italic">
            Admin controls active: edit or delete records below
          </span>
        )}
      </div>

      {error && (
        <div className="p-3 bg-red-950/50 border border-red-500/40 rounded-xl text-red-300 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          <span>{error}</span>
        </div>
      )}

      {/* SUB-TAB 1: PAYMENTS */}
      {subTab === 'payments' && (
        <div className="space-y-3">
          {payments.length === 0 ? (
            <div className="p-8 text-center bg-felt-card/50 rounded-2xl border border-gold-accent/20 text-cream-text/60">
              <Receipt className="w-8 h-8 mx-auto mb-2 text-gold-accent/40" />
              <p className="font-semibold text-sm">No payment history yet</p>
              <p className="text-xs text-cream-text/40 mt-1">
                Transfers recorded or settled will appear here.
              </p>
            </div>
          ) : (
            payments.map((pm) => {
              const fromName = pm.fromPlayer || pm.from_player || 'Unknown';
              const toName = pm.toPlayer || pm.to_player || 'Unknown';
              const createdTs = pm.createdAt || pm.created_at;

              return (
                <div
                  key={pm.id}
                  className="p-4 bg-felt-card/90 rounded-2xl border border-gold-accent/40 hover:border-gold-accent transition shadow-md flex items-center justify-between gap-4"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 text-sm font-bold text-cream-text">
                      <span className="text-cream-text">{fromName}</span>
                      <span className="text-gold-accent font-extrabold">→</span>
                      <span className="text-cream-text">{toName}</span>
                    </div>

                    <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                      <span className="text-[11px] text-cream-text/50 flex items-center gap-1">
                        <Clock className="w-3 h-3 text-gold-accent/60" />
                        {formatTimestamp(createdTs)}
                      </span>
                      <span className="text-[10px] text-amber-300/80 bg-amber-950/40 px-2 py-0.5 rounded border border-amber-500/20">
                        {fromName} paid cash to {toName}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <span className="text-gold-accent font-black text-lg tracking-wide">
                        ${pm.amount}
                      </span>
                    </div>

                    {canManageEntryFees && (
                      <div className="flex items-center gap-1 pl-2 border-l border-gold-accent/20">
                        <button
                          type="button"
                          onClick={() => handleOpenEditPayment(pm)}
                          title="Edit Payment"
                          className="p-1.5 rounded-lg text-cream-text/70 hover:text-gold-accent hover:bg-zinc-800 transition cursor-pointer"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeletingPayment(pm)}
                          title="Delete Payment"
                          className="p-1.5 rounded-lg text-cream-text/70 hover:text-red-400 hover:bg-zinc-800 transition cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* SUB-TAB 2: ENTRY FEES */}
      {subTab === 'entry_fees' && (
        <div className="space-y-3">
          {entryFees.length === 0 ? (
            <div className="p-8 text-center bg-felt-card/50 rounded-2xl border border-gold-accent/20 text-cream-text/60">
              <DollarSign className="w-8 h-8 mx-auto mb-2 text-gold-accent/40" />
              <p className="font-semibold text-sm">No entry fee history yet</p>
              <p className="text-xs text-cream-text/40 mt-1">
                Entry fees generated from tables will be tracked here.
              </p>
            </div>
          ) : (
            entryFees.map((ef) => {
              const ts = ef.timestamp || ef.created_at;

              return (
                <div
                  key={ef.id}
                  className="p-4 bg-felt-card/90 rounded-2xl border border-gold-accent/40 hover:border-gold-accent transition shadow-md flex items-center justify-between gap-4"
                >
                  <div className="flex-1 min-w-0">
                    <h4 className="font-bold text-cream-text text-sm">{ef.playerName}</h4>
                    <div className="text-xs text-cream-text/70 mt-0.5">
                      Table: <span className="text-gold-light font-medium">{ef.tableName || 'Table'}</span>
                    </div>
                    {ts && (
                      <div className="text-[11px] text-cream-text/50 mt-1 flex items-center gap-1">
                        <Clock className="w-3 h-3 text-gold-accent/60" />
                        {formatTimestamp(ts)}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <div className="text-gold-accent font-black text-base">${ef.amount}</div>
                    </div>

                    {/* Paid/Unpaid Badge or Toggle */}
                    <div>
                      {canManageEntryFees ? (
                        <button
                          type="button"
                          onClick={() => handleToggleEntryFeePaid(ef)}
                          title="Click to toggle paid status"
                          className={`px-3 py-1 rounded-lg text-xs font-bold border transition cursor-pointer flex items-center gap-1 shadow-sm ${
                            ef.paid
                              ? 'bg-emerald-950 text-emerald-300 border-emerald-500/60 hover:bg-emerald-900'
                              : 'bg-red-950 text-red-300 border-red-500/60 hover:bg-red-900'
                          }`}
                        >
                          {ef.paid ? (
                            <>
                              <Check className="w-3.5 h-3.5" />
                              <span>Paid ✓</span>
                            </>
                          ) : (
                            <>
                              <X className="w-3.5 h-3.5" />
                              <span>Unpaid ✗</span>
                            </>
                          )}
                        </button>
                      ) : (
                        <span
                          className={`px-3 py-1 rounded-lg text-xs font-bold border inline-flex items-center gap-1 shadow-sm ${
                            ef.paid
                              ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/40'
                              : 'bg-red-950/80 text-red-300 border-red-500/40'
                          }`}
                        >
                          {ef.paid ? 'Paid ✓' : 'Unpaid ✗'}
                        </span>
                      )}
                    </div>

                    {canManageEntryFees && (
                      <div className="flex items-center gap-1 pl-2 border-l border-gold-accent/20">
                        <button
                          type="button"
                          onClick={() => handleOpenEditEntryFee(ef)}
                          title="Edit Entry Fee"
                          className="p-1.5 rounded-lg text-cream-text/70 hover:text-gold-accent hover:bg-zinc-800 transition cursor-pointer"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeletingEntryFee(ef)}
                          title="Delete Entry Fee"
                          className="p-1.5 rounded-lg text-cream-text/70 hover:text-red-400 hover:bg-zinc-800 transition cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* EDIT PAYMENT MODAL */}
      {editingPayment && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-3xl w-full max-w-md p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-gold-accent/30">
              <div className="flex items-center gap-2">
                <span className="text-gold-accent text-sm font-bold">♠</span>
                <h3 className="text-sm font-bold text-cream-text uppercase tracking-wider">
                  EDIT PAYMENT
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setEditingPayment(null)}
                className="text-cream-text/60 hover:text-cream-text p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEditPayment} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-cream-text/80 mb-1">
                  Amount ($)
                </label>
                <input
                  type="number"
                  min="1"
                  step="1"
                  required
                  value={editPaymentAmount}
                  onChange={(e) => setEditPaymentAmount(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-900/90 border border-gold-accent/40 rounded-xl text-gold-accent font-bold text-base focus:border-gold-accent outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-cream-text/80 mb-1">
                  From (Payer)
                </label>
                <input
                  type="text"
                  required
                  value={editPaymentFrom}
                  onChange={(e) => setEditPaymentFrom(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-900/90 border border-gold-accent/40 rounded-xl text-cream-text text-sm focus:border-gold-accent outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-cream-text/80 mb-1">
                  To (Receiver)
                </label>
                <input
                  type="text"
                  required
                  value={editPaymentTo}
                  onChange={(e) => setEditPaymentTo(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-900/90 border border-gold-accent/40 rounded-xl text-cream-text text-sm focus:border-gold-accent outline-none"
                />
              </div>

              <div className="text-[11px] text-cream-text/50 italic bg-black/30 p-2.5 rounded-lg border border-gold-accent/20">
                Notice: Editing payment details immediately recomputes balances for all players in this group.
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingPayment(null)}
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl border border-zinc-700 text-cream-text text-xs font-bold hover:bg-zinc-800 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl bg-gold-accent text-black text-xs font-bold hover:bg-gold-light transition shadow disabled:opacity-50 cursor-pointer"
                >
                  {actionLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE PAYMENT CONFIRMATION MODAL */}
      {deletingPayment && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-red-500/60 rounded-3xl w-full max-w-md p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400 mb-3">
              <ShieldAlert className="w-6 h-6" />
              <h3 className="text-base font-bold text-cream-text">Delete Payment?</h3>
            </div>

            <p className="text-xs text-cream-text/80 mb-4 leading-relaxed">
              Are you sure you want to delete the payment of{' '}
              <strong className="text-gold-accent">${deletingPayment.amount}</strong> from{' '}
              <strong className="text-cream-text">{deletingPayment.fromPlayer || deletingPayment.from_player}</strong> to{' '}
              <strong className="text-cream-text">{deletingPayment.toPlayer || deletingPayment.to_player}</strong>?
              This will soft-delete the record and recompute all group balances.
            </p>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setDeletingPayment(null)}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-xl border border-zinc-700 text-cream-text text-xs font-bold hover:bg-zinc-800 transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDeletePayment}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold transition shadow disabled:opacity-50 cursor-pointer"
              >
                {actionLoading ? 'Deleting...' : 'Delete Payment'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* EDIT ENTRY FEE MODAL */}
      {editingEntryFee && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-gold-accent rounded-3xl w-full max-w-md p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-gold-accent/30">
              <div className="flex items-center gap-2">
                <span className="text-gold-accent text-sm font-bold">♠</span>
                <h3 className="text-sm font-bold text-cream-text uppercase tracking-wider">
                  EDIT ENTRY FEE
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setEditingEntryFee(null)}
                className="text-cream-text/60 hover:text-cream-text p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEditEntryFee} className="space-y-4">
              <div className="bg-black/30 p-3 rounded-xl border border-gold-accent/20">
                <div className="text-xs text-cream-text/60">Player</div>
                <div className="text-sm font-bold text-cream-text">{editingEntryFee.playerName}</div>
                <div className="text-xs text-cream-text/60 mt-1">Table</div>
                <div className="text-xs font-semibold text-gold-light">{editingEntryFee.tableName || 'Table'}</div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-cream-text/80 mb-1">
                  Amount ($)
                </label>
                <input
                  type="number"
                  min="0"
                  step="1"
                  required
                  value={editEntryFeeAmount}
                  onChange={(e) => setEditEntryFeeAmount(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-900/90 border border-gold-accent/40 rounded-xl text-gold-accent font-bold text-base focus:border-gold-accent outline-none"
                />
              </div>

              <label className="flex items-center gap-2.5 p-3 rounded-xl bg-zinc-900/80 border border-gold-accent/30 cursor-pointer">
                <input
                  type="checkbox"
                  checked={editEntryFeePaid}
                  onChange={(e) => setEditEntryFeePaid(e.target.checked)}
                  className="w-4 h-4 rounded text-gold-accent focus:ring-0 cursor-pointer"
                />
                <span className="text-xs font-bold text-cream-text">Mark as Entry Fee Paid ✓</span>
              </label>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingEntryFee(null)}
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl border border-zinc-700 text-cream-text text-xs font-bold hover:bg-zinc-800 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl bg-gold-accent text-black text-xs font-bold hover:bg-gold-light transition shadow disabled:opacity-50 cursor-pointer"
                >
                  {actionLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE ENTRY FEE CONFIRMATION MODAL */}
      {deletingEntryFee && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-felt-card border-2 border-red-500/60 rounded-3xl w-full max-w-md p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400 mb-3">
              <ShieldAlert className="w-6 h-6" />
              <h3 className="text-base font-bold text-cream-text">Delete Entry Fee Record?</h3>
            </div>

            <p className="text-xs text-cream-text/80 mb-4 leading-relaxed">
              Are you sure you want to delete the entry fee record of{' '}
              <strong className="text-gold-accent">${deletingEntryFee.amount}</strong> for player{' '}
              <strong className="text-cream-text">{deletingEntryFee.playerName}</strong> on{' '}
              <strong className="text-cream-text">{deletingEntryFee.tableName || 'Table'}</strong>?
            </p>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setDeletingEntryFee(null)}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-xl border border-zinc-700 text-cream-text text-xs font-bold hover:bg-zinc-800 transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDeleteEntryFee}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold transition shadow disabled:opacity-50 cursor-pointer"
              >
                {actionLoading ? 'Deleting...' : 'Delete Record'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HistoryTab;
