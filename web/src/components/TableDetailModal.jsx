import React from 'react';
import { X, Layers, PlusCircle, MinusCircle, DollarSign, Clock, ShieldCheck } from 'lucide-react';

const TableDetailModal = ({
  isOpen,
  onClose,
  table,
  myRequests = [],
  onRequestBuyIn,
  onRequestExit,
}) => {
  if (!isOpen || !table) return null;

  const tableRequests = myRequests.filter((r) => r.table_id === table.id);
  const activeBuyIns = tableRequests.filter((r) => r.status === 'CONFIRMED');

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 mb-5">
          <div className="p-3 bg-felt-dark text-gold-accent border border-gold-accent/40 rounded-xl">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-cream-text">{table.name || `Table ${table.id}`}</h2>
              <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                {table.status || 'ACTIVE'}
              </span>
            </div>
            <p className="text-xs text-cream-text/60">Select an action for this table below</p>
          </div>
        </div>

        {/* Table Details Box */}
        <div className="bg-felt-dark/90 border border-gold-accent/30 rounded-xl p-4 mb-5 space-y-2.5">
          <div className="flex items-center justify-between text-xs">
            <span className="text-cream-text/60">Chip Value</span>
            <span className="font-mono font-bold text-gold-accent">
              {table.chip_value ? `$${table.chip_value} / chip` : 'Standard 1:1'}
            </span>
          </div>

          {table.has_entry_fee && table.entry_fee > 0 && (
            <div className="flex items-center justify-between text-xs border-t border-gold-accent/15 pt-2">
              <span className="text-cream-text/60">Entry Fee</span>
              <span className="font-mono font-bold text-amber-400">${table.entry_fee}</span>
            </div>
          )}

          <div className="flex items-center justify-between text-xs border-t border-gold-accent/15 pt-2">
            <span className="text-cream-text/60">Table ID</span>
            <span className="font-mono text-cream-text/80 text-[11px] truncate max-w-[180px]">
              {table.id}
            </span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="space-y-3">
          <button
            onClick={() => onRequestBuyIn(table)}
            className="w-full py-3 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-500 hover:to-green-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Request Buy-In Chips</span>
          </button>

          <button
            onClick={() => onRequestExit(table)}
            className="w-full py-3 bg-gradient-to-r from-amber-500 to-yellow-600 hover:from-amber-400 hover:to-yellow-500 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow-lg transition active:scale-95 flex items-center justify-center gap-2"
          >
            <MinusCircle className="w-4 h-4" />
            <span>Request Exit / Cashout</span>
          </button>
        </div>

        {/* Footer */}
        <div className="mt-4 pt-3 border-t border-gold-accent/20 text-center">
          <button
            onClick={onClose}
            className="text-xs text-cream-text/60 hover:text-cream-text transition font-semibold"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default TableDetailModal;
