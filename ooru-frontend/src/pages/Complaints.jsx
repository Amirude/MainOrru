import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';

export default function Complaints() {
  const [complaints, setComplaints] = useState([]);
  const [form, setForm] = useState({ subject: '', description: '', bookingId: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function load() {
    try {
      const res = await apiClient.get('/complaints/mine');
      setComplaints(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  useEffect(() => { load(); }, []);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await apiClient.post('/complaints', {
        subject: form.subject,
        description: form.description,
        bookingId: form.bookingId ? Number(form.bookingId) : null,
      });
      setForm({ subject: '', description: '', bookingId: '' });
      setSuccess("Submitted — we'll get back to you here.");
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>Help &amp; support</h2>
        <p className="sub">Raise an issue about an order, a shop, or anything else — an admin will respond here.</p>
        {error && <div className="error-banner">{error}</div>}
        {success && <div className="success-banner">{success}</div>}
        <form onSubmit={submit}>
          <div className="field">
            <label>Subject</label>
            <input value={form.subject} onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value }))} required />
          </div>
          <div className="field">
            <label>Booking reference number (optional, if this is about a specific order)</label>
            <input type="number" value={form.bookingId} onChange={(e) => setForm((f) => ({ ...f, bookingId: e.target.value }))} placeholder="e.g. 42" />
          </div>
          <div className="field">
            <label>Describe the issue</label>
            <textarea rows={4} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} required />
          </div>
          <button className="btn full" type="submit">Submit</button>
        </form>
      </div>

      <div className="card">
        <h2>My tickets</h2>
        {complaints.length === 0 ? (
          <div className="empty">No tickets yet.</div>
        ) : (
          complaints.map((c) => (
            <div className="booking-row" key={c.id}>
              <div className="top">
                <div><strong>{c.subject}</strong>{c.bookingReference && <span className="mono" style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}> · {c.bookingReference}</span>}</div>
                <span className="status-chip status-ACCEPTED">{c.status}</span>
              </div>
              <div className="details">{c.description}</div>
              {c.adminResponse && <div style={{ marginTop: 8, fontSize: '0.85rem', color: 'var(--teal)' }}>Response: {c.adminResponse}</div>}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
