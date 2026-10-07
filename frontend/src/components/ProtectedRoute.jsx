import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Spinner from './Spinner';

/** Renders children only for signed-in users (optionally restricted to some roles). */
export default function ProtectedRoute({ children, roles }) {
  const { user, ready, signedOut } = useAuth();
  const location = useLocation();

  if (!ready) return <Spinner />;
  if (!user) {
    return signedOut ? <Navigate to="/" replace /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) return <Navigate to="/posts" replace />;
  return children;
}
