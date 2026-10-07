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
  EVENT: 'Event',
  ANNOUNCEMENT: 'Announcement',
};
