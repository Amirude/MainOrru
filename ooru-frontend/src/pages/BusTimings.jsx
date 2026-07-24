import { useState } from 'react';
import { apiClient, extractErrorMessage } from '../api/client';

export default function BusTimings() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [error, setError] = useState('');

  async function search(e) {
    e.preventDefault();
    setError('');
    if (!query.trim()) { setResults(null); return; }
    try {
      const res = await apiClient.get('/bus-routes/search', { params: { q: query } });
      setResults(res.data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="wrap">
      <div className="card">
        <h2>🚌 Bus timings</h2>
        <p className="sub">Search a route number or a stop name. This is a lookup, not a booking.</p>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={search} style={{ display: 'flex', gap: 8 }}>
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="e.g. 21G or Tambaram" />
          <button className="btn" type="submit">Search</button>
        </form>

        {results && (
          results.length === 0 ? (
            <div className="empty">No sample route matches — try a route number or a stop name.</div>
          ) : (
            <div style={{ marginTop: 16 }}>
              {results.map((r) => (
                <div className="booking-row" key={r.id}>
                  <div className="top">
                    <div><span className="ref">{r.routeNumber}</span> · {r.fromStop} → {r.toStop}</div>
                  </div>
                  <div className="details">{r.departures}</div>
                </div>
              ))}
              <p style={{ fontSize: '0.78rem', color: 'var(--ink-soft)' }}>Sample schedule for demo purposes — connect a live transit feed for real timings.</p>
            </div>
          )
        )}
      </div>
    </div>
  );
}
