import { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import Alert from '../../components/Alert';
import CitySelect from '../../components/CitySelect';
import { useDebounce } from '../../hooks/useDebounce';
import { withAdminReplacement } from '../../utils/adminActions';
import { errorMessage } from '../../utils/errors';
import { displayMobile } from '../../utils/format';

/** Main admin appoints / removes the head (admin) of each city. */
export default function CityAdminsPanel() {
  const [admins, setAdmins] = useState([]);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState(null);
  const [cityId, setCityId] = useState('');
  const [message, setMessage] = useState({});
  const q = useDebounce(query, 300);

  const loadAdmins = () => adminApi.cityAdmins().then(setAdmins);
  useEffect(() => {
    loadAdmins();
  }, []);

  useEffect(() => {
    if (q.trim().length < 2) {
      setResults([]);
      return;
    }
    adminApi.users({ q: q.trim(), size: 8 }).then((p) => setResults(p.content));
  }, [q]);

  const assign = async (e) => {
    e.preventDefault();
    setMessage({});
    try {
      const u = await withAdminReplacement((replace) => adminApi.assignCityAdmin(selected.id, Number(cityId), replace), selected.name);
      if (!u) return; // kept the current admin
      setMessage({ type: 'success', text: `${u.name} is now the admin of ${u.managedCity.name}.` });
      setSelected(null);
      setQuery('');
      loadAdmins();
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    }
  };

  const revoke = async (u) => {
    if (!window.confirm(`Remove ${u.name} as admin of ${u.managedCity?.name}?`)) return;
    try {
      await adminApi.revokeCityAdmin(u.id);
      loadAdmins();
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) });
    }
  };

  return (
    <div className="two-col">
      <form className="card" onSubmit={assign}>
        <h3>Appoint a city admin</h3>
        <Alert type={message.type}>{message.text}</Alert>
        <div className="field">
          <label htmlFor="member">Find member by name or mobile</label>
          <input id="member" value={query} onChange={(e) => { setQuery(e.target.value); setSelected(null); }} placeholder="Start typing..." />
          {!selected && results.length > 0 && (
            <ul className="pick-list">
              {results.map((u) => (
                <li key={u.id}>
                  <button type="button" onClick={() => { setSelected(u); setCityId(String(u.city.id)); setQuery(u.name); }}>
                    <strong>{u.name}</strong> <span className="muted small">{displayMobile(u.mobile)} · {u.city.name} · {u.role}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="field">
          <label htmlFor="admin-city">City they will head</label>
          <CitySelect id="admin-city" required value={cityId} onChange={setCityId} />
        </div>
        <button className="btn btn-primary" disabled={!selected || !cityId}>Make city admin</button>
      </form>

      <div className="card">
        <h3>Current city admins</h3>
        {admins.length === 0 ? <p className="muted small">No city admins yet.</p> : (
          <ul className="simple-list">
            {admins.map((u) => (
              <li key={u.id}>
                <span><strong>{u.name}</strong><br /><span className="muted small">{u.managedCity?.name} · {displayMobile(u.mobile)}</span></span>
                <button className="link-btn danger" onClick={() => revoke(u)}>Remove</button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
