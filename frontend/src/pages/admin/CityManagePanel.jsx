import { useEffect, useState } from 'react';
import { campaignApi, cityApi, donationApi } from '../../api';
import Alert from '../../components/Alert';
import CitySelect from '../../components/CitySelect';
import Pagination from '../../components/Pagination';
import ProgressBar from '../../components/ProgressBar';
import Spinner from '../../components/Spinner';
import { errorMessage } from '../../utils/errors';
import { formatDate, formatMoney } from '../../utils/format';

const SECTIONS = ['Settings', 'Donations', 'Causes'];

/** What a city head manages. The main admin can open it for any city. */
export default function CityManagePanel({ fixedCityId }) {
  const [cityId, setCityId] = useState(fixedCityId ? String(fixedCityId) : '');
  const [section, setSection] = useState('Settings');

  return (
    <div>
      {!fixedCityId && (
        <div className="card form-row">
          <div className="field grow">
            <label htmlFor="manage-city">City to manage</label>
            <CitySelect id="manage-city" value={cityId} onChange={setCityId} />
          </div>
        </div>
      )}
      {cityId && (
        <>
          <div className="tabs secondary">
            {SECTIONS.map((s) => (
              <button key={s} className={`tab ${section === s ? 'active' : ''}`} onClick={() => setSection(s)}>{s}</button>
            ))}
          </div>
          {section === 'Settings' && <SettingsForm key={cityId} cityId={cityId} />}
          {section === 'Donations' && <DonationsReview key={cityId} cityId={cityId} />}
          {section === 'Causes' && <Campaigns key={cityId} cityId={cityId} />}
        </>
      )}
    </div>
  );
}

function SettingsForm({ cityId }) {
  const [form, setForm] = useState(null);
  const [message, setMessage] = useState({});

  useEffect(() => {
    cityApi.get(cityId).then((c) =>
      setForm({
        whatsappGroupUrl: c.whatsappGroupUrl || '',
        bankAccountName: c.bank.accountName || '',
        bankAccountNumber: c.bank.accountNumber || '',
        bankIfsc: c.bank.ifsc || '',
        bankName: c.bank.bankName || '',
        upiId: c.bank.upiId || '',
      }),
    );
  }, [cityId]);

  if (!form) return <Spinner />;
  const field = (key, label, props = {}) => (
    <div className="field">
      <label htmlFor={key}>{label}</label>
      <input id={key} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} {...props} />
    </div>
  );

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage({});
    try {
      await cityApi.updateSettings(cityId, form);
      setMessage({ type: 'success', text: 'Saved. Members now see the updated details.' });
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    }
  };

  return (
    <form className="card form-card" onSubmit={onSubmit}>
      <Alert type={message.type}>{message.text}</Alert>
      <h3>WhatsApp group</h3>
      {field('whatsappGroupUrl', 'Group invite link', { placeholder: 'https://chat.whatsapp.com/...', type: 'url' })}
      <h3>Bank details for donations</h3>
      <div className="form-row">
        {field('bankAccountName', 'Account holder name')}
        {field('bankName', 'Bank name & branch')}
      </div>
      <div className="form-row">
        {field('bankAccountNumber', 'Account number', { inputMode: 'numeric' })}
        {field('bankIfsc', 'IFSC code', { placeholder: 'SBIN0001234' })}
      </div>
      {field('upiId', 'UPI ID', { placeholder: 'name@okaxis' })}
      <div className="form-actions">
        <button className="btn btn-primary">Save settings</button>
      </div>
    </form>
  );
}

