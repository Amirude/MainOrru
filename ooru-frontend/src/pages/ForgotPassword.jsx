import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';

export default function ForgotPassword() {
  const { forgotPassword, resetPassword } = useAuth();
  const navigate = useNavigate();
  const [step, setStep] = useState('request'); // 'request' | 'reset'
  const [identifier, setIdentifier] = useState('');
  const [phone, setPhone] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleRequest(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await forgotPassword({ identifier });
      setMessage(res.message);
      setPhone(identifier); // if they typed a phone, this pre-fills correctly for the OTP step
      setStep('reset');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleReset(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await resetPassword({ phone, otp, newPassword });
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
        <h2>Forgot password</h2>
        {error && <div className="error-banner">{error}</div>}
        {message && <div className="success-banner">{message}</div>}

        {step === 'request' ? (
          <form onSubmit={handleRequest}>
            <p className="sub">Enter your phone number or email — an OTP always goes to the phone on your account.</p>
            <div className="field">
              <label>Phone number or email</label>
              <input value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
            </div>
            <button className="btn full" type="submit" disabled={loading}>{loading ? 'Sending…' : 'Send OTP'}</button>
          </form>
        ) : (
          <form onSubmit={handleReset}>
            <p className="sub">Check the backend console for the OTP (see OtpService.java) until a real SMS provider is configured.</p>
            <div className="field">
              <label>Phone number</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
            </div>
            <div className="field">
              <label>OTP</label>
              <input value={otp} onChange={(e) => setOtp(e.target.value)} required />
            </div>
            <div className="field">
              <label>New password</label>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} required />
            </div>
            <button className="btn full" type="submit" disabled={loading}>{loading ? 'Resetting…' : 'Reset password'}</button>
          </form>
        )}
      </div>
    </div>
  );
}
