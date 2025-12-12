import { useState, useEffect } from 'react';
import './ScreenNameModal.css';

const ScreenNameModal = ({ isOpen, onClose, onSave, currentScreenName }) => {
  const [screenName, setScreenName] = useState(currentScreenName || '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Update screen name when prop changes
  useEffect(() => {
    if (currentScreenName) {
      setScreenName(currentScreenName);
    }
  }, [currentScreenName]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (screenName.trim().length < 2) {
      setError('Screen name must be at least 2 characters');
      return;
    }

    if (screenName.trim().length > 50) {
      setError('Screen name must be 50 characters or less');
      return;
    }

    setLoading(true);
    try {
      await onSave(screenName.trim());
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to save screen name');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Choose Your Screen Name</h2>
          <p>This name will be displayed on the leaderboard</p>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div className="error-message">{error}</div>}

          <div className="form-group">
            <label htmlFor="screenName">Screen Name</label>
            <input
              type="text"
              id="screenName"
              value={screenName}
              onChange={(e) => setScreenName(e.target.value)}
              placeholder="Enter your screen name (2-50 characters)"
              required
              minLength={2}
              maxLength={50}
              disabled={loading}
              autoFocus
            />
            <small className="form-hint">
              This is how other players will see you on the leaderboard
            </small>
          </div>

          <div className="modal-actions">
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : 'Save Screen Name'}
            </button>
            {currentScreenName && (
              <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};

export default ScreenNameModal;

