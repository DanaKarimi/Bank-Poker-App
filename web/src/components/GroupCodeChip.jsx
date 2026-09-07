import React, { useState } from 'react';
import { Copy, Check, Share2 } from 'lucide-react';

export const GroupCodeChip = ({ code, groupName = 'Poker Group', className = '' }) => {
  const [copied, setCopied] = useState(false);

  if (!code) return null;

  const cleanCode = code.toUpperCase();
  const shareUrl = `${window.location.origin}/#/join/${cleanCode}`;

  const handleCopy = (e) => {
    e.stopPropagation();
    navigator.clipboard.writeText(cleanCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = async (e) => {
    e.stopPropagation();
    const shareData = {
      title: `Join ${groupName} on BankPoker`,
      text: `Join our poker group "${groupName}" on BankPoker! Use code: ${cleanCode}`,
      url: shareUrl
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch (err) {
        if (err.name !== 'AbortError') {
          handleCopy(e);
        }
      }
    } else {
      handleCopy(e);
    }
  };

  return (
    <div
      onClick={handleCopy}
      title="Click to copy code"
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 bg-gold-accent/15 hover:bg-gold-accent/25 border border-gold-accent/50 rounded-lg text-xs cursor-pointer transition select-none ${className}`}
    >
      <span className="font-mono font-bold tracking-wider text-gold-accent">{cleanCode}</span>

      <button
        type="button"
        onClick={handleCopy}
        className="text-gold-accent/80 hover:text-gold-accent p-0.5 rounded transition"
        title="Copy code"
      >
        {copied ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
      </button>

      <button
        type="button"
        onClick={handleShare}
        className="text-gold-accent/80 hover:text-gold-accent p-0.5 rounded transition"
        title="Share invite link"
      >
        <Share2 className="w-3.5 h-3.5" />
      </button>
    </div>
  );
};

export default GroupCodeChip;
