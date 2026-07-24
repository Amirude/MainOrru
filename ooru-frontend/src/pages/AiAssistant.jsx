import { useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const ESTIMATE_RUPEES = { tailor: 300, xerox: 50, ac: 799, plumber: 400, electrician: 350, scrap: 0 };

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

export default function AiAssistant() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Tell me what you need — for example, "book AC service tomorrow at 4pm" or "pick up my old clothes for donation."' },
  ]);
  const [input, setInput] = useState('');
  const [thinking, setThinking] = useState(false);
  const [state, setState] = useState({ category: null, data: {} });
  const [confirmedBooking, setConfirmedBooking] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('idle');
  const [error, setError] = useState('');

  async function handleSend() {
    if (!user) { navigate('/login'); return; }
    const text = input.trim();
    if (!text) return;
    setInput('');
    setMessages((m) => [...m, { role: 'user', text }]);
    setThinking(true);
    setError('');
    try {
      const res = await apiClient.post('/assistant/chat', { state, message: text });
      const result = res.data;
      const newState = { category: result.category || state.category, data: result.data || state.data };
      setState(newState);
      setMessages((m) => [...m, { role: 'assistant', text: result.reply }]);
      if (result.ready && newState.category) {
        setMessages((m) => [...m, { role: 'preview', category: newState.category, data: newState.data }]);
      }
    } catch (err) {
      setMessages((m) => [...m, { role: 'assistant', text: 'I had trouble reaching the assistant just now — could you try again?' }]);
      setError(extractErrorMessage(err));
    } finally {
      setThinking(false);
    }
  }

  async function handleConfirm(category, data) {
    setError('');
    try {
      const res = await apiClient.post('/assistant/confirm', { category, data });
      setConfirmedBooking(res.data);
      setMessages((m) => m.filter((msg) => msg.role !== 'preview'));
      setMessages((m) => [...m, { role: 'assistant', text: `Booked! Reference ${res.data.reference}.` }]);
      setState({ category: null, data: {} });
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handlePayNow() {
    setPaymentStatus('processing');
    const scriptOk = await loadRazorpayScript();
    if (!scriptOk) { setPaymentStatus('failed'); return; }
    try {
      const surgeRes = await apiClient.get('/pricing/surge', { params: { categoryCode: confirmedBooking.categoryCode } });
      const amountPaise = Math.round(ESTIMATE_RUPEES[confirmedBooking.categoryCode] * 100 * surgeRes.data.multiplier);
      const orderRes = await apiClient.post('/payments/create-order', { bookingId: confirmedBooking.id, amountPaise });
      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID || '',
        amount: orderRes.data.amountPaise,
        currency: orderRes.data.currency,
        order_id: orderRes.data.razorpayOrderId,
        name: 'Altenul One',
        description: confirmedBooking.reference,
        handler: async (response) => {
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
      new window.Razorpay(options).open();
      setPaymentStatus('idle');
    } catch (err) {
      setPaymentStatus('failed');
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>🤖 Altenul One Assistant</h2>
        <p className="sub">
          This calls Claude from the backend, not the browser — the API key never touches the
          frontend. Confirming a booking here creates the exact same kind of booking the manual
          form does.
        </p>
        {error && <div className="error-banner">{error}</div>}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 14, maxHeight: '50vh', overflowY: 'auto' }}>
          {messages.map((m, i) => {
            if (m.role === 'preview') {
              return (
                <div key={i} className="card" style={{ margin: 0, borderColor: 'var(--brick)' }}>
                  <h3 style={{ fontSize: '1rem' }}>Ready to confirm — {m.category}</h3>
                  {Object.entries(m.data).map(([k, v]) => (
                    <div key={k} style={{ fontSize: '0.85rem', color: 'var(--ink-soft)' }}>{k}: {v}</div>
                  ))}
                  <button className="btn brick" style={{ marginTop: 10 }} onClick={() => handleConfirm(m.category, m.data)}>Confirm booking</button>
                </div>
              );
            }
            return (
              <div key={i} style={{
                alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start',
                background: m.role === 'user' ? 'var(--yellow)' : 'var(--teal-soft)',
                border: m.role === 'user' ? 'none' : '1.5px solid var(--teal)',
                padding: '8px 14px', borderRadius: 12, maxWidth: '80%', fontSize: '0.92rem',
              }}>
                {m.text}
              </div>
            );
          })}
          {thinking && <div style={{ fontStyle: 'italic', color: 'var(--ink-soft)', fontSize: '0.88rem' }}>thinking…</div>}
        </div>

        {confirmedBooking && (
          <div className="success-banner">
            Reference <span className="mono">{confirmedBooking.reference}</span>.
            {ESTIMATE_RUPEES[confirmedBooking.categoryCode] ? (
              paymentStatus === 'paid' ? ' Payment verified.' : (
                <button className="btn brick small" style={{ marginLeft: 10 }} onClick={handlePayNow} disabled={paymentStatus === 'processing'}>
                  {paymentStatus === 'processing' ? 'Opening…' : `Pay ₹${ESTIMATE_RUPEES[confirmedBooking.categoryCode]}`}
                </button>
              )
            ) : ' No payment needed right now.'}
          </div>
        )}

        <div style={{ display: 'flex', gap: 8 }}>
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder={user ? 'Tell me what you need…' : 'Log in to use the assistant'}
          />
          <button className="btn" onClick={handleSend} disabled={thinking}>Send</button>
        </div>
      </div>
    </div>
  );
}
