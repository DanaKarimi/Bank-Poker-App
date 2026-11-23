import React, { useState, useEffect } from 'react';
import { Smartphone, Download, X, Info } from 'lucide-react';

const AndroidDownloadButton = ({ compact = false, className = '' }) => {
  const [releaseInfo, setReleaseInfo] = useState(null);
  const [showTip, setShowTip] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetch('/releases/version.json', { cache: 'no-store' })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!cancelled && data && (data.versionName || data.sizeMB || data.sizeBytes)) {
          setReleaseInfo(data);
        }
      })
      .catch(() => {
        // Gracefully ignore missing version.json or network failure
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!showTip) return;
    const timer = setTimeout(() => setShowTip(false), 9000);
    return () => clearTimeout(timer);
  }, [showTip]);

  const sizeLabel = releaseInfo?.sizeMB
    ? `${releaseInfo.sizeMB} MB`
    : releaseInfo?.sizeBytes
    ? `${(Number(releaseInfo.sizeBytes) / (1024 * 1024)).toFixed(1)} MB`
    : null;

  const fileName =
    releaseInfo?.fileName ||
    (releaseInfo?.versionName ? `BankPoker-v${releaseInfo.versionName}.apk` : 'BankPoker-latest.apk');

  const apkUrl = releaseInfo?.apkUrl || `/releases/${fileName}`;

  const versionBadge = releaseInfo?.versionName
    ? `v${releaseInfo.versionName}${sizeLabel ? ` • ${sizeLabel}` : ''}`
    : sizeLabel;

  return (
    <div className={`relative inline-flex items-center max-w-full ${className}`}>
      <a
        href={apkUrl}
        download={fileName}
        onClick={() => setShowTip(true)}
        className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-950/90 hover:bg-emerald-900 border border-gold-accent/60 hover:border-gold-accent text-cream-text text-xs font-bold shadow-md transition active:scale-95 cursor-pointer max-w-full"
        title={`Download ${fileName}`}
      >
        <Smartphone className="w-3.5 h-3.5 text-gold-accent shrink-0" />
        <span className="truncate">
          {compact ? fileName : `Download ${fileName}`}
        </span>
        {versionBadge && (
          <span className="inline-flex items-center px-1.5 py-0.5 rounded-full bg-gold-accent/20 border border-gold-accent/40 text-gold-light text-[10px] font-mono font-bold shrink-0">
            {versionBadge}
          </span>
        )}
        <Download className="w-3.5 h-3.5 text-gold-accent shrink-0" />
      </a>

      {showTip && (
        <div className="fixed bottom-4 left-1/2 -translate-x-1/2 sm:left-auto sm:translate-x-0 sm:right-5 z-50 w-[calc(100vw-2rem)] max-w-sm bg-felt-dark/95 border-2 border-gold-accent rounded-2xl p-3.5 shadow-2xl text-xs text-cream-text flex items-start gap-2.5">
          <Info className="w-4 h-4 text-gold-accent shrink-0 mt-0.5" />
          <div className="flex-1 min-w-0 leading-relaxed">
            <div className="font-bold text-gold-accent mb-0.5">Downloading {fileName}...</div>
            <p className="text-cream-text/85 text-[11px]">
              If prompted by your browser, allow &ldquo;Install unknown apps&rdquo; to update BankPoker without losing offline data.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setShowTip(false)}
            className="text-cream-text/60 hover:text-cream-text p-0.5 cursor-pointer shrink-0"
            aria-label="Dismiss install tip"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
};

export default AndroidDownloadButton;
