/** WhatsApp "click to chat" with a pre-filled message; opens the WhatsApp app on the phone. */
export function whatsappLink(mobile, message) {
  return `https://wa.me/${String(mobile || '').replace(/\D/g, '')}?text=${encodeURIComponent(message)}`;
}

/** UPI deep link: opens GPay / PhonePe / Paytm with the payee filled in (Android). */
export function upiLink({ upiId, name, amount, note }) {
  const params = [`pa=${encodeURIComponent(upiId)}`, `pn=${encodeURIComponent(name || 'Kamsar o Bar')}`, 'cu=INR'];
  if (amount) params.push(`am=${encodeURIComponent(String(amount))}`);
  if (note) params.push(`tn=${encodeURIComponent(note)}`);
  return `upi://pay?${params.join('&')}`;
}

const stamp = (date) => new Date(date).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');

export function googleCalendarLink(post) {
  const end = post.event.endsAt || new Date(new Date(post.event.startsAt).getTime() + 2 * 3600 * 1000).toISOString();
  const q = [
    'action=TEMPLATE',
    `text=${encodeURIComponent(post.title || 'Kamsar o Bar event')}`,
    `dates=${stamp(post.event.startsAt)}/${stamp(end)}`,
    `details=${encodeURIComponent([post.content, post.event.link].filter(Boolean).join('\n\n'))}`,
    `location=${encodeURIComponent(post.event.location || post.event.link || '')}`,
  ];
  return `https://calendar.google.com/calendar/render?${q.join('&')}`;
}

const signature = (me, profile) =>
  [`- ${me.name}${me.city ? ` (${me.city.name})` : ''}`, profile?.linkedinUrl ? `LinkedIn: ${profile.linkedinUrl}` : null]
    .filter(Boolean)
    .join('\n');

export function referralMessage({ member, me, myProfile, company, role, jobLink }) {
  const target = member.matched?.[0] || company;
  return [
    `Hi ${member.name},`,
    '',
    `I found you on the Kamsar o Bar community app. I am interested in ${role ? `the *${role}* role` : 'a role'} at *${target}*` +
      (jobLink ? ` (${jobLink})` : '') +
      '.',
    'Would you be able to refer me? I can share my resume right away.',
    '',
    'Thank you!',
    signature(me, myProfile),
  ].join('\n');
}

export function adviceMessage({ member, me, myProfile, topic }) {
  return [
    `Hi ${member.name},`,
    '',
    `I found you on the Kamsar o Bar community app. I would really value your advice on *${member.matched?.[0] || topic}*.`,
    'Could we have a short chat when you are free?',
    '',
    'Thank you!',
    signature(me, myProfile),
  ].join('\n');
}
