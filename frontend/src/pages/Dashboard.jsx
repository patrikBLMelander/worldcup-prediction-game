import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import { getFlagUrl } from '../utils/countryFlags';
import './Dashboard.css';

const Dashboard = () => {
  const { user, updateUser } = useAuth();
  const [stats, setStats] = useState({
    totalMatches: 0,
    scheduledMatches: 0,
    myPredictions: 0,
    leaderboardPosition: 0,
  });
  const [upcomingMatches, setUpcomingMatches] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return; // Don't fetch if user is not loaded yet

    const fetchStats = async () => {
      try {
        // Fetch matches
        const matchesResponse = await apiClient.get('/matches');
        const allMatches = matchesResponse.data;
        const scheduledMatches = allMatches.filter(m => m.status === 'SCHEDULED');

        // Fetch my predictions
        const predictionsResponse = await apiClient.get('/predictions/my-predictions');
        const myPredictions = predictionsResponse.data;

        // Fetch leaderboard to find position
        const leaderboardResponse = await apiClient.get('/users/leaderboard');
        const leaderboard = leaderboardResponse.data;
        const position = leaderboard.findIndex(u => u.userId === user.id) + 1;

        setStats({
          totalMatches: allMatches.length,
          scheduledMatches: scheduledMatches.length,
          myPredictions: myPredictions.length,
          leaderboardPosition: position || leaderboard.length + 1,
        });

        // Update user to get latest points (only once, not in dependency)
        await updateUser();

        // Fetch upcoming matches without predictions
        try {
          const upcomingResponse = await apiClient.get('/users/me/upcoming-matches-without-prediction');
          setUpcomingMatches(upcomingResponse.data);
        } catch (error) {
          console.error('Failed to fetch upcoming matches:', error);
        }
      } catch (error) {
        console.error('Failed to fetch stats:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]); // Only depend on user.id, not the whole user object or updateUser

  if (loading) {
    return (
      <>
        <Navigation />
        <div className="dashboard-loading">Loading...</div>
      </>
    );
  }

  return (
    <>
      <Navigation />
      <div className="dashboard-container">
        <div className="dashboard-header">
          <h1>Welcome back, {user?.screenName || user?.email}!</h1>
          <p className="dashboard-subtitle">World Cup 2026 Prediction Game</p>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">🏆</div>
            <div className="stat-content">
              <h3>{user?.totalPoints || 0}</h3>
              <p>Total Points</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">🥇</div>
            <div className="stat-content">
              <h3>#{stats.leaderboardPosition}</h3>
              <p>Leaderboard Position</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">🎯</div>
            <div className="stat-content">
              <h3>{stats.myPredictions}</h3>
              <p>My Predictions</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">⚽</div>
            <div className="stat-content">
              <h3>{stats.scheduledMatches}</h3>
              <p>Scheduled Matches</p>
            </div>
          </div>
        </div>

        {upcomingMatches.length > 0 && (
          <div className="upcoming-matches-section">
            <h2>Upcoming Matches Without Predictions</h2>
            <p className="section-description">
              Don't miss out! Make predictions for these upcoming matches:
            </p>
            <div className="upcoming-matches-list">
              {upcomingMatches.slice(0, 5).map((match) => {
                const homeLogoUrl = match.homeTeamCrest || getFlagUrl(match.homeTeam);
                const awayLogoUrl = match.awayTeamCrest || getFlagUrl(match.awayTeam);
                return (
                  <Link 
                    key={match.id} 
                    to="/matches" 
                    className="upcoming-match-card"
                  >
                    <div className="match-teams">
                      <div className="team-info">
                        <img 
                          src={homeLogoUrl} 
                          alt={match.homeTeam} 
                          className="team-logo-small"
                          onError={(e) => {
                            if (match.homeTeamCrest) {
                              e.target.src = getFlagUrl(match.homeTeam);
                            }
                          }}
                        />
                        <span className="team-name-small">{match.homeTeam}</span>
                      </div>
                      <span className="vs-text">vs</span>
                      <div className="team-info">
                        <img 
                          src={awayLogoUrl} 
                          alt={match.awayTeam} 
                          className="team-logo-small"
                          onError={(e) => {
                            if (match.awayTeamCrest) {
                              e.target.src = getFlagUrl(match.awayTeam);
                            }
                          }}
                        />
                        <span className="team-name-small">{match.awayTeam}</span>
                      </div>
                    </div>
                    <div className="match-date-small">
                      {(() => {
                        const dateStr = match.matchDate;
                        const utcDate = dateStr.endsWith('Z') 
                          ? new Date(dateStr)
                          : new Date(dateStr + 'Z');
                        return utcDate.toLocaleString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                          timeZone: 'UTC'
                        });
                      })()}
                    </div>
                  </Link>
                );
              })}
            </div>
            {upcomingMatches.length > 5 && (
              <Link to="/matches" className="view-all-link">
                View all {upcomingMatches.length} matches →
              </Link>
            )}
          </div>
        )}

        <div className="dashboard-actions">
          <Link to="/matches" className="action-card">
            <h3>View Matches</h3>
            <p>See all upcoming matches and make predictions</p>
            <span className="action-arrow">→</span>
          </Link>

          <Link to="/leaderboard" className="action-card">
            <h3>Leaderboard</h3>
            <p>See how you rank against other players</p>
            <span className="action-arrow">→</span>
          </Link>
        </div>
      </div>
    </>
  );
};

export default Dashboard;

