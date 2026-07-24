import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient, extractErrorMessage } from '../api/client';
import { findCategory } from '../categories';

export default function Receipt() {
  const { bookingId } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    apiClient.get(`/bookings/${bookingId}/receipt`)
      .then((res) => setData(res.data))
      .catch((err) => setError(extractErrorMessage(err)));
  }, [bookingId]);

  if (error) return <div className="wrap"><div className="error-banner">{error}</div></div>;
  if (!data) return <div className="wrap"><div className="empty">Loading…</div></div>;

  const { booking, payment } = data;
  const cat = findCategory(booking.categoryCode);

  return (
    <div className="wrap narrow">
      <div className="card" id="receipt-card">
        <h2>Receipt</h2>
        <p className="sub">{cat ? `${cat.icon} ${cat.name}` : booking.categoryCode} · <span className="mono">{booking.reference}</span></p>

        <div style={{ borderTop: '2px dashed var(--paper-deep)', paddingTop: 12, marginTop: 4 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', padding: '4px 0' }}>
            <span>Status</span><span>{booking.status}</span>
          </div>
          {booking.shopName && (
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', padding: '4px 0' }}>
              <span>Shop</span><span>{booking.shopName}</span>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', padding: '4px 0' }}>
            <span>Booked on</span><span>{new Date(booking.createdAt).toLocaleString()}</span>
          </div>
        </div>

        {Array.isArray(booking.details.items) && (
          <div style={{ borderTop: '2px dashed var(--paper-deep)', paddingTop: 12, marginTop: 12 }}>
            {booking.details.items.map((it, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', padding: '3px 0' }}>
                <span>{it.name} × {it.quantity}</span>
                <span className="mono">₹{(it.lineTotalPaise / 100).toFixed(2)}</span>
              </div>
            ))}
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.86rem', padding: '3px 0', color: 'var(--ink-soft)' }}>
              <span>Items subtotal</span><span className="mono">₹{(booking.details.itemsSubtotalPaise / 100).toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.86rem', padding: '3px 0', color: 'var(--ink-soft)' }}>
              <span>Delivery fee {booking.details.surgeMultiplier > 1 ? `(${booking.details.surgeMultiplier}× demand surge)` : ''}</span>
              <span className="mono">₹{(booking.details.deliveryFeePaise / 100).toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, borderTop: '2px solid var(--ink)', marginTop: 8, paddingTop: 8 }}>
              <span>Total</span><span className="mono">₹{(booking.details.totalPaise / 100).toFixed(2)}</span>
            </div>
          </div>
        )}

        <div style={{ borderTop: '2px dashed var(--paper-deep)', paddingTop: 12, marginTop: 12 }}>
          {payment ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', padding: '4px 0' }}>
                <span>Payment status</span><span>{payment.status}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '0.94rem', padding: '4px 0' }}>
                <span>Amount</span><span className="mono">₹{(payment.amountPaise / 100).toFixed(2)} {payment.currency}</span>
              </div>
              {payment.razorpayPaymentId && (
                <div style={{ fontSize: '0.76rem', color: 'var(--ink-soft)' }}>Payment ref: {payment.razorpayPaymentId}</div>
              )}
            </>
          ) : (
            <div style={{ fontSize: '0.86rem', color: 'var(--ink-soft)' }}>No payment recorded for this booking.</div>
          )}
        </div>

        <button className="btn full" style={{ marginTop: 18 }} onClick={() => window.print()}>Print / Save as PDF</button>
      </div>
    </div>
  );
}
