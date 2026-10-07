import { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import CitySelect from '../../components/CitySelect';
import Pagination from '../../components/Pagination';
import { useDebounce } from '../../hooks/useDebounce';
import { displayMobile, formatDate } from '../../utils/format';

export default function MembersPanel() {
  const [query, setQuery] = useState('');
  const [cityId, setCityId] = useState('');
  const [page, setPage] = useState(null);
  const q = useDebounce(query, 300);

  const load = (p = 0) => adminApi.users({ q: q || undefined, cityId: cityId || undefined, page: p }).then(setPage);
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, cityId]);

  return (
    <div className="card">
      <div className="form-row">
        <div className="field grow"><input placeholder="Search name or mobile" value={query} onChange={(e) => setQuery(e.target.value)} /></div>
        <div className="field"><CitySelect value={cityId} onChange={setCityId} allLabel="All cities" /></div>
      </div>
      <p className="muted small">{page?.totalElements ?? 0} members</p>
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>Name</th><th>Mobile</th><th>City</th><th>Role</th><th>Joined</th></tr></thead>
          <tbody>
            {page?.content.map((u) => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td>{displayMobile(u.mobile)}</td>
                <td>{u.city.name}</td>
                <td>{u.role === 'CITY_ADMIN' ? `City admin (${u.managedCity?.name})` : u.role === 'MAIN_ADMIN' ? 'Main admin' : 'Member'}</td>
                <td className="muted">{formatDate(u.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} onChange={load} />
    </div>
  );
}
