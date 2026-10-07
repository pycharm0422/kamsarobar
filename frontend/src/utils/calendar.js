const stamp = (date) => new Date(date).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');

const endOf = (event) => event.endsAt || new Date(new Date(event.startsAt).getTime() + 2 * 3600 * 1000).toISOString();

/** "Add to Google Calendar" link for an event / seminar post. */
export function googleCalendarLink(post) {
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: post.title || 'Kamsar o Bar event',
    dates: `${stamp(post.event.startsAt)}/${stamp(endOf(post.event))}`,
    details: [post.content, post.event.link].filter(Boolean).join('\n\n'),
    location: post.event.location || post.event.link || '',
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

/** Downloads an .ics file - opens in Apple Calendar, Outlook and most phone calendar apps. */
export function downloadIcs(post) {
  const escape = (s) => String(s || '').replace(/[\\,;]/g, (m) => `\\${m}`).replace(/\n/g, '\\n');
  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Kamsar o Bar//Events//EN',
    'BEGIN:VEVENT',
    `UID:post-${post.id}@kamsarobar`,
    `DTSTAMP:${stamp(new Date())}`,
    `DTSTART:${stamp(post.event.startsAt)}`,
    `DTEND:${stamp(endOf(post.event))}`,
    `SUMMARY:${escape(post.title || 'Kamsar o Bar event')}`,
    `DESCRIPTION:${escape([post.content, post.event.link].filter(Boolean).join('\n\n'))}`,
    `LOCATION:${escape(post.event.location || post.event.link)}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n');
  const url = URL.createObjectURL(new Blob([ics], { type: 'text/calendar' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `${(post.title || 'event').replace(/[^\w-]+/g, '-')}.ics`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
