import { useState } from 'react';
import { CATEGORY_LABELS } from '../utils/format';

export default function PostForm({ initial, onSubmit, onCancel, submitLabel = 'Post' }) {
  const [form, setForm] = useState(initial || { category: 'GENERAL', title: '', content: '' });
  const [busy, setBusy] = useState(false);

  const handle = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await onSubmit(form);
      if (!initial) setForm({ category: form.category, title: '', content: '' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="post-form" onSubmit={handle}>
      <div className="form-row">
        <div className="field grow">
          <input aria-label="Title" required maxLength={200} placeholder="Title" value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </div>
        <div className="field">
          <select aria-label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>
      </div>
      <textarea aria-label="Content" required rows={3} maxLength={5000} placeholder="Share an update, a job opening, an event..."
        value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
      <div className="form-actions">
        {onCancel && <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>}
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : submitLabel}</button>
      </div>
    </form>
  );
}
