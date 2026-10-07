import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CATEGORY_LABELS, formatDateTime } from '../utils/format';
import EventDetails from './EventDetails';
import PostForm, { formFromPost } from './PostForm';
import PostImages from './PostImages';

/**
 * One post. `onUpdate(id, payload)` saves an edit (and should throw on failure), `onDelete(id)` removes it,
 * `onChange(updatedPost)` replaces it after an "Add to my events" click.
 */
// created/updated timestamps are set a few microseconds apart on save; only real edits count.
const wasEdited = (post) => new Date(post.updatedAt) - new Date(post.createdAt) > 2000;

export default function PostCard({ post, onUpdate, onDelete, onChange, full = false }) {
  const [editing, setEditing] = useState(false);

  if (editing) {
    return (
      <article className="card post-card">
        <PostForm
          initial={formFromPost(post)}
          submitLabel="Save"
          onCancel={() => setEditing(false)}
          onSubmit={async (payload) => {
            await onUpdate(post.id, payload);
            setEditing(false);
          }}
        />
      </article>
    );
  }

  return (
    <article className="card post-card">
      <div className="post-meta">
        <span className="avatar small">{post.author?.name?.[0]}</span>
        <div className="grow">
          <strong className="small">{post.author?.name}</strong>
          <span className="muted small">
            {' '}· {post.city?.name} ·{' '}
            {full ? formatDateTime(post.createdAt) : <Link to={`/posts/${post.id}`} className="muted">{formatDateTime(post.createdAt)}</Link>}
            {wasEdited(post) && ' · edited'}
          </span>
        </div>
        {post.category !== 'GENERAL' && (
          <span className={`badge badge-${post.category.toLowerCase()}`}>{CATEGORY_LABELS[post.category]}</span>
        )}
      </div>
      {post.title && <h3>{full ? post.title : <Link to={`/posts/${post.id}`}>{post.title}</Link>}</h3>}
      {post.content && <p className={`post-content ${full ? '' : 'clamp'}`}>{post.content}</p>}
      <PostImages images={post.images} />
      {post.event && <EventDetails post={post} onChange={onChange} />}
      <div className="post-footer">
        {!full && (
          <Link to={`/posts/${post.id}`} className="muted small">
            💬 {post.commentCount} comment{post.commentCount === 1 ? '' : 's'}
          </Link>
        )}
        {post.canEdit && (
          <div className="post-actions">
            <button className="link-btn" onClick={() => setEditing(true)}>Edit</button>
            <button className="link-btn danger" onClick={() => window.confirm('Delete this post?') && onDelete(post.id)}>Delete</button>
          </div>
        )}
      </div>
    </article>
  );
}
