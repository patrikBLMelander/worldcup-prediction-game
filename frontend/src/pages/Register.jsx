import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ScreenNameModal from '../components/ScreenNameModal';
import './Login.css'; // Reuse Login styles

const Register = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showScreenNameModal, setShowScreenNameModal] = useState(false);
  const [hasNavigated, setHasNavigated] = useState(false);
  const [registerSuccess, setRegisterSuccess] = useState(false);
  const { register, updateScreenName, user } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (password.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);

    const result = await register(email, password);
    
    if (result.success) {
      // User will be loaded by AuthContext, check in useEffect
      setRegisterSuccess(true);
      setLoading(false);
    } else {
      setError(result.error);
      setLoading(false);
    }
  };

  // Check if user needs to set screen name after registration
  useEffect(() => {
    // Only check after successful registration (when user is loaded and we're not loading)
    if (registerSuccess && user && !loading && !hasNavigated) {
      if (!user.screenName || user.screenName === null) {
        // User has no screen name - show modal
        setShowScreenNameModal(true);
      } else {
        // User has screen name - navigate to dashboard
        setHasNavigated(true);
        navigate('/dashboard');
      }
    }
  }, [user, loading, navigate, hasNavigated, registerSuccess]);

  const handleScreenNameSave = async (screenName) => {
    await updateScreenName(screenName);
    setShowScreenNameModal(false);
    setHasNavigated(true);
    navigate('/dashboard');
  };

  return (
    <>
      <div className={`auth-container ${showScreenNameModal ? 'modal-open' : ''}`}>
        <div className="auth-card">
          <h1>World Cup 2026</h1>
          <h2>Register</h2>
          
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
                  placeholder="Enter your password (min 6 characters)"
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

            <div className="form-group">
              <label htmlFor="confirmPassword">Confirm Password</label>
              <div className="password-input-wrapper">
                <input
                  type={showConfirmPassword ? "text" : "password"}
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  placeholder="Confirm your password"
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  disabled={loading}
                  aria-label={showConfirmPassword ? "Hide password" : "Show password"}
                >
                  {showConfirmPassword ? "👁️" : "👁️‍🗨️"}
                </button>
              </div>
            </div>
            
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Registering...' : 'Register'}
            </button>
          </form>
          
          <p className="auth-link">
            Already have an account? <Link to="/login">Login here</Link>
          </p>
        </div>
      </div>

      {showScreenNameModal && (
        <ScreenNameModal
          isOpen={showScreenNameModal}
          onClose={() => {
            // Don't allow closing if no screen name is set (required after registration)
            // Only allow closing if user already has a screen name
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

export default Register;

