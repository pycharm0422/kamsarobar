export default function ProgressBar({ value, max }) {
  const pct = max > 0 ? Math.min(100, Math.round((Number(value) / Number(max)) * 100)) : 0;
  return (
    <div className="progress" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
      <div className="progress-fill" style={{ width: `${pct}%` }} />
    </div>
  );
}
