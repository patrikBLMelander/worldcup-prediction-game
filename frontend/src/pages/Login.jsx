import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ScreenNameModal from '../components/ScreenNameModal';
import CountryFlagsBackground from '../components/CountryFlagsBackground';
import apiClient from '../config/api';
import './Login.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showScreenNameModal, setShowScreenNameModal] = useState(false);
  const { login, user, updateScreenName } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const result = await login(email, password);
    
    if (result.success) {
      // User will be loaded by AuthContext, check in useEffect
      setLoginSuccess(true);
      setLoading(false);
    } else {
      setError(result.error);
      setLoading(false);
    }
  };

  // Track if we've already handled navigation to prevent multiple navigations
  const [hasNavigated, setHasNavigated] = useState(false);
  const [loginSuccess, setLoginSuccess] = useState(false);

  const joinPendingLeagueIfAny = useCallback(async () => {
    const code = localStorage.getItem('pendingLeagueInvite');
    if (!code) return false;
    try {
      await apiClient.post('/leagues/join', { joinCode: code });
      localStorage.removeItem('pendingLeagueInvite');
      return true;
    } catch (err) {
      console.error('Failed to join league from pending invite after login:', err);
      localStorage.removeItem('pendingLeagueInvite');
      setError(
        err.response?.data?.error ||
          err.response?.data?.message ||
          'Failed to join league from invite.'
      );
      return false;
    }
  }, []);

  // Check if user needs to set screen name after login
  useEffect(() => {
    // Only check after successful login (when user is loaded and we're not loading)
    if (loginSuccess && user && !loading && !hasNavigated) {
      const handlePostLogin = async () => {
        if (!user.screenName || user.screenName === null) {
          // User has no screen name - show modal
          setShowScreenNameModal(true);
          return;
        }

        const joinedFromInvite = await joinPendingLeagueIfAny();
        setHasNavigated(true);
        if (joinedFromInvite) {
          navigate('/leagues');
        } else {
          navigate('/dashboard');
        }
      };

      handlePostLogin();
    }
  }, [user, loading, navigate, hasNavigated, loginSuccess, joinPendingLeagueIfAny]);

  const handleScreenNameSave = async (screenName) => {
    await updateScreenName(screenName);
    setShowScreenNameModal(false);
    const joinedFromInvite = await joinPendingLeagueIfAny();
    setHasNavigated(true);
    if (joinedFromInvite) {
      navigate('/leagues');
    } else {
      navigate('/dashboard');
    }
  };

  return (
    <>
    <div className="auth-container">
      <CountryFlagsBackground />
      <div className="auth-card">
        <h1>World Cup 2026</h1>
        <h2>Login</h2>
        
        {error && <div className="error-message">{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="Enter your email"
              disabled={loading}
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="password-input-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Enter your password"
                disabled={loading}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                disabled={loading}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? "👁️" : "👁️‍🗨️"}
              </button>
            </div>
          </div>
          
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        
        <p className="auth-link">
          Don't have an account? <Link to="/register">Register here</Link>
        </p>
      </div>
    </div>

    {showScreenNameModal && (
    <ScreenNameModal
      isOpen={showScreenNameModal}
      onClose={() => {
        // Don't allow closing if no screen name is set (required)
        if (user?.screenName) {
          setShowScreenNameModal(false);
        }
      }}
      onSave={handleScreenNameSave}
      currentScreenName={user?.screenName}
    />
    )}
    </>
  );
};

export default Login;

