import React from 'react';
import { Users, DollarSign } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const BalancesTab = ({ balances = [], loading = false }) => {
  const { user } = useAuth();

  if (loading && balances.length === 0) {
    return (
      <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
        Loading player balances...
      </div>
    );
  }

  if (!balances || balances.length === 0) {
    return (
      <div className="p-8 bg-felt-card rounded-2xl text-center text-xs text-cream-text/50 border border-gold-accent/20">
        No player balances recorded in this group yet.
      </div>
    );
  }

  // Sort by balance (highest first)
  const sortedBalances = [...balances].sort((a, b) => (b.balance ?? 0) - (a.balance ?? 0));

  return (
    <div className="space-y-3 animate-in fade-in duration-200">
      <div className="flex items-center justify-between px-1">
        <h2 className="text-sm font-bold uppercase tracking-wider text-gold-accent flex items-center gap-2">
          <Users className="w-4 h-4" />
          <span>Player Balances ({sortedBalances.length})</span>
        </h2>
        <span className="text-xs text-cream-text/50">
          Ranked by net session results
        </span>
      </div>

      <div className="space-y-2.5">
        {sortedBalances.map((item, idx) => {
          const balance = item.balance ?? 0;
          const isPositive = balance > 0;
          const isNegative = balance < 0;
          const isZero = balance === 0;

          const isMe = (user && item.userId === user.id) || item.isMe || (user && item.username?.toLowerCase() === user.username?.toLowerCase());

          const balanceColor = isPositive
            ? 'text-[#10b981]'
            : isNegative
            ? 'text-[#ef4444]'
            : 'text-[#d4af37]';

          const formattedBalance = isPositive
            ? `+$${balance.toLocaleString()}`
            : isNegative
            ? `-$${Math.abs(balance).toLocaleString()}`
            : `$0`;

          return (
            <div
              key={item.userId || item.username || idx}
              className={`bg-felt-card border ${isMe ? 'border-gold-accent shadow-gold-accent/20' : 'border-gold-accent/40'} hover:border-gold-accent/80 rounded-2xl p-4 shadow-lg transition-all duration-150 flex items-center justify-between`}
            >
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-felt-dark border border-gold-accent/30 flex items-center justify-center font-bold text-xs text-gold-accent">
                  #{idx + 1}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-base text-[#f5f5dc] tracking-tight">
                      {item.username || item.name || 'Player'}
                    </h3>
                    {isMe && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-gold-accent text-black shadow-sm">
                        ME
                      </span>
                    )}
                  </div>
                  <span className="text-[11px] text-cream-text/50">
                    {isPositive ? 'In Profit' : isNegative ? 'Owes Money' : 'Settled'}
                  </span>
                </div>
              </div>

              <div className="text-right">
                <div className={`text-lg font-black font-mono tracking-tight ${balanceColor}`}>
                  {formattedBalance}
                </div>
                <span className="text-[10px] text-cream-text/50 uppercase font-semibold">
                  Net Balance
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default BalancesTab;
