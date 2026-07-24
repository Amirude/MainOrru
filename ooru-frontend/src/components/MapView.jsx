import { useEffect, useRef } from 'react';

let loadPromise = null;
function loadGoogleMapsScript(apiKey) {
  if (window.google && window.google.maps) return Promise.resolve(true);
  if (loadPromise) return loadPromise;
  loadPromise = new Promise((resolve) => {
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}`;
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
  return loadPromise;
}

/**
 * markers: [{ lat, lng, label, isMe }]
 * Renders nothing (and the parent should show its own fallback) if no API key is configured.
 */
export default function MapView({ markers, height = 320 }) {
  const containerRef = useRef(null);
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

  useEffect(() => {
    if (!apiKey || !markers || markers.length === 0) return;
    let cancelled = false;

    loadGoogleMapsScript(apiKey).then((ok) => {
      if (!ok || cancelled || !containerRef.current) return;

      const center = markers.find((m) => m.isMe) || markers[0];
      const map = new window.google.maps.Map(containerRef.current, {
        center: { lat: center.lat, lng: center.lng },
        zoom: 13,
      });

      markers.forEach((m) => {
        new window.google.maps.Marker({
          position: { lat: m.lat, lng: m.lng },
          map,
          title: m.label,
          icon: m.isMe
            ? 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png'
            : undefined,
        });
      });
    });

    return () => { cancelled = true; };
  }, [apiKey, markers]);

  if (!apiKey) {
    return (
      <div style={{ padding: 16, textAlign: 'center', color: 'var(--ink-soft)', fontSize: '0.86rem', border: '1.5px dashed var(--paper-deep)', borderRadius: 8 }}>
        Map view needs a Google Maps API key (VITE_GOOGLE_MAPS_API_KEY) — showing the plain list below instead.
      </div>
    );
  }

  return <div ref={containerRef} style={{ width: '100%', height, borderRadius: 8, border: '2px solid var(--ink)' }} />;
}
