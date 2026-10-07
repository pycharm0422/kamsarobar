import { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import Alert from '../../components/Alert';
import { invalidateCities } from '../../hooks/useCities';
import { errorMessage } from '../../utils/errors';

export default function CitiesPanel() {
  const [cities, setCities] = useState([]);
  const [form, setForm] = useState({ name: '', state: '' });
  const [error, setError] = useState('');

  const load = () => adminApi.cities().then(setCities);
  useEffect(() => {
    load();
  }, []);

  const run = async (fn) => {
    setError('');
    try {
      await fn();
      invalidateCities();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div className="card">
      <Alert onClose={() => setError('')}>{error}</Alert>
      <form className="form-row" onSubmit={(e) => { e.preventDefault(); run(async () => { await adminApi.createCity(form); setForm({ name: '', state: '' }); }); }}>
        <div className="field grow"><input required placeholder="New city name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
        <div className="field grow"><input placeholder="State / country" value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} /></div>
        <div className="field"><button className="btn btn-primary">Add city</button></div>
      </form>
      <table className="table">
        <thead><tr><th>City</th><th>State</th><th>Status</th><th /></tr></thead>
        <tbody>
          {cities.map((c) => (
            <tr key={c.id}>
              <td>{c.name}</td>
              <td className="muted">{c.state}</td>
              <td><span className={`status ${c.active ? 'status-verified' : 'status-rejected'}`}>{c.active ? 'Active' : 'Hidden'}</span></td>
              <td>
                <button className="link-btn" onClick={() => {
                  const name = window.prompt('City name', c.name);
                  if (name) run(() => adminApi.updateCity(c.id, { name, state: c.state, active: c.active }));
                }}>Rename</button>
                <button className="link-btn" onClick={() => run(() => adminApi.updateCity(c.id, { name: c.name, state: c.state, active: !c.active }))}>
                  {c.active ? 'Hide' : 'Activate'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
