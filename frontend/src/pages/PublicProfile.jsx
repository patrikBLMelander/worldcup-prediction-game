import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import { getFlagUrl } from '../utils/countryFlags';
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

  const getPointsColor = (points) => {
    if (points === null || points === undefined) return 'pending';
    if (points === 0) return '0';
    if (points === 1) return '1';
    if (points === 3) return '3';
    return 'pending';
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
              {profile.finishedPredictions.map((prediction) => {
                const homeLogoUrl = prediction.homeTeamCrest || getFlagUrl(prediction.homeTeam);
                const awayLogoUrl = prediction.awayTeamCrest || getFlagUrl(prediction.awayTeam);
                const pointsColor = getPointsColor(prediction.points);
                
                return (
                  <div key={prediction.matchId} className="prediction-card match-card results-view">
                    <div className="match-header">
                      <div className="match-header-left">
                        <span className="match-status status-finished">FINISHED</span>
                        <span className="match-group">{prediction.group || 'Match'}</span>
                        {prediction.points !== null && prediction.points !== undefined ? (
                          <div className="header-points">
                            <span className={`header-points-badge points-${pointsColor}`}>
                              {prediction.points === 1 ? '1 pt' : `${prediction.points} pts`}
                            </span>
                          </div>
                        ) : (
                          <div className="header-points">
                            <span className="header-points-badge points-pending">Pending</span>
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <div className="match-compact">
                      <div className="compact-row">
                        <img 
                          src={homeLogoUrl} 
                          alt={prediction.homeTeam} 
                          className="team-logo" 
                          onError={(e) => {
                            if (prediction.homeTeamCrest) {
                              e.target.src = getFlagUrl(prediction.homeTeam);
                            }
                          }} 
                        />
                        <span className="team-name">{prediction.homeTeam}</span>
                        {prediction.actualHomeScore !== null && prediction.actualAwayScore !== null && (
                          <span className="score">{prediction.actualHomeScore}</span>
                        )}
                        <span className="vs">vs</span>
                        {prediction.actualHomeScore !== null && prediction.actualAwayScore !== null && (
                          <span className="score">{prediction.actualAwayScore}</span>
                        )}
                        <span className="team-name">{prediction.awayTeam}</span>
                        <img 
                          src={awayLogoUrl} 
                          alt={prediction.awayTeam} 
                          className="team-logo" 
                          onError={(e) => {
                            if (prediction.awayTeamCrest) {
                              e.target.src = getFlagUrl(prediction.awayTeam);
                            }
                          }} 
                        />
                        {prediction.predictedHomeScore !== undefined && prediction.predictedAwayScore !== undefined ? (
                          <span className={`prediction-result points-${pointsColor}`}>
                            ({prediction.predictedHomeScore}-{prediction.predictedAwayScore})
                          </span>
                        ) : (
                          <span className="no-prediction">No prediction</span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
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


