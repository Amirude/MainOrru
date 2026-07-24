import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { enableNotifications } from '../firebaseNotifications';
import { apiClient } from '../api/client';

function SearchBox() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [open, setOpen] = useState(false);
  const debounceRef = useRef(null);
  const boxRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleChange(e) {
    const value = e.target.value;
    setQuery(value);
    clearTimeout(debounceRef.current);
    if (!value.trim()) { setResults(null); setOpen(false); return; }
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await apiClient.get('/search', { params: { q: value } });
        setResults(res.data);
        setOpen(true);
      } catch {
        setResults(null);
      }
    }, 300); // debounced — real substring search, not called on every keystroke
  }

  function goToShop(shop) {
    setOpen(false);
    setQuery('');
    if (shop.categoryCode === 'food' || shop.categoryCode === 'grocery') navigate('/food');
    else if (shop.categoryCode === 'tailor') navigate('/tailor');
    else navigate('/');
  }

  function goToMenuItem(item) {
    setOpen(false);
    setQuery('');
    navigate('/food');
  }

  const hasResults = results && (results.shops.length > 0 || results.menuItems.length > 0);

  return (
    <div ref={boxRef} className="search-box-wrap" style={{ position: 'relative', flex: 1, maxWidth: 280 }}>
      <input
        value={query}
        onChange={handleChange}
        onFocus={() => query && setOpen(true)}
        placeholder="🔍 Search shops or dishes…"
        style={{ padding: '7px 10px', fontSize: '0.86rem' }}
      />
      {open && (
        <div style={{
          position: 'absolute', top: '110%', left: 0, right: 0, background: '#fffef9',
          border: '2px solid var(--ink)', borderRadius: 8, zIndex: 100, maxHeight: 320,
          overflowY: 'auto', boxShadow: '4px 4px 0 rgba(27,42,74,0.18)',
        }}>
          {!hasResults ? (
            <div style={{ padding: 12, fontSize: '0.84rem', color: 'var(--ink-soft)' }}>No matches.</div>
          ) : (
            <>
              {results.shops.map((s) => (
                <button key={'shop-' + s.id} onClick={() => goToShop(s)} style={{
                  display: 'block', width: '100%', textAlign: 'left', padding: '10px 12px',
                  background: 'none', border: 'none', borderBottom: '1px solid #ecdfb8', fontSize: '0.86rem',
                }}>
                  <strong>{s.shopName}</strong> <span style={{ color: 'var(--ink-soft)' }}>· {s.categoryCode}</span>
                </button>
              ))}
              {results.menuItems.map((m) => (
                <button key={'item-' + m.id} onClick={() => goToMenuItem(m)} style={{
                  display: 'block', width: '100%', textAlign: 'left', padding: '10px 12px',
                  background: 'none', border: 'none', borderBottom: '1px solid #ecdfb8', fontSize: '0.86rem',
                }}>
                  {m.name} <span style={{ color: 'var(--ink-soft)' }}>· {m.shopName} · ₹{(m.pricePaise / 100).toFixed(2)}</span>
                </button>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default function Nav() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [notifStatus, setNotifStatus] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate('/login');
    setMenuOpen(false);
  }

  async function handleEnableNotifications() {
    setNotifStatus('Requesting…');
    const result = await enableNotifications();
    setNotifStatus(result.message);
  }

  return (
    <nav className="nav">
      <Link to="/" className="brand"><span className="dot"></span>Altenul One</Link>
      <SearchBox />
      <button className="nav-toggle" onClick={() => setMenuOpen((o) => !o)} aria-label="Menu">{menuOpen ? '✕' : '☰'}</button>
      <div className={`nav-links ${menuOpen ? 'open' : ''}`} onClick={() => setMenuOpen(false)}>
        <Link to="/">Book a service</Link>
        <Link to="/tailor">Tailor</Link>
        <Link to="/food">Food &amp; Grocery</Link>
        <Link to="/bus-timings">Bus timings</Link>
        <Link to="/nearby-shops">Nearby shops</Link>
        {user && <Link to="/assistant">🤖 Assistant</Link>}
        {user && <Link to="/my-bookings">My bookings</Link>}
        {user && <Link to="/support">Help</Link>}
        {user && user.role === 'SHOP_OWNER' && <Link to="/shop">Shop dashboard</Link>}
        {user && user.role === 'DELIVERY_PARTNER' && <Link to="/delivery">Deliveries</Link>}
        {user && user.role === 'ADMIN' && <Link to="/admin">Admin</Link>}
        {user ? (
          <>
            <button onClick={handleEnableNotifications} title={notifStatus}>🔔 Notify me</button>
            <Link to="/change-password">Change password</Link>
            <span className="role-badge">{user.name} · {user.role}</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <Link to="/login">Log in</Link>
        )}
      </div>
    </nav>
  );
}
