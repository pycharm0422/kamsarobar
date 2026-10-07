import { useCallback, useState } from 'react';
import { CATEGORY_LABELS, fromLocalInput, isEventCategory, toLocalInput } from '../utils/format';
import { errorMessage } from '../utils/errors';
import Alert from './Alert';
import ImagePicker from './ImagePicker';

const EMPTY = { category: 'GENERAL', title: '', content: '', images: [], eventStartsAt: '', eventEndsAt: '', eventLocation: '', eventLink: '' };

export const formFromPost = (post) => ({
  category: post.category,
  title: post.title || '',
  content: post.content || '',
  images: post.images || [],
  eventStartsAt: toLocalInput(post.event?.startsAt),
  eventEndsAt: toLocalInput(post.event?.endsAt),
  eventLocation: post.event?.location || '',
  eventLink: post.event?.link || '',
});

/** Create / edit a post: text, photos, or both - and for seminars & events, date and place. */
export default function PostForm({ initial, onSubmit, onCancel, submitLabel = 'Post' }) {
  const [form, setForm] = useState(initial || EMPTY);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const isEvent = isEventCategory(form.category);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });
  const onBusyChange = useCallback(setUploading, []);

  const handle = async (e) => {
    e.preventDefault();
    if (!isEvent && !form.content.trim() && form.images.length === 0) {
      setError('Write something or add a photo.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await onSubmit({
        category: form.category,
        title: form.title,
        content: form.content,
        imageIds: form.images.map((img) => img.id),
        ...(isEvent && {
          eventStartsAt: fromLocalInput(form.eventStartsAt),
          eventEndsAt: fromLocalInput(form.eventEndsAt),
          eventLocation: form.eventLocation,
          eventLink: form.eventLink,
        }),
      });
      if (!initial) setForm({ ...EMPTY, category: form.category });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="post-form" onSubmit={handle}>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="form-row">
        <div className="field">
          <label htmlFor="post-category">What are you posting?</label>
          <select id="post-category" value={form.category} onChange={set('category')}>
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>
        <div className="field grow">
          <label htmlFor="post-title">{isEvent ? 'Title' : 'Title (optional)'}</label>
          <input id="post-title" maxLength={200} required={isEvent}
            placeholder={isEvent ? 'e.g. Career guidance seminar' : 'Add a heading'} value={form.title} onChange={set('title')} />
        </div>
      </div>

      {isEvent && (
        <div className="event-fields">
          <div className="form-row">
            <div className="field">
              <label htmlFor="ev-start">Starts</label>
              <input id="ev-start" type="datetime-local" required value={form.eventStartsAt} onChange={set('eventStartsAt')} />
            </div>
            <div className="field">
              <label htmlFor="ev-end">Ends (optional)</label>
              <input id="ev-end" type="datetime-local" min={form.eventStartsAt || undefined} value={form.eventEndsAt} onChange={set('eventEndsAt')} />
            </div>
          </div>
          <div className="form-row">
            <div className="field grow">
              <label htmlFor="ev-location">Venue</label>
              <input id="ev-location" maxLength={300} placeholder="Address or hall name" value={form.eventLocation} onChange={set('eventLocation')} />
            </div>
            <div className="field grow">
              <label htmlFor="ev-link">Online link (optional)</label>
              <input id="ev-link" type="url" maxLength={500} placeholder="https://meet.google.com/..." value={form.eventLink} onChange={set('eventLink')} />
            </div>
          </div>
        </div>
      )}

      <label htmlFor="post-content" className="sr-only">Post text</label>
      <textarea id="post-content" rows={3} maxLength={5000}
        placeholder={isEvent ? 'What is it about? Who should come?' : "What's on your mind? Share anything - news, photos, a job, a question..."}
        value={form.content} onChange={set('content')} />

      <ImagePicker images={form.images} onChange={(images) => setForm((f) => ({ ...f, images }))} onBusyChange={onBusyChange} />

      <div className="form-actions">
        {onCancel && <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>}
        <button className="btn btn-primary" disabled={busy || uploading}>
          {uploading ? 'Uploading photos…' : busy ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}
