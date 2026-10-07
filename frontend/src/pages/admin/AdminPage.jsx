import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import CityAdminsPanel from './CityAdminsPanel';
import CityManagePanel from './CityManagePanel';
import CitiesPanel from './CitiesPanel';
import MembersPanel from './MembersPanel';
import OverviewPanel from './OverviewPanel';

const MAIN_TABS = [
  { key: 'overview', label: 'Overview', Panel: OverviewPanel },
  { key: 'city-admins', label: 'City admins', Panel: CityAdminsPanel },
  { key: 'manage-city', label: 'Manage a city', Panel: CityManagePanel },
  { key: 'cities', label: 'Cities', Panel: CitiesPanel },
  { key: 'members', label: 'Members', Panel: MembersPanel },
];

export default function AdminPage() {
  const { user, isMainAdmin } = useAuth();
  const [tab, setTab] = useState('overview');

  const [cityTab, setCityTab] = useState('city');

  if (!isMainAdmin) {
    return (
      <div className="container page">
        <div className="page-header">
          <h1>{user.managedCity?.name} - city admin</h1>
          <p className="muted">Manage your city’s members, WhatsApp group, bank details, causes and donations.</p>
        </div>
        <div className="tabs">
          <button className={`tab ${cityTab === 'city' ? 'active' : ''}`} onClick={() => setCityTab('city')}>Manage city</button>
          <button className={`tab ${cityTab === 'members' ? 'active' : ''}`} onClick={() => setCityTab('members')}>Members</button>
        </div>
        {cityTab === 'city' ? <CityManagePanel fixedCityId={user.managedCity?.id} /> : <MembersPanel fixedCityId={user.managedCity?.id} />}
      </div>
    );
  }

  const { Panel } = MAIN_TABS.find((t) => t.key === tab);
  return (
    <div className="container page">
      <div className="page-header">
        <h1>Main admin</h1>
        <p className="muted">Appoint city heads, manage cities and oversee the community.</p>
      </div>
      <div className="tabs">
        {MAIN_TABS.map((t) => (
          <button key={t.key} className={`tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      <Panel />
    </div>
  );
}
