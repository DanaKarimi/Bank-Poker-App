import React, { useState, useEffect } from 'react';
import { X, ArrowDownLeft, ArrowUpRight, DollarSign, AlertCircle, Layers } from 'lucide-react';

const RequestModal = ({ isOpen, onClose, type = 'buy-in', tables = [], onSubmit }) => {
  const [amount, setAmount] = useState('');
  const [selectedTableId, setSelectedTableId] = useState('');
  const [customTableId, setCustomTableId] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const isBuyIn = type === 'buy-in';

  useEffect(() => {
    if (isOpen) {
      setAmount('');
      setError('');
      if (tables && tables.length > 0) {
        setSelectedTableId(tables[0].id);
      } else {
        setSelectedTableId('');
      }
    }
  }, [isOpen, tables]);

  if (!isOpen) return null;

  const quickAmounts = [100, 500, 1000, 5000];

  const handleAddAmount = (val) => {
    const current = Number(amount) || 0;
    setAmount((current + val).toString());
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const numAmount = Number(amount);
    if (!amount || isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid positive amount.');
      return;
    }

    const tableId = selectedTableId || customTableId;
    if (!tableId) {
      setError('Please select or specify a table.');
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(numAmount, tableId);
      onClose();
    } catch (err) {
      console.error('Request submission error:', err);
      setError(err.response?.data?.error || err.message || 'Failed to submit request.');
    } finally {
      setIsSubmitting(false);
    }
  };

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

        {/* Modal Header */}
        <div className="flex items-center gap-3 mb-4">
          <div
            className={`p-2.5 rounded-xl border ${
              isBuyIn
                ? 'bg-green-950/70 text-green-400 border-green-500/40'
                : 'bg-amber-950/70 text-amber-400 border-amber-500/40'
            }`}
          >
            {isBuyIn ? <ArrowDownLeft className="w-6 h-6" /> : <ArrowUpRight className="w-6 h-6" />}
          </div>
          <div>
            <h2 className="text-xl font-bold text-gold-accent">
              {isBuyIn ? 'Request Buy-In' : 'Request Exit / Cashout'}
            </h2>
            <p className="text-xs text-cream-text/70">
              {isBuyIn
                ? 'Submit a request to buy chips at a table.'
                : 'Submit a request to cash out your chips from a table.'}
            </p>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-lg flex items-center gap-2 text-red-200 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Table Selection */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1.5 flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-gold-accent" />
              <span>Select Poker Table</span>
            </label>
            {tables && tables.length > 0 ? (
              <select
                value={selectedTableId}
                onChange={(e) => setSelectedTableId(e.target.value)}
                className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text focus:outline-none focus:border-gold-accent transition font-semibold"
                required
              >
                {tables.map((t) => (
                  <option key={t.id} value={t.id} className="bg-felt-dark text-cream-text">
                    {t.name || `Table ${t.id}`} {t.chip_value ? `($${t.chip_value}/chip)` : ''}
                  </option>
                ))}
              </select>
            ) : (
              <div>
                <input
                  type="text"
                  value={customTableId}
                  onChange={(e) => setCustomTableId(e.target.value)}
                  placeholder="Enter Table ID or name"
                  className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 focus:outline-none focus:border-gold-accent transition"
                  required
                />
                <p className="text-[11px] text-cream-text/50 mt-1">
                  No active tables auto-detected. Please enter the table ID provided by your host.
                </p>
              </div>
            )}
          </div>

          {/* Amount Input */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1.5 flex items-center gap-1.5">
              <DollarSign className="w-3.5 h-3.5 text-gold-accent" />
              <span>{isBuyIn ? 'Buy-In Chip Amount' : 'Exit Chip Amount'}</span>
            </label>
            <input
              type="number"
              min="1"
              step="1"
              value={amount}
              onChange={(e) => {
                setAmount(e.target.value);
                setError('');
              }}
              placeholder="e.g. 1000"
              className="w-full px-4 py-2.5 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text font-mono text-lg font-bold placeholder-cream-text/40 focus:outline-none focus:border-gold-accent transition"
              required
            />

            {/* Quick Chip Presets */}
            <div className="grid grid-cols-4 gap-2 mt-2.5">
              {quickAmounts.map((val) => (
                <button
                  key={val}
                  type="button"
                  onClick={() => handleAddAmount(val)}
                  className="py-1.5 bg-felt-dark hover:bg-gold-accent hover:text-black text-gold-accent border border-gold-accent/40 rounded-lg text-xs font-bold font-mono transition active:scale-95"
                >
                  +{val >= 1000 ? `${val / 1000}k` : val}
                </button>
              ))}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-gold-accent/20">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 bg-felt-dark hover:bg-felt-dark/80 text-cream-text/80 rounded-xl text-sm font-semibold transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className={`px-5 py-2.5 font-bold uppercase tracking-wider text-sm rounded-xl shadow transition disabled:opacity-50 ${
                isBuyIn
                  ? 'bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black hover:opacity-95'
                  : 'bg-gradient-to-r from-amber-500 via-yellow-600 to-amber-500 text-black hover:opacity-95'
              }`}
            >
              {isSubmitting ? 'Submitting...' : isBuyIn ? 'Submit Buy-In' : 'Submit Exit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RequestModal;
