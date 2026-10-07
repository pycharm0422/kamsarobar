import { Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import AdminPage from './pages/admin/AdminPage';
import CommunityPage from './pages/CommunityPage';
import DonatePage from './pages/DonatePage';
import FindExpertPage from './pages/FindExpertPage';
import FindReferralPage from './pages/FindReferralPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import PostDetailPage from './pages/PostDetailPage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';

const member = (element) => <ProtectedRoute>{element}</ProtectedRoute>;

export default function App() {
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/donate" element={<DonatePage />} />
          <Route path="/profile" element={member(<ProfilePage />)} />
          <Route path="/community" element={member(<CommunityPage />)} />
          <Route path="/posts/:id" element={member(<PostDetailPage />)} />
          <Route path="/referrals" element={member(<FindReferralPage />)} />
          <Route path="/experts" element={member(<FindExpertPage />)} />
          <Route
            path="/admin"
            element={<ProtectedRoute roles={['MAIN_ADMIN', 'CITY_ADMIN']}><AdminPage /></ProtectedRoute>}
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <footer className="footer">
        <div className="container">Kamsar o Bar · a community platform · made with care</div>
      </footer>
    </>
  );
}
