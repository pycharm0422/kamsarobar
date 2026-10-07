import { Link } from 'react-router-dom';
import EmptyState from '../components/EmptyState';

export default function NotFoundPage() {
  return (
    <div className="container page">
      <EmptyState icon="🧭" title="Page not found">
        <Link to="/">Go to the home page</Link>
      </EmptyState>
    </div>
  );
}
