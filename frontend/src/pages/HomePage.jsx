import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { donationApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { formatMoney } from '../utils/format';

const FEATURES = [
  { icon: '🤝', title: 'Get referred', text: 'Saw a job at a company? Type its name and find members who can refer you - then message them on WhatsApp in one tap.', to: '/referrals' },
  { icon: '💡', title: 'Ask an expert', text: 'Need advice on a career, a skill or a field? Find community members who know it well.', to: '/experts' },
  { icon: '📸', title: 'Posts & seminars', text: 'Share anything with your city - news, photos, openings. Add seminars and events to your own upcoming list.', to: '/posts' },
  { icon: '❤️', title: 'Give back', text: 'Small contributions, pooled city-wise, for social causes. Every rupee collected is shown openly.', to: '/donate' },
];

export default function HomePage() {
  const { user } = useAuth();
  const [amount, setAmount] = useState(null);

  // Members see their own city's fund by default; visitors see the community-wide total.
  useEffect(() => {
    const request = user
      ? donationApi.citySummary(user.city.id).then((s) => s.collected)
      : donationApi.summary().then((s) => s.totalCollected);
    request.then(setAmount).catch(() => {});
  }, [user]);

  return (
    <>
      <section className="hero">
        <div className="container hero-inner">
          <div>
            <p className="eyebrow">Community network</p>
            <h1>
              Kamsar o Bar
              <span className="hero-sub">connect · help · grow together</span>
            </h1>
            <p className="hero-text">
              One place for our community across every city - find someone who can refer you, someone who can guide you,
              and a way to give back together.
            </p>
            <div className="hero-actions">
              {user ? (
                <>
                  <Link to="/referrals" className="btn btn-primary btn-lg">Find a referral</Link>
                  <Link to="/posts" className="btn btn-light btn-lg">See posts</Link>
                </>
              ) : (
                <>
                  <Link to="/register" className="btn btn-primary btn-lg">Join in 30 seconds</Link>
                  <Link to="/login" className="btn btn-light btn-lg">I already have an account</Link>
                </>
              )}
            </div>
          </div>
          <div className="hero-card card">
            <p className="muted small">
              {user ? `Collected in ${user.city.name} for community causes` : 'Collected so far for community causes'}
            </p>
            <p className="hero-amount">{formatMoney(amount)}</p>
            <Link to="/donate" className="btn btn-ghost">{user ? 'Contribute or see other cities →' : 'Learn more →'}</Link>
          </div>
        </div>
      </section>

      <section className="container features">
        {FEATURES.map((f) => (
          <Link key={f.title} to={user ? f.to : '/register'} className="card feature">
            <div className="feature-icon">{f.icon}</div>
            <h3>{f.title}</h3>
            <p className="muted">{f.text}</p>
          </Link>
        ))}
      </section>

      <section className="container how">
        <h2>How it works</h2>
        <ol className="steps">
          <li><strong>Sign up</strong> with your name, mobile number and city.</li>
          <li><strong>Complete your profile</strong> - LinkedIn, company, role, companies you can refer to and your expertise.</li>
          <li><strong>Search</strong> by company or expertise and reach out on WhatsApp with a ready-made message.</li>
          <li><strong>Join your city</strong> WhatsApp group, post updates and photos, attend seminars and support local causes.</li>
        </ol>
      </section>
    </>
  );
}
