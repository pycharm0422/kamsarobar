import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { adminApi } from '../../api';
import Alert from '../../components/Alert';
import { WhatsAppIcon } from '../../components/MemberCard';
import Spinner from '../../components/Spinner';
import { errorMessage } from '../../utils/errors';
import { displayMobile, formatDate, formatDateTime, formatMoney } from '../../utils/format';
import { whatsappLink } from '../../utils/whatsapp';

const ROLE = { MEMBER: 'Member', CITY_ADMIN: 'City admin', MAIN_ADMIN: 'Main admin' };

/** Admin view of one member: full profile, activity, and block / unblock. */
export default function MemberPage() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    adminApi.member(id).then(setData).catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error && !data) {
    return (
      <div className="container page narrow">
        <Link to="/admin" className="muted">← Back to admin</Link>
        <Alert>{error}</Alert>
      </div>
    );
  }
  if (!data) return <Spinner />;

  const { member, profile, activity, block } = data;
  return (
    <div className="container page narrow">
      <Link to="/admin" className="muted">← Back to admin</Link>

      <div className="card member-header">
        <span className="avatar large">{member.name[0]?.toUpperCase()}</span>
        <div className="grow">
          <h1>
            {member.name} {block.blocked && <span className="status status-rejected">Blocked</span>}
          </h1>
          <p className="muted">
            {ROLE[member.role]}
            {member.managedCity && ` of ${member.managedCity.name}`} · 📍 {member.city.name} · joined {formatDate(member.createdAt)}
          </p>
          <div className="member-actions">
            <a className="btn btn-whatsapp" href={whatsappLink(member.mobile, `Hi ${member.name}, `)} target="_blank" rel="noreferrer">
              <WhatsAppIcon /> WhatsApp
            </a>
            <a className="btn btn-ghost" href={`tel:+${member.mobile}`}>📞 {displayMobile(member.mobile)}</a>
          </div>
        </div>
      </div>

      <div className="card">
        <h3>Work profile</h3>
        {!profile.completed ? (
          <p className="muted">This member has not filled in their work profile yet.</p>
        ) : (
          <>
            <dl className="details">
              <dt>Position</dt>
              <dd>{[profile.position, profile.currentCompany].filter(Boolean).join(' at ') || '—'}</dd>
              <dt>Experience</dt>
              <dd>{profile.yearsOfExperience != null ? `${profile.yearsOfExperience} years` : '—'}</dd>
              <dt>LinkedIn</dt>
              <dd>{profile.linkedinUrl ? <a href={profile.linkedinUrl} target="_blank" rel="noreferrer">{profile.linkedinUrl}</a> : '—'}</dd>
              <dt>In search</dt>
              <dd>{profile.openToHelp ? 'Shown in referral & expert search' : 'Hidden from search'}</dd>
            </dl>
            {profile.bio && <p className="member-bio">{profile.bio}</p>}
            <p className="small"><strong>Can refer to</strong></p>
            <div className="tags">
              {profile.referralCompanies.length ? profile.referralCompanies.map((c) => <span key={c} className="tag">🏢 {c}</span>) : <span className="muted small">—</span>}
            </div>
            <p className="small"><strong>Expertise</strong></p>
            <div className="tags">
              {profile.expertise.length ? profile.expertise.map((x) => <span key={x} className="tag">💡 {x}</span>) : <span className="muted small">—</span>}
            </div>
          </>
        )}
      </div>

      <div className="stats-grid member-stats">
        <Stat label="Posts" value={activity.posts} />
        <Stat label="Comments" value={activity.comments} />
        <Stat label="Events added" value={activity.eventsAdded} />
        <Stat label="Donations" value={activity.donations} />
        <Stat label="Donated (verified)" value={formatMoney(activity.donatedVerified)} />
      </div>

      <BlockCard data={data} onChange={setData} />
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="card stat">
      <p className="muted small">{label}</p>
      <p className="stat-value">{value}</p>
    </div>
  );
}

function BlockCard({ data, onChange }) {
  const { member, block, canBlock } = data;
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try {
      onChange(await fn());
      setReason('');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (block.blocked) {
    return (
      <div className="card block-card blocked">
        <h3>🚫 This member is blocked</h3>
        <p><strong>Reason:</strong> {block.reason}</p>
        <p className="muted small">Blocked {formatDateTime(block.blockedAt)}{block.blockedBy && ` by ${block.blockedBy}`}</p>
        <p className="small">
          They cannot log in, they are hidden from referral and expert search, and their posts and comments are hidden.
          Nothing has been deleted.
        </p>
        <Alert onClose={() => setError('')}>{error}</Alert>
        {canBlock && (
          <button className="btn btn-primary" disabled={busy} onClick={() => run(() => adminApi.unblock(member.id))}>
            Unblock {member.name.split(' ')[0]}
          </button>
        )}
      </div>
    );
  }

  if (!canBlock) {
    return (
      <div className="card block-card">
        <p className="muted small">
          {member.role === 'MEMBER' ? 'You cannot block this member.' : 'Admins can only be blocked by the main admin (and the main admin cannot be blocked).'}
        </p>
      </div>
    );
  }

  return (
    <form
      className="card block-card"
      onSubmit={(e) => {
        e.preventDefault();
        if (window.confirm(`Block ${member.name}? They will be logged out immediately.`)) {
          run(() => adminApi.block(member.id, reason));
        }
      }}
    >
      <h3>Block this member</h3>
      <p className="muted small">
        Use for spam, fake accounts or abuse. They are logged out at once, cannot log in or register again with this number,
        disappear from search, and their posts and comments are hidden. You can unblock them any time - nothing is deleted.
      </p>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="field">
        <label htmlFor="block-reason">Reason</label>
        <textarea id="block-reason" rows={2} required maxLength={300} placeholder="e.g. Posting spam links"
          value={reason} onChange={(e) => setReason(e.target.value)} />
      </div>
      <div className="form-actions">
        <button className="btn btn-danger" disabled={busy || !reason.trim()}>Block member</button>
      </div>
    </form>
  );
}
