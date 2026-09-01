import React from 'react';
import { Trophy, TrendingDown, Layers, Users, CheckCircle2, Clock } from 'lucide-react';

const StatsTab = ({
  stats = null,
  settlement = [],
  balances = [],
  loading = false,
}) => {
  if (loading && !stats) {
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
          <span className="text-[10px] text-cream-text/50 uppercase font-semibold bg-felt-dark px-2.5 py-1 rounded-lg border border-gold-accent/20">
            Read-Only
          </span>
        </div>

        {balances.length === 0 && settlement.length === 0 ? (
          <div className="py-6 text-center text-xs text-cream-text/60">
            No data yet. Close a table in this group first.
          </div>
        ) : settlement.length === 0 ? (
          <div className="py-6 text-center text-sm font-bold text-emerald-400">
            All settled! 🎉
          </div>
        ) : (
          <div className="space-y-2.5 pt-1">
            {settlement.map((item, idx) => {
              const payer = item.payerName || item.fromPlayer || 'Player';
              const receiver = item.receiverName || item.toPlayer || 'Player';
              const amount = item.amount || 0;
              const isPaid = Boolean(item.isPaid || item.paid);

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
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-emerald-950 text-emerald-400 border border-emerald-500/50 shadow-sm">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>PAID ✓</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-amber-950 text-amber-300 border border-amber-500/50 shadow-sm">
                        <Clock className="w-3.5 h-3.5 animate-pulse" />
                        <span>PENDING</span>
                      </span>
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
