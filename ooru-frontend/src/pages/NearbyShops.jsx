import { useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import MapView from '../components/MapView';

const LOOKUP_CATEGORIES = [
  { code: 'petrol', label: 'Petrol bunk' },
  { code: 'xerox', label: 'Xerox / print shop' },
];

export default function NearbyShops() {
  const [category, setCategory] = useState('petrol');
  const [status, setStatus] = useState('');
  const [results, setResults] = useState(null);
  const [myLocation, setMyLocation] = useState(null);
  const [error, setError] = useState('');

  function findNearby() {
    setError('');
    setStatus('Finding your location…');
    if (!navigator.geolocation) {
      setStatus("Location isn't available in this browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        setStatus('Location found — searching…');
        setMyLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        try {
          const res = await apiClient.get('/shops/nearby', {
            params: { categoryCode: category, lat: pos.coords.latitude, lng: pos.coords.longitude },
          });
          setResults(res.data);
          setStatus('');
        } catch (err) {
          setError(extractErrorMessage(err));
          setStatus('');
        }
      },
      () => setStatus('Location permission was not given.')
    );
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>📍 Nearby shops</h2>
        <p className="sub">Distance is computed on the server from your device's location — the same lookup works for petrol bunks or any other shop category.</p>
        {error && <div className="error-banner">{error}</div>}
        <div className="field-row">
          <div className="field">
            <label>Looking for</label>
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              {LOOKUP_CATEGORIES.map((c) => <option key={c.code} value={c.code}>{c.label}</option>)}
            </select>
          </div>
          <div className="field" style={{ alignSelf: 'end' }}>
            <button className="btn outline full" onClick={findNearby}>Use my location</button>
          </div>
        </div>
        {status && <p style={{ fontSize: '0.86rem', color: 'var(--ink-soft)' }}>{status}</p>}

        {results && results.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <MapView
              markers={[
                ...(myLocation ? [{ ...myLocation, label: 'You are here', isMe: true }] : []),
                ...results.filter((s) => s.latitude && s.longitude).map((s) => ({ lat: s.latitude, lng: s.longitude, label: s.shopName })),
              ]}
            />
          </div>
        )}

        {results && (
          results.length === 0 ? (
            <div className="empty">No approved shops in this category yet.</div>
          ) : (
            results.map((s) => (
              <div key={s.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #ecdfb8' }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{s.shopName}</div>
                  <div style={{ fontSize: '0.82rem', color: 'var(--ink-soft)' }}>{s.address}</div>
                </div>
                <span className="mono" style={{ color: 'var(--brick)', fontWeight: 700 }}>{s.distanceKm.toFixed(1)} km</span>
              </div>
            ))
          )
        )}
      </div>
    </div>
  );
}
