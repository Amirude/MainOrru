import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';

export default function VerifyOtp() {
  const { verifyOtp } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [phone, setPhone] = useState(location.state?.phone || '');
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await verifyOtp({ phone, otp });
      navigate('/login');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="wrap narrow">
      <div className="card">
        <h2>Verify your phone</h2>
        <p className="sub">
          In production this OTP arrives by SMS. Right now the backend logs it to its server
          console instead (see OtpService.java) — check there for the code.
        </p>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Phone number</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
          </div>
          <div className="field">
            <label>OTP</label>
            <input value={otp} onChange={(e) => setOtp(e.target.value)} placeholder="6-digit code" required />
          </div>
          <button className="btn full" type="submit" disabled={loading}>{loading ? 'Verifying…' : 'Verify'}</button>
        </form>
      </div>
    </div>
  );
}
