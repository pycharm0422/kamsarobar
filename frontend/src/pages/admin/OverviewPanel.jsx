import { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import Spinner from '../../components/Spinner';
import { formatMoney } from '../../utils/format';

export default function OverviewPanel() {
  const [stats, setStats] = useState(null);
  useEffect(() => {
    adminApi.stats().then(setStats);
  }, []);
  if (!stats) return <Spinner />;

  const items = [
    ['Members', stats.members],
    ['Cities', stats.cities],
    ['City admins', stats.cityAdmins],
    ['Posts', stats.posts],
    ['Collected', formatMoney(stats.totalCollected)],
  ];
  return (
    <div className="stats-grid">
      {items.map(([label, value]) => (
        <div className="card stat" key={label}>
          <p className="muted small">{label}</p>
          <p className="big-number">{value}</p>
        </div>
      ))}
    </div>
  );
}
