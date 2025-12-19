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

    try {
      const result = await login(email, password);
      
      if (result.success) {
        // User will be loaded by AuthContext, check in useEffect
        setLoginSuccess(true);
        setLoading(false);
      } else {
        setError(result.error);
        setLoading(false);
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Login failed. Please try again.');
      setLoading(false);
    }
  };

  // Track if we've already handled navigation to prevent multiple navigations
  const [hasNavigated, setHasNavigated] = useState(false);
  const [loginSuccess, setLoginSuccess] = useState(false);

  const joinPendingLeagueIfAny = useCallback(async () => {
    const code = localStorage.getItem('pendingLeagueInvite');
    if (!code) {
      console.log('No pending invite code found');
      return false;
    }
    
    console.log('joinPendingLeagueIfAny: Found code:', code);
    
    // Wait a bit to ensure token is set in apiClient
    console.log('Waiting for token to be set...');
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // Verify token is set
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('No token found after login, cannot join league');
      setError('Authentication token not found. Please try logging in again.');
      return false;
    }
    
    try {
      console.log('Calling /leagues/join with code:', code);
      const response = await apiClient.post('/leagues/join', { joinCode: code });
      console.log('Join league response:', response.data);
      localStorage.removeItem('pendingLeagueInvite');
      console.log('Successfully joined league and removed pending invite');
      return true;
    } catch (err) {
      console.error('Failed to join league from pending invite after login:', err);
      console.error('Error details:', {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message
      });
      // Don't remove the code on error - redirect to invite page so user can see error and try again
      const errorMsg = err.response?.data?.error ||
        err.response?.data?.message ||
        'Failed to join league from invite.';
      setError(errorMsg);
      return false;
    }
  }, []);

  // Check if user needs to set screen name after login
  useEffect(() => {
    // Only check after successful login (when user is loaded and we're not loading)
    // Also wait for AuthContext to finish loading
    if (loginSuccess && user && !loading && !hasNavigated) {
      const handlePostLogin = async () => {
        console.log('Post-login handler running, user:', user?.email, 'hasScreenName:', !!user?.screenName);
        
        // Wait a bit more to ensure everything is ready
        await new Promise(resolve => setTimeout(resolve, 100));
        
        if (!user.screenName || user.screenName === null) {
          // User has no screen name - show modal
          console.log('User needs screen name, showing modal');
          setShowScreenNameModal(true);
          return;
        }

        // Check if there's a pending invite
        const pendingCode = localStorage.getItem('pendingLeagueInvite');
        console.log('Pending invite code:', pendingCode);
        
        if (pendingCode) {
          console.log('Attempting to join league with code:', pendingCode);
          try {
            const joinedFromInvite = await joinPendingLeagueIfAny();
            console.log('Join result:', joinedFromInvite);
            setHasNavigated(true);
            if (joinedFromInvite) {
              console.log('Successfully joined, navigating to /leagues');
              navigate('/leagues');
            } else {
              // If join failed, redirect back to invite page so user can see error and try again
              console.log('Join failed, redirecting to invite page');
              navigate(`/invite/${pendingCode}`);
            }
          } catch (err) {
            console.error('Error in joinPendingLeagueIfAny:', err);
            setHasNavigated(true);
            navigate(`/invite/${pendingCode}`);
          }
        } else {
          console.log('No pending invite, navigating to dashboard');
          setHasNavigated(true);
          navigate('/dashboard');
        }
      };

      handlePostLogin();
    }
  }, [user, loading, navigate, hasNavigated, loginSuccess, joinPendingLeagueIfAny]);

  const handleScreenNameSave = async (screenName) => {
    await updateScreenName(screenName);
    setShowScreenNameModal(false);
    
    // Check if there's a pending invite
    const pendingCode = localStorage.getItem('pendingLeagueInvite');
    if (pendingCode) {
      const joinedFromInvite = await joinPendingLeagueIfAny();
      setHasNavigated(true);
      if (joinedFromInvite) {
        navigate('/leagues');
      } else {
        // If join failed, redirect back to invite page so user can see error and try again
        navigate(`/invite/${pendingCode}`);
      }
    } else {
      setHasNavigated(true);
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

