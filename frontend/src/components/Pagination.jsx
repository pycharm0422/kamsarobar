export default function Pagination({ page, onChange }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button className="btn btn-ghost" disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>
        ← Previous
      </button>
      <span className="muted">
        Page {page.page + 1} of {page.totalPages}
      </span>
      <button className="btn btn-ghost" disabled={page.last} onClick={() => onChange(page.page + 1)}>
        Next →
      </button>
    </div>
  );
}
