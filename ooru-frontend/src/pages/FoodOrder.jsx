import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) { resolve(true); return; }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function FoodOrder() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [category, setCategory] = useState('food'); // 'food' | 'grocery'
  const [shops, setShops] = useState([]);
  const [selectedShop, setSelectedShop] = useState(null);
  const [shopRating, setShopRating] = useState(null);
  const [menu, setMenu] = useState([]);
  const [cart, setCart] = useState({}); // menuItemId -> quantity
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  const [confirmedBooking, setConfirmedBooking] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('idle');
  const [frequentItems, setFrequentItems] = useState([]);
  const [surge, setSurge] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('ONLINE');

  async function loadShops(cat) {
    setError('');
    setSelectedShop(null);
    setMenu([]);
    setCart({});
    try {
      const res = await apiClient.get(`/shops/by-category/${cat}`);
      setShops(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => { loadShops(category); }, [category]);

  useEffect(() => {
    if (!user) return;
    apiClient.get('/bookings/mine/frequent-items')
      .then((res) => setFrequentItems(res.data))
      .catch(() => {});
  }, [user]);

  async function pickShop(shop) {
    setSelectedShop(shop);
    setCart({});
    setShopRating(null);
    setSurge(null);
    try {
      const [menuRes, reviewsRes, surgeRes] = await Promise.all([
        apiClient.get(`/shops/${shop.id}/menu`),
        apiClient.get(`/shops/${shop.id}/reviews`),
        apiClient.get('/pricing/surge', { params: { categoryCode: category } }),
      ]);
      setMenu(menuRes.data);
      setShopRating(reviewsRes.data);
      setSurge(surgeRes.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function orderAgain(item) {
    setCategory(item.categoryCode);
    await pickShop({ id: item.shopId, shopName: item.shopName });
    setCart({ [item.menuItemId]: 1 });
  }

  function changeQty(menuItemId, delta) {
    setCart((c) => {
      const next = { ...c, [menuItemId]: Math.max(0, (c[menuItemId] || 0) + delta) };
      return next;
    });
  }

  const cartLines = menu.filter((m) => cart[m.id] > 0);
  const cartTotalPaise = cartLines.reduce((sum, m) => sum + m.pricePaise * cart[m.id], 0);

  function getMyLocation() {
    return new Promise((resolve) => {
      if (!navigator.geolocation) { resolve(null); return; }
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => resolve(null),
        { timeout: 5000 }
      );
    });
  }

  async function handleCheckout() {
    if (!user) { navigate('/login'); return; }
    if (!address.trim()) { setError('Enter a delivery address.'); return; }
    if (cartLines.length === 0) { setError('Your cart is empty.'); return; }
    setError('');
    try {
      // Real distance-based delivery fee needs your actual location — if you decline, the
      // backend falls back to a flat estimate instead of failing the order.
      const myLocation = await getMyLocation();
      const res = await apiClient.post('/bookings', {
        categoryCode: category,
        shopId: selectedShop.id,
        details: { address },
        items: cartLines.map((m) => ({ menuItemId: m.id, quantity: cart[m.id] })),
        customerLat: myLocation?.lat,
        customerLng: myLocation?.lng,
        paymentMethod,
      });
      setConfirmedBooking(res.data);
      if (paymentMethod === 'COD') {
        await apiClient.post('/payments/cod', { bookingId: res.data.id, amountPaise: res.data.details.totalPaise });
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handlePayNow() {
    setPaymentStatus('processing');
    const scriptOk = await loadRazorpayScript();
    if (!scriptOk) {
      setPaymentStatus('failed');
      setError('Could not load the payment gateway.');
      return;
    }
    try {
      const amountPaise = confirmedBooking.details.totalPaise;
      const orderRes = await apiClient.post('/payments/create-order', {
        bookingId: confirmedBooking.id,
        amountPaise,
      });
      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID || '',
        amount: orderRes.data.amountPaise,
        currency: orderRes.data.currency,
        order_id: orderRes.data.razorpayOrderId,
        name: 'Altenul One',
        description: `${selectedShop.shopName} — ${confirmedBooking.reference}`,
        handler: async function (response) {
          try {
            await apiClient.post('/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            setPaymentStatus('paid');
          } catch (err) {
            setPaymentStatus('failed');
            setError(extractErrorMessage(err));
          }
        },
        prefill: { name: user.name },
        theme: { color: '#0E6E6B' },
      };
      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', () => setPaymentStatus('failed'));
      rzp.open();
      setPaymentStatus('idle');
    } catch (err) {
      setPaymentStatus('failed');
      setError(extractErrorMessage(err));
    }
  }

  function startOver() {
    setConfirmedBooking(null);
    setPaymentStatus('idle');
    setCart({});
    setSelectedShop(null);
    setMenu([]);
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>Food &amp; Grocery</h2>
        <p className="sub">Prices shown here always come from the shop's own menu — the total is computed server-side, never trusted from the cart in your browser.</p>

        {error && <div className="error-banner">{error}</div>}

        {!confirmedBooking && !selectedShop && frequentItems.length > 0 && (
          <div style={{ marginBottom: 18 }}>
            <div className="section-title" style={{ fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: 1, color: 'var(--brick)', fontFamily: "'IBM Plex Mono',monospace", marginBottom: 8 }}>Order again</div>
            <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}>
              {frequentItems.map((it) => (
                <button
                  key={it.menuItemId}
                  onClick={() => orderAgain(it)}
                  className="cat-card"
                  style={{ minWidth: 140, flexShrink: 0, textAlign: 'left' }}
                >
                  {it.imageUrl && <img src={it.imageUrl} alt={it.name} style={{ width: '100%', height: 60, objectFit: 'cover', borderRadius: 6, marginBottom: 6 }} />}
                  <div style={{ fontWeight: 600, fontSize: '0.88rem' }}>{it.name}</div>
                  <div style={{ fontSize: '0.76rem', color: 'var(--ink-soft)' }}>{it.shopName} · ordered {it.timesOrdered}×</div>
                </button>
              ))}
            </div>
          </div>
        )}

        {!confirmedBooking && (
          <>
            <div className="field-row" style={{ marginBottom: 16 }}>
              <button className={`btn ${category === 'food' ? '' : 'outline'}`} onClick={() => setCategory('food')}>🍲 Food</button>
              <button className={`btn ${category === 'grocery' ? '' : 'outline'}`} onClick={() => setCategory('grocery')}>🛒 Grocery</button>
              <button className={`btn ${category === 'fashion' ? '' : 'outline'}`} onClick={() => setCategory('fashion')}>👗 Fashion</button>
            </div>

            {!selectedShop ? (
              shops.length === 0 ? (
                <div className="empty">No approved shops in this category yet.</div>
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
              <>
                <h3 style={{ marginBottom: 4 }}>{selectedShop.shopName}</h3>
                {shopRating && (
                  <div style={{ fontSize: '0.84rem', color: 'var(--ink-soft)', marginBottom: 10 }}>
                    {shopRating.averageRating ? `★ ${shopRating.averageRating.toFixed(1)} (${shopRating.reviewCount} review${shopRating.reviewCount === 1 ? '' : 's'})` : 'No reviews yet'}
                  </div>
                )}
                {menu.length === 0 ? (
                  <div className="empty">This shop hasn't added a menu yet.</div>
                ) : (
                  menu.map((m) => (
                    <div key={m.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #ecdfb8' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        {m.imageUrl && <img src={m.imageUrl} alt={m.name} style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 8 }} />}
                        <div>
                          <div style={{ fontWeight: 600 }}>{m.name}</div>
                          <div className="mono" style={{ fontSize: '0.82rem', color: 'var(--ink-soft)' }}>₹{(m.pricePaise / 100).toFixed(2)}</div>
                        </div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <button className="btn small outline" onClick={() => changeQty(m.id, -1)}>−</button>
                        <span className="mono">{cart[m.id] || 0}</span>
                        <button className="btn small outline" onClick={() => changeQty(m.id, 1)}>+</button>
                      </div>
                    </div>
                  ))
                )}

                {cartLines.length > 0 && (
                  <div style={{ marginTop: 16 }}>
                    {cartLines.map((m) => (
                      <div key={m.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.86rem', padding: '2px 0' }}>
                        <span>{m.name} × {cart[m.id]}</span>
                        <span className="mono">₹{((m.pricePaise * cart[m.id]) / 100).toFixed(2)}</span>
                      </div>
                    ))}
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.86rem', padding: '2px 0', color: 'var(--ink-soft)' }}>
                      <span>Delivery fee {surge && surge.multiplier > 1 ? `(demand surge ${surge.multiplier}×)` : ''}</span>
                      <span className="mono">{surge ? `≈₹${((25 * surge.multiplier)).toFixed(2)}` : '…'}</span>
                    </div>
                    {surge && surge.multiplier > 1 && (
                      <div style={{ fontSize: '0.74rem', color: 'var(--brick)', marginBottom: 4 }}>{surge.explanation}</div>
                    )}
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, borderTop: '2px solid var(--ink)', marginTop: 8, paddingTop: 8 }}>
                      <span>Total (delivery fee confirmed at checkout)</span><span className="mono">≈₹{(cartTotalPaise / 100 + (surge ? 25 * surge.multiplier : 25)).toFixed(2)}</span>
                    </div>
                    <div className="field" style={{ marginTop: 14 }}>
                      <label>Delivery address</label>
                      <input value={address} onChange={(e) => setAddress(e.target.value)} required />
                    </div>
                    <div className="field" style={{ marginTop: 10 }}>
                      <label>Payment method</label>
                      <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                        <option value="ONLINE">Pay online now</option>
                        <option value="COD">Cash on delivery</option>
                      </select>
                    </div>
                    <button className="btn full" onClick={handleCheckout}>
                      {user ? 'Place order' : 'Log in to order'}
                    </button>
                  </div>
                )}
                <button className="btn outline small" style={{ marginTop: 12 }} onClick={() => setSelectedShop(null)}>← Choose a different shop</button>
              </>
            )}
          </>
        )}

        {confirmedBooking && (
          <div>
            <div className="success-banner">
              Order placed! Reference <span className="mono">{confirmedBooking.reference}</span>
              <div style={{ marginTop: 6, fontSize: '0.86rem' }}>
                Items ₹{(confirmedBooking.details.itemsSubtotalPaise / 100).toFixed(2)}
                {' + Delivery ₹'}{(confirmedBooking.details.deliveryFeePaise / 100).toFixed(2)}
                {confirmedBooking.details.surgeMultiplier > 1 && ` (${confirmedBooking.details.surgeMultiplier}× surge)`}
                {confirmedBooking.details.gstPaise > 0 && ` + GST ₹${(confirmedBooking.details.gstPaise / 100).toFixed(2)}`}
                {confirmedBooking.details.handlingFeePaise > 0 && ` + Handling ₹${(confirmedBooking.details.handlingFeePaise / 100).toFixed(2)}`}
                {' '}= <strong>₹{(confirmedBooking.details.totalPaise / 100).toFixed(2)}</strong>
              </div>
            </div>
            {confirmedBooking.details.paymentMethod === 'COD' ? (
              <div className="success-banner">Pay ₹{(confirmedBooking.details.totalPaise / 100).toFixed(2)} in cash when it arrives.</div>
            ) : paymentStatus === 'paid' ? (
              <div className="success-banner">Payment received — verified server-side.</div>
            ) : (
              <button className="btn brick full" onClick={handlePayNow} disabled={paymentStatus === 'processing'}>
                {paymentStatus === 'processing' ? 'Opening payment…' : `Pay ₹${(confirmedBooking.details.totalPaise / 100).toFixed(2)} now`}
              </button>
            )}
            <button className="btn outline full" style={{ marginTop: 10 }} onClick={startOver}>Order something else</button>
          </div>
        )}
      </div>
    </div>
  );
}
