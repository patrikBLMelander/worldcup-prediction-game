import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import './Profile.css';

const Profile = () => {
  const { user, updateUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Screen name form state
  const [screenName, setScreenName] = useState('');
  const [screenNameLoading, setScreenNameLoading] = useState(false);
  const [screenNameError, setScreenNameError] = useState('');

  // Password form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordError, setPasswordError] = useState('');

  useEffect(() => {
    if (user) {
      setScreenName(user.screenName || '');
    }
  }, [user]);

  const handleScreenNameSubmit = async (e) => {
    e.preventDefault();
    setScreenNameError('');
    setSuccess('');

    if (screenName.trim().length < 2 || screenName.trim().length > 50) {
      setScreenNameError('Screen name must be between 2 and 50 characters');
      return;
    }

    setScreenNameLoading(true);
    try {
      await apiClient.put('/users/me/screen-name', { screenName: screenName.trim() });
      await updateUser();
      setSuccess('Screen name updated successfully!');
      setScreenNameError('');
    } catch (err) {
      setScreenNameError(err.response?.data?.error || 'Failed to update screen name');
    } finally {
      setScreenNameLoading(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setSuccess('');

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters long');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match');
      return;
    }

    if (currentPassword === newPassword) {
      setPasswordError('New password must be different from current password');
      return;
    }

    setPasswordLoading(true);
    try {
      await apiClient.put('/users/me/password', {
        currentPassword,
        newPassword,
      });
      setSuccess('Password changed successfully!');
      setPasswordError('');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPasswordError(err.response?.data?.error || 'Failed to change password');
    } finally {
      setPasswordLoading(false);
    }
  };

  if (!user) {
    return (
      <div className="profile-container">
        <Navigation />
        <div className="loading">Loading...</div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <Navigation />
      <div className="profile-content">
        <div className="profile-header">
          <h1>My Profile</h1>
        </div>

        {success && <div className="success-message">{success}</div>}
        {error && <div className="error-message">{error}</div>}

        <div className="profile-sections">
          {/* Account Information */}
          <section className="profile-section">
            <h2>Account Information</h2>
            <div className="info-grid">
              <div className="info-item">
                <label>Email</label>
                <div className="info-value">{user.email}</div>
              </div>
              <div className="info-item">
                <label>Screen Name</label>
                <div className="info-value">{user.screenName || 'Not set'}</div>
              </div>
              <div className="info-item">
                <label>Total Points</label>
                <div className="info-value">{user.totalPoints || 0}</div>
              </div>
              <div className="info-item">
                <label>Predictions Made</label>
                <div className="info-value">{user.predictionCount || 0}</div>
              </div>
              <div className="info-item">
                <label>Member Since</label>
                <div className="info-value">
                  {user.createdAt
                    ? new Date(user.createdAt).toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                      })
                    : 'N/A'}
                </div>
              </div>
            </div>
          </section>

          {/* Change Screen Name */}
          <section className="profile-section">
            <h2>Change Screen Name</h2>
            <p className="section-description">
              Your screen name is displayed on the leaderboard.
            </p>
            <form onSubmit={handleScreenNameSubmit} className="profile-form">
              {screenNameError && (
                <div className="error-message">{screenNameError}</div>
              )}
              <div className="form-group">
                <label htmlFor="screenName">Screen Name</label>
                <input
                  type="text"
                  id="screenName"
                  value={screenName}
                  onChange={(e) => setScreenName(e.target.value)}
                  placeholder="Enter your screen name (2-50 characters)"
                  minLength={2}
                  maxLength={50}
                  required
                  disabled={screenNameLoading}
                />
              </div>
              <button
                type="submit"
                className="btn-primary"
                disabled={screenNameLoading}
              >
                {screenNameLoading ? 'Updating...' : 'Update Screen Name'}
              </button>
            </form>
          </section>

          {/* Change Password */}
          <section className="profile-section">
            <h2>Change Password</h2>
            <p className="section-description">
              Update your password to keep your account secure.
            </p>
            <form onSubmit={handlePasswordSubmit} className="profile-form">
              {passwordError && (
                <div className="error-message">{passwordError}</div>
              )}
              <div className="form-group">
                <label htmlFor="currentPassword">Current Password</label>
                <div className="password-input-wrapper">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    id="currentPassword"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter your current password"
                    required
                    disabled={passwordLoading}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    disabled={passwordLoading}
                    aria-label={
                      showCurrentPassword ? 'Hide password' : 'Show password'
                    }
                  >
                    {showCurrentPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
              </div>
              <div className="form-group">
                <label htmlFor="newPassword">New Password</label>
                <div className="password-input-wrapper">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    id="newPassword"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter your new password (min 6 characters)"
                    minLength={6}
                    required
                    disabled={passwordLoading}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    disabled={passwordLoading}
                    aria-label={
                      showNewPassword ? 'Hide password' : 'Show password'
                    }
                  >
                    {showNewPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
              </div>
              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm New Password</label>
                <div className="password-input-wrapper">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    id="confirmPassword"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm your new password"
                    minLength={6}
                    required
                    disabled={passwordLoading}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    disabled={passwordLoading}
                    aria-label={
                      showConfirmPassword ? 'Hide password' : 'Show password'
                    }
                  >
                    {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                  </button>
                </div>
              </div>
              <button
                type="submit"
                className="btn-primary"
                disabled={passwordLoading}
              >
                {passwordLoading ? 'Changing...' : 'Change Password'}
              </button>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Profile;


