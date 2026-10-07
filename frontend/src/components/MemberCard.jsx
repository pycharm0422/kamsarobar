import { displayMobile } from '../utils/format';

/** A community member in search results, with their matching companies / skills highlighted. */
export default function MemberCard({ member, actionLabel, onAction }) {
  const matched = new Set((member.matched || []).map((m) => m.toLowerCase()));
  const tags = [...member.referralCompanies.map((t) => ({ t, kind: 'company' })), ...member.expertise.map((t) => ({ t, kind: 'skill' }))];

  return (
    <article className="card member-card">
      <div className="member-head">
        <span className="avatar">{member.name[0]?.toUpperCase()}</span>
        <div>
          <h3>{member.name}</h3>
          <p className="muted">
            {[member.position, member.currentCompany].filter(Boolean).join(' at ') || 'Community member'}
            {member.yearsOfExperience != null && ` · ${member.yearsOfExperience} yrs`}
          </p>
          <p className="muted small">📍 {member.city?.name}</p>
        </div>
      </div>
      {member.bio && <p className="member-bio">{member.bio}</p>}
      <div className="tags">
        {tags.map(({ t, kind }) => (
          <span key={kind + t} className={`tag ${matched.has(t.toLowerCase()) ? 'tag-match' : ''} tag-${kind}`}>
            {kind === 'company' ? '🏢 ' : '💡 '}
            {t}
          </span>
        ))}
      </div>
      <div className="member-actions">
        <button className="btn btn-whatsapp" onClick={() => onAction(member)}>
          <WhatsAppIcon /> {actionLabel}
        </button>
        {member.linkedinUrl && (
          <a className="btn btn-ghost" href={member.linkedinUrl} target="_blank" rel="noreferrer">
            LinkedIn ↗
          </a>
        )}
        <span className="muted small">{displayMobile(member.mobile)}</span>
      </div>
    </article>
  );
}

export function WhatsAppIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.5 14.4c-.3-.1-1.8-.9-2-1-.3-.1-.5-.1-.7.1-.2.3-.8 1-.9 1.2-.2.2-.3.2-.6.1-.3-.1-1.3-.5-2.4-1.5-.9-.8-1.5-1.8-1.7-2.1-.2-.3 0-.5.1-.6l.4-.5c.2-.2.2-.3.3-.5.1-.2 0-.4 0-.5l-.9-2.2c-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4-.3.3-1 1-1 2.5s1.1 2.9 1.2 3.1c.1.2 2.1 3.2 5.1 4.5.7.3 1.3.5 1.7.6.7.2 1.4.2 1.9.1.6-.1 1.8-.7 2-1.4.2-.7.2-1.3.2-1.4-.1-.1-.3-.2-.6-.3zM12 21.8c-1.8 0-3.5-.5-5-1.4l-.4-.2-3.7 1 1-3.6-.2-.4c-1-1.6-1.5-3.4-1.5-5.2 0-5.4 4.4-9.8 9.8-9.8 2.6 0 5.1 1 6.9 2.9 1.8 1.8 2.9 4.3 2.9 6.9 0 5.4-4.4 9.8-9.8 9.8zm8.4-18.2C18.1 1.3 15.1 0 12 0 5.4 0 .1 5.3.1 11.9c0 2.1.5 4.1 1.6 5.9L0 24l6.3-1.7c1.7.9 3.7 1.4 5.7 1.4 6.6 0 11.9-5.3 11.9-11.9 0-3.2-1.2-6.2-3.5-8.2z" />
    </svg>
  );
}
