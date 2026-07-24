import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { findCategory } from '../categories';
import { useAuth } from '../context/AuthContext';
import { subscribeToMyBookings } from '../liveBookings';
import MapView from '../components/MapView';

function FulfillmentChoice({ booking, onChoose }) {
  const [showDeliveryAddress, setShowDeliveryAddress] = useState(false);
  const [address, setAddress] = useState('');

  return (
    <div style={{ marginTop: 10, paddingTop: 10, borderTop: '1.5px dashed var(--paper-deep)' }}>
      <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: 6 }}>Your clothes are ready — how would you like them?</div>
      {!showDeliveryAddress ? (
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn small" onClick={() => onChoose(booking.id, 'PICKUP')}>I'll pick up</button>
          <button className="btn small outline" onClick={() => setShowDeliveryAddress(true)}>Deliver to me</button>
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 8 }}>
          <input placeholder="Delivery address" value={address} onChange={(e) => setAddress(e.target.value)} />
          <button className="btn small" onClick={() => onChoose(booking.id, 'DELIVERY', address)} disabled={!address.trim()}>Confirm</button>
        </div>
      )}
    </div>
  );
}

function ReviewForm({ booking, onSubmitted }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit() {
    setSubmitting(true);
    setError('');
    try {
      await apiClient.post('/reviews', { bookingId: booking.id, rating, comment });
      onSubmitted();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ marginTop: 10, paddingTop: 10, borderTop: '1.5px dashed var(--paper-deep)' }}>
      <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: 6 }}>How was it? Leave a review for {booking.shopName}.</div>
      {error && <div className="error-banner">{error}</div>}
      <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => setRating(n)}
            style={{ background: 'none', border: 'none', fontSize: '1.3rem', opacity: n <= rating ? 1 : 0.3, padding: 0 }}
          >★</button>
        ))}
      </div>
      <textarea rows={2} placeholder="Optional comment" value={comment} onChange={(e) => setComment(e.target.value)} style={{ marginBottom: 8 }} />
      <button className="btn small" onClick={submit} disabled={submitting}>{submitting ? 'Submitting…' : 'Submit review'}</button>
    </div>
  );
}

export default function MyBookings() {
  const { user } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [liveNote, setLiveNote] = useState('');

  async function load() {
    setLoading(true);
    try {
      const res = await apiClient.get('/bookings/mine');
      setBookings(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  useEffect(() => {
    if (!user) return;
    const unsubscribe = subscribeToMyBookings(user.userId, (updated) => {
      setBookings((prev) => {
        const exists = prev.some((b) => b.id === updated.id);
        return exists ? prev.map((b) => (b.id === updated.id ? updated : b)) : [updated, ...prev];
      });
      setLiveNote(`${updated.reference} updated to ${updated.status}`);
      setTimeout(() => setLiveNote(''), 4000);
    });
    return unsubscribe;
  }, [user]);

  async function chooseFulfillment(bookingId, method, address) {
    setError('');
    try {
      await apiClient.patch(`/bookings/${bookingId}/fulfillment`, { method, address });
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>My bookings</h2>
        <p className="sub">Everything you've requested, and where it stands — updates live, no need to refresh.</p>
        {liveNote && <div className="success-banner">🔴 Live: {liveNote}</div>}
        {error && <div className="error-banner">{error}</div>}
        {loading ? (
          <>
            <div className="skeleton line" style={{ width: '60%' }} />
            <div className="skeleton block" />
            <div className="skeleton block" />
          </>
        ) : bookings.length === 0 ? (
          <div className="empty">No bookings yet — go book a service.</div>
        ) : (
          bookings.map((b) => {
            const cat = findCategory(b.categoryCode);
            return (
              <div className="booking-row" key={b.id}>
                <div className="top">
                  <div><span className="ref">{b.reference}</span> · {cat ? `${cat.icon} ${cat.name}` : b.categoryCode}</div>
                  <span className={`status-chip status-${b.status}`}>{b.status}</span>
                </div>
                <div className="details">
                  {Object.entries(b.details)
                    .filter(([k]) => !['items', 'totalPaise', 'itemsSubtotalPaise', 'deliveryFeePaise', 'surgeMultiplier', 'surgeExplanation'].includes(k))
                    .map(([k, v]) => `${k}: ${v}`).join(' · ')}
                  {Array.isArray(b.details.items) && (
                    <div>
                      {b.details.items.map((it) => `${it.name} ×${it.quantity}`).join(', ')} — items ₹{(b.details.itemsSubtotalPaise / 100).toFixed(2)} + delivery ₹{(b.details.deliveryFeePaise / 100).toFixed(2)} = ₹{(b.details.totalPaise / 100).toFixed(2)}
                    </div>
                  )}
                  {b.shopName && <div>Shop: {b.shopName}</div>}
                </div>
                {b.deliveryPartnerName && (
                  <div style={{ marginTop: 8, fontSize: '0.85rem', color: 'var(--ink-soft)' }}>
                    🛵 {b.deliveryPartnerName} is delivering this
                  </div>
                )}
                {b.deliveryLat && b.deliveryLng && (
                  <div style={{ marginTop: 8 }}>
                    <MapView markers={[{ lat: b.deliveryLat, lng: b.deliveryLng, label: b.deliveryPartnerName || 'Delivery partner' }]} height={200} />
                  </div>
                )}
                {b.categoryCode === 'tailor' && b.status === 'COMPLETED' && !b.details.fulfillmentMethod && (
                  <FulfillmentChoice booking={b} onChoose={chooseFulfillment} />
                )}
                {b.details.fulfillmentMethod && (
                  <div style={{ marginTop: 8, fontSize: '0.85rem', color: 'var(--teal)' }}>
                    {b.details.fulfillmentMethod === 'PICKUP' ? '✓ You chose pickup' : `✓ Delivery to: ${b.details.fulfillmentAddress}`}
                  </div>
                )}
                {b.status === 'COMPLETED' && (
                  <div style={{ marginTop: 8 }}>
                    <a href={`/receipt/${b.id}`} target="_blank" rel="noreferrer" style={{ fontSize: '0.84rem' }}>🧾 View receipt</a>
                  </div>
                )}
                {b.status === 'COMPLETED' && b.shopId && !b.hasReview && (
                  <ReviewForm booking={b} onSubmitted={load} />
                )}
                {b.hasReview && <div style={{ marginTop: 8, fontSize: '0.85rem', color: 'var(--ink-soft)' }}>✓ You reviewed this</div>}
              </div>
            );
          })
        )}
        <button className="btn outline small" style={{ marginTop: 10 }} onClick={load}>Refresh</button>
      </div>
    </div>
  );
}
