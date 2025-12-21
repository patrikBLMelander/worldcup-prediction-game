/**
 * Formats a numeric amount as Swedish Krona (SEK).
 * Removes trailing zeros and decimal point if not needed.
 * Handles negative amounts and edge cases.
 * 
 * @param {number|string|null|undefined} amount - The amount to format
 * @returns {string} Formatted currency string (e.g., "500 SEK", "500.5 SEK", "500.55 SEK", "-100 SEK")
 */
export const formatCurrency = (amount) => {
  if (amount == null || amount === '') return '0 SEK';
  const num = parseFloat(amount);
  if (isNaN(num) || !isFinite(num)) return '0 SEK';
  
  // Handle negative amounts
  const isNegative = num < 0;
  const absNum = Math.abs(num);
  
  // Remove trailing zeros and decimal point if not needed
  const formatted = absNum % 1 === 0 
    ? absNum.toString() 
    : absNum.toFixed(2).replace(/\.?0+$/, '');
  
  return `${isNegative ? '-' : ''}${formatted} SEK`;
};

