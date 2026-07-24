import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// The WebSocket endpoint lives at the backend's root (/ws), not under /api like everything else.
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const WS_BASE = API_BASE.replace(/\/api\/?$/, '');

/**
 * Subscribes to real-time booking status updates for one customer. Returns an unsubscribe
 * function — call it on unmount so you don't leak a connection every time the page is visited.
 * onUpdate receives the updated BookingResponse the moment the backend pushes it, no polling.
 */
export function subscribeToMyBookings(userId, onUpdate) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${WS_BASE}/ws`),
    reconnectDelay: 4000,
    onConnect: () => {
      client.subscribe(`/topic/customer/${userId}/bookings`, (message) => {
        try {
          onUpdate(JSON.parse(message.body));
        } catch (e) {
          console.error('Could not parse live booking update', e);
        }
      });
    },
  });
  client.activate();
  return () => client.deactivate();
}
