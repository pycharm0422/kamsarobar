import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const close = () => setOpen(false);

  const handleLogout = () => {
    logout();
    close();
    navigate('/');
  };

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand" onClick={close}>
          <img src="/logo.svg" alt="" width="32" height="32" />
          <span>Kamsar o Bar</span>
        </Link>
        <button className="nav-toggle" aria-label="Menu" aria-expanded={open} onClick={() => setOpen(!open)}>
          <span />
          <span />
          <span />
        </button>
        <nav className={`nav-links ${open ? 'open' : ''}`}>
          {user ? (
            <>
              <NavLink to="/posts" onClick={close}>Posts</NavLink>
              <NavLink to="/events" onClick={close}>Events</NavLink>
              <NavLink to="/referrals" onClick={close}>Find referral</NavLink>
              <NavLink to="/experts" onClick={close}>Find expert</NavLink>
              <NavLink to="/donate" onClick={close}>Donate</NavLink>
              {isAdmin && <NavLink to="/admin" onClick={close}>Admin</NavLink>}
              <NavLink to="/profile" onClick={close} className="nav-user">
                <span className="avatar small">{user.name?.[0]?.toUpperCase()}</span>
                {user.name.split(' ')[0]}
              </NavLink>
              <button className="btn btn-ghost" onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <NavLink to="/donate" onClick={close}>Donations</NavLink>
              <NavLink to="/login" onClick={close}>Log in</NavLink>
              <Link to="/register" className="btn btn-primary" onClick={close}>Join the community</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
