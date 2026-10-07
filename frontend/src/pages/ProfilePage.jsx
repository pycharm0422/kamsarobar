import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { profileApi, userApi } from '../api';
import Alert from '../components/Alert';
import CitySelect from '../components/CitySelect';
import Spinner from '../components/Spinner';
import TagInput from '../components/TagInput';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';

const TABS = [
  { key: 'professional', label: 'Work & referrals' },
  { key: 'basic', label: 'Basic info' },
  { key: 'password', label: 'Password' },
];

export default function ProfilePage() {
  const [params] = useSearchParams();
  const welcome = params.get('welcome');
  const [tab, setTab] = useState('professional');

  return (
    <div className="container page narrow">
      <div className="page-header">
        {welcome ? <p className="step-badge">Step 2 of 2</p> : null}
        <h1>{welcome ? 'Tell the community about your work' : 'My profile'}</h1>
        <p className="muted">
          {welcome
            ? 'Add the companies you can refer people to and what you are good at, so members can find you. You can skip this and come back any time.'
            : 'Everything here can be edited any time.'}
        </p>
      </div>
      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.key} className={`tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'professional' && <ProfessionalForm welcome={welcome} />}
      {tab === 'basic' && <BasicForm />}
      {tab === 'password' && <PasswordForm />}
    </div>
  );
}

/** Form 2 - detailed professional profile. */
function ProfessionalForm({ welcome }) {
  const [form, setForm] = useState(null);
  const [message, setMessage] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    profileApi.mine().then((p) =>
      setForm({
        linkedinUrl: p.linkedinUrl || '',
        currentCompany: p.currentCompany || '',
        position: p.position || '',
        yearsOfExperience: p.yearsOfExperience ?? '',
        bio: p.bio || '',
        openToHelp: p.openToHelp,
        referralCompanies: p.referralCompanies,
        expertise: p.expertise,
      }),
    );
  }, []);

  if (!form) return <Spinner />;
  const set = (key) => (e) => setForm({ ...form, [key]: e?.target ? e.target.value : e });

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMessage({});
    try {
      await profileApi.save({
        ...form,
        yearsOfExperience: form.yearsOfExperience === '' ? null : Number(form.yearsOfExperience),
      });
      setMessage({ type: 'success', text: 'Profile saved. Members can now find you in search.' });
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card form-card" onSubmit={onSubmit}>
      <Alert type={message.type}>{message.text}</Alert>
      <div className="field">
        <label htmlFor="linkedin">LinkedIn profile URL</label>
        <input id="linkedin" type="url" placeholder="https://www.linkedin.com/in/your-name" value={form.linkedinUrl}
          onChange={set('linkedinUrl')} />
      </div>
      <div className="form-row">
        <div className="field">
          <label htmlFor="company">Current company</label>
          <input id="company" placeholder="e.g. Infosys" value={form.currentCompany} onChange={set('currentCompany')} />
        </div>
        <div className="field">
          <label htmlFor="position">Position</label>
          <input id="position" placeholder="e.g. Senior Software Engineer" value={form.position} onChange={set('position')} />
        </div>
        <div className="field small-field">
          <label htmlFor="years">Experience (yrs)</label>
          <input id="years" type="number" min="0" max="60" value={form.yearsOfExperience} onChange={set('yearsOfExperience')} />
        </div>
      </div>
      <div className="field">
        <label htmlFor="referrals">Companies where you can give a referral</label>
        <TagInput id="referrals" value={form.referralCompanies} onChange={set('referralCompanies')}
          suggest={profileApi.suggestCompanies} placeholder="Type a company and press Enter" />
      </div>
      <div className="field">
        <label htmlFor="expertise">Your expertise (people can ask you for advice)</label>
        <TagInput id="expertise" value={form.expertise} onChange={set('expertise')} max={30}
          suggest={profileApi.suggestExpertise} placeholder="e.g. Java, UPSC preparation, Real estate, Medicine" />
      </div>
      <div className="field">
        <label htmlFor="bio">Short bio</label>
        <textarea id="bio" rows={3} maxLength={1000} value={form.bio} onChange={set('bio')}
          placeholder="A line or two about you and how you can help" />
      </div>
      <label className="checkbox">
        <input type="checkbox" checked={form.openToHelp} onChange={(e) => setForm({ ...form, openToHelp: e.target.checked })} />
        Show me in referral & expert search
      </label>
      <div className="form-actions">
        {welcome && <Link to="/posts" className="btn btn-ghost">Skip for now</Link>}
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save profile'}</button>
      </div>
    </form>
  );
}

/** Edit form 1 (name / mobile / city). */
function BasicForm() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ name: user.name, mobile: `+${user.mobile}`, cityId: String(user.city.id) });
  const [message, setMessage] = useState({});
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMessage({});
    try {
      setUser(await userApi.update({ ...form, cityId: Number(form.cityId) }));
      setMessage({ type: 'success', text: 'Saved.' });
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card form-card" onSubmit={onSubmit}>
      <Alert type={message.type}>{message.text}</Alert>
      <div className="field">
        <label htmlFor="name">Full name</label>
        <input id="name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="mobile">Mobile number (WhatsApp)</label>
        <input id="mobile" type="tel" required value={form.mobile} onChange={(e) => setForm({ ...form, mobile: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="city">City</label>
        <CitySelect id="city" required value={form.cityId} onChange={(v) => setForm({ ...form, cityId: v })} />
        <small className="muted">Moving cities? Change it here and you will join that city’s community.</small>
      </div>
      <div className="form-actions">
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save'}</button>
      </div>
    </form>
  );
}

function PasswordForm() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [message, setMessage] = useState({});

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage({});
    try {
      await userApi.changePassword(form);
      setForm({ currentPassword: '', newPassword: '' });
      setMessage({ type: 'success', text: 'Password changed.' });
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    }
  };

  return (
    <form className="card form-card" onSubmit={onSubmit}>
      <Alert type={message.type}>{message.text}</Alert>
      <div className="field">
        <label htmlFor="current">Current password</label>
        <input id="current" type="password" required autoComplete="current-password" value={form.currentPassword}
          onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="new">New password</label>
        <input id="new" type="password" required minLength={6} autoComplete="new-password" value={form.newPassword}
          onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
      </div>
      <div className="form-actions">
        <button className="btn btn-primary">Change password</button>
      </div>
    </form>
  );
}
