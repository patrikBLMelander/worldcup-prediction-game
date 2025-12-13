import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import './Leaderboard.css';

const Leaderboard = () => {
  const { user } = useAuth();
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);
  const [userPosition, setUserPosition] = useState(null);

  useEffect(() => {
    const fetchLeaderboard = async () => {
      try {
        const response = await apiClient.get('/users/leaderboard');
        const data = response.data;
        setLeaderboard(data);
        
        // Find current user's position
        const position = data.findIndex(entry => entry.userId === user?.id);
        setUserPosition(position >= 0 ? position + 1 : null);
      } catch (error) {
        console.error('Failed to fetch leaderboard:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboard();
  }, [user?.id]);

  const getRankIcon = (position) => {
    switch (position) {
      case 1:
        return '🥇';
      case 2:
        return '🥈';
      case 3:
        return '🥉';
      default:
        return null;
    }
  };

  const getRankClass = (position) => {
    switch (position) {
      case 1:
        return 'rank-first';
      case 2:
        return 'rank-second';
      case 3:
        return 'rank-third';
      default:
        return '';
    }
  };

  if (loading) {
    return (
      <>
        <Navigation />
        <div className="leaderboard-loading">Loading leaderboard...</div>
      </>
    );
  }

  return (
    <>
      <Navigation />
      <div className="leaderboard-container">
        <div className="leaderboard-header">
          <h1>🏆 Leaderboard</h1>
          <p>See how you rank against other players</p>
          {userPosition && (
            <div className="user-position-badge">
              Your Position: <span className="position-number">#{userPosition}</span>
            </div>
          )}
        </div>

        {leaderboard.length === 0 ? (
          <div className="no-leaderboard">
            <p>No players on the leaderboard yet. Be the first to make predictions!</p>
          </div>
        ) : (
          <div className="leaderboard-table">
            <div className="leaderboard-header-row">
              <div className="rank-col">Rank</div>
              <div className="player-col">Player</div>
              <div className="points-col">Points</div>
              <div className="predictions-col">Predictions</div>
            </div>
            
            {leaderboard.map((entry, index) => {
              const position = index + 1;
              const isCurrentUser = entry.userId === user?.id;
              const rankIcon = getRankIcon(position);
              
              return (
                <div
                  key={entry.userId}
                  className={`leaderboard-row ${getRankClass(position)} ${isCurrentUser ? 'current-user' : ''}`}
                >
                  <div className="rank-col">
                    <span className="mobile-label">Rank:</span>
                    {rankIcon ? (
                      <span className="rank-icon">{rankIcon}</span>
                    ) : (
                      <span className="rank-number">#{position}</span>
                    )}
                  </div>
                  <div className="player-col">
                    <span className="mobile-label">Player:</span>
                    <span className="player-name">
                      {entry.screenName || entry.email}
                    </span>
                    {isCurrentUser && <span className="you-badge">You</span>}
                  </div>
                  <div className="points-col">
                    <span className="mobile-label">Points:</span>
                    <span className="points-value">{entry.totalPoints || 0}</span>
                    <span className="points-label">pts</span>
                  </div>
                  <div className="predictions-col">
                    <span className="mobile-label">Predictions:</span>
                    <span className="predictions-count">{entry.predictionCount || 0}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {leaderboard.length > 0 && (
          <div className="leaderboard-stats">
            <div className="stat-item">
              <span className="stat-label">Total Players</span>
              <span className="stat-value">{leaderboard.length}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Top Score</span>
              <span className="stat-value">{leaderboard[0]?.totalPoints || 0} pts</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Avg Score</span>
              <span className="stat-value">
                {Math.round(leaderboard.reduce((sum, entry) => sum + (entry.totalPoints || 0), 0) / leaderboard.length)} pts
              </span>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default Leaderboard;

