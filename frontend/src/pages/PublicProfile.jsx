import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import './PublicProfile.css';

const PublicProfile = () => {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get(`/users/${userId}/public-profile`);
        setProfile(response.data);
        setError('');
      } catch (err) {
        setError(err.response?.data?.error || 'Failed to load profile');
        console.error('Failed to fetch public profile:', err);
      } finally {
        setLoading(false);
      }
    };

    if (userId) {
      fetchProfile();
    }
  }, [userId]);

  const getResultTypeBadge = (resultType) => {
    switch (resultType) {
      case 'EXACT':
        return <span className="badge badge-exact">🎯 Exact Score</span>;
      case 'CORRECT_WINNER':
        return <span className="badge badge-correct">✓ Correct Winner</span>;
      case 'WRONG':
        return <span className="badge badge-wrong">✗ Wrong</span>;
      default:
        return null;
    }
  };

  const formatDate = (dateString) => {
    // Parse date string as UTC (backend stores as UTC LocalDateTime)
    const utcDate = dateString.endsWith('Z') 
      ? new Date(dateString)
      : new Date(dateString + 'Z'); // Append Z if not present to treat as UTC
    return utcDate.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'UTC'
    });
  };

  if (loading) {
    return (
      <>
        <Navigation />
        <div className="public-profile-loading">Loading profile...</div>
      </>
    );
  }

  if (error || !profile) {
    return (
      <>
        <Navigation />
        <div className="public-profile-container">
          <div className="public-profile-error">
            <p>{error || 'Profile not found'}</p>
            <button onClick={() => navigate('/leaderboard')} className="btn-primary">
              Back to Leaderboard
            </button>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navigation />
      <div className="public-profile-container">
        <div className="public-profile-header">
          <button onClick={() => navigate('/leaderboard')} className="back-button">
            ← Back to Leaderboard
          </button>
          <h1>👤 {profile.screenName || 'Anonymous Player'}</h1>
        </div>

        {/* Stats Overview */}
        <div className="profile-stats-overview">
          <div className="stat-card-large">
            <div className="stat-icon-large">🏆</div>
            <div className="stat-content-large">
              <div className="stat-value-large">{profile.totalPoints || 0}</div>
              <div className="stat-label-large">Total Points</div>
            </div>
          </div>
          <div className="stat-card-large">
            <div className="stat-icon-large">📊</div>
            <div className="stat-content-large">
              <div className="stat-value-large">{profile.predictionCount || 0}</div>
              <div className="stat-label-large">Total Predictions</div>
            </div>
          </div>
          {profile.statistics && profile.statistics.totalPredictions > 0 && (
            <>
              <div className="stat-card-large">
                <div className="stat-icon-large">🎯</div>
                <div className="stat-content-large">
                  <div className="stat-value-large">
                    {profile.statistics.accuracyPercentage.toFixed(1)}%
                  </div>
                  <div className="stat-label-large">Accuracy</div>
                </div>
              </div>
              <div className="stat-card-large">
                <div className="stat-icon-large">⭐</div>
                <div className="stat-content-large">
                  <div className="stat-value-large">{profile.statistics.exactScores}</div>
                  <div className="stat-label-large">Exact Scores</div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Detailed Statistics */}
        {profile.statistics && profile.statistics.totalPredictions > 0 && (
          <section className="profile-section">
            <h2>📈 Prediction Statistics</h2>
            <div className="statistics-grid">
              <div className="stat-card">
                <div className="stat-icon">🎯</div>
                <div className="stat-content">
                  <div className="stat-value">{profile.statistics.accuracyPercentage.toFixed(1)}%</div>
                  <div className="stat-label">Accuracy</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">⭐</div>
                <div className="stat-content">
                  <div className="stat-value">{profile.statistics.exactScores}</div>
                  <div className="stat-label">Exact Scores (3 pts)</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">✓</div>
                <div className="stat-content">
                  <div className="stat-value">{profile.statistics.correctWinners}</div>
                  <div className="stat-label">Correct Winners (1 pt)</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">✗</div>
                <div className="stat-content">
                  <div className="stat-value">{profile.statistics.wrongPredictions}</div>
                  <div className="stat-label">Wrong Predictions</div>
                </div>
              </div>
            </div>
            <p className="section-description">
              Based on {profile.statistics.totalPredictions} finished match
              {profile.statistics.totalPredictions !== 1 ? 'es' : ''}
            </p>
          </section>
        )}

        {/* Finished Predictions */}
        <section className="profile-section">
          <h2>🎮 Finished Predictions</h2>
          {profile.finishedPredictions && profile.finishedPredictions.length > 0 ? (
            <div className="predictions-list">
              {profile.finishedPredictions.map((prediction) => (
                <div key={prediction.matchId} className="prediction-card">
                  <div className="prediction-header">
                    <div className="match-info">
                      <div className="match-teams">
                        <div className="team">
                          {prediction.homeTeamCrest && (
                            <img
                              src={prediction.homeTeamCrest}
                              alt={prediction.homeTeam}
                              className="team-crest"
                            />
                          )}
                          <span className="team-name">{prediction.homeTeam}</span>
                        </div>
                        <div className="score-divider">vs</div>
                        <div className="team">
                          {prediction.awayTeamCrest && (
                            <img
                              src={prediction.awayTeamCrest}
                              alt={prediction.awayTeam}
                              className="team-crest"
                            />
                          )}
                          <span className="team-name">{prediction.awayTeam}</span>
                        </div>
                      </div>
                      <div className="match-meta">
                        <span className="match-date">{formatDate(prediction.matchDate)}</span>
                        {prediction.venue && (
                          <span className="match-venue">📍 {prediction.venue}</span>
                        )}
                        {prediction.group && (
                          <span className="match-group">{prediction.group}</span>
                        )}
                      </div>
                    </div>
                    <div className="prediction-result">
                      {getResultTypeBadge(prediction.resultType)}
                      <div className="points-earned">+{prediction.points} pts</div>
                    </div>
                  </div>
                  <div className="prediction-scores">
                    <div className="score-comparison">
                      <div className="score-item">
                        <span className="score-label">Predicted</span>
                        <span className="score-value">
                          {prediction.predictedHomeScore} - {prediction.predictedAwayScore}
                        </span>
                      </div>
                      <div className="score-arrow">→</div>
                      <div className="score-item">
                        <span className="score-label">Actual</span>
                        <span className="score-value actual">
                          {prediction.actualHomeScore} - {prediction.actualAwayScore}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="section-description">
              No finished predictions to display yet. Predictions will appear here once matches are finished.
            </p>
          )}
        </section>
      </div>
    </>
  );
};

export default PublicProfile;


