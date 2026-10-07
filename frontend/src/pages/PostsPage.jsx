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

export default function PostsPage() {
  const { user } = useAuth();
  const [cityId, setCityId] = useState(String(user.city.id));
  const [category, setCategory] = useState('');
  const [feed, setFeed] = useState(null);
  const [city, setCity] = useState(null);
  const [fund, setFund] = useState(null);
  const [error, setError] = useState('');

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
    setFund(null);
    if (!cityId) return;
    cityApi.get(cityId).then(setCity).catch(() => {});
    donationApi.citySummary(cityId).then(setFund).catch(() => {});
  }, [cityId]);

  const replace = (updated) => setFeed((f) => ({ ...f, content: f.content.map((p) => (p.id === updated.id ? updated : p)) }));

  const createPost = async (payload) => {
    await postApi.create(payload);
    if (cityId && cityId !== String(user.city.id)) setCityId(String(user.city.id));
    else load(0);
  };
  const updatePost = async (id, payload) => replace(await postApi.update(id, payload));
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
          <h1>Posts</h1>
          <p className="muted">{city ? `What's happening in ${city.name}` : 'Posts from every city'}</p>
        </div>
        <div className="filters">
          <CitySelect value={cityId} onChange={setCityId} allLabel="All cities" />
          <select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="Filter by type">
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
          <div className="card">
            <PostForm onSubmit={createPost} />

          </div>
          {cityId && cityId !== String(user.city.id) && city && (
            <p className="muted small other-city-note">
              Showing {city.name} posts that are shared with everyone. Posts meant only for {city.name} members are not shown.
            </p>
          )}
          {!feed ? (
            <Spinner />
          ) : feed.content.length === 0 ? (
            <EmptyState icon="📝" title="No posts yet">Be the first to share something!</EmptyState>
          ) : (
            feed.content.map((p) => (
              <PostCard key={p.id} post={p} onUpdate={updatePost} onDelete={deletePost} onChange={replace} />
            ))
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
              <Link to="/events" className="btn btn-ghost btn-block sidebar-link">🗓 Upcoming events</Link>
            </div>
            <div className="card">
              <h3>❤️ {city.name} fund</h3>
              <p className="big-number">{formatMoney(fund?.collected)}</p>
              <p className="muted small">collected from {fund?.donations || 0} contribution(s)</p>
              <Link to={`/donate?city=${city.id}`} className="btn btn-primary btn-block">Contribute</Link>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
