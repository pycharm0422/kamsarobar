import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import CitySelect from '../components/CitySelect';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';

/** Form 1 - the short sign-up form: name, mobile, city. */
export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', mobile: '', cityId: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target ? e.target.value : e });

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await register({ ...form, cityId: Number(form.cityId) });
      navigate('/profile?welcome=1', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container auth-page">
      <form className="card auth-card" onSubmit={onSubmit}>
        <p className="step-badge">Step 1 of 2</p>
        <h1>Join Kamsar o Bar</h1>
        <p className="muted">Just the basics - you can add your work details next.</p>
        <Alert>{error}</Alert>
        <div className="field">
          <label htmlFor="name">Full name</label>
          <input id="name" required maxLength={120} autoComplete="name" value={form.name} onChange={set('name')} />
        </div>
        <div className="field">
          <label htmlFor="mobile">Mobile number (WhatsApp)</label>
          <input id="mobile" type="tel" inputMode="tel" autoComplete="tel" required placeholder="98765 43210"
            value={form.mobile} onChange={set('mobile')} />
          <small className="muted">10-digit Indian numbers get +91 automatically. Add the country code for other countries.</small>
        </div>
        <div className="field">
          <label htmlFor="city">City you live in</label>
          <CitySelect id="city" required value={form.cityId} onChange={set('cityId')} />
        </div>
        <div className="field">
          <label htmlFor="password">Choose a password</label>
          <input id="password" type="password" minLength={6} required autoComplete="new-password"
            value={form.password} onChange={set('password')} />
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button>
        <p className="muted center">
          Already a member? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
