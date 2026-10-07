import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { donationApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { formatMoney } from '../utils/format';

const FEATURES = [
  { icon: '🤝', title: 'Get referred', text: 'Saw a job at a company? Type its name and find members who can refer you - then message them on WhatsApp in one tap.', to: '/referrals' },
  { icon: '💡', title: 'Ask an expert', text: 'Need advice on a career, a skill or a field? Find community members who know it well.', to: '/experts' },
  { icon: '🏙️', title: 'Your city circle', text: 'Members are grouped by city. Join your city WhatsApp group and follow local posts, events and openings.', to: '/community' },
  { icon: '❤️', title: 'Give back', text: 'Small contributions, pooled city-wise, for social causes. Every rupee collected is shown openly.', to: '/donate' },
];

export default function HomePage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    donationApi.summary().then(setSummary).catch(() => {});
  }, []);

  const topCities = summary?.cities.filter((c) => Number(c.collected) > 0).slice(0, 5) || [];

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
                  <Link to="/community" className="btn btn-light btn-lg">Go to my city</Link>
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
            <p className="muted small">Collected so far for community causes</p>
            <p className="hero-amount">{formatMoney(summary?.totalCollected)}</p>
            {topCities.length > 0 ? (
              <ul className="city-bars">
                {topCities.map((c) => (
                  <li key={c.cityId}>
                    <span>{c.cityName}</span>
                    <strong>{formatMoney(c.collected)}</strong>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="muted small">Be the first city to start a cause.</p>
            )}
            <Link to="/donate" className="btn btn-ghost">See all cities →</Link>
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
          <li><strong>Join your city</strong> WhatsApp group, post updates and support local causes.</li>
        </ol>
      </section>
    </>
  );
}
