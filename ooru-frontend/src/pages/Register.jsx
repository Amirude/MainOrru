import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', phone: '', email: '', password: '', role: 'CUSTOMER' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(form);
      navigate('/verify-otp', { state: { phone: form.phone } });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="wrap narrow">
      <div className="card">
        <h2>Create an account</h2>
        <p className="sub">Book services, or register a shop, or sign on as a delivery partner.</p>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Full name</label>
            <input value={form.name} onChange={(e) => update('name', e.target.value)} required />
          </div>
          <div className="field">
            <label>Phone number</label>
            <input type="tel" value={form.phone} onChange={(e) => update('phone', e.target.value)} placeholder="10-digit mobile number" required />
          </div>
          <div className="field">
            <label>Email (optional)</label>
            <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)} />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={form.password} onChange={(e) => update('password', e.target.value)} required />
          </div>
          <div className="field">
            <label>I am a...</label>
            <select value={form.role} onChange={(e) => update('role', e.target.value)}>
              <option value="CUSTOMER">Customer — booking services</option>
              <option value="SHOP_OWNER">Shop owner — offering a service</option>
              <option value="DELIVERY_PARTNER">Delivery partner</option>
            </select>
          </div>
          <button className="btn full" type="submit" disabled={loading}>{loading ? 'Creating account…' : 'Create account'}</button>
        </form>
        <p style={{ marginTop: 14, fontSize: '0.86rem' }}>Already have an account? <Link to="/login">Log in</Link></p>
      </div>
    </div>
  );
}
