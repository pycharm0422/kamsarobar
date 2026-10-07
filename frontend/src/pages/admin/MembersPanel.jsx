import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api';
import CitySelect from '../../components/CitySelect';
import Pagination from '../../components/Pagination';
import { useDebounce } from '../../hooks/useDebounce';
import { displayMobile, formatDate } from '../../utils/format';

const roleLabel = (u) =>
  u.role === 'CITY_ADMIN' ? `City admin (${u.managedCity?.name})` : u.role === 'MAIN_ADMIN' ? 'Main admin' : 'Member';

/** Member list. Pass fixedCityId for a city admin (they only ever see their own city). */
export default function MembersPanel({ fixedCityId }) {
  const [query, setQuery] = useState('');
  const [cityId, setCityId] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(null);
  const q = useDebounce(query, 300);

  const load = (p = 0) =>
    adminApi
      .members({ q: q || undefined, cityId: fixedCityId || cityId || undefined, status: status || undefined, page: p })
      .then(setPage);

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, cityId, status, fixedCityId]);

  return (
    <div className="card">
      <div className="form-row">
        <div className="field grow">
          <input placeholder="Search name or mobile" aria-label="Search members" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
        {!fixedCityId && (
          <div className="field"><CitySelect value={cityId} onChange={setCityId} allLabel="All cities" /></div>
        )}
        <div className="field">
          <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status">
            <option value="">Everyone</option>
            <option value="ACTIVE">Active</option>
            <option value="BLOCKED">Blocked</option>
          </select>
        </div>
      </div>
      <p className="muted small">{page?.totalElements ?? 0} members · tap a name to see the full profile</p>
      <ul className="member-list">
        {page?.content.map((u) => (
          <li key={u.id}>
            <Link to={`/admin/members/${u.id}`} className="member-row">
              <span className="avatar small">{u.name[0]?.toUpperCase()}</span>
              <span className="grow">
                <strong>{u.name}</strong>
                {u.blocked && <span className="status status-rejected">Blocked</span>}
                <br />
                <span className="muted small">
                  {displayMobile(u.mobile)} · {u.city.name} · {roleLabel(u)} · joined {formatDate(u.createdAt)}
                </span>
              </span>
              <span className="muted">›</span>
            </Link>
          </li>
        ))}
      </ul>
      <Pagination page={page} onChange={load} />
    </div>
  );
}
