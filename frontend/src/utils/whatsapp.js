/**
 * WhatsApp "click to chat" link with a pre-filled message.
 * Works on phones (opens the app) and desktops (opens WhatsApp Web / Desktop).
 * The number must be digits only, with country code - which is how the backend stores it.
 */
export function whatsappLink(mobile, message) {
  const number = String(mobile || '').replace(/\D/g, '');
  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}

const signature = (me, profile) => {
  const lines = [`- ${me.name}${me.city ? ` (${me.city.name})` : ''}`];
  if (profile?.linkedinUrl) lines.push(`LinkedIn: ${profile.linkedinUrl}`);
  return lines.join('\n');
};

export function referralMessage({ member, me, myProfile, company, role, jobLink }) {
  const target = member.matched?.[0] || company;
  const roleText = role ? `the *${role}* role` : 'a role';
  return [
    `Hi ${member.name},`,
    '',
    `I found you on the Kamsar o Bar community app. I am interested in ${roleText} at *${target}*` +
      (jobLink ? ` (${jobLink})` : '') +
      '.',
    'Would you be able to refer me? I can share my resume right away.',
    '',
    'Thank you!',
    signature(me, myProfile),
  ].join('\n');
}

export function adviceMessage({ member, me, myProfile, topic }) {
  const area = member.matched?.[0] || topic;
  return [
    `Hi ${member.name},`,
    '',
    `I found you on the Kamsar o Bar community app. I would really value your advice on *${area}*.`,
    'Could we have a short chat when you are free?',
    '',
    'Thank you!',
    signature(me, myProfile),
  ].join('\n');
}

/** UPI deep link: opens GPay / PhonePe / Paytm etc. on a phone with the payee pre-filled. */
export function upiLink({ upiId, name, amount, note }) {
  const params = new URLSearchParams({ pa: upiId, pn: name || 'Kamsar o Bar', cu: 'INR' });
  if (amount) params.set('am', String(amount));
  if (note) params.set('tn', note);
  return `upi://pay?${params.toString()}`;
}
