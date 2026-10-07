import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { postApi } from '../api';
import Alert from '../components/Alert';
import PostCard from '../components/PostCard';
import Spinner from '../components/Spinner';
import { errorMessage } from '../utils/errors';
import { formatDateTime } from '../utils/format';

export default function PostDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [text, setText] = useState('');
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const loadComments = useCallback(() => postApi.comments(id).then((p) => setComments(p.content)), [id]);

  useEffect(() => {
    postApi.get(id).then(setPost).catch((e) => setError(errorMessage(e)));
    loadComments();
  }, [id, loadComments]);

  const run = (fn) => fn().catch((e) => setError(errorMessage(e)));

  const addComment = (e) => {
    e.preventDefault();
    run(async () => {
      await postApi.addComment(id, { content: text });
      setText('');
      loadComments();
    });
  };

  if (error && !post) return <div className="container page"><Alert>{error}</Alert></div>;
  if (!post) return <Spinner />;

  return (
    <div className="container page narrow">
      <Link to="/community" className="muted">← Back to community</Link>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <PostCard
        full
        post={post}
        onUpdate={(pid, form) => run(async () => setPost(await postApi.update(pid, form)))}
        onDelete={(pid) => run(async () => { await postApi.remove(pid); navigate('/community'); })}
      />

      <section className="card">
        <h3>Comments ({comments.length})</h3>
        <ul className="comments">
          {comments.map((c) => (
            <li key={c.id} className="comment">
              <span className="avatar small">{c.author?.name?.[0]}</span>
              <div className="grow">
                <p className="small"><strong>{c.author?.name}</strong> <span className="muted">· {formatDateTime(c.createdAt)}</span></p>
                {editing?.id === c.id ? (
                  <form onSubmit={(e) => {
                    e.preventDefault();
                    run(async () => { await postApi.updateComment(c.id, { content: editing.content }); setEditing(null); loadComments(); });
                  }}>
                    <textarea rows={2} value={editing.content} onChange={(e) => setEditing({ ...editing, content: e.target.value })} />
                    <div className="form-actions">
                      <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)}>Cancel</button>
                      <button className="btn btn-primary">Save</button>
                    </div>
                  </form>
                ) : (
                  <p className="post-content">{c.content}</p>
                )}
                {c.canEdit && editing?.id !== c.id && (
                  <div className="post-actions">
                    <button className="link-btn" onClick={() => setEditing({ id: c.id, content: c.content })}>Edit</button>
                    <button className="link-btn danger" onClick={() => window.confirm('Delete this comment?') &&
                      run(async () => { await postApi.removeComment(c.id); loadComments(); })}>Delete</button>
                  </div>
                )}
              </div>
            </li>
          ))}
        </ul>
        <form onSubmit={addComment}>
          <textarea rows={2} required maxLength={2000} placeholder="Write a comment..." value={text} onChange={(e) => setText(e.target.value)} />
          <div className="form-actions">
            <button className="btn btn-primary">Comment</button>
          </div>
        </form>
      </section>
    </div>
  );
}
