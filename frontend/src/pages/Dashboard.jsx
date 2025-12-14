import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import CountdownTimer from '../components/CountdownTimer';
import { getFlagUrl } from '../utils/countryFlags';
import './Dashboard.css';
import './Matches.css';

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
          const allUpcoming = upcomingResponse.data;
          
          // Filter to only show matches starting within 2 days
          const now = new Date();
          const twoDaysFromNow = new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000);
          
          const filteredUpcoming = allUpcoming.filter(match => {
            const dateStr = match.matchDate;
            const matchTime = dateStr.endsWith('Z') 
              ? new Date(dateStr)
              : new Date(dateStr + 'Z');
            return matchTime <= twoDaysFromNow;
          });
          
          setUpcomingMatches(filteredUpcoming);
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
              Don't miss out! Make predictions for these upcoming matches (within 2 days):
            </p>
            <div className="matches-grid">
              {upcomingMatches.map((match) => {
                const homeLogoUrl = match.homeTeamCrest || getFlagUrl(match.homeTeam);
                const awayLogoUrl = match.awayTeamCrest || getFlagUrl(match.awayTeam);
                
                // Calculate time remaining for color coding
                let timeRemainingClass = '';
                if (match.status === 'SCHEDULED' && match.matchDate) {
                  const dateStr = match.matchDate;
                  const matchTime = dateStr.endsWith('Z') 
                    ? new Date(dateStr)
                    : new Date(dateStr + 'Z');
                  const now = new Date();
                  const diffMs = matchTime - now;
                  const diffHours = diffMs / (1000 * 60 * 60);
                  
                  if (diffHours < 1) {
                    timeRemainingClass = 'time-very-soon';
                  } else if (diffHours < 12) {
                    timeRemainingClass = 'time-soon';
                  } else if (diffHours < 24) {
                    timeRemainingClass = 'time-medium';
                  } else {
                    timeRemainingClass = 'time-far';
                  }
                }
                
                return (
                  <Link 
                    key={match.id} 
                    to="/matches" 
                    className={`match-card upcoming-view ${timeRemainingClass}`}
                  >
                    <div className="match-header">
                      <div className="match-header-left">
                        <span className="match-status status-scheduled desktop-header-text">{match.status}</span>
                        <span className="match-group desktop-header-text">{match.group}</span>
                        {match.status === 'SCHEDULED' && (
                          <div className="desktop-header-timer">
                            <CountdownTimer 
                              matchDate={match.matchDate} 
                              status={match.status}
                              matchId={match.id}
                            />
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <div className="match-compact desktop-match-compact">
                      <div className="compact-row desktop-compact-row">
                        <div className="desktop-team">
                          <img 
                            src={homeLogoUrl} 
                            alt={match.homeTeam} 
                            className="desktop-team-logo"
                            onError={(e) => {
                              if (match.homeTeamCrest) {
                                e.target.src = getFlagUrl(match.homeTeam);
                              }
                            }}
                          />
                          <span className="desktop-team-name">{match.homeTeam}</span>
                        </div>
                        <span className="desktop-vs">vs</span>
                        <div className="desktop-team">
                          <img 
                            src={awayLogoUrl} 
                            alt={match.awayTeam} 
                            className="desktop-team-logo"
                            onError={(e) => {
                              if (match.awayTeamCrest) {
                                e.target.src = getFlagUrl(match.awayTeam);
                              }
                            }}
                          />
                          <span className="desktop-team-name">{match.awayTeam}</span>
                        </div>
                        <span className="desktop-no-prediction">No prediction</span>
                      </div>
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
      </div>
    </>
  );
};

export default Dashboard;

