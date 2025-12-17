import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import CountryFlagsBackground from '../components/CountryFlagsBackground';
import './Login.css';

const Invite = () => {
  const { joinCode } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [status, setStatus] = useState('loading'); // 'loading' | 'needs-auth' | 'joining' | 'joined' | 'error'
  const [message, setMessage] = useState('');

  useEffect(() => {
    const code = (joinCode || '').toUpperCase().trim();
    if (!code) {
      setStatus('error');
      setMessage('Invalid invite link.');
      return;
    }

    const handleInvite = async () => {
      if (isAuthenticated) {
        try {
          setStatus('joining');
          setMessage('Joining league...');
          await apiClient.post('/leagues/join', { joinCode: code });
          localStorage.removeItem('pendingLeagueInvite');
          setStatus('joined');
          setMessage('You have joined the league! Redirecting to your leagues...');
          setTimeout(() => navigate('/leagues'), 1500);
        } catch (err) {
          console.error('Failed to join league from invite:', err);
          const errorMsg =
            err.response?.data?.error ||
            err.response?.data?.message ||
            'This invite link is no longer valid or the league is locked.';
          setStatus('error');
          setMessage(errorMsg);
        }
      } else {
        // Store invite code so we can join right after login/registration
        localStorage.setItem('pendingLeagueInvite', code);
        setStatus('needs-auth');
        setMessage('You have been invited to join a private league.');
      }
    };

    handleInvite();
  }, [joinCode, isAuthenticated, navigate]);

  const handleGoToLogin = () => {
    navigate('/login');
  };

  const handleGoToRegister = () => {
    navigate('/register');
  };

  return (
    <div className="auth-container">
      <CountryFlagsBackground />
      <div className="auth-card">
        <h1>World Cup 2026</h1>
        <h2>League Invite</h2>

        {message && (
          <div className={status === 'error' ? 'error-message' : 'success-message'}>
            {message}
          </div>
        )}

        {status === 'loading' || status === 'joining' || status === 'joined' ? (
          <p className="auth-link">Please wait...</p>
        ) : null}

        {status === 'needs-auth' && (
          <>
            <p className="auth-link">
              To join this league, please login if you already have an account, or register if
              you are new.
            </p>
            <div className="invite-actions">
              <button className="btn-primary" onClick={handleGoToLogin}>
                Login
              </button>
              <button className="btn-secondary" onClick={handleGoToRegister}>
                Register
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default Invite;


