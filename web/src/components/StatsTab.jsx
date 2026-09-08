import React, { useEffect, useState } from 'react';
import { Trophy, TrendingDown, Layers, Users, CheckCircle2, Clock, RefreshCw } from 'lucide-react';
import {
  getGroupSettlement,
  getGroupSettlementPlan,
  getGroupStats,
  recordGroupPayment,
  toggleSettlementPaid,
  regenerateSettlementPlan
} from '../api';
import { getSocket } from '../socket';

const StatsTab = ({
  groupId = null,
  stats = null,
  settlement = [],
  balances = [],
  loading = false,
  onRefresh = null,
}) => {
  const deduplicateSettlement = (list) => {
    if (!Array.isArray(list)) return [];
    const map = new Map();
    list.forEach((item) => {
      const payer = (item.debtorName || item.payerName || item.fromPlayer || '').trim().toLowerCase();
      const receiver = (item.creditorName || item.receiverName || item.toPlayer || '').trim().toLowerCase();
      const key = item.id || `${payer}->${receiver}->${item.amount}`;
      map.set(key, item);
    });
    return Array.from(map.values());
  };

  const [rows, setRows] = useState(() => deduplicateSettlement(settlement || []));
  const [fetchLoading, setFetchLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [submittingKeys, setSubmittingKeys] = useState(new Set());

  const handleTogglePaid = async (item) => {
    const payer = item.debtorName || item.payerName || item.fromPlayer;
    const receiver = item.creditorName || item.receiverName || item.toPlayer;
    const key = item.id || `${payer}->${receiver}->${item.amount}`;
    if (submittingKeys.has(key)) return; // Debounce rapid double-click

    const nextPaid = !Boolean(item.isPaid || item.paid);
    setSubmittingKeys((prev) => new Set(prev).add(key));
    try {
      if (groupId) {
        if (item.id) {
          await toggleSettlementPaid(groupId, item.id, nextPaid);
        } else if (nextPaid) {
          await recordGroupPayment(groupId, {
            fromPlayer: payer,
            toPlayer: receiver,
            amount: Number(item.amount) || 0,
          });
        }
      }
      setRows((prev) =>
        prev.map((r) => {
          const rPayer = r.debtorName || r.payerName || r.fromPlayer;
          const rReceiver = r.creditorName || r.receiverName || r.toPlayer;
          const rKey = r.id || `${rPayer}->${rReceiver}->${r.amount}`;
          return rKey === key ? { ...r, paid: nextPaid ? 1 : 0, isPaid: nextPaid } : r;
        })
      );
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to toggle settlement paid status:', err);
    } finally {
      setSubmittingKeys((prev) => {
        const next = new Set(prev);
        next.delete(key);
        return next;
      });
    }
  };

  const handleRegenerate = async () => {
    if (!groupId || regenerating) return;
    setRegenerating(true);
    try {
      const res = await regenerateSettlementPlan(groupId);
      const list = res.data?.settlement || [];
      setRows(deduplicateSettlement(list));
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to regenerate settlement plan:', err);
    } finally {
      setRegenerating(false);
    }
  };

  useEffect(() => {
    setRows(deduplicateSettlement(settlement || []));
  }, [settlement]);

  useEffect(() => {
    if (groupId) {
      setFetchLoading(true);
      Promise.allSettled([
        getGroupSettlementPlan(groupId),
        getGroupStats(groupId),
      ]).then(([settleRes]) => {
        if (settleRes.status === 'fulfilled') {
          const list = settleRes.value.data?.settlement || [];
          setRows(deduplicateSettlement(list));
        }
      }).catch((err) => {
        console.error("Failed to fetch settlement in StatsTab:", err);
      }).finally(() => {
        setFetchLoading(false);
      });
    }

    const socket = getSocket();
    const handleSettlementDone = (payload) => {
      if (groupId && (!payload?.groupId || payload.groupId === groupId)) {
        getGroupSettlementPlan(groupId).then((res) => {
          const list = res.data?.settlement || [];
          setRows(deduplicateSettlement(list));
        }).catch((err) => {
          console.error("Failed to refresh settlement on settlement_done:", err);
        });
      }
      if (onRefresh) onRefresh();
    };

    socket.on('settlement_done', handleSettlementDone);
    return () => {
      socket.off('settlement_done', handleSettlementDone);
    };
  }, [groupId, onRefresh]);

  const activeSettlement = React.useMemo(() => {
    const source = rows.length > 0 ? rows : settlement;
    return deduplicateSettlement(source);
  }, [rows, settlement]);

  if ((loading || fetchLoading) && !stats && activeSettlement.length === 0) {
    return (
      <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
        Loading group statistics...
      </div>
    );
  }

  const totalTables = stats?.totalTables ?? 0;
  const closedTables = stats?.closedTables ?? 0;
  const totalPlayers = stats?.totalPlayers ?? balances.length;
  const biggestWinner = stats?.biggestWinner;
  const biggestDebtor = stats?.biggestDebtor;

  return (
    <div className="space-y-4 animate-in fade-in duration-200">
      {/* Section 1: Overview Group Stats Card */}
      <div className="bg-felt-card border-2 border-gold-accent/70 rounded-2xl p-6 shadow-xl relative overflow-hidden">
        <div className="flex items-center justify-center gap-2 mb-4">
          <span className="text-gold-accent text-lg font-bold">♠</span>
          <h2 className="text-sm font-black uppercase tracking-[3px] text-cream-text">
            GROUP STATS
          </h2>
        </div>

        <div className="grid grid-cols-3 gap-3 text-center">
          <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/30">
            <div className="text-[10px] text-cream-text/60 uppercase font-bold tracking-wider">
              TABLES
            </div>
            <div className="text-xl sm:text-2xl font-black font-mono text-cream-text mt-0.5">
              {totalTables}
            </div>
          </div>

          <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/30">
            <div className="text-[10px] text-amber-400 uppercase font-bold tracking-wider">
              CLOSED
            </div>
            <div className="text-xl sm:text-2xl font-black font-mono text-amber-400 mt-0.5">
              {closedTables}
            </div>
          </div>

          <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/30">
            <div className="text-[10px] text-emerald-400 uppercase font-bold tracking-wider">
              PLAYERS
            </div>
            <div className="text-xl sm:text-2xl font-black font-mono text-emerald-400 mt-0.5">
              {totalPlayers}
            </div>
          </div>
        </div>
      </div>

      {/* Section 2: Biggest Winner Card */}
      {biggestWinner && biggestWinner.balance > 0 && (
        <div className="bg-felt-card border-2 border-[#10b981]/70 rounded-2xl p-4 shadow-lg flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-lg">🏆</span>
            <span className="text-xs font-black uppercase tracking-wider text-gold-accent">
              BIGGEST WINNER
            </span>
          </div>
          <div className="text-right">
            <span className="font-bold text-sm text-[#f5f5dc] mr-2">
              {biggestWinner.name}
            </span>
            <span className="font-mono font-black text-base text-[#10b981]">
              +${biggestWinner.balance.toLocaleString()}
            </span>
          </div>
        </div>
      )}

      {/* Section 3: Biggest Debtor Card */}
      {biggestDebtor && biggestDebtor.balance < 0 && (
        <div className="bg-felt-card border-2 border-[#ef4444]/70 rounded-2xl p-4 shadow-lg flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-lg">💸</span>
            <span className="text-xs font-black uppercase tracking-wider text-gold-accent">
              BIGGEST DEBTOR
            </span>
          </div>
          <div className="text-right">
            <span className="font-bold text-sm text-[#f5f5dc] mr-2">
              {biggestDebtor.name}
            </span>
            <span className="font-mono font-black text-base text-[#ef4444]">
              -${Math.abs(biggestDebtor.balance).toLocaleString()}
            </span>
          </div>
        </div>
      )}

      {/* Section 4: Settlement Plan */}
      <div className="bg-felt-card border-2 border-gold-accent/70 rounded-2xl p-5 shadow-xl space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-gold-accent/20">
          <div className="flex items-center gap-2">
            <span className="text-gold-accent font-bold">♠</span>
            <h3 className="text-sm font-black uppercase tracking-[2px] text-gold-accent">
              SETTLEMENT PLAN
            </h3>
          </div>
          {groupId && (
            <button
              type="button"
              disabled={regenerating}
              onClick={handleRegenerate}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-bold rounded-lg border border-gold-accent/30 bg-felt-dark hover:bg-gold-accent/10 text-gold-accent transition cursor-pointer disabled:opacity-50"
              title="Regenerate minimal settlement plan"
            >
              <RefreshCw className={`w-3 h-3 ${regenerating ? 'animate-spin' : ''}`} />
              <span>{regenerating ? 'REGENERATING...' : 'REGENERATE'}</span>
            </button>
          )}
        </div>

        {balances.length === 0 && activeSettlement.length === 0 ? (
          <div className="py-6 text-center text-xs text-cream-text/60">
            No data yet. Close a table in this group first.
          </div>
        ) : activeSettlement.length === 0 ? (
          <div className="py-6 text-center text-sm font-bold text-emerald-400">
            All settled! 🎉
          </div>
        ) : (
          <div className="space-y-2.5 pt-1">
            {activeSettlement.map((item, idx) => {
              const payer = item.debtorName || item.payerName || item.fromPlayer || 'Player';
              const receiver = item.creditorName || item.receiverName || item.toPlayer || 'Player';
              const amount = item.amount || 0;
              const isPaid = Boolean(item.isPaid || item.paid);
              const key = item.id || `${payer}->${receiver}->${amount}`;
              const isSubmitting = submittingKeys.has(key);

              return (
                <div
                  key={item.id || idx}
                  className="p-3.5 bg-felt-dark rounded-xl border border-gold-accent/30 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 shadow-sm"
                >
                  <div className="flex items-center flex-wrap gap-1.5 text-sm font-bold">
                    <span className="text-[#ef4444] font-black">{payer}</span>
                    <span className="text-cream-text/70 font-normal text-xs">pays</span>
                    <span className="text-[#10b981] font-black">{receiver}</span>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-3">
                    <span className="text-base font-black font-mono text-gold-accent">
                      ${amount.toLocaleString()}
                    </span>

                    {isPaid ? (
                      <button
                        type="button"
                        disabled={isSubmitting}
                        onClick={() => handleTogglePaid(item)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-emerald-950 hover:bg-emerald-900 active:scale-95 text-emerald-400 border border-emerald-500/50 shadow-sm transition cursor-pointer disabled:opacity-50"
                        title="Click to mark unpaid"
                      >
                        {isSubmitting ? (
                          <div className="w-3.5 h-3.5 border-2 border-emerald-400 border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <CheckCircle2 className="w-3.5 h-3.5" />
                        )}
                        <span>PAID ✓</span>
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={isSubmitting}
                        onClick={() => handleTogglePaid(item)}
                        className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-amber-950 hover:bg-amber-900 active:scale-95 text-amber-300 border border-amber-500/50 shadow-sm transition cursor-pointer disabled:opacity-50"
                        title="Mark this payment as paid"
                      >
                        {isSubmitting ? (
                          <>
                            <div className="w-3.5 h-3.5 border-2 border-amber-300 border-t-transparent rounded-full animate-spin" />
                            <span>SAVING...</span>
                          </>
                        ) : (
                          <>
                            <Clock className="w-3.5 h-3.5" />
                            <span>MARK PAID</span>
                          </>
                        )}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default StatsTab;
