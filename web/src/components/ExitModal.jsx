import React, { useState, useEffect } from 'react';
import { X, AlertCircle } from 'lucide-react';

const ExitModal = ({
  isOpen,
  onClose,
  playerName = 'Player',
  currentBalance = 0,
  onSubmit,
}) => {
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setAmount('');
      setNote('');
      setError('');
      setIsSubmitting(false);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleAddChip = (val) => {
    const current = Number(amount) || 0;
    setAmount((current + val).toString());
    setError('');
  };

  const handleRemoveLast = () => {
    const current = Number(amount) || 0;
    const updated = Math.max(0, current - 100);
    setAmount(updated === 0 ? '' : updated.toString());
    setError('');
  };

  const handleClear = () => {
    setAmount('');
    setError('');
  };

  const handleCustomAmountChange = (e) => {
    const val = e.target.value;
    // Only positive digits
    if (val === '' || Number(val) >= 0) {
      setAmount(val);
      setError('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const numAmount = Number(amount);
    if (!amount || isNaN(numAmount) || numAmount < 0) {
      setError('Amount must be zero or a positive number');
      return;
    }

    setIsSubmitting(true);
    try {
      if (onSubmit) {
        await onSubmit(numAmount, note);
      }
      onClose();
    } catch (err) {
      console.error('Exit submission error:', err);
      setError(err.response?.data?.error || err.message || 'Failed to submit exit request');
    } finally {
      setIsSubmitting(false);
    }
  };

  const numAmount = Number(amount) || 0;
  const chipDenominations = [
    { label: '100', val: 100 },
    { label: '500', val: 500 },
    { label: '1K', val: 1000 },
    { label: '5K', val: 5000 },
  ];

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-[#064e3b] border-2 border-[#d4af37] rounded-2xl w-full max-w-md p-6 shadow-2xl relative text-[#f5f5dc] animate-in fade-in zoom-in-95 duration-150">
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 text-[#f5f5dc]/70 hover:text-[#f5f5dc] p-1.5 rounded-xl hover:bg-black/20 transition active:scale-95"
          aria-label="Close modal"
        >
          <X className="w-5 h-5" />
        </button>

        {/* 1. Header */}
        <div className="mb-4">
          <h2 className="text-xl font-black text-[#d4af37] tracking-tight">
            Request Exit
          </h2>
          <p className="text-xs text-[#f5f5dc]/70 mt-0.5">
            Submit an exit cashout request for table chips
          </p>
        </div>

        {/* 2. Player Info Card */}
        <div className="bg-[#043327] border border-[#d4af37]/30 rounded-xl p-3.5 flex items-center justify-between shadow-inner mb-4">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#f5f5dc]/60 block">
              Player
            </span>
            <span className="text-sm font-bold text-[#f5f5dc]">
              {playerName}
            </span>
          </div>
          <div className="text-right">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#f5f5dc]/60 block">
              Current Balance
            </span>
            <span className="text-sm font-bold text-[#d4af37] font-mono">
              {Number(currentBalance).toLocaleString()} chips
            </span>
          </div>
        </div>

        {/* Error message */}
        {error && (
          <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-xl text-xs text-red-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 3. Big Total Display */}
          <div className="text-center py-2 bg-[#043327]/60 rounded-xl border border-[#d4af37]/20">
            <span className="text-[10px] font-bold uppercase tracking-[1.5px] text-[#d4af37]/80 block">
              TOTAL EXIT
            </span>
            <div className="text-3xl font-black font-mono text-[#d4af37] mt-0.5">
              {numAmount > 0 ? `${numAmount.toLocaleString()} CHIPS` : '0 CHIPS'}
            </div>
          </div>

          {/* 4. Poker Chip Selector */}
          <div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#f5f5dc]/60 block mb-2">
              TAP CHIPS TO ADD
            </span>
            <div className="flex items-center justify-between gap-1.5 sm:gap-2">
              {chipDenominations.map((chip) => (
                <button
                  key={chip.label}
                  type="button"
                  onClick={() => handleAddChip(chip.val)}
                  className="w-11 h-11 sm:w-12 sm:h-12 rounded-full flex items-center justify-center bg-gradient-to-b from-[#0a664e] to-[#043327] border-2 border-[#d4af37] shadow-[0_4px_10px_rgba(0,0,0,0.4)] hover:scale-105 active:scale-95 transition-transform relative cursor-pointer group"
                >
                  {/* Inner dashed ring styling for poker chip look */}
                  <span className="absolute inset-[2.5px] rounded-full border border-dashed border-[#f5f5dc]/50 pointer-events-none" />
                  <span className="relative z-10 font-mono font-extrabold text-xs sm:text-sm text-white drop-shadow-sm">
                    {chip.label}
                  </span>
                </button>
              ))}

              {/* Subtract 100 button */}
              <button
                type="button"
                onClick={handleRemoveLast}
                title="Subtract 100"
                className="w-11 h-11 sm:w-12 sm:h-12 rounded-full flex items-center justify-center bg-[#043327] border border-red-500/60 text-red-400 font-bold text-xl shadow-md hover:scale-105 active:scale-95 transition-transform cursor-pointer"
              >
                −
              </button>

              {/* Clear button */}
              <button
                type="button"
                onClick={handleClear}
                title="Clear all"
                className="w-11 h-11 sm:w-12 sm:h-12 rounded-full flex items-center justify-center bg-[#043327] border border-[#d4af37]/60 text-[#d4af37] font-bold text-sm shadow-md hover:scale-105 active:scale-95 transition-transform cursor-pointer"
              >
                C
              </button>
            </div>
          </div>

          {/* 5. Custom Amount Input */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[#f5f5dc]/80 mb-1.5">
              Custom Amount
            </label>
            <input
              type="number"
              min="0"
              step="1"
              value={amount}
              onChange={handleCustomAmountChange}
              placeholder="e.g. 1000"
              className="w-full px-4 py-2.5 sm:py-3 bg-[#043327] border border-[#d4af37]/50 focus:border-[#d4af37] focus:ring-1 focus:ring-[#d4af37] rounded-xl text-[#f5f5dc] font-mono text-lg font-bold placeholder-[#f5f5dc]/30 outline-none transition"
            />
          </div>

          {/* 6. Note Input */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-[#f5f5dc]/80 mb-1.5">
              Note (optional)
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. Cashout at table close"
              className="w-full px-4 py-2 bg-[#043327] border border-[#d4af37]/50 focus:border-[#d4af37] focus:ring-1 focus:ring-[#d4af37] rounded-xl text-[#f5f5dc] text-sm placeholder-[#f5f5dc]/30 outline-none transition"
            />
          </div>

          {/* 7. Confirm Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting || amount === '' || numAmount < 0}
              className="w-full py-3.5 bg-gradient-to-r from-[#d4af37] via-[#f3d068] to-[#d4af37] hover:brightness-105 active:scale-[0.98] text-black font-black uppercase tracking-wider text-sm rounded-xl shadow-lg transition-all disabled:opacity-50 flex items-center justify-center cursor-pointer"
            >
              {isSubmitting ? 'Submitting...' : 'CONFIRM EXIT'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ExitModal;