function DonationsReview({ cityId }) {
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(null);
  const [error, setError] = useState('');

  const load = (p = 0) =>
    donationApi.forCity(cityId, { status: status || undefined, page: p }).then(setPage).catch((e) => setError(errorMessage(e)));
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cityId, status]);

  const review = async (id, next) => {
    try {
      await donationApi.review(id, next);
      load(page.page);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div className="card">
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="row-between">
        <p className="muted small">Match each entry with your bank / UPI statement before verifying.</p>
        <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status">
          <option value="PENDING">Pending</option>
          <option value="VERIFIED">Verified</option>
          <option value="REJECTED">Rejected</option>
          <option value="">All</option>
        </select>
      </div>
      {!page ? <Spinner /> : page.content.length === 0 ? <p className="muted">Nothing here.</p> : (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Date</th><th>Donor</th><th>Amount</th><th>Txn ref</th><th>Cause</th><th>Status</th><th /></tr></thead>
            <tbody>
              {page.content.map((d) => (
                <tr key={d.id}>
                  <td>{formatDate(d.createdAt)}</td>
                  <td>{d.donorName}{d.anonymous && <span className="muted small"> (anon)</span>}{d.note && <><br /><span className="muted small">{d.note}</span></>}</td>
                  <td><strong>{formatMoney(d.amount)}</strong></td>
                  <td className="mono">{d.transactionRef || '-'}</td>
                  <td>{d.campaignTitle || 'City fund'}</td>
                  <td><span className={`status status-${d.status.toLowerCase()}`}>{d.status}</span></td>
                  <td className="nowrap">
                    {d.status !== 'VERIFIED' && <button className="link-btn" onClick={() => review(d.id, 'VERIFIED')}>Verify</button>}
                    {d.status !== 'REJECTED' && <button className="link-btn danger" onClick={() => review(d.id, 'REJECTED')}>Reject</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={page} onChange={load} />
    </div>
  );
}

function Campaigns({ cityId }) {
  const [list, setList] = useState([]);
  const [form, setForm] = useState({ title: '', description: '', goalAmount: '' });
  const [error, setError] = useState('');

  const load = () => campaignApi.forCity(cityId, false).then(setList);
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cityId]);

  const run = async (fn) => {
    setError('');
    try {
      await fn();
      load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const goal = (v) => (v === '' || v == null ? null : Number(v));

  return (
    <div className="two-col">
      <form className="card" onSubmit={(e) => {
        e.preventDefault();
        run(async () => {
          await campaignApi.create(cityId, { ...form, goalAmount: goal(form.goalAmount) });
          setForm({ title: '', description: '', goalAmount: '' });
        });
      }}>
        <h3>Start a cause</h3>
        <Alert onClose={() => setError('')}>{error}</Alert>
        <div className="field"><label htmlFor="ctitle">Title</label>
          <input id="ctitle" required maxLength={200} placeholder="e.g. Winter blankets drive" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
        <div className="field"><label htmlFor="cdesc">Description</label>
          <textarea id="cdesc" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
        <div className="field"><label htmlFor="cgoal">Goal (₹, optional)</label>
          <input id="cgoal" type="number" min="1" value={form.goalAmount} onChange={(e) => setForm({ ...form, goalAmount: e.target.value })} /></div>
        <button className="btn btn-primary">Create</button>
      </form>
      <div className="card">
        <h3>Causes</h3>
        {list.length === 0 ? <p className="muted small">No causes yet.</p> : list.map((c) => (
          <div key={c.id} className="campaign">
            <div className="row-between">
              <strong>{c.title} {!c.active && <span className="status status-rejected">Closed</span>}</strong>
              <span className="small">{formatMoney(c.raisedAmount)}{c.goalAmount ? ` / ${formatMoney(c.goalAmount)}` : ''}</span>
            </div>
            {c.goalAmount && <ProgressBar value={c.raisedAmount} max={c.goalAmount} />}
            <div className="post-actions">
              <button className="link-btn" onClick={() => run(() => campaignApi.update(c.id, { title: c.title, description: c.description, goalAmount: c.goalAmount, active: !c.active }))}>
                {c.active ? 'Close' : 'Reopen'}
              </button>
              <button className="link-btn danger" onClick={() => window.confirm('Delete this cause?') && run(() => campaignApi.remove(c.id))}>Delete</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
