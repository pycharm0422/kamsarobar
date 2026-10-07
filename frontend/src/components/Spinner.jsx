export default function Spinner({ label = 'Loading...' }) {
  return (
    <div className="spinner-wrap" role="status">
      <div className="spinner" />
      <span className="muted">{label}</span>
    </div>
  );
}
