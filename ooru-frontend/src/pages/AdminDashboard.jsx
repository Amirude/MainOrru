import { useEffect, useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';
import { findCategory } from '../categories';

function AnalyticsPanel() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  async function load() {
    try {
      const res = await apiClient.get('/admin/analytics');
      setData(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  useEffect(() => { load(); }, []);

  if (error) return <div className="error-banner">{error}</div>;
  if (!data) return <div className="empty">Loading…</div>;

  return (
    <div className="card">
      <h2>Analytics</h2>
      <p className="sub">Real counts from the database — nothing predicted or modeled.</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(140px,1fr))', gap: 12, marginBottom: 16 }}>
        <div style={{ background: 'var(--teal-soft)', borderRadius: 8, padding: 12, textAlign: 'center' }}>
          <div className="mono" style={{ fontSize: '1.4rem', fontWeight: 700 }}>{data.totalUsers}</div>
          <div style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>Users</div>
        </div>
        <div style={{ background: 'var(--teal-soft)', borderRadius: 8, padding: 12, textAlign: 'center' }}>
          <div className="mono" style={{ fontSize: '1.4rem', fontWeight: 700 }}>{data.totalShops}</div>
          <div style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>Shops</div>
        </div>
        <div style={{ background: 'var(--teal-soft)', borderRadius: 8, padding: 12, textAlign: 'center' }}>
          <div className="mono" style={{ fontSize: '1.4rem', fontWeight: 700 }}>{data.totalBookings}</div>
          <div style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>Bookings</div>
        </div>
        <div style={{ background: 'var(--teal-soft)', borderRadius: 8, padding: 12, textAlign: 'center' }}>
          <div className="mono" style={{ fontSize: '1.4rem', fontWeight: 700 }}>₹{(data.totalRevenuePaise / 100).toFixed(0)}</div>
          <div style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>Revenue ({data.paidTransactionCount} paid)</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.86rem', marginBottom: 6 }}>Bookings by status</div>
          {Object.entries(data.bookingsByStatus).map(([k, v]) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem', padding: '3px 0' }}>
              <span>{k}</span><span className="mono">{v}</span>
            </div>
          ))}
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.86rem', marginBottom: 6 }}>Shops by status</div>
          {Object.entries(data.shopsByStatus).map(([k, v]) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem', padding: '3px 0' }}>
              <span>{k}</span><span className="mono">{v}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ marginTop: 16 }}>
        <div style={{ fontWeight: 700, fontSize: '0.86rem', marginBottom: 6 }}>Bookings by category</div>
        {Object.entries(data.bookingsByCategory).map(([k, v]) => (
          <div key={k} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem', padding: '3px 0' }}>
            <span>{k}</span><span className="mono">{v}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function CategoryManager() {
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [newCat, setNewCat] = useState({ code: '', displayName: '', icon: '', fields: [] });

  async function load() {
    try {
      const res = await apiClient.get('/admin/categories');
      setCategories(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  useEffect(() => { load(); }, []);

  function addField() {
    setNewCat((c) => ({ ...c, fields: [...c.fields, { id: '', label: '', type: 'text', options: [] }] }));
  }
  function updateField(idx, key, value) {
    setNewCat((c) => {
      const fields = [...c.fields];
      fields[idx] = { ...fields[idx], [key]: key === 'options' ? value.split(',').map((s) => s.trim()).filter(Boolean) : value };
      return { ...c, fields };
    });
  }
  function removeField(idx) {
    setNewCat((c) => ({ ...c, fields: c.fields.filter((_, i) => i !== idx) }));
  }

  async function createCategory(e) {
    e.preventDefault();
    setError('');
    try {
      await apiClient.post('/admin/categories', newCat);
      setNewCat({ code: '', displayName: '', icon: '', fields: [] });
      setShowCreate(false);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function toggleActive(cat) {
    try {
      await apiClient.patch(`/admin/categories/${cat.id}`, { active: !cat.active });
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="card">
      <h2>Manage categories</h2>
      <p className="sub">Add or retire a service category here — no code change needed. New categories with fields appear immediately on the "Book a service" page.</p>
      {error && <div className="error-banner">{error}</div>}

      {categories.map((cat) => (
        <div key={cat.id} className="booking-row">
          <div className="top">
            <div>{cat.icon} <strong>{cat.displayName}</strong> <span className="mono" style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>({cat.code})</span></div>
            <span className={`status-chip ${cat.active ? 'status-ACCEPTED' : 'status-REJECTED'}`}>{cat.active ? 'ACTIVE' : 'INACTIVE'}</span>
          </div>
          <div className="details">{cat.fields.length} field(s){cat.fields.length === 0 && ' — has its own dedicated page'}</div>
          <button className="btn small outline" style={{ marginTop: 8 }} onClick={() => toggleActive(cat)}>
            {cat.active ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      ))}

      {!showCreate ? (
        <button className="btn" onClick={() => setShowCreate(true)}>+ New category</button>
      ) : (
        <form onSubmit={createCategory} style={{ marginTop: 14, borderTop: '2px dashed var(--paper-deep)', paddingTop: 14 }}>
          <div className="field-row">
            <div className="field">
              <label>Code (unique, e.g. "carpenter")</label>
              <input value={newCat.code} onChange={(e) => setNewCat((c) => ({ ...c, code: e.target.value }))} required />
            </div>
            <div className="field">
              <label>Display name</label>
              <input value={newCat.displayName} onChange={(e) => setNewCat((c) => ({ ...c, displayName: e.target.value }))} required />
            </div>
          </div>
          <div className="field">
            <label>Icon (an emoji)</label>
            <input value={newCat.icon} onChange={(e) => setNewCat((c) => ({ ...c, icon: e.target.value }))} placeholder="🔨" />
          </div>

          <label style={{ marginTop: 10 }}>Booking form fields</label>
          {newCat.fields.map((f, idx) => (
            <div key={idx} style={{ display: 'flex', gap: 6, marginBottom: 6, flexWrap: 'wrap', alignItems: 'center' }}>
              <input placeholder="field id" value={f.id} onChange={(e) => updateField(idx, 'id', e.target.value)} style={{ flex: 1, minWidth: 90 }} required />
              <input placeholder="label" value={f.label} onChange={(e) => updateField(idx, 'label', e.target.value)} style={{ flex: 1, minWidth: 90 }} required />
              <select value={f.type} onChange={(e) => updateField(idx, 'type', e.target.value)} style={{ flex: 1, minWidth: 90 }}>
                <option value="text">text</option>
                <option value="textarea">textarea</option>
                <option value="select">select</option>
                <option value="date">date</option>
                <option value="time">time</option>
              </select>
              {f.type === 'select' && (
                <input placeholder="options, comma separated" onChange={(e) => updateField(idx, 'options', e.target.value)} style={{ flex: 2, minWidth: 140 }} />
              )}
              <button type="button" className="btn small brick" onClick={() => removeField(idx)}>×</button>
            </div>
          ))}
          <button type="button" className="btn small outline" onClick={addField}>+ Add field</button>

          <div style={{ marginTop: 12 }}>
            <button className="btn" type="submit">Create category</button>
            <button className="btn outline" type="button" style={{ marginLeft: 8 }} onClick={() => setShowCreate(false)}>Cancel</button>
          </div>
        </form>
      )}
    </div>
  );
}

function ComplaintQueue() {
  const [complaints, setComplaints] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try {
      const res = await apiClient.get('/admin/complaints');
      setComplaints(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  useEffect(() => { load(); }, []);

  async function respond(id, status, adminResponse) {
    try {
      await apiClient.patch(`/admin/complaints/${id}`, { status, adminResponse });
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="card">
      <h2>Support tickets</h2>
      {error && <div className="error-banner">{error}</div>}
      {complaints.length === 0 ? (
        <div className="empty">No tickets.</div>
      ) : (
        complaints.map((c) => (
          <div className="booking-row" key={c.id}>
            <div className="top">
              <div><strong>{c.subject}</strong> — {c.raisedByName}</div>
              <span className="status-chip status-ACCEPTED">{c.status}</span>
            </div>
            <div className="details">{c.description}</div>
            {c.status !== 'RESOLVED' && (
              <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
                <button className="btn small" onClick={() => respond(c.id, 'IN_PROGRESS', c.adminResponse)}>Mark in progress</button>
                <button className="btn small" onClick={() => {
                  const resp = window.prompt('Response to send:');
                  if (resp !== null) respond(c.id, 'RESOLVED', resp);
                }}>Resolve with response</button>
              </div>
            )}
          </div>
        ))
      )}
    </div>
  );
}

function ShopManager() {
  const [shops, setShops] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try {
      const res = await apiClient.get('/admin/shops');
      setShops(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  useEffect(() => { load(); }, []);

  async function suspend(id) {
    try {
      await apiClient.patch(`/admin/shops/${id}/suspend`);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }
  async function reapprove(id) {
    try {
      await apiClient.patch(`/admin/shops/${id}/approve`);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="card">
      <h2>All shops</h2>
      <p className="sub">"Delete" here means suspend — real orders and reviews stay linked to a real shop, they don't vanish.</p>
      {error && <div className="error-banner">{error}</div>}
      {shops.map((s) => (
        <div className="booking-row" key={s.id}>
          <div className="top">
            <div><strong>{s.shopName}</strong> <span className="mono" style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>({s.categoryCode})</span></div>
            <span className={`status-chip ${s.status === 'APPROVED' ? 'status-ACCEPTED' : 'status-REJECTED'}`}>{s.status}</span>
          </div>
          <div style={{ marginTop: 8 }}>
            {s.status !== 'SUSPENDED' ? (
              <button className="btn small brick" onClick={() => suspend(s.id)}>Suspend / delete</button>
            ) : (
              <button className="btn small" onClick={() => reapprove(s.id)}>Reinstate</button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

export default function AdminDashboard() {
  const [pending, setPending] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const res = await apiClient.get('/admin/shops/pending');
      setPending(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function decide(shopId, action) {
    try {
      await apiClient.patch(`/admin/shops/${shopId}/${action}`);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="wrap">
      <AnalyticsPanel />
      <CategoryManager />
      <ShopManager />
      <ComplaintQueue />
      <div className="card">
        <h2>Shop approval queue</h2>
        <p className="sub">New shop registrations wait here until you approve or reject them.</p>
        {error && <div className="error-banner">{error}</div>}
        {loading ? (
          <div className="empty">Loading…</div>
        ) : pending.length === 0 ? (
          <div className="empty">Nothing waiting on approval.</div>
        ) : (
          pending.map((s) => {
            const cat = findCategory(s.categoryCode);
            return (
              <div className="booking-row" key={s.id}>
                <div className="top">
                  <div><strong>{s.shopName}</strong> — {cat ? `${cat.icon} ${cat.name}` : s.categoryCode}</div>
                </div>
                <div className="details">{s.address}</div>
                <div className="actions">
                  <button className="btn small" onClick={() => decide(s.id, 'approve')}>Approve</button>
                  <button className="btn small brick" onClick={() => decide(s.id, 'reject')}>Reject</button>
                </div>
              </div>
            );
          })
        )}
        <button className="btn outline small" style={{ marginTop: 10 }} onClick={load}>Refresh</button>
      </div>
    </div>
  );
}
