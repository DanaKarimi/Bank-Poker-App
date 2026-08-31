import React from 'react';

const StatusBadge = ({ status }) => {
  const isClosed = status === 'CLOSED' || status === false;

  if (isClosed) {
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-extrabold uppercase tracking-wide bg-zinc-800 text-zinc-300 border border-zinc-600/50 shadow-sm">
        CLOSED
      </span>
    );
  }

  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-extrabold uppercase tracking-wide bg-emerald-950 text-emerald-300 border border-emerald-500/50 shadow-sm animate-pulse">
      ACTIVE
    </span>
  );
};

export default StatusBadge;
