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
  const [achievements, setAchievements] = useState([]);
  const [achievementsLoading, setAchievementsLoading] = useState(true);

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

    const fetchAchievements = async () => {
      try {
        const response = await apiClient.get(`/users/${userId}/achievements`);
        setAchievements(response.data);
      } catch (err) {
        console.error('Failed to fetch achievements:', err);
      } finally {
        setAchievementsLoading(false);
      }
    };

    if (userId) {
      fetchProfile();
      fetchAchievements();
    }
  }, [userId]);

  const getPointsColor = (points) => {
    if (points === null || points === undefined) return 'pending';
    if (points === 0) return '0';
    return '3'; // any positive score uses the "good" colour
  };

  const outcomeLabel = (prediction, outcome) => {
    if (outcome === 'DRAW') return 'Draw';
    if (outcome === 'HOME_WIN') return prediction.homeTeam;
    if (outcome === 'AWAY_WIN') return prediction.awayTeam;
    return '—';
  };

  // Small superscript next to a team's regulation score for knockout matches
  // decided after 90 minutes (display only). Penalties show as "+N".
  const scoreSuffix = (prediction, side) => {
    if (prediction.duration === 'PENALTY_SHOOTOUT') {
      const p = side === 'home' ? prediction.penaltiesHome : prediction.penaltiesAway;
      return p !== null && p !== undefined ? `+${p}` : null;
    }
    if (prediction.duration === 'EXTRA_TIME') {
      const et = side === 'home' ? prediction.extraTimeHome : prediction.extraTimeAway;
      return et !== null && et !== undefined ? `${et}` : null;
    }
    return null;
  };

  // Short label for how a knockout was decided, or null for a normal result.
  const deciderLabel = (prediction) => {
    if (prediction.duration === 'PENALTY_SHOOTOUT') return 'pens';
    if (prediction.duration === 'EXTRA_TIME') return 'a.e.t.';
    return null;
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
      <div className="profile-container">
        <Navigation />
        <div className="profile-content">
          <div className="public-profile-loading">Loading profile...</div>
        </div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="profile-container">
        <Navigation />
        <div className="profile-content">
          <div className="public-profile-error">
            <p>{error || 'Profile not found'}</p>
            <button onClick={() => navigate('/leaderboard')} className="btn-primary">
              Back to Leaderboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <Navigation />
      <div className="profile-content">
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
                <div className="stat-icon-large">✓</div>
                <div className="stat-content-large">
                  <div className="stat-value-large">{profile.statistics.correctPredictions}</div>
                  <div className="stat-label-large">Correct Results</div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Achievements */}
        {achievements.length > 0 && (
          <section className="profile-section">
            <h2>🏆 Achievements</h2>
            {achievementsLoading ? (
              <div className="loading">Loading achievements...</div>
            ) : (
              <>
                <p className="section-description">
                  {profile.screenName || 'This player'} has earned {achievements.length} achievement{achievements.length !== 1 ? 's' : ''}!
                </p>
                <div className="achievements-grid">
                  {achievements.map((achievement) => (
                    <div
                      key={achievement.id}
                      className="achievement-card earned"
                      title={achievement.description}
                    >
                      <div className="achievement-icon">{achievement.icon}</div>
                      <div className="achievement-content">
                        <div className="achievement-name">{achievement.name}</div>
                        <div className="achievement-description">{achievement.description}</div>
                        {achievement.earnedAt && (
                          <div className="achievement-date">
                            Earned: {new Date(achievement.earnedAt).toLocaleDateString()}
                          </div>
                        )}
                        <div className={`achievement-rarity rarity-${achievement.rarity}`}>
                          ✓ Earned
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </section>
        )}

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
                <div className="stat-icon">✓</div>
                <div className="stat-content">
                  <div className="stat-value">{profile.statistics.correctPredictions}</div>
                  <div className="stat-label">Correct Results</div>
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
                
                const isLive = prediction.matchStatus === 'LIVE';
                const isFinished = prediction.matchStatus === 'FINISHED';

                // Colour the card's left border by correctness, matching the
                // Results page: green for a correct outcome, red for wrong.
                let resultType = null;
                if (isFinished && prediction.points !== null && prediction.points !== undefined) {
                  resultType = prediction.points > 0 ? 'result-correct-winner' : 'result-wrong';
                }

                return (
                  <div key={prediction.matchId} className={`prediction-card match-card results-view ${resultType || ''}`}>
                    <div className="match-header">
                      <div className="match-header-left">
                        {isLive && <span className="match-status status-live">LIVE</span>}
                        {isFinished && <span className="match-status status-finished">FINISHED</span>}
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
                          <span className="score">
                            {prediction.actualHomeScore}
                            {scoreSuffix(prediction, 'home') && <sup className="score-extra">{scoreSuffix(prediction, 'home')}</sup>}
                          </span>
                        )}
                        <span className="vs">vs</span>
                        {prediction.actualHomeScore !== null && prediction.actualAwayScore !== null && (
                          <span className="score">
                            {prediction.actualAwayScore}
                            {scoreSuffix(prediction, 'away') && <sup className="score-extra">{scoreSuffix(prediction, 'away')}</sup>}
                          </span>
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
                        {deciderLabel(prediction) && (
                          <span className="score-decider">{deciderLabel(prediction)}</span>
                        )}
                        {prediction.predictedOutcome ? (
                          <span className={`prediction-result points-${pointsColor}`}>
                            {outcomeLabel(prediction, prediction.predictedOutcome)}
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
    </div>
  );
};

export default PublicProfile;


