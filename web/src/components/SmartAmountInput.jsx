import React, { useState } from 'react';
import { DollarSign } from 'lucide-react';

const SmartAmountInput = ({ chipValue, onSubmit, isSubmitting }) => {
  const [amount, setAmount] = useState('');

  const numChipValue = chipValue ? Number(chipValue) : null;
  const hasChipValue = numChipValue && !isNaN(numChipValue) && numChipValue > 0;

  const handleAddChip = (multiplier) => {
    const current = Number(amount) || 0;
    const addValue = numChipValue * multiplier;
    setAmount((current + addValue).toString());
  };

  const handleClear = () => setAmount('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSubmit) {
      onSubmit(amount);
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1.5 flex items-center gap-1.5">
          <DollarSign className="w-3.5 h-3.5 text-gold-accent" />
          <span>Amount</span>
        </label>
        <div className="flex gap-2">
          <input
            type="number"
            min="1"
            step="1"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="e.g. 1000"
            className="flex-1 px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text font-mono text-lg font-bold placeholder-cream-text/40 focus:outline-none focus:border-gold-accent transition"
            required
            autoFocus
          />
          {hasChipValue && (
            <button
              type="button"
              onClick={handleClear}
              className="px-4 py-2.5 bg-red-950/40 text-red-400 border border-red-500/30 rounded-xl text-xs font-bold uppercase tracking-wider hover:bg-red-950/60 transition"
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {hasChipValue && (
        <div className="space-y-2">
          <div className="text-[10px] text-cream-text/60 uppercase font-bold tracking-wider mb-2">
            Quick Add Chips (Value: ${numChipValue})
          </div>
          <div className="grid grid-cols-4 gap-2">
            {[1, 5, 10, 50].map((multiplier) => (
              <button
                key={multiplier}
                type="button"
                onClick={() => handleAddChip(multiplier)}
                className="py-3 bg-gradient-to-b from-green-700 to-green-900 border border-green-500 rounded-full shadow-[0_4px_0_rgb(20,83,45),0_5px_4px_rgba(0,0,0,0.5)] active:shadow-[0_0px_0_rgb(20,83,45),0_1px_2px_rgba(0,0,0,0.5)] active:translate-y-[4px] transition-all flex flex-col items-center justify-center gap-1"
              >
                <span className="text-[10px] text-green-200 font-bold uppercase tracking-wider leading-none">
                  {multiplier}x
                </span>
                <span className="text-sm font-black text-white font-mono leading-none">
                  ${numChipValue * multiplier}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={handleSubmit}
        disabled={isSubmitting || !amount || Number(amount) <= 0}
        className="w-full mt-4 px-5 py-3.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-extrabold uppercase tracking-wider text-sm rounded-xl shadow-lg hover:opacity-95 transition active:scale-95 disabled:opacity-50"
      >
        {isSubmitting ? 'Submitting...' : 'Confirm Amount'}
      </button>
    </div>
  );
};

export default SmartAmountInput;
