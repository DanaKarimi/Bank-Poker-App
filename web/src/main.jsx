import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

// Register service worker for PWA app shell and offline support
if ('serviceWorker' in navigator && !window.location.host.includes('localhost:5173')) {
  let didReload = false;

  window.addEventListener('load', () => {
    // Clear debounce timestamp after page has been loaded and stable
    setTimeout(() => {
      try {
        sessionStorage.removeItem('bp_sw_reload_ts');
      } catch (e) {}
    }, 15000);

    navigator.serviceWorker
      .register('/sw.js')
      .then((reg) => {
        console.log('BankPoker PWA ServiceWorker active:', reg.scope);
        // Check for updates on load
        reg.update().catch(() => {});

        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              // Only request skip waiting if there is an existing controller taking traffic
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                newWorker.postMessage({ type: 'SKIP_WAITING' });
              }
            });
          }
        });
      })
      .catch((err) => {
        console.warn('BankPoker ServiceWorker registration skipped:', err);
      });

    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (didReload) return;
      didReload = true;

      try {
        const lastReload = sessionStorage.getItem('bp_sw_reload_ts');
        const now = Date.now();
        if (lastReload && now - Number(lastReload) < 15000) {
          console.warn('[SW] Suppressed repeated reload cycle within debounce window.');
          return;
        }
        sessionStorage.setItem('bp_sw_reload_ts', String(now));
      } catch (e) {}

      console.log('[SW] Controller changed, performing single reload for fresh bundle.');
      window.location.reload();
    });
  });
}
