import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { cityApi, donationApi, postApi } from '../api';
import Alert from '../components/Alert';
import CitySelect from '../components/CitySelect';
import EmptyState from '../components/EmptyState';
import { WhatsAppIcon } from '../components/MemberCard';
import Pagination from '../components/Pagination';
import PostCard from '../components/PostCard';
import PostForm from '../components/PostForm';
import Spinner from '../components/Spinner';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';
import { CATEGORY_LABELS, formatMoney } from '../utils/format';

export default function CommunityPage() {
  const { user } = useAuth();
  const [cityId, setCityId] = useState(String(user.city.id));
  const [category, setCategory] = useState('');
  const [feed, setFeed] = useState(null);
  const [city, setCity] = useState(null);
  const [collected, setCollected] = useState(null);
  const [error, setError] = useState('');
  const isMyCity = cityId === String(user.city.id);

  const load = useCallback(
    (page = 0) => {
      setFeed(null);
      postApi
        .feed({ cityId: cityId || undefined, category: category || undefined, page })
        .then(setFeed)
        .catch((e) => setError(errorMessage(e)));
    },
    [cityId, category],
  );

  useEffect(() => load(0), [load]);

  useEffect(() => {
    setCity(null);
    if (!cityId) return;
    cityApi.get(cityId).then(setCity).catch(() => {});
    donationApi
      .summary()
      .then((s) => setCollected(s.cities.find((c) => String(c.cityId) === cityId) || null))
      .catch(() => {});
  }, [cityId]);

  const createPost = async (form) => {
    try {
      await postApi.create(form);
      load(0);
    } catch (e) {
      setError(errorMessage(e));
    }
  };
  const updatePost = async (id, form) => {
    try {
      const updated = await postApi.update(id, form);
      setFeed((f) => ({ ...f, content: f.content.map((p) => (p.id === id ? updated : p)) }));
    } catch (e) {
      setError(errorMessage(e));
    }
  };
  const deletePost = async (id) => {
    try {
      await postApi.remove(id);
      load(feed.page);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div className="container page">
      <div className="page-header row-between">
        <div>
          <h1>{city ? `${city.name} community` : 'All cities'}</h1>
          <p className="muted">Posts, openings and events from members{city ? ` in ${city.name}` : ''}.</p>
        </div>
        <div className="filters">
          <CitySelect value={cityId} onChange={setCityId} allLabel="All cities" />
          <select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="Category">
            <option value="">All posts</option>
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="layout-sidebar">
        <div>
          <Alert onClose={() => setError('')}>{error}</Alert>
          {isMyCity && (
            <div className="card">
              <PostForm onSubmit={createPost} />
            </div>
          )}
          {!feed ? (
            <Spinner />
          ) : feed.content.length === 0 ? (
            <EmptyState icon="📝" title="No posts yet">
              {isMyCity ? 'Start the conversation in your city!' : 'Nothing has been posted here yet.'}
            </EmptyState>
          ) : (
            feed.content.map((p) => <PostCard key={p.id} post={p} onUpdate={updatePost} onDelete={deletePost} />)
          )}
          <Pagination page={feed} onChange={load} />
        </div>

        {city && (
          <aside className="sidebar">
            <div className="card">
              <h3>📍 {city.name}</h3>
              {city.whatsappGroupUrl ? (
                <a className="btn btn-whatsapp btn-block" href={city.whatsappGroupUrl} target="_blank" rel="noreferrer">
                  <WhatsAppIcon /> Join {city.name} WhatsApp group
                </a>
              ) : (
                <p className="muted small">The city admin has not added a WhatsApp group link yet.</p>
              )}
            </div>
            <div className="card">
              <h3>❤️ Community fund</h3>
              <p className="big-number">{formatMoney(collected?.collected)}</p>
              <p className="muted small">collected in {city.name} from {collected?.donations || 0} contribution(s)</p>
              <Link to={`/donate?city=${city.id}`} className="btn btn-primary btn-block">Contribute</Link>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
