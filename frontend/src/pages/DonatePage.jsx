import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { campaignApi, cityApi, donationApi } from '../api';
import Alert from '../components/Alert';
import CitySelect from '../components/CitySelect';
import ProgressBar from '../components/ProgressBar';
import Spinner from '../components/Spinner';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';
import { formatDate, formatMoney } from '../utils/format';
import { upiLink } from '../utils/whatsapp';

const QUICK_AMOUNTS = [51, 101, 251, 501, 1001];

export default function DonatePage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState(null);

  const refreshSummary = () => donationApi.summary().then(setSummary).catch(() => {});
  useEffect(() => {
    refreshSummary();
  }, []);

  const max = Math.max(1, ...(summary?.cities.map((c) => Number(c.collected)) || [1]));

  return (
    <div className="container page">
      <div className="page-header">
        <h1>Community fund</h1>
        <p className="muted">Small amounts, pooled city-wise, for social good. Totals count only donations verified by the city admin.</p>
      </div>

      <div className="layout-sidebar reverse">
        <div>{user ? <DonateForm onRecorded={refreshSummary} /> : <LoginPrompt />}</div>
        <aside className="sidebar">
          <div className="card">
            <p className="muted small">Total collected</p>
            <p className="big-number">{formatMoney(summary?.totalCollected)}</p>
            {!summary ? (
              <Spinner />
            ) : (
              <ul className="city-totals">
                {summary.cities.map((c) => (
                  <li key={c.cityId}>
                    <div className="row-between small">
                      <span>{c.cityName}</span>
                      <strong>{formatMoney(c.collected)}</strong>
                    </div>
                    <ProgressBar value={c.collected} max={max} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

function LoginPrompt() {
  return (
    <div className="card">
      <h3>Want to contribute?</h3>
      <p className="muted">Log in to see your city’s bank details and record your contribution.</p>
      <Link to="/login" className="btn btn-primary">Log in</Link>
    </div>
  );
}

function DonateForm({ onRecorded }) {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [cityId, setCityId] = useState(params.get('city') || String(user.city.id));
  const [city, setCity] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [supporters, setSupporters] = useState([]);
  const [mine, setMine] = useState([]);
  const [form, setForm] = useState({ amount: '', campaignId: '', transactionRef: '', note: '', anonymous: false });
  const [message, setMessage] = useState({});

  useEffect(() => {
    setCity(null);
    cityApi.get(cityId).then(setCity);
    campaignApi.forCity(cityId).then(setCampaigns);
    donationApi.supporters(cityId, { size: 10 }).then((p) => setSupporters(p.content));
    setForm((f) => ({ ...f, campaignId: '' }));
  }, [cityId]);

  const loadMine = () => donationApi.mine({ size: 10 }).then((p) => setMine(p.content));
  useEffect(() => {
    loadMine();
  }, []);

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage({});
    try {
      await donationApi.record({
        ...form,
        cityId: Number(cityId),
        campaignId: form.campaignId ? Number(form.campaignId) : null,
        amount: Number(form.amount),
      });
      setForm({ amount: '', campaignId: '', transactionRef: '', note: '', anonymous: false });
      setMessage({ type: 'success', text: 'Thank you! Your contribution is recorded and will count once the city admin verifies it.' });
      loadMine();
      onRecorded();
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    }
  };

  const bank = city?.bank;
  const hasBank = bank && (bank.accountNumber || bank.upiId);

  return (
    <>
      <div className="card">
        <div className="row-between">
          <h3>1. Send money to the city account</h3>
          <CitySelect value={cityId} onChange={setCityId} />
        </div>
        {!city ? (
          <Spinner />
        ) : hasBank ? (
          <dl className="bank-details">
            {bank.accountName && <BankRow label="Account name" value={bank.accountName} />}
            {bank.accountNumber && <BankRow label="Account number" value={bank.accountNumber} copy />}
            {bank.ifsc && <BankRow label="IFSC" value={bank.ifsc} copy />}
            {bank.bankName && <BankRow label="Bank" value={bank.bankName} />}
            {bank.upiId && <BankRow label="UPI ID" value={bank.upiId} copy />}
          </dl>
        ) : (
          <p className="muted">The admin of {city.name} has not added bank details yet.</p>
        )}
        {bank?.upiId && (
          <a className="btn btn-light" href={upiLink({ upiId: bank.upiId, name: bank.accountName, amount: form.amount, note: 'Kamsar o Bar' })}>
            📱 Pay with a UPI app
          </a>
        )}
      </div>

      {campaigns.length > 0 && (
        <div className="card">
          <h3>Active causes in {city?.name}</h3>
          {campaigns.map((c) => (
            <div key={c.id} className="campaign">
              <div className="row-between">
                <strong>{c.title}</strong>
                <span className="small">
                  {formatMoney(c.raisedAmount)}{c.goalAmount ? ` of ${formatMoney(c.goalAmount)}` : ''}
                </span>
              </div>
              {c.description && <p className="muted small">{c.description}</p>}
              {c.goalAmount && <ProgressBar value={c.raisedAmount} max={c.goalAmount} />}
            </div>
          ))}
        </div>
      )}

      <form className="card" onSubmit={onSubmit}>
        <h3>2. Tell us about your contribution</h3>
        <Alert type={message.type}>{message.text}</Alert>
        <div className="field">
          <label htmlFor="amount">Amount (₹)</label>
          <div className="quick-amounts">
            {QUICK_AMOUNTS.map((a) => (
              <button type="button" key={a} className={`chip-btn ${Number(form.amount) === a ? 'active' : ''}`}
                onClick={() => setForm({ ...form, amount: String(a) })}>
                ₹{a}
              </button>
            ))}
          </div>
          <input id="amount" type="number" min="1" step="1" required value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.target.value })} />
        </div>
        {campaigns.length > 0 && (
          <div className="field">
            <label htmlFor="campaign">For a cause (optional)</label>
            <select id="campaign" value={form.campaignId} onChange={(e) => setForm({ ...form, campaignId: e.target.value })}>
              <option value="">General city fund</option>
              {campaigns.map((c) => <option key={c.id} value={c.id}>{c.title}</option>)}
            </select>
          </div>
        )}
        <div className="field">
          <label htmlFor="ref">UPI / bank transaction reference</label>
          <input id="ref" maxLength={100} placeholder="Helps the admin match your payment" value={form.transactionRef}
            onChange={(e) => setForm({ ...form, transactionRef: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="note">Note (optional)</label>
          <input id="note" maxLength={500} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
        </div>
        <label className="checkbox">
          <input type="checkbox" checked={form.anonymous} onChange={(e) => setForm({ ...form, anonymous: e.target.checked })} />
          Show me as “Anonymous” on the supporters list
        </label>
        <div className="form-actions">
          <button className="btn btn-primary">Record contribution</button>
        </div>
      </form>

      <div className="two-col">
        <div className="card">
          <h3>My contributions</h3>
          {mine.length === 0 ? <p className="muted small">None yet.</p> : (
            <ul className="simple-list">
              {mine.map((d) => (
                <li key={d.id}>
                  <span>{formatMoney(d.amount)} · {d.city.name}<br /><span className="muted small">{formatDate(d.createdAt)}</span></span>
                  <span className={`status status-${d.status.toLowerCase()}`}>{d.status}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="card">
          <h3>Recent supporters</h3>
          {supporters.length === 0 ? <p className="muted small">Be the first!</p> : (
            <ul className="simple-list">
              {supporters.map((s, i) => (
                <li key={i}>
                  <span>{s.name}<br /><span className="muted small">{s.campaignTitle || 'City fund'}</span></span>
                  <strong>{formatMoney(s.amount)}</strong>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}

function BankRow({ label, value, copy }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>
        {value}
        {copy && (
          <button type="button" className="link-btn" onClick={() => navigator.clipboard?.writeText(value)}>Copy</button>
        )}
      </dd>
    </>
  );
}
