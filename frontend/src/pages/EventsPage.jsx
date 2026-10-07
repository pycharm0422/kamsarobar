import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { eventApi } from '../api';
import Alert from '../components/Alert';
import CitySelect from '../components/CitySelect';
import EmptyState from '../components/EmptyState';
import EventActions from '../components/EventActions';
import Pagination from '../components/Pagination';
import Spinner from '../components/Spinner';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';
import { CATEGORY_LABELS, formatEventWhen } from '../utils/format';

export default function EventsPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState('mine');
  const [cityId, setCityId] = useState(String(user.city.id));
  const [page, setPage] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(
    (p = 0) => {
      setPage(null);
      const request = tab === 'mine' ? eventApi.mine({ page: p }) : eventApi.upcoming({ cityId: cityId || undefined, page: p });
      request.then(setPage).catch((e) => setError(errorMessage(e)));
    },
    [tab, cityId],
  );

  useEffect(() => load(0), [load]);

  const onChange = (updated) => {
    // Removing an event from "My upcoming events" takes it off that list.
    if (tab === 'mine' && !updated.event.attending) {
      setPage((pg) => ({ ...pg, content: pg.content.filter((p) => p.id !== updated.id), totalElements: pg.totalElements - 1 }));
    } else {
      setPage((pg) => ({ ...pg, content: pg.content.map((p) => (p.id === updated.id ? updated : p)) }));
    }
  };

  return (
    <div className="container page narrow">
      <div className="page-header">
        <h1>Events & seminars</h1>
        <p className="muted">Tap “Add to my events” on any event or seminar to keep it in your list.</p>
      </div>
      <div className="tabs">
        <button className={`tab ${tab === 'mine' ? 'active' : ''}`} onClick={() => setTab('mine')}>My upcoming events</button>
        <button className={`tab ${tab === 'all' ? 'active' : ''}`} onClick={() => setTab('all')}>Discover</button>
      </div>
      {tab === 'all' && (
        <div className="filters events-filter">
          <CitySelect value={cityId} onChange={setCityId} allLabel="All cities" />
        </div>
      )}
      <Alert onClose={() => setError('')}>{error}</Alert>

      {!page ? (
        <Spinner />
      ) : page.content.length === 0 ? (
        <EmptyState icon="🗓" title={tab === 'mine' ? 'No upcoming events yet' : 'No upcoming events here'}>
          {tab === 'mine' ? (
            <>Find one under <button className="link-btn" onClick={() => setTab('all')}>Discover</button>, or post a seminar on the <Link to="/posts">Posts</Link> page.</>
          ) : (
            <>Organising something? Post it as a Seminar or Event on the <Link to="/posts">Posts</Link> page.</>
          )}
        </EmptyState>
      ) : (
        page.content.map((post) => <EventCard key={post.id} post={post} onChange={onChange} />)
      )}
      <Pagination page={page} onChange={load} />
    </div>
  );
}

function EventCard({ post, onChange }) {
  const start = new Date(post.event.startsAt);
  return (
    <article className="card event-card">
      <div className="date-badge large" aria-hidden="true">
        <span className="date-month">{start.toLocaleDateString('en-IN', { month: 'short' })}</span>
        <span className="date-day">{start.getDate()}</span>
        <span className="date-weekday">{start.toLocaleDateString('en-IN', { weekday: 'short' })}</span>
      </div>
      <div className="grow">
        <span className={`badge badge-${post.category.toLowerCase()}`}>{CATEGORY_LABELS[post.category]}</span>
        <h3><Link to={`/posts/${post.id}`}>{post.title}</Link></h3>
        <p className="small">🗓 {formatEventWhen(post.event.startsAt, post.event.endsAt)}</p>
        {post.event.location && <p className="small">📍 {post.event.location}</p>}
        {post.event.link && <p className="small">🔗 <a href={post.event.link} target="_blank" rel="noreferrer">Join online</a></p>}
        <p className="muted small">By {post.author?.name} · {post.city?.name}</p>
        <EventActions post={post} onChange={onChange} />
      </div>
    </article>
  );
}
