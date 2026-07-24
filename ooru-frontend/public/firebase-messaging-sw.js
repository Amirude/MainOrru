// Firebase requires this exact file at this exact path (/firebase-messaging-sw.js) to deliver
// notifications when the app isn't the active tab. It can't read Vite's .env variables (service
// workers load before your app does), so the same config from src/firebaseNotifications.js is
// duplicated here, in plain values, not env vars. Fill in your own project's config in both places.
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'REPLACE_WITH_YOUR_FIREBASE_API_KEY',
  authDomain: 'REPLACE_WITH_YOUR_PROJECT.firebaseapp.com',
  projectId: 'REPLACE_WITH_YOUR_PROJECT_ID',
  messagingSenderId: 'REPLACE_WITH_YOUR_SENDER_ID',
  appId: 'REPLACE_WITH_YOUR_APP_ID',
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const { title, body } = payload.notification || {};
  self.registration.showNotification(title || 'Altenul One', {
    body: body || '',
    icon: '/favicon.ico',
  });
});
