import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, isSupported } from 'firebase/messaging';
import { apiClient } from './api/client';

// Get every one of these from Firebase Console -> Project Settings -> General -> "Your apps"
// (web app config), and the VAPID key from Project Settings -> Cloud Messaging -> Web Push
// certificates. Nothing here works until you fill these in with your own project's values.
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};
const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;

/**
 * Asks the browser for notification permission, registers the service worker, gets an FCM
 * device token, and saves it against the logged-in user on the backend. Returns a short status
 * string so the UI calling this can show something sensible either way — this is best-effort,
 * not something that should ever block the rest of the app.
 */
export async function enableNotifications() {
  if (!firebaseConfig.apiKey) {
    return { ok: false, message: 'Notifications are not configured yet (no Firebase project set up).' };
  }

  const supported = await isSupported().catch(() => false);
  if (!supported) {
    return { ok: false, message: 'This browser does not support push notifications.' };
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return { ok: false, message: 'Notification permission was not granted.' };
    }

    const app = initializeApp(firebaseConfig);
    const messaging = getMessaging(app);
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
    const token = await getToken(messaging, { vapidKey, serviceWorkerRegistration: registration });

    if (!token) {
      return { ok: false, message: 'Could not get a device token.' };
    }

    await apiClient.patch('/users/me/fcm-token', { fcmToken: token });
    return { ok: true, message: 'Notifications enabled.' };
  } catch (err) {
    return { ok: false, message: 'Could not enable notifications: ' + err.message };
  }
}
