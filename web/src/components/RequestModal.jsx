import React, { useState, useEffect } from 'react';
import { X, ArrowDownLeft, ArrowUpRight, DollarSign, AlertCircle, Layers } from 'lucide-react';
import SmartAmountInput from './SmartAmountInput';

const RequestModal = ({
  isOpen,
  onClose,
  type = 'buy-in',
  tables = [],
  initialTableId = '',
  selectedTable = null,
  onSubmit,
}) => {
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
      if (initialTableId) {
        setSelectedTableId(initialTableId);
      } else if (selectedTable?.id) {
        setSelectedTableId(selectedTable.id);
      } else if (tables && tables.length > 0) {
        setSelectedTableId(tables[0].id);
      } else {
        setSelectedTableId('');
      }
    }
  }, [isOpen, initialTableId, selectedTable, tables]);

  if (!isOpen) return null;

  const currentTable = selectedTable || tables.find((t) => t.id === (selectedTableId || initialTableId));

  const quickAmounts = [100, 500, 1000, 5000];

  const handleAddAmount = (val) => {
    const current = Number(amount) || 0;
    setAmount((current + val).toString());
    setError('');
  };

  const handleAmountSubmit = async (submitAmount) => {
    setError('');

    const numAmount = Number(submitAmount);
    if (!submitAmount || isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid positive amount.');
      return;
    }

    const tableId = selectedTableId || initialTableId || customTableId;
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
                ? 'Submit a request to buy chips for this table.'
                : 'Submit a request to cash out chips from this table.'}
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

        <div className="space-y-4">
          {/* Table Display / Selection */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1.5 flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-gold-accent" />
              <span>Poker Table</span>
            </label>

            {currentTable ? (
              <div className="p-3 bg-felt-dark rounded-xl border border-gold-accent/40 flex items-center justify-between">
                <div>
                  <div className="font-bold text-cream-text text-sm">
                    {currentTable.name || `Table ${currentTable.id}`}
                  </div>
                  {currentTable.chip_value && (
                    <div className="text-[11px] text-cream-text/60">
                      Chip value: ${currentTable.chip_value}
                    </div>
                  )}
                </div>
                <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                  {currentTable.status || 'ACTIVE'}
                </span>
              </div>
            ) : tables && tables.length > 0 ? (
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
                  No active tables detected. Please enter the table ID provided by your host.
                </p>
              </div>
            )}
          </div>

          {/* Smart Amount Input */}
          <SmartAmountInput 
            chipValue={currentTable?.chip_value || currentTable?.chipValue} 
            onSubmit={(amount) => handleAmountSubmit(amount)} 
            onCancel={onClose}
            isSubmitting={isSubmitting} 
          />
        </div>
      </div>
    </div>
  );
};

export default RequestModal;
