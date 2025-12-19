import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FiUsers } from 'react-icons/fi';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import './Leaderboard.css';

const Leaderboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [leaderboard, setLeaderboard] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [leaguesLoading, setLeaguesLoading] = useState(true);
  const [userPosition, setUserPosition] = useState(null);
  const { markSectionAsRead } = useNotifications();

  // Clear any notifications that belong to the Leaderboard section when this page is viewed
  useEffect(() => {
    markSectionAsRead('/leaderboard');
  }, [markSectionAsRead]);

  // Fetch user's leagues
  useEffect(() => {
    const fetchLeagues = async () => {
      try {
        const response = await apiClient.get('/leagues/mine');
        setLeagues(response.data);
        
        // Check for league query parameter
        const leagueParam = searchParams.get('league');
        if (leagueParam) {
          const leagueId = parseInt(leagueParam, 10);
          const leagueExists = response.data.some(l => l.id === leagueId);
          if (leagueExists) {
            setSelectedLeagueId(leagueId);
          }
        }
      } catch (error) {
        console.error('Failed to fetch leagues:', error);
      } finally {
        setLeaguesLoading(false);
      }
    };

    fetchLeagues();
  }, [searchParams]);

  // Fetch leaderboard based on selected league
  useEffect(() => {
    const fetchLeaderboard = async () => {
      if (leaguesLoading) return; // Wait for leagues to load first
      
      try {
        setLoading(true);
        let response;
        
        if (selectedLeagueId) {
          response = await apiClient.get(`/leagues/${selectedLeagueId}/leaderboard`);
        } else {
          response = await apiClient.get('/users/leaderboard');
        }
        
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
  }, [selectedLeagueId, user?.id, leaguesLoading]);

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

  const handleLeagueChange = (leagueId) => {
    setSelectedLeagueId(leagueId);
    // Update URL without page reload
    if (leagueId) {
      navigate(`/leaderboard?league=${leagueId}`, { replace: true });
    } else {
      navigate('/leaderboard', { replace: true });
    }
  };

  const getSelectedLeague = () => {
    if (!selectedLeagueId) return null;
    return leagues.find(l => l.id === selectedLeagueId);
  };

  const selectedLeague = getSelectedLeague();

  if (loading || leaguesLoading) {
    return (
      <div className="profile-container">
        <Navigation />
        <div className="profile-content">
          <div className="leaderboard-loading">Loading leaderboard...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <Navigation />
      <div className="profile-content">
        <div className="leaderboard-container">
        {/* League Tabs */}
        <div className="league-tabs-container">
          <div className="league-tabs">
            <button
              type="button"
              onClick={() => handleLeagueChange(null)}
              className={`league-tab ${!selectedLeagueId ? 'active' : ''}`}
            >
              🌍 Global
            </button>
            {leagues.map((league) => (
              <button
                key={league.id}
                type="button"
                onClick={() => handleLeagueChange(league.id)}
                className={`league-tab ${selectedLeagueId === league.id ? 'active' : ''}`}
              >
                <FiUsers className="league-tab-icon" />
                {league.name}
              </button>
            ))}
          </div>
        </div>

        <div className="leaderboard-header">
          <h1>🏆 Leaderboard</h1>
          <p>
            {selectedLeague 
              ? `${selectedLeague.name} (${new Date(selectedLeague.startDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - ${new Date(selectedLeague.endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })})`
              : 'See how you rank against other players'}
          </p>
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
                  className={`leaderboard-row ${getRankClass(position)} ${isCurrentUser ? 'current-user' : ''} clickable`}
                  onClick={() => navigate(`/user/${entry.userId}`)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      navigate(`/user/${entry.userId}`);
                    }
                  }}
                >
                  <div className="rank-col">
                    {rankIcon ? (
                      <span className="rank-icon">{rankIcon}</span>
                    ) : (
                      <span className="rank-number">#{position}</span>
                    )}
                  </div>
                  <div className="player-col">
                    <span className="player-name">
                      {entry.screenName || entry.email}
                    </span>
                    {isCurrentUser && <span className="you-badge">You</span>}
                  </div>
                  <div className="points-col">
                    <span className="points-value">{entry.totalPoints || 0}</span>
                    <span className="points-label">pts</span>
                  </div>
                  <div className="predictions-col">
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
      </div>
    </div>
  );
};

export default Leaderboard;

