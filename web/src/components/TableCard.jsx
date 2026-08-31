import React from 'react';
import { Layers, Users, ChevronRight, Coins, DollarSign } from 'lucide-react';
import StatusBadge from './StatusBadge';

const TableCard = ({ table, onClick }) => {
  const isClosed = table.status === 'CLOSED' || table.is_active === 0;

  return (
    <div
      onClick={onClick}
      className={`p-4 bg-felt-card/90 rounded-xl border transition-all cursor-pointer flex flex-col justify-between group shadow-md hover:shadow-xl ${
        isClosed
          ? 'border-zinc-700/60 opacity-80 hover:border-zinc-500 bg-zinc-900/60'
          : 'border-gold-accent/40 hover:border-gold-accent hover:scale-[1.02] bg-felt-card'
      }`}
    >
      <div>
        {/* Header: Name + Status */}
        <div className="flex items-start justify-between gap-2 mb-2.5">
          <div className="flex items-center gap-2">
            <div
              className={`p-2 rounded-lg border ${
                isClosed
                  ? 'bg-zinc-800 text-zinc-400 border-zinc-700'
                  : 'bg-felt-dark text-gold-accent border-gold-accent/40'
              }`}
            >
              <Layers className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-cream-text text-sm group-hover:text-gold-accent transition">
                {table.name || `Table ${table.id}`}
              </h3>
              <div className="text-[10px] text-cream-text/50">
                Created: {new Date(table.created_at || table.createdAt || Date.now()).toLocaleDateString()}
              </div>
            </div>
          </div>
          <StatusBadge status={table.status} />
        </div>

        {/* Info Grid */}
        <div className="grid grid-cols-2 gap-2 mt-3 pt-2.5 border-t border-gold-accent/15 text-xs">
          <div className="flex items-center gap-1.5 text-cream-text/70">
            <Coins className="w-3.5 h-3.5 text-gold-accent/80" />
            <span>
              {table.chip_value || table.chipValue
                ? `$${table.chip_value || table.chipValue} / chip`
                : '1:1 Standard'}
            </span>
          </div>

          <div className="flex items-center gap-1.5 text-cream-text/70">
            <Users className="w-3.5 h-3.5 text-gold-accent/80" />
            <span>{table.playerCount || 0} players seated</span>
          </div>
        </div>

        {(table.has_entry_fee || table.hasEntryFee) && (table.entry_fee || table.entryFee) > 0 && (
          <div className="mt-2 text-[11px] text-amber-300 bg-amber-950/40 border border-amber-500/20 px-2 py-0.5 rounded inline-flex items-center gap-1">
            <DollarSign className="w-3 h-3" />
            <span>Entry Fee: ${table.entry_fee || table.entryFee}</span>
          </div>
        )}
      </div>

      {/* Footer / CTA */}
      <div className="mt-4 pt-2.5 border-t border-gold-accent/15 flex items-center justify-between text-xs font-semibold">
        <span className={isClosed ? 'text-zinc-400' : 'text-gold-accent'}>
          {isClosed ? 'View Table Record' : 'Enter Table Room'}
        </span>
        <ChevronRight
          className={`w-4 h-4 transition-transform group-hover:translate-x-1 ${
            isClosed ? 'text-zinc-500' : 'text-gold-accent'
          }`}
        />
      </div>
    </div>
  );
};

export default TableCard;
