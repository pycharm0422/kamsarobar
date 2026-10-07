import { formatEventWhen } from '../utils/format';
import EventActions from './EventActions';

export default function EventDetails({ post, onChange }) {
  const { event } = post;
  const start = new Date(event.startsAt);
  return (
    <div className="event-box">
      <div className="date-badge" aria-hidden="true">
        <span className="date-month">{start.toLocaleDateString('en-IN', { month: 'short' })}</span>
        <span className="date-day">{start.getDate()}</span>
      </div>
      <div className="grow">
        <p className="event-when">🗓 {formatEventWhen(event.startsAt, event.endsAt)}</p>
        {event.location && <p className="small">📍 {event.location}</p>}
        {event.link && (
          <p className="small">
            🔗 <a href={event.link} target="_blank" rel="noreferrer">Join online</a>
          </p>
        )}
        <EventActions post={post} onChange={onChange} />
      </div>
    </div>
  );
}
