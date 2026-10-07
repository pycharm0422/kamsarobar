const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const formatMoney = (value) => inr.format(Number(value || 0));

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '';

export const formatDateTime = (value) =>
  value
    ? new Date(value).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })
    : '';

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

/** e.g. "Sat, 12 Oct 2026 · 6:00 pm – 8:00 pm" in the viewer's own time zone. */
export function formatEventWhen(startsAt, endsAt) {
  const start = new Date(startsAt);
  const day = start.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  if (!endsAt) return `${day} · ${time(start)}`;
  const end = new Date(endsAt);
  const sameDay = start.toDateString() === end.toDateString();
  return sameDay
    ? `${day} · ${time(start)} – ${time(end)}`
    : `${day} · ${time(start)} – ${end.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })} ${time(end)}`;
}

/** ISO instant <-> value of an <input type="datetime-local"> (local time). */
export function toLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export const fromLocalInput = (value) => (value ? new Date(value).toISOString() : null);
