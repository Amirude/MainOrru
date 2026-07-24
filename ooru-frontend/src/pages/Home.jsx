import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

// Indicative BASE rates only — the actual charge also factors in real-time demand surge from
// the backend (see PricingService). Categories not listed here just don't have a payment step.
const ESTIMATE_RUPEES = { tailor: 300, xerox: 50, ac: 799, plumber: 400, electrician: 350 };

// Categories with their own dedicated page (real appointment slots, or a cart+menu) rather than
// this generic field-based form — they still come from the same /api/categories list, so we
// just skip rendering a card for them here.
const DEDICATED_PAGE_CODES = new Set(['tailor', 'food', 'grocery']);

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

export default function Home() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [activeCategory, setActiveCategory] = useState(null);
  const [formValues, setFormValues] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [confirmedBooking, setConfirmedBooking] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('idle');
  const [lastSurge, setLastSurge] = useState(null);

  useEffect(() => {
    apiClient.get('/categories')
      .then((res) => setCategories(res.data.filter((c) => !DEDICATED_PAGE_CODES.has(c.code))))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoadingCategories(false));
  }, []);

  function openCategory(cat) {
    setActiveCategory(cat);
    setFormValues({});
    setError('');
    setConfirmedBooking(null);
    setPaymentStatus('idle');
  }
  function closeModal() {
    setActiveCategory(null);
  }
  function updateField(id, value) {
    setFormValues((f) => ({ ...f, [id]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!user) {
      navigate('/login');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      const res = await apiClient.post('/bookings', {
        categoryCode: activeCategory.code,
        details: formValues,
      });
      setConfirmedBooking(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePayNow() {
    setPaymentStatus('processing');
    const scriptOk = await loadRazorpayScript();
    if (!scriptOk) {
      setPaymentStatus('failed');
      setError('Could not load the payment gateway. Check your connection and try again.');
      return;
    }
    try {
      const surgeRes = await apiClient.get('/pricing/surge', { params: { categoryCode: activeCategory.code } });
      const surge = surgeRes.data;
      setLastSurge(surge);
      const amountPaise = Math.round(ESTIMATE_RUPEES[activeCategory.code] * 100 * surge.multiplier);
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
        description: `${activeCategory.displayName} — ${confirmedBooking.reference}`,
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

  return (
    <div className="wrap">
      <div style={{ background: 'var(--ink)', borderRadius: 12, padding: '16px 20px', marginBottom: 16, display: 'flex', gap: 16, overflowX: 'auto' }}>
        <div style={{ color: 'var(--yellow)', fontFamily: "'Baloo 2',sans-serif", fontWeight: 700, whiteSpace: 'nowrap' }}>🎉 Flat ₹50 off your first Food order</div>
        <div style={{ color: '#c7cee2', whiteSpace: 'nowrap' }}>·</div>
        <div style={{ color: 'var(--yellow)', fontFamily: "'Baloo 2',sans-serif", fontWeight: 700, whiteSpace: 'nowrap' }}>⚡ Free delivery on Grocery orders over ₹499</div>
        <div style={{ color: '#c7cee2', whiteSpace: 'nowrap' }}>·</div>
        <div style={{ color: 'var(--yellow)', fontFamily: "'Baloo 2',sans-serif", fontWeight: 700, whiteSpace: 'nowrap' }}>👗 New: Fashion &amp; Accessories now live</div>
      </div>
      <div className="card">
        <h2>Book a service</h2>
        <p className="sub">Every category here — including any an admin adds later — comes straight from the database, not hardcoded pages.</p>
        {error && <div className="error-banner">{error}</div>}
        {loadingCategories ? (
          <div className="cat-grid">
            {[...Array(6)].map((_, i) => <div key={i} className="skeleton block" />)}
          </div>
        ) : (
          <div className="cat-grid fade-in">
            {categories.map((cat) => (
              <button key={cat.code} className="cat-card" onClick={() => openCategory(cat)}>
                <span className="icon">{cat.icon || '🔧'}</span>
                <span className="name">{cat.displayName}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {activeCategory && (
        <div className="overlay" onClick={(e) => e.target === e.currentTarget && closeModal()}>
          <div className="modal">
            <div className="modal-head">
              <h2>{activeCategory.icon} {activeCategory.displayName}</h2>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {!confirmedBooking ? (
              <>
                {!user && <div className="error-banner">You'll need to log in before booking — you can still fill this in first.</div>}
                {error && <div className="error-banner">{error}</div>}
                <form onSubmit={handleSubmit}>
                  {activeCategory.fields.map((f) => (
                    <div className="field" key={f.id}>
                      <label>{f.label}</label>
                      {f.type === 'select' ? (
                        <select required onChange={(e) => updateField(f.id, e.target.value)} defaultValue="">
                          <option value="" disabled>Choose one</option>
                          {f.options.map((o) => <option key={o} value={o}>{o}</option>)}
                        </select>
                      ) : (
                        <input type={f.type} required onChange={(e) => updateField(f.id, e.target.value)} />
                      )}
                    </div>
                  ))}
                  <button className="btn full" type="submit" disabled={submitting}>
                    {submitting ? 'Sending…' : (user ? 'Send request' : 'Log in to continue')}
                  </button>
                </form>
              </>
            ) : (
              <div>
                <div className="success-banner">
                  Booked! Reference <span className="mono">{confirmedBooking.reference}</span>. Status: {confirmedBooking.status}.
                </div>
                {error && <div className="error-banner">{error}</div>}
                {paymentStatus === 'paid' ? (
                  <div className="success-banner">Payment received — verified server-side, not just trusted from the browser.</div>
                ) : ESTIMATE_RUPEES[activeCategory.code] ? (
                  <>
                    <p style={{ fontSize: '0.86rem', color: 'var(--ink-soft)' }}>
                      Base estimate: ₹{ESTIMATE_RUPEES[activeCategory.code]}
                      {lastSurge && lastSurge.multiplier > 1 && ` — current demand surge ${lastSurge.multiplier}× (${lastSurge.explanation})`}.
                      This calls the real Razorpay checkout widget — use Razorpay's test card numbers if your key is in test mode.
                    </p>
                    <button className="btn brick full" onClick={handlePayNow} disabled={paymentStatus === 'processing'}>
                      {paymentStatus === 'processing' ? 'Checking current price…' : `Pay now (₹${ESTIMATE_RUPEES[activeCategory.code]} base + demand pricing)`}
                    </button>
                  </>
                ) : (
                  <p style={{ fontSize: '0.86rem', color: 'var(--ink-soft)' }}>
                    No payment needed right now for this category — a shop or dispatcher will follow up.
                  </p>
                )}
                <button className="btn outline full" style={{ marginTop: 10 }} onClick={closeModal}>Done</button>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="card">
        <h2>About Altenul One</h2>
        <p className="sub" style={{ marginBottom: 0 }}>
          A local-services platform bringing tailors, delivery, retail, and everyday errands
          onto one app. Replace this paragraph with your real company description.
        </p>
      </div>
    </div>
  );
}
