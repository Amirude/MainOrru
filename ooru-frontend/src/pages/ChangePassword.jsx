import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient, extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function ChangePassword() {
  const { deleteAccount } = useAuth();
  const navigate = useNavigate();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  async function handleDelete() {
    setDeleteError('');
    try {
      await deleteAccount();
      navigate('/');
    } catch (err) {
      setDeleteError(extractErrorMessage(err));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess(false);
    if (newPassword !== confirmPassword) {
      setError("New passwords don't match.");
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.patch('/auth/change-password', { currentPassword, newPassword });
      setSuccess(true);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="wrap narrow">
      <div className="card">
        <h2>Change password</h2>
        <p className="sub">Works for any account, including the admin — no database access needed.</p>
        {success && <div className="success-banner">Password changed.</div>}
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Current password</label>
            <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
          </div>
          <div className="field">
            <label>New password</label>
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} required />
          </div>
          <div className="field">
            <label>Confirm new password</label>
            <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} minLength={8} required />
          </div>
          <button className="btn full" type="submit" disabled={submitting}>{submitting ? 'Changing…' : 'Change password'}</button>
        </form>

        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '2px dashed var(--paper-deep)' }}>
          <h3 style={{ fontSize: '1rem', color: 'var(--brick)' }}>Delete account</h3>
          <p className="sub" style={{ marginBottom: 10 }}>
            Your name, phone, and email are scrubbed and you can never log in again. Your past
            orders stay on record (so shops can still see their own order history) but are no
            longer linked to your identity.
          </p>
          {deleteError && <div className="error-banner">{deleteError}</div>}
          {!confirmDelete ? (
            <button className="btn brick" onClick={() => setConfirmDelete(true)}>Delete my account</button>
          ) : (
            <div>
              <p style={{ fontWeight: 600, fontSize: '0.88rem' }}>Are you sure? This can't be undone.</p>
              <button className="btn brick" onClick={handleDelete}>Yes, delete permanently</button>
              <button className="btn outline" style={{ marginLeft: 8 }} onClick={() => setConfirmDelete(false)}>Cancel</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
