import React, { useState } from 'react';
import { Clock, CheckCircle2, AlertCircle, Check, ArrowDownLeft, ArrowUpRight, UserPlus } from 'lucide-react';

const RequestCard = ({ request, type, onConfirm }) => {
  const [isConfirming, setIsConfirming] = useState(false);

  const handleConfirm = async () => {
    if (!onConfirm) return;
    setIsConfirming(true);
    try {
      await onConfirm(request);
    } finally {
      setIsConfirming(false);
    }
  };

  const formattedDate = request.created_at
    ? new Date(Number(request.created_at)).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        month: 'short',
        day: 'numeric',
      })
    : '';

  // Status badge colors & icons
  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-amber-950/80 text-amber-300 border border-amber-500/50 shadow-sm">
            <Clock className="w-3 h-3 animate-pulse" />
            <span>Pending</span>
          </span>
        );
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-blue-950/80 text-blue-300 border border-blue-500/50 shadow-sm">
            <CheckCircle2 className="w-3 h-3" />
            <span>Approved</span>
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-green-950/80 text-green-300 border border-green-500/50 shadow-sm">
            <Check className="w-3 h-3" />
            <span>Confirmed</span>
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-red-950/80 text-red-300 border border-red-500/50 shadow-sm">
            <AlertCircle className="w-3 h-3" />
            <span>Rejected</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-felt-dark text-cream-text/70 border border-gold-accent/20">
            {status}
          </span>
        );
    }
  };

  // Header icon & label based on request type
  const renderTypeHeader = () => {
    if (type === 'buy-in') {
      return (
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-green-950/70 text-green-400 border border-green-500/30 rounded-lg">
            <ArrowDownLeft className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-cream-text">Buy-In Request</h4>
            {request.table_name && (
              <span className="text-xs text-cream-text/60">Table: {request.table_name}</span>
            )}
          </div>
        </div>
      );
    }

    if (type === 'exit') {
      return (
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-amber-950/70 text-amber-400 border border-amber-500/30 rounded-lg">
            <ArrowUpRight className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-cream-text">Exit / Cashout Request</h4>
            {request.table_name && (
              <span className="text-xs text-cream-text/60">Table: {request.table_name}</span>
            )}
          </div>
        </div>
      );
    }

    return (
      <div className="flex items-center gap-2">
        <div className="p-1.5 bg-felt-dark text-gold-accent border border-gold-accent/30 rounded-lg">
          <UserPlus className="w-4 h-4" />
        </div>
        <div>
          <h4 className="text-sm font-bold text-cream-text">Join Group Request</h4>
          {request.group_name && (
            <span className="text-xs text-cream-text/60">{request.group_name}</span>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="bg-felt-card/95 border border-gold-accent/40 hover:border-gold-accent/70 rounded-xl p-4 shadow-md transition duration-150 flex flex-col justify-between gap-3">
      <div className="flex items-start justify-between gap-2">
        {renderTypeHeader()}
        {getStatusBadge(request.status)}
      </div>

      <div className="flex items-baseline justify-between border-t border-gold-accent/15 pt-3">
        {request.amount != null ? (
          <div>
            <span className="text-xs uppercase text-cream-text/50 font-bold block">Amount</span>
            <span className="text-xl font-mono font-black text-gold-accent">
              {request.amount.toLocaleString()}
            </span>
          </div>
        ) : (
          <div>
            <span className="text-xs text-cream-text/60">Status: {request.status}</span>
          </div>
        )}

        <div className="text-right">
          <span className="text-xs text-cream-text/50 block">Submitted</span>
          <span className="text-xs font-mono text-cream-text/75">{formattedDate}</span>
        </div>
      </div>

      {/* Action button if status is APPROVED */}
      {request.status === 'APPROVED' && (
        <div className="pt-2 border-t border-blue-500/20">
          <div className="bg-blue-950/40 p-2 rounded-lg border border-blue-500/30 mb-2 text-xs text-blue-200">
            Approved by Admin! Tap confirm once you have received your physical chips/payout.
          </div>
          <button
            onClick={handleConfirm}
            disabled={isConfirming}
            className="w-full py-2 bg-gradient-to-r from-green-600 via-emerald-500 to-green-600 hover:from-green-500 hover:to-emerald-400 text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow transition active:scale-95 disabled:opacity-50 flex items-center justify-center gap-1.5"
          >
            <Check className="w-4 h-4" />
            <span>{isConfirming ? 'Confirming...' : 'Confirm Receipt'}</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default RequestCard;
