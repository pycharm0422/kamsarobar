import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { profileApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../utils/errors';
import Alert from './Alert';
import CitySelect from './CitySelect';
import EmptyState from './EmptyState';
import MemberCard from './MemberCard';
import Pagination from './Pagination';
import Spinner from './Spinner';
import SuggestInput from './SuggestInput';
import WhatsAppComposer from './WhatsAppComposer';

/**
 * Generic "find a member" page. Referral search and expert search are both just configurations
 * of this component (see FindReferralPage / FindExpertPage) - a new search type needs no changes here.
 */
export default function DirectorySearch({ config }) {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const [term, setTerm] = useState(params.get('q') || '');
  const [cityId, setCityId] = useState(params.get('city') || '');
  const [extras, setExtras] = useState({});
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [myProfile, setMyProfile] = useState(null);

  useEffect(() => {
    profileApi.mine().then(setMyProfile).catch(() => {});
  }, []);

  const run = (page = 0) => {
    const q = term.trim();
    if (q.length < 2) {
      setError('Type at least 2 characters.');
      return;
    }
    setError('');
    setLoading(true);
    setParams({ q, ...(cityId ? { city: cityId } : {}) }, { replace: true });
    config
      .search({ [config.param]: q, cityId: cityId || undefined, page })
      .then(setResult)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  };

  // Re-run a search that came from the URL (e.g. a shared link).
  useEffect(() => {
    if (params.get('q')) run();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSubmit = (e) => {
    e.preventDefault();
    run(0);
  };

  return (
    <div className="container page">
      <div className="page-header">
        <h1>{config.title}</h1>
        <p className="muted">{config.subtitle}</p>
      </div>

      <form className="card search-card" onSubmit={onSubmit}>
        <div className="search-grid">
          <div className="field grow">
            <label htmlFor="term">{config.label}</label>
            <SuggestInput id="term" value={term} onChange={setTerm} suggest={config.suggest} placeholder={config.placeholder} />
          </div>
          <div className="field">
            <label htmlFor="city">City</label>
            <CitySelect id="city" value={cityId} onChange={setCityId} allLabel="All cities" />
          </div>
          {config.extraFields?.map((f) => (
            <div className="field" key={f.name}>
              <label htmlFor={f.name}>{f.label}</label>
              <input
                id={f.name}
                placeholder={f.placeholder}
                value={extras[f.name] || ''}
                onChange={(e) => setExtras({ ...extras, [f.name]: e.target.value })}
              />
            </div>
          ))}
          <div className="field field-action">
            <button className="btn btn-primary" disabled={loading}>
              {loading ? 'Searching...' : 'Search'}
            </button>
          </div>
        </div>
      </form>

      <Alert onClose={() => setError('')}>{error}</Alert>
      {loading && <Spinner label="Searching the community..." />}

      {!loading && result && (
        <>
          <p className="muted result-count">
            {result.totalElements} member{result.totalElements === 1 ? '' : 's'} found
          </p>
          {result.content.length === 0 ? (
            <EmptyState title="No one found yet" icon={config.emptyIcon}>
              {config.emptyText}
            </EmptyState>
          ) : (
            <div className="grid-cards">
              {result.content.map((m) => (
                <MemberCard key={m.userId} member={m} actionLabel={config.actionLabel} onAction={setSelected} />
              ))}
            </div>
          )}
          <Pagination page={result} onChange={run} />
        </>
      )}

      {selected && (
        <WhatsAppComposer
          member={selected}
          initialMessage={config.buildMessage({ member: selected, me: user, myProfile, term: term.trim(), ...extras })}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}
