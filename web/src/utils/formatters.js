/**
 * BankPoker Web Formatters
 * Consolidated formatting utilities to prevent duplication across components.
 */

/**
 * Format balance as +$X, -$X, or $0 with thousands separators
 * @param {number|string} balance
 * @returns {string}
 */
export function formatBalance(balance) {
  const num = Number(balance) || 0;
  const formatted = Math.abs(num).toLocaleString();
  if (num > 0) return `+$${formatted}`;
  if (num < 0) return `-$${formatted}`;
  return '$0';
}

/**
 * Format timestamp as a readable date-time string
 * @param {number|string} ts
 * @returns {string}
 */
export function formatTimestamp(ts) {
  if (!ts) return '';
  try {
    const d = new Date(Number(ts));
    return d.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (e) {
    return '';
  }
}
