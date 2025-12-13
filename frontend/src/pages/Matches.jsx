import { useEffect, useState, useCallback, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import CountdownTimer from '../components/CountdownTimer';
import { getFlagUrl } from '../utils/countryFlags';
import './Matches.css';

const Matches = () => {
  const { user } = useAuth();
  const [matches, setMatches] = useState([]);
  const [filteredMatches, setFilteredMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [groupFilter, setGroupFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('date'); // 'date', 'group', 'status', 'team'
  const [sortOrder, setSortOrder] = useState('asc'); // 'asc', 'desc'
  const [filtersExpanded, setFiltersExpanded] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [userPredictions, setUserPredictions] = useState({});
  const [predictionInputs, setPredictionInputs] = useState({});
  const [savingStates, setSavingStates] = useState({});
  const debounceTimers = useRef({});

  // Fetch predictions with points for finished matches
  const fetchPredictions = useCallback(async () => {
    try {
      const predictionsResponse = await apiClient.get('/predictions/my-predictions');
      const predictions = predictionsResponse.data;
      const predictionsMap = {};
      const inputsMap = {};
      
      // Find finished matches that need points calculation
      const finishedMatchesNeedingCalculation = [];
      
      predictions.forEach(pred => {
        predictionsMap[pred.matchId] = {
          homeScore: pred.predictedHomeScore,
          awayScore: pred.predictedAwayScore,
          points: pred.points,
        };
        // Initialize input values with existing predictions
        inputsMap[pred.matchId] = {
          homeScore: pred.predictedHomeScore.toString(),
          awayScore: pred.predictedAwayScore.toString(),
        };
        
        // Check if this prediction is for a finished match without points
        const match = matches.find(m => m.id === pred.matchId);
        if (match && match.status === 'FINISHED' && pred.points === null && match.homeScore !== null && match.awayScore !== null) {
          finishedMatchesNeedingCalculation.push(pred.matchId);
        }
      });
      
      // Trigger points calculation for matches that need it
      if (finishedMatchesNeedingCalculation.length > 0) {
        await Promise.all(
          finishedMatchesNeedingCalculation.map(matchId =>
            apiClient.post(`/matches/${matchId}/calculate-points`).catch(err => {
              console.error(`Failed to calculate points for match ${matchId}:`, err);
            })
          )
        );
        // Refetch predictions after calculation
        const updatedResponse = await apiClient.get('/predictions/my-predictions');
        updatedResponse.data.forEach(pred => {
          if (predictionsMap[pred.matchId]) {
            predictionsMap[pred.matchId].points = pred.points;
          }
        });
      }
      
      setUserPredictions(predictionsMap);
      setPredictionInputs(inputsMap);
    } catch (error) {
      console.error('Failed to fetch predictions:', error);
    }
  }, [matches]);

  const fetchMatches = useCallback(async () => {
    try {
      const matchesResponse = await apiClient.get('/matches');
      const allMatches = matchesResponse.data;
      setMatches(allMatches);
    } catch (error) {
      console.error('Failed to fetch matches:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Handle WebSocket match updates
  const handleMatchUpdate = useCallback((updatedMatch) => {
    setMatches(prevMatches => {
      const index = prevMatches.findIndex(m => m.id === updatedMatch.id);
      if (index !== -1) {
        // Update existing match
        const updated = [...prevMatches];
        // Handle status - can be enum object or string
        const statusValue = updatedMatch.status?.name || updatedMatch.status || updatedMatch.status;
        updated[index] = {
          ...updated[index],
          ...updatedMatch,
          status: statusValue,
        };
        return updated;
      } else {
        // New match added
        const statusValue = updatedMatch.status?.name || updatedMatch.status || updatedMatch.status;
        return [...prevMatches, { ...updatedMatch, status: statusValue }];
      }
    });
    
    // If match status changed or scores updated, refresh predictions
    const statusValue = updatedMatch.status?.name || updatedMatch.status || updatedMatch.status;
    if (statusValue === 'FINISHED' || updatedMatch.homeScore !== null) {
      fetchPredictions();
    }
  }, [fetchPredictions]);

  // Connect to WebSocket for real-time updates
  useWebSocket(handleMatchUpdate);

  useEffect(() => {
    fetchMatches();
    
    // Reduced polling frequency since WebSocket handles real-time updates
    // Poll every 30 seconds as a fallback
    const interval = setInterval(() => {
      fetchMatches();
    }, 30000);

    return () => clearInterval(interval);
  }, [fetchMatches]);

  // Additional aggressive polling for matches that are about to start or have just started
  useEffect(() => {
    const now = new Date();
    const matchesNeedingPolling = matches.filter(match => {
      if (match.status !== 'SCHEDULED') return false;
      const matchDate = new Date(match.matchDate);
      const timeDiff = matchDate - now;
      // Poll aggressively for matches starting within the next 5 minutes or that have just passed
      return timeDiff <= 5 * 60 * 1000 && timeDiff >= -10 * 60 * 1000;
    });

    if (matchesNeedingPolling.length === 0) return;

    // Poll every 2 seconds for matches that are about to start
    const aggressiveInterval = setInterval(() => {
      fetchMatches();
    }, 2000);

    return () => clearInterval(aggressiveInterval);
  }, [matches, fetchMatches]);

  // Handle countdown expiration - refresh matches to pick up status change to LIVE
  const handleCountdownExpired = useCallback(async (matchId) => {
    // Immediately refresh matches to pick up the status change from backend scheduler
    // The backend scheduler runs every 10 seconds, so status should update quickly
    await fetchMatches();
  }, [fetchMatches]);

  // Fetch predictions when matches are loaded
  useEffect(() => {
    if (matches.length > 0) {
      fetchPredictions();
    }
  }, [matches, fetchPredictions]);

  // Auto-save prediction with debouncing
  const savePrediction = useCallback(async (matchId, homeScore, awayScore) => {
    // Validate inputs
    const home = parseInt(homeScore, 10);
    const away = parseInt(awayScore, 10);
    
    if (isNaN(home) || isNaN(away) || home < 0 || away < 0) {
      return; // Don't save invalid inputs
    }

    setSavingStates(prev => ({ ...prev, [matchId]: 'saving' }));

    try {
      await apiClient.post('/predictions', {
        matchId,
        predictedHomeScore: home,
        predictedAwayScore: away,
      });

      // Refresh predictions to get updated data (including points if match is finished)
      await fetchPredictions();

      setSavingStates(prev => ({ ...prev, [matchId]: 'saved' }));
      
      // Clear saved status after 2 seconds
      setTimeout(() => {
        setSavingStates(prev => {
          const newState = { ...prev };
          delete newState[matchId];
          return newState;
        });
      }, 2000);
    } catch (error) {
      console.error('Failed to save prediction:', error);
      setSavingStates(prev => ({ ...prev, [matchId]: 'error' }));
      
      // Clear error status after 3 seconds
      setTimeout(() => {
        setSavingStates(prev => {
          const newState = { ...prev };
          delete newState[matchId];
          return newState;
        });
      }, 3000);
    }
  }, [fetchPredictions]);

  // Handle input change with debouncing
  const handlePredictionChange = useCallback((matchId, field, value) => {
    // Update input state immediately using functional update
    setPredictionInputs(prev => {
      const updated = {
        ...prev,
        [matchId]: {
          ...(prev[matchId] || {}),
          [field]: value,
        }
      };

      // Clear existing timer for this match
      if (debounceTimers.current[matchId]) {
        clearTimeout(debounceTimers.current[matchId]);
      }

      // Get the other field value from updated state
      const currentInputs = updated[matchId] || {};
      const homeScore = field === 'homeScore' ? value : (currentInputs.homeScore || '');
      const awayScore = field === 'awayScore' ? value : (currentInputs.awayScore || '');

      // Only save if both fields have valid values
      const home = parseInt(homeScore, 10);
      const away = parseInt(awayScore, 10);
      
      if (!isNaN(home) && !isNaN(away) && home >= 0 && away >= 0) {
        // Debounce: wait 800ms after user stops typing
        debounceTimers.current[matchId] = setTimeout(() => {
          savePrediction(matchId, homeScore, awayScore);
        }, 800);
      }

      return updated;
    });
  }, [savePrediction]);

  useEffect(() => {
    let filtered = matches;

    // Filter by status
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(m => m.status === statusFilter);
    }

    // Filter by group
    if (groupFilter !== 'ALL') {
      filtered = filtered.filter(m => m.group === groupFilter);
    }

    // Filter by search term (team name)
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter(m => 
        m.homeTeam.toLowerCase().includes(searchLower) ||
        m.awayTeam.toLowerCase().includes(searchLower)
      );
    }

    // Sort matches
    filtered.sort((a, b) => {
      let comparison = 0;
      
      switch (sortBy) {
        case 'date':
          comparison = new Date(a.matchDate) - new Date(b.matchDate);
          break;
        case 'group':
          comparison = (a.group || '').localeCompare(b.group || '');
          break;
        case 'status':
          comparison = a.status.localeCompare(b.status);
          break;
        case 'team':
          const aTeam = `${a.homeTeam} vs ${a.awayTeam}`;
          const bTeam = `${b.homeTeam} vs ${b.awayTeam}`;
          comparison = aTeam.localeCompare(bTeam);
          break;
        default:
          comparison = new Date(a.matchDate) - new Date(b.matchDate);
      }
      
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    setFilteredMatches(filtered);
  }, [matches, statusFilter, groupFilter, sortBy, sortOrder, searchTerm]);

  // Cleanup timers on unmount
  useEffect(() => {
    return () => {
      Object.values(debounceTimers.current).forEach(timer => {
        if (timer) clearTimeout(timer);
      });
    };
  }, []);

  // Get unique groups for filter
  const uniqueGroups = ['ALL', ...new Set(matches.map(m => m.group).filter(Boolean))];

  if (loading) {
    return (
      <>
        <Navigation />
        <div className="matches-loading">Loading matches...</div>
      </>
    );
  }

  return (
    <>
      <Navigation />
      <div className="matches-container">
        <div className="matches-header">
          <h1>World Cup 2026 Matches</h1>
          <p>View all matches and make your predictions</p>
        </div>

        <div className="matches-filters-wrapper">
          <button 
            className="filters-toggle"
            onClick={() => setFiltersExpanded(!filtersExpanded)}
            aria-expanded={filtersExpanded}
          >
            {filtersExpanded ? '▼' : '▶'} Filters
          </button>
          <div className={`matches-filters ${filtersExpanded ? 'expanded' : ''}`}>
            <div className="filter-group">
              <label htmlFor="search-filter">Search Teams:</label>
              <input
                type="text"
                id="search-filter"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search by team name..."
                className="search-input"
              />
            </div>
            <div className="filter-group">
              <label htmlFor="status-filter">Status:</label>
              <select
                id="status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All</option>
                <option value="SCHEDULED">Scheduled</option>
                <option value="LIVE">Live</option>
                <option value="FINISHED">Finished</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="group-filter">Group/Stage:</label>
              <select
                id="group-filter"
                value={groupFilter}
                onChange={(e) => setGroupFilter(e.target.value)}
              >
                {uniqueGroups.map(group => (
                  <option key={group} value={group}>{group}</option>
                ))}
              </select>
            </div>
            <div className="filter-group">
              <label htmlFor="sort-by">Sort By:</label>
              <select
                id="sort-by"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
              >
                <option value="date">Date</option>
                <option value="group">Group</option>
                <option value="status">Status</option>
                <option value="team">Team Name</option>
              </select>
            </div>
            <div className="filter-group">
              <label htmlFor="sort-order">Order:</label>
              <select
                id="sort-order"
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              >
                <option value="asc">Ascending</option>
                <option value="desc">Descending</option>
              </select>
            </div>
          </div>
        </div>

        <div className="matches-count">
          Showing {filteredMatches.length} of {matches.length} matches
        </div>

        {filteredMatches.length === 0 ? (
          <div className="no-matches">
            <p>No matches found with the selected filters.</p>
          </div>
        ) : (
          <div className="matches-grid">
            {filteredMatches.map(match => {
              const prediction = userPredictions[match.id];
              // Use team crest/logo if available, otherwise fallback to flag
              const homeLogoUrl = match.homeTeamCrest || getFlagUrl(match.homeTeam);
              const awayLogoUrl = match.awayTeamCrest || getFlagUrl(match.awayTeam);
              return (
                <div key={match.id} className="match-card">
                  <div className="match-header">
                    <span className="match-status">{match.status}</span>
                    <span className="match-group">{match.group}</span>
                  </div>
                  
                  <div className="match-teams">
                    <div className="team">
                      <div className="team-name">
                        <img src={homeLogoUrl} alt={match.homeTeam} className="team-logo" onError={(e) => {
                          // Fallback to flag if logo fails to load
                          if (match.homeTeamCrest) {
                            e.target.src = getFlagUrl(match.homeTeam);
                          }
                        }} />
                        <span>{match.homeTeam}</span>
                      </div>
                      {(match.status === 'FINISHED' || match.status === 'LIVE') && (
                        <div className="team-score">{match.homeScore}</div>
                      )}
                      {match.status === 'SCHEDULED' && new Date(match.matchDate) > new Date() && (
                        <div className="prediction-input-wrapper">
                          <input
                            type="number"
                            min="0"
                            max="20"
                            className="prediction-input"
                            value={predictionInputs[match.id]?.homeScore || ''}
                            onChange={(e) => handlePredictionChange(match.id, 'homeScore', e.target.value)}
                            placeholder="0"
                          />
                        </div>
                      )}
                    </div>
                    
                    <div className="match-vs">vs</div>
                    
                    <div className="team">
                      <div className="team-name">
                        <img src={awayLogoUrl} alt={match.awayTeam} className="team-logo" onError={(e) => {
                          // Fallback to flag if logo fails to load
                          if (match.awayTeamCrest) {
                            e.target.src = getFlagUrl(match.awayTeam);
                          }
                        }} />
                        <span>{match.awayTeam}</span>
                      </div>
                      {(match.status === 'FINISHED' || match.status === 'LIVE') && (
                        <div className="team-score">{match.awayScore}</div>
                      )}
                      {match.status === 'SCHEDULED' && new Date(match.matchDate) > new Date() && (
                        <div className="prediction-input-wrapper">
                          <input
                            type="number"
                            min="0"
                            max="20"
                            className="prediction-input"
                            value={predictionInputs[match.id]?.awayScore || ''}
                            onChange={(e) => handlePredictionChange(match.id, 'awayScore', e.target.value)}
                            placeholder="0"
                          />
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="match-info">
                    <div className="match-date">
                      {new Date(match.matchDate).toLocaleString('en-US', {
                        weekday: 'short',
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </div>
                  </div>

                  {/* Countdown timer at bottom center */}
                  {match.status === 'SCHEDULED' && (
                    <div className="countdown-wrapper-bottom">
                      <CountdownTimer 
                        matchDate={match.matchDate} 
                        status={match.status}
                        matchId={match.id}
                        onExpired={handleCountdownExpired}
                      />
                    </div>
                  )}

                  {/* Prediction status for scheduled matches */}
                  {match.status === 'SCHEDULED' && (
                    <div className="match-actions">
                      {savingStates[match.id] === 'saving' && (
                        <span className="prediction-status saving">💾 Saving...</span>
                      )}
                      {savingStates[match.id] === 'saved' && (
                        <span className="prediction-status saved">✓ Saved</span>
                      )}
                      {savingStates[match.id] === 'error' && (
                        <span className="prediction-status error">✗ Error saving</span>
                      )}
                      {!savingStates[match.id] && predictionInputs[match.id]?.homeScore && predictionInputs[match.id]?.awayScore && (
                        <span className="prediction-status">Enter scores above to save</span>
                      )}
                    </div>
                  )}
                  
                  {/* Locked message for live matches */}
                  {match.status === 'LIVE' && (
                    <div className="match-actions">
                      <span className="prediction-status locked">🔒 Predictions locked - Match is LIVE</span>
                    </div>
                  )}

                  {/* Results summary for finished matches */}
                  {match.status === 'FINISHED' && (
                    <div className="match-result-summary">
                      <div className="result-comparison">
                        <div className="result-row">
                          <span className="result-label">Your Prediction</span>
                          <span className="result-prediction-score">
                            {userPredictions[match.id] 
                              ? `${userPredictions[match.id].homeScore} - ${userPredictions[match.id].awayScore}`
                              : 'No prediction'}
                          </span>
                        </div>
                        <div className="result-row">
                          <span className="result-label">Final Score</span>
                          <span className="result-actual-score">
                            {match.homeScore} - {match.awayScore}
                          </span>
                        </div>
                      </div>
                      {userPredictions[match.id] && (
                        <div className="points-display">
                          <span className="points-label">Points Earned</span>
                          <span className={`points-badge ${userPredictions[match.id].points !== null && userPredictions[match.id].points !== undefined ? 'points-calculated' : 'points-pending'}`}>
                            {userPredictions[match.id].points !== null && userPredictions[match.id].points !== undefined
                              ? `${userPredictions[match.id].points} ${userPredictions[match.id].points === 1 ? 'point' : 'points'}`
                              : (
                                <button 
                                  className="btn-calculate-points"
                                  onClick={async () => {
                                    try {
                                      await apiClient.post(`/matches/${match.id}/calculate-points`);
                                      await fetchPredictions();
                                    } catch (error) {
                                      console.error('Failed to calculate points:', error);
                                    }
                                  }}
                                >
                                  Calculate Points
                                </button>
                              )}
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </>
  );
};

export default Matches;

