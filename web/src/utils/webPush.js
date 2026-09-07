import api from '../api';

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding)
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export const subscribeWebPush = async () => {
  if (
    typeof window === 'undefined' ||
    !('serviceWorker' in navigator) ||
    !('PushManager' in window) ||
    !('Notification' in window)
  ) {
    return; // Graceful no-op where unsupported (e.g. older iOS / WebViews)
  }

  try {
    const reg = await navigator.serviceWorker.ready;
    if (!reg || !reg.pushManager) return;

    if (Notification.permission === 'denied') return;

    let permission = Notification.permission;
    if (permission === 'default') {
      permission = await Notification.requestPermission();
    }
    if (permission !== 'granted') return;

    const vapidRes = await api.get('/api/notifications/vapid-key');
    const publicKey = vapidRes.data?.publicKey;
    if (!publicKey) return;

    let subscription = await reg.pushManager.getSubscription();
    if (!subscription) {
      const convertedKey = urlBase64ToUint8Array(publicKey);
      subscription = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: convertedKey
      });
    }

    if (subscription) {
      await api.post('/api/notifications/web-subscribe', { subscription });
      console.log('BankPoker: Web Push successfully registered');
    }
  } catch (err) {
    console.warn('BankPoker: Web push registration skipped/failed:', err);
  }
};
