import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const GARMENTS = ['Shirt/Kurta', 'Pant/Trouser', 'Saree', 'Blouse', 'Other'];
const WORK_TYPES = ['New stitching', 'Alteration / fitting', 'Stretch / loosen', 'Repair'];

export default function TailorBooking() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [shops, setShops] = useState([]);
  const [selectedShop, setSelectedShop] = useState(null);
  const [slots, setSlots] = useState([]);
  const [selectedSlotId, setSelectedSlotId] = useState(null);
  const [form, setForm] = useState({ garment: GARMENTS[0], work: WORK_TYPES[0], notes: '' });
  const [error, setError] = useState('');
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  useEffect(() => {
    apiClient.get('/shops/by-category/tailor')
      .then((res) => setShops(res.data))
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);

  async function pickShop(shop) {
    setSelectedShop(shop);
    setSelectedSlotId(null);
    setError('');
    try {
      const res = await apiClient.get(`/shops/${shop.id}/slots`);
      setSlots(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!user) { navigate('/login'); return; }
    if (!selectedSlotId) { setError('Pick an available time slot first.'); return; }
    setError('');
    try {
      const res = await apiClient.post('/bookings', {
        categoryCode: 'tailor',
        shopId: selectedShop.id,
        slotId: selectedSlotId,
        details: { garment: form.garment, work: form.work, notes: form.notes },
      });
      setConfirmedBooking(res.data);
    } catch (err) {
      // If someone else grabbed the same slot a moment earlier, the backend says so here —
      // refresh the slot list so the customer picks a different one instead of retrying blindly.
      setError(extractErrorMessage(err));
      if (selectedShop) pickShop(selectedShop);
    }
  }

  function startOver() {
    setConfirmedBooking(null);
    setSelectedShop(null);
    setSelectedSlotId(null);
    setSlots([]);
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>🧵 Tailor</h2>
        <p className="sub">Pick a shop, then a real open time slot — no two customers can book the same slot.</p>
        {error && <div className="error-banner">{error}</div>}

        {!confirmedBooking && (
          !selectedShop ? (
            shops.length === 0 ? (
              <div className="empty">No approved tailor shops yet.</div>
            ) : (
              <div className="cat-grid">
                {shops.map((s) => (
                  <button key={s.id} className="cat-card" onClick={() => pickShop(s)}>
                    <span className="name">{s.shopName}</span>
                    <div style={{ fontSize: '0.8rem', color: 'var(--ink-soft)', marginTop: 4 }}>{s.address}</div>
                  </button>
                ))}
              </div>
            )
          ) : (
            <form onSubmit={handleSubmit}>
              <h3 style={{ marginBottom: 10 }}>{selectedShop.shopName}</h3>

              <div className="field">
                <label>Available time slots</label>
                {slots.length === 0 ? (
                  <div className="empty">No open slots right now — try another shop.</div>
                ) : (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {slots.map((s) => (
                      <button
                        type="button"
                        key={s.id}
                        onClick={() => setSelectedSlotId(s.id)}
                        className={`btn small ${selectedSlotId === s.id ? '' : 'outline'}`}
                      >
                        {s.date} · {s.startTime}–{s.endTime}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="field">
                <label>Garment</label>
                <select value={form.garment} onChange={(e) => setForm((f) => ({ ...f, garment: e.target.value }))}>
                  {GARMENTS.map((g) => <option key={g} value={g}>{g}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Work needed</label>
                <select value={form.work} onChange={(e) => setForm((f) => ({ ...f, work: e.target.value }))}>
                  {WORK_TYPES.map((w) => <option key={w} value={w}>{w}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Notes (measurements, fabric care, etc.)</label>
                <textarea rows={3} value={form.notes} onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))} />
              </div>

              <button className="btn full" type="submit" disabled={!selectedSlotId}>
                {user ? 'Book this slot' : 'Log in to book'}
              </button>
              <button className="btn outline full" type="button" style={{ marginTop: 8 }} onClick={() => setSelectedShop(null)}>
                ← Choose a different shop
              </button>
            </form>
          )
        )}

        {confirmedBooking && (
          <div>
            <div className="success-banner">
              Booked! Reference <span className="mono">{confirmedBooking.reference}</span> —
              {' '}{confirmedBooking.details.appointmentDate} at {confirmedBooking.details.appointmentTime}.
            </div>
            <p style={{ fontSize: '0.86rem', color: 'var(--ink-soft)' }}>
              You'll get a notification the moment your clothes are ready — then you can choose
              pickup or delivery from "My bookings".
            </p>
            <button className="btn full" onClick={startOver}>Book another</button>
          </div>
        )}
      </div>
    </div>
  );
}
