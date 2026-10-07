import { useState } from 'react';
import Modal from './Modal';
import { WhatsAppIcon } from './MemberCard';
import { whatsappLink } from '../utils/whatsapp';

/** Lets the member review / edit the pre-written message before WhatsApp opens. */
export default function WhatsAppComposer({ member, initialMessage, onClose }) {
  const [message, setMessage] = useState(initialMessage);

  return (
    <Modal title={`Message ${member.name}`} onClose={onClose}>
      <p className="muted small">Edit the message if you like, then tap “Open WhatsApp”.</p>
      <textarea rows={10} value={message} onChange={(e) => setMessage(e.target.value)} />
      <div className="modal-actions">
        <button className="btn btn-ghost" onClick={() => navigator.clipboard?.writeText(message)}>
          Copy text
        </button>
        <a
          className="btn btn-whatsapp"
          href={whatsappLink(member.mobile, message)}
          target="_blank"
          rel="noreferrer"
          onClick={onClose}
        >
          <WhatsAppIcon /> Open WhatsApp
        </a>
      </div>
    </Modal>
  );
}
