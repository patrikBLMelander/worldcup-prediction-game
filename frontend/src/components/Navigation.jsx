import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navigation.css';

const Navigation = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <nav className="navbar">
      <div className="nav-container">
        <Link to="/dashboard" className="nav-logo">
          ⚽ World Cup 2026
        </Link>
        
        <div className="nav-links">
          <Link to="/dashboard" className="nav-link">Dashboard</Link>
          <Link to="/matches" className="nav-link">Matches</Link>
          <Link to="/leaderboard" className="nav-link">Leaderboard</Link>
          <Link to="/profile" className="nav-link">Profile</Link>
          {user?.role === 'ADMIN' && (
            <Link to="/admin" className="nav-link">Admin</Link>
          )}
        </div>
        
        <div className="nav-user">
          <span className="nav-email">{user?.screenName || user?.email}</span>
          <button onClick={handleLogout} className="btn-logout">
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;

