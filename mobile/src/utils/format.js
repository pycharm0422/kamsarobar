const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const formatMoney = (value) => inr.format(Number(value || 0));

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '';

export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '';

export const displayMobile = (mobile) => (mobile ? `+${mobile}` : '');

export const CATEGORY_LABELS = {
  GENERAL: 'General',
  JOB_OPENING: 'Job opening',
  HELP_NEEDED: 'Help needed',
  SEMINAR: 'Seminar',
  EVENT: 'Event',
  ANNOUNCEMENT: 'Announcement',
};

export const isEventCategory = (category) => category === 'EVENT' || category === 'SEMINAR';

const time = (d) => d.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });

export function formatEventWhen(startsAt, endsAt) {
  const start = new Date(startsAt);
  const day = start.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  if (!endsAt) return `${day} · ${time(start)}`;
  const end = new Date(endsAt);
  return start.toDateString() === end.toDateString()
    ? `${day} · ${time(start)} – ${time(end)}`
    : `${day} · ${time(start)} – ${end.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })} ${time(end)}`;
}

/** Created/updated are stamped a few microseconds apart on save; only real edits count. */
export const wasEdited = (post) => new Date(post.updatedAt) - new Date(post.createdAt) > 2000;
