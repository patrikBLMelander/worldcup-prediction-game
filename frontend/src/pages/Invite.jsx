import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import CountryFlagsBackground from '../components/CountryFlagsBackground';
import './Login.css';

const Invite = () => {
  const { joinCode } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const hasProcessedRef = useRef(false);

  const [status, setStatus] = useState('loading'); // 'loading' | 'needs-auth' | 'joining' | 'joined' | 'error'
  const [message, setMessage] = useState('');

  useEffect(() => {
    const code = (joinCode || '').toUpperCase().trim();
    if (!code) {
      setStatus('error');
      setMessage('Invalid invite link.');
      return;
    }

    // Reset processing flag if joinCode changes
    if (hasProcessedRef.current && hasProcessedRef.current.code !== code) {
      hasProcessedRef.current = false;
      setStatus('loading'); // Reset status when code changes
    }

    // Don't process if we've already successfully joined or are currently joining
    if (hasProcessedRef.current && hasProcessedRef.current.code === code) {
      // But allow retry if:
      // 1. User just became authenticated (was needs-auth, now authenticated)
      // 2. Previous attempt failed (status was error) and user is now authenticated
      if (isAuthenticated && hasProcessedRef.current.status === 'needs-auth') {
        console.log('User became authenticated, allowing retry');
        hasProcessedRef.current = false; // Allow retry
        setStatus('loading'); // Reset to loading state
      } else if (hasProcessedRef.current.status === 'error' && isAuthenticated) {
        // Allow retry if previous attempt failed and user is authenticated
        console.log('Previous attempt failed, allowing retry for authenticated user');
        hasProcessedRef.current = false;
        setStatus('loading');
      } else if (hasProcessedRef.current.status === 'joined') {
        // Already successfully joined, don't process again
        return;
      } else if (hasProcessedRef.current.status === 'joining') {
        // Currently joining, wait for it to complete
        console.log('Already joining, waiting...');
        return;
      }
    }

    const handleInvite = async () => {
      console.log('handleInvite called, isAuthenticated:', isAuthenticated, 'code:', code);
      
      if (isAuthenticated) {
        try {
          setStatus('joining');
          setMessage('Joining league...');
          hasProcessedRef.current = { code, status: 'joining' };
          console.log('Calling /leagues/join with code:', code);
          const response = await apiClient.post('/leagues/join', { joinCode: code });
          console.log('Join successful, response:', response.data);
          localStorage.removeItem('pendingLeagueInvite');
          hasProcessedRef.current = { code, status: 'joined' };
          setStatus('joined');
          setMessage('You have joined the league! Redirecting to your leagues...');
          setTimeout(() => navigate('/leagues'), 1500);
        } catch (err) {
          console.error('Failed to join league from invite:', err);
          console.error('Error details:', {
            status: err.response?.status,
            data: err.response?.data,
            message: err.message
          });
          const errorMsg =
            err.response?.data?.error ||
            err.response?.data?.message ||
            'This invite link is no longer valid or the league is locked.';
          hasProcessedRef.current = { code, status: 'error' };
          setStatus('error');
          setMessage(errorMsg);
        }
      } else {
        // Store invite code so we can join right after login/registration
        console.log('User not authenticated, storing code in localStorage:', code);
        localStorage.setItem('pendingLeagueInvite', code);
        hasProcessedRef.current = { code, status: 'needs-auth' };
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

        {status === 'error' && (
          <div className="invite-actions">
            <button className="btn-primary" onClick={() => navigate('/login')}>
              Go to Login
            </button>
            <button className="btn-secondary" onClick={() => navigate('/dashboard')}>
              Go to Dashboard
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Invite;


