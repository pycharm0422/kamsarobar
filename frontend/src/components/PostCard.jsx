import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CATEGORY_LABELS, formatDateTime } from '../utils/format';
import PostForm from './PostForm';

export default function PostCard({ post, onUpdate, onDelete, full = false }) {
  const [editing, setEditing] = useState(false);

  if (editing) {
    return (
      <article className="card post-card">
        <PostForm
          initial={{ category: post.category, title: post.title, content: post.content }}
          submitLabel="Save"
          onCancel={() => setEditing(false)}
          onSubmit={async (form) => {
            await onUpdate(post.id, form);
            setEditing(false);
          }}
        />
      </article>
    );
  }

  return (
    <article className="card post-card">
      <div className="post-meta">
        <span className={`badge badge-${post.category.toLowerCase()}`}>{CATEGORY_LABELS[post.category]}</span>
        <span className="muted small">
          {post.author?.name} · {post.city?.name} · {formatDateTime(post.createdAt)}
          {post.updatedAt !== post.createdAt && ' · edited'}
        </span>
      </div>
      <h3>{full ? post.title : <Link to={`/posts/${post.id}`}>{post.title}</Link>}</h3>
      <p className={`post-content ${full ? '' : 'clamp'}`}>{post.content}</p>
      <div className="post-footer">
        {!full && <Link to={`/posts/${post.id}`} className="muted small">💬 {post.commentCount} comment{post.commentCount === 1 ? '' : 's'}</Link>}
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
