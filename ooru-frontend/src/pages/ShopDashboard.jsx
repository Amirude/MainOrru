import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { findCategory, CART_CATEGORIES } from '../categories';
const CART_BASED_CODES = new Set(CART_CATEGORIES.map((c) => c.code));

export default function ShopDashboard() {
  const [shops, setShops] = useState([]);
  const [selectedShopId, setSelectedShopId] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [menu, setMenu] = useState([]);
  const [menuForm, setMenuForm] = useState({ name: '', priceRupees: '', imageUrl: '' });
  const [slots, setSlots] = useState([]);
  const [slotForm, setSlotForm] = useState({ date: '', startTime: '', endTime: '' });
  const [error, setError] = useState('');
  const [showRegisterForm, setShowRegisterForm] = useState(false);
  const [categoryOptions, setCategoryOptions] = useState([]);
  const [billing, setBilling] = useState({ gstPercent: '', handlingFeeRupees: '' });
  const [form, setForm] = useState({ shopName: '', categoryCode: '', address: '' });

  async function loadShops() {
    try {
      const res = await apiClient.get('/shops/mine');
      setShops(res.data);
      if (res.data.length > 0 && !selectedShopId) setSelectedShopId(res.data[0].id);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function loadBookings(shopId) {
    if (!shopId) return;
    try {
      const res = await apiClient.get(`/bookings/shop/${shopId}`);
      setBookings(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function loadMenu(shopId) {
    if (!shopId) { setMenu([]); return; }
    try {
      const res = await apiClient.get(`/shops/${shopId}/menu`);
      setMenu(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    loadShops();
    apiClient.get('/categories')
      .then((res) => {
        setCategoryOptions(res.data);
        if (res.data.length > 0) setForm((f) => ({ ...f, categoryCode: res.data[0].code }));
      })
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);
  useEffect(() => { loadBookings(selectedShopId); loadMenu(selectedShopId); loadSlots(selectedShopId); }, [selectedShopId]);

  async function loadSlots(shopId) {
    if (!shopId) { setSlots([]); return; }
    try {
      const res = await apiClient.get(`/shops/${shopId}/slots`);
      setSlots(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddSlot(e) {
    e.preventDefault();
    setError('');
    try {
      await apiClient.post(`/shops/${selectedShopId}/slots`, slotForm);
      setSlotForm({ date: '', startTime: '', endTime: '' });
      loadSlots(selectedShopId);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function saveBillingSettings(e) {
    e.preventDefault();
    setError('');
    try {
      await apiClient.patch(`/shops/${selectedShopId}/billing-settings`, {
        gstPercent: billing.gstPercent ? parseFloat(billing.gstPercent) : null,
        handlingFeePaise: billing.handlingFeeRupees ? Math.round(parseFloat(billing.handlingFeeRupees) * 100) : null,
      });
      loadShops();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddMenuItem(e) {
    e.preventDefault();
    setError('');
    try {
      const pricePaise = Math.round(parseFloat(menuForm.priceRupees) * 100);
      await apiClient.post(`/shops/${selectedShopId}/menu`, { name: menuForm.name, pricePaise, imageUrl: menuForm.imageUrl || null });
      setMenuForm({ name: '', priceRupees: '', imageUrl: '' });
      loadMenu(selectedShopId);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleRegisterShop(e) {
    e.preventDefault();
    setError('');
    try {
      await apiClient.post('/shops/register', form);
      setShowRegisterForm(false);
      setForm({ shopName: '', categoryCode: CATEGORIES[0].code, address: '' });
      loadShops();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function updateStatus(bookingId, status) {
    try {
      await apiClient.patch(`/bookings/${bookingId}/status`, { status });
      loadBookings(selectedShopId);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  const selectedShop = shops.find((s) => s.id === selectedShopId);

  return (
    <div className="wrap">
      <div className="card">
        <h2>Shop dashboard</h2>
        <p className="sub">Register a shop, wait for admin approval, then manage bookings once it's live.</p>
        {error && <div className="error-banner">{error}</div>}

        {shops.length === 0 && !showRegisterForm && (
          <div className="empty">
            You don't have a shop yet.
            <div style={{ marginTop: 12 }}><button className="btn" onClick={() => setShowRegisterForm(true)}>Register a shop</button></div>
          </div>
        )}

        {shops.length > 0 && (
          <div className="field">
            <label>Your shops</label>
            <select value={selectedShopId || ''} onChange={(e) => setSelectedShopId(Number(e.target.value))}>
              {shops.map((s) => (
                <option key={s.id} value={s.id}>{s.shopName} — {s.status}</option>
              ))}
            </select>
            <button className="btn outline small" style={{ marginTop: 8 }} onClick={() => setShowRegisterForm(true)}>+ Register another shop</button>
          </div>
        )}

        {showRegisterForm && (
          <form onSubmit={handleRegisterShop} style={{ marginTop: 14, borderTop: '2px dashed var(--paper-deep)', paddingTop: 14 }}>
            <div className="field">
              <label>Shop name</label>
              <input value={form.shopName} onChange={(e) => setForm((f) => ({ ...f, shopName: e.target.value }))} required />
            </div>
            <div className="field">
              <label>Category</label>
              <select value={form.categoryCode} onChange={(e) => setForm((f) => ({ ...f, categoryCode: e.target.value }))}>
                {categoryOptions.map((c) => <option key={c.code} value={c.code}>{c.icon} {c.displayName}</option>)}
              </select>
            </div>
            <div className="field">
              <label>Address</label>
              <input value={form.address} onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))} required />
            </div>
            <button className="btn" type="submit">Submit for approval</button>
            <button className="btn outline" type="button" style={{ marginLeft: 8 }} onClick={() => setShowRegisterForm(false)}>Cancel</button>
          </form>
        )}
      </div>

      {selectedShop && selectedShop.status !== 'APPROVED' && (
        <div className="card">
          <p className="sub" style={{ marginBottom: 0 }}>
            This shop is <strong>{selectedShop.status}</strong>. Bookings only start arriving once an admin approves it.
          </p>
        </div>
      )}

      {selectedShop && selectedShop.status === 'APPROVED' && selectedShop.categoryCode === 'tailor' && (
        <div className="card">
          <h2>Time slots</h2>
          <p className="sub">Customers can only book a slot listed here — once taken, it disappears for everyone else.</p>
          {slots.length === 0 ? (
            <div className="empty">No open slots yet.</div>
          ) : (
            slots.map((s) => (
              <div key={s.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #ecdfb8' }}>
                <span>{s.date}</span>
                <span className="mono">{s.startTime} – {s.endTime}</span>
              </div>
            ))
          )}
          <form onSubmit={handleAddSlot} style={{ marginTop: 14, display: 'flex', gap: 8, alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Date</label>
              <input type="date" value={slotForm.date} onChange={(e) => setSlotForm((f) => ({ ...f, date: e.target.value }))} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Start time</label>
              <input type="time" value={slotForm.startTime} onChange={(e) => setSlotForm((f) => ({ ...f, startTime: e.target.value }))} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>End time</label>
              <input type="time" value={slotForm.endTime} onChange={(e) => setSlotForm((f) => ({ ...f, endTime: e.target.value }))} required />
            </div>
            <button className="btn" type="submit">Open slot</button>
          </form>
        </div>
      )}

      {selectedShop && selectedShop.status === 'APPROVED' && CART_BASED_CODES.has(selectedShop.categoryCode) && (
        <div className="card">
          <h2>Billing settings</h2>
          <p className="sub">Optional — leave blank to charge neither. Applied to every order from this shop.</p>
          <form onSubmit={saveBillingSettings} style={{ display: 'flex', gap: 10, alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>GST %</label>
              <input type="number" step="0.1" min="0" value={billing.gstPercent} onChange={(e) => setBilling((b) => ({ ...b, gstPercent: e.target.value }))} placeholder="e.g. 5" />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Handling fee (₹)</label>
              <input type="number" step="0.5" min="0" value={billing.handlingFeeRupees} onChange={(e) => setBilling((b) => ({ ...b, handlingFeeRupees: e.target.value }))} placeholder="e.g. 5" />
            </div>
            <button className="btn" type="submit">Save</button>
          </form>
        </div>
      )}

      {selectedShop && selectedShop.status === 'APPROVED' && CART_BASED_CODES.has(selectedShop.categoryCode) && (
        <div className="card">
          <h2>Menu</h2>
          <p className="sub">Customers can only order items listed here, at the price you set here.</p>
          {menu.length === 0 ? (
            <div className="empty">No items yet.</div>
          ) : (
            menu.map((m) => (
              <div key={m.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid #ecdfb8' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  {m.imageUrl && <img src={m.imageUrl} alt={m.name} style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 6 }} />}
                  <span>{m.name}</span>
                </div>
                <span className="mono">₹{(m.pricePaise / 100).toFixed(2)}</span>
              </div>
            ))
          )}
          <form onSubmit={handleAddMenuItem} style={{ marginTop: 14, display: 'flex', gap: 8, alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="field" style={{ flex: 2, marginBottom: 0, minWidth: 140 }}>
              <label>Item name</label>
              <input value={menuForm.name} onChange={(e) => setMenuForm((f) => ({ ...f, name: e.target.value }))} required />
            </div>
            <div className="field" style={{ flex: 1, marginBottom: 0, minWidth: 90 }}>
              <label>Price (₹)</label>
              <input type="number" step="0.01" min="0" value={menuForm.priceRupees} onChange={(e) => setMenuForm((f) => ({ ...f, priceRupees: e.target.value }))} required />
            </div>
            <div className="field" style={{ flex: 2, marginBottom: 0, minWidth: 160 }}>
              <label>Image URL (optional)</label>
              <input type="url" placeholder="https://…" value={menuForm.imageUrl} onChange={(e) => setMenuForm((f) => ({ ...f, imageUrl: e.target.value }))} />
            </div>
            <button className="btn" type="submit">Add</button>
          </form>
        </div>
      )}

      {selectedShop && selectedShop.status === 'APPROVED' && (
        <div className="card">
          <h2>Bookings for {selectedShop.shopName}</h2>
          {bookings.length === 0 ? (
            <div className="empty">No bookings yet.</div>
          ) : (
            bookings.map((b) => {
              const cat = findCategory(b.categoryCode);
              return (
                <div className="booking-row" key={b.id}>
                  <div className="top">
                    <div><span className="ref">{b.reference}</span> · {cat ? cat.name : b.categoryCode}</div>
                    <span className={`status-chip status-${b.status}`}>{b.status}</span>
                  </div>
                  <div className="details">{Object.entries(b.details).map(([k, v]) => `${k}: ${v}`).join(' · ')}</div>
                  <div className="actions">
                    {b.status === 'REQUESTED' && <>
                      <button className="btn small" onClick={() => updateStatus(b.id, 'ACCEPTED')}>Accept</button>
                      <button className="btn small brick" onClick={() => updateStatus(b.id, 'REJECTED')}>Reject</button>
                    </>}
                    {b.status === 'ACCEPTED' && <button className="btn small" onClick={() => updateStatus(b.id, 'IN_PROGRESS')}>Start job</button>}
                    {b.status === 'IN_PROGRESS' && <button className="btn small" onClick={() => updateStatus(b.id, 'COMPLETED')}>Mark completed</button>}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
