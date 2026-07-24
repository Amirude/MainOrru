import { useEffect, useRef, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { findCategory } from '../categories';

export default function DeliveryDashboard() {
  const [available, setAvailable] = useState([]);
  const [mine, setMine] = useState([]);
  const [stats, setStats] = useState(null);
  const [sharingId, setSharingId] = useState(null);
  const [error, setError] = useState('');
  const watchIdRef = useRef(null);

  async function loadAvailable() {
    try {
      const res = await apiClient.get('/bookings/deliveries/available');
      setAvailable(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  async function loadMine() {
    try {
      const res = await apiClient.get('/bookings/deliveries/mine');
      setMine(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  async function loadStats() {
    try {
      const res = await apiClient.get('/bookings/deliveries/stats');
      setStats(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    loadAvailable();
    loadMine();
    loadStats();
    const interval = setInterval(() => { loadAvailable(); loadMine(); loadStats(); }, 6000);
    return () => clearInterval(interval);
  }, []);

  async function claim(bookingId) {
    setError('');
    try {
      await apiClient.post(`/bookings/${bookingId}/claim-delivery`);
      loadAvailable();
      loadMine();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  function startSharing(bookingId) {
    if (!navigator.geolocation) { setError("Location isn't available in this browser."); return; }
    setSharingId(bookingId);
    watchIdRef.current = navigator.geolocation.watchPosition(
      async (pos) => {
        try {
          await apiClient.patch(`/bookings/${bookingId}/location`, {
            lat: pos.coords.latitude, lng: pos.coords.longitude,
          });
        } catch (err) {
          // Don't spam errors for every failed ping — just log it.
          console.error('Location update failed', err);
        }
      },
      () => setError('Location permission was not given.'),
      { enableHighAccuracy: true, maximumAge: 5000 }
    );
  }

  function stopSharing() {
    if (watchIdRef.current !== null) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
    setSharingId(null);
  }

  useEffect(() => () => stopSharing(), []); // stop sharing location if the page unmounts

  return (
    <div className="wrap">
      <div className="card">
        <h2>🛵 Delivery partner</h2>
        <p className="sub">Claim a delivery, then share your live location while you're en route — the customer sees it move on a map in real time.</p>
        {error && <div className="error-banner">{error}</div>}

        {stats && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(110px,1fr))', gap: 10, marginBottom: 18 }}>
            {[['today', 'Today'], ['thisWeek', 'This week'], ['thisMonth', 'This month'], ['thisYear', 'This year']].map(([key, label]) => (
              <div key={key} style={{ background: 'var(--teal-soft)', borderRadius: 8, padding: 10, textAlign: 'center' }}>
                <div className="mono" style={{ fontWeight: 700, fontSize: '1.1rem' }}>₹{(stats[key].earningsPaise / 100).toFixed(0)}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--ink-soft)' }}>{label} · {stats[key].orders} order{stats[key].orders === 1 ? '' : 's'}</div>
              </div>
            ))}
          </div>
        )}

        <div className="section-title" style={{ fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: 1, color: 'var(--brick)', fontFamily: "'IBM Plex Mono',monospace", margin: '18px 0 8px' }}>Available deliveries</div>
        {available.length === 0 ? (
          <div className="empty">Nothing waiting right now.</div>
        ) : (
          available.map((b) => {
            const cat = findCategory(b.categoryCode);
            return (
              <div className="booking-row" key={b.id}>
                <div className="top">
                  <div><span className="ref">{b.reference}</span> · {cat ? cat.name : b.categoryCode}</div>
                </div>
                <div className="details">Shop: {b.shopName || '—'} · {b.details.address || ''}</div>
                <button className="btn small" style={{ marginTop: 8 }} onClick={() => claim(b.id)}>Claim this delivery</button>
              </div>
            );
          })
        )}

        <div className="section-title" style={{ fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: 1, color: 'var(--brick)', fontFamily: "'IBM Plex Mono',monospace", margin: '18px 0 8px' }}>My deliveries</div>
        {mine.length === 0 ? (
          <div className="empty">You haven't claimed any deliveries yet.</div>
        ) : (
          mine.map((b) => {
            const cat = findCategory(b.categoryCode);
            const isSharing = sharingId === b.id;
            return (
              <div className="booking-row" key={b.id}>
                <div className="top">
                  <div><span className="ref">{b.reference}</span> · {cat ? cat.name : b.categoryCode}</div>
                  <span className={`status-chip status-${b.status}`}>{b.status}</span>
                </div>
                <div className="details">Shop: {b.shopName || '—'} · Deliver to: {b.details.address || '—'}</div>
                <div style={{ marginTop: 8 }}>
                  {!isSharing ? (
                    <button className="btn small" onClick={() => startSharing(b.id)}>Start sharing my location</button>
                  ) : (
                    <button className="btn small brick" onClick={stopSharing}>Stop sharing (arrived / done)</button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
