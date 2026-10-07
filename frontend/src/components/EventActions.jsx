import { useState } from 'react';
import { eventApi } from '../api';
import { downloadIcs, googleCalendarLink } from '../utils/calendar';
import { errorMessage } from '../utils/errors';

/** "Add to my events" toggle plus "add to calendar" links for an event / seminar post. */
export default function EventActions({ post, onChange }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const { event } = post;

  const toggle = async () => {
    setBusy(true);
    setError('');
    try {
      onChange(await (event.attending ? eventApi.leave(post.id) : eventApi.attend(post.id)));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (event.ended) {
    return <p className="muted small">This event has ended · {event.attendeeCount} attended</p>;
  }

  return (
    <div className="event-actions">
      <button className={`btn ${event.attending ? 'btn-going' : 'btn-primary'}`} onClick={toggle} disabled={busy}>
        {event.attending ? '✓ In my events' : '＋ Add to my events'}
      </button>
      <span className="muted small">{event.attendeeCount} going</span>
      {event.attending && (
        <span className="calendar-links small">
          Add to: <a href={googleCalendarLink(post)} target="_blank" rel="noreferrer">Google Calendar</a>
          {' · '}
          <button type="button" className="link-btn" onClick={() => downloadIcs(post)}>Phone / Outlook</button>
        </span>
      )}
      {error && <p className="field-error">{error}</p>}
    </div>
  );
}
