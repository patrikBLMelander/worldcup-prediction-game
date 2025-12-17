import { useEffect, useState, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
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
  const [expandedFinishedMatches, setExpandedFinishedMatches] = useState(new Set());
  const [expandedMobileMatches, setExpandedMobileMatches] = useState(new Set()); // Track expanded matches on mobile
  const [isMobile, setIsMobile] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(() => {
    // Initialize from URL parameter if present
    const tabParam = searchParams.get('tab');
    return tabParam === 'results' ? 'results' : 'upcoming';
  });
  const debounceTimers = useRef({});

  // Sync activeTab with URL parameter (for browser back/forward navigation)
  useEffect(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam === 'results' || tabParam === 'upcoming') {
      setActiveTab(tabParam);
    } else if (!tabParam) {
      // If no tab param, default to upcoming
      setActiveTab('upcoming');
    }
  }, [searchParams]);

  // Detect mobile viewport
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

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
      // Parse match date as UTC
      const dateStr = match.matchDate;
      const matchDate = dateStr.endsWith('Z') 
        ? new Date(dateStr)
        : new Date(dateStr + 'Z');
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
      const homeScore = field === 'homeScore' ? value : (currentInputs.homeScore ?? '');
      const awayScore = field === 'awayScore' ? value : (currentInputs.awayScore ?? '');

      // Only save if both fields have valid numeric values (not empty strings)
      // Empty string means no prediction yet, so don't save
      if (homeScore !== '' && awayScore !== '') {
        const home = parseInt(homeScore, 10);
        const away = parseInt(awayScore, 10);
        
        if (!isNaN(home) && !isNaN(away) && home >= 0 && away >= 0) {
          // Debounce: wait 800ms after user stops typing
          debounceTimers.current[matchId] = setTimeout(() => {
            savePrediction(matchId, homeScore, awayScore);
          }, 800);
        }
      }

      return updated;
    });
  }, [savePrediction]);

  useEffect(() => {
    let filtered = matches;

    // Filter by active tab first
    if (activeTab === 'upcoming') {
      filtered = filtered.filter(m => m.status === 'SCHEDULED' || m.status === 'LIVE');
    } else if (activeTab === 'results') {
      filtered = filtered.filter(m => m.status === 'FINISHED');
    }

    // Filter by status (only if not using tabs)
    if (statusFilter !== 'ALL' && activeTab === 'upcoming') {
      // For upcoming tab, we already filtered, but allow further filtering within LIVE/SCHEDULED
      if (statusFilter === 'SCHEDULED' || statusFilter === 'LIVE') {
        filtered = filtered.filter(m => m.status === statusFilter);
      }
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

    // For results tab, default to newest first
    if (activeTab === 'results' && sortBy === 'date' && sortOrder === 'asc') {
      filtered.reverse();
    }

    setFilteredMatches(filtered);
  }, [matches, statusFilter, groupFilter, sortBy, sortOrder, searchTerm, activeTab]);

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
      <div className="profile-container">
        <Navigation />
        <div className="matches-loading">Loading matches...</div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <Navigation />
      <div className="profile-content">
        <div className="matches-container">
        <div className="matches-header">
          <h1>World Cup 2026 Matches</h1>
          <p>View all matches and make your predictions</p>
        </div>

        {/* Tab Navigation */}
        <div className="matches-tabs">
          <button
            className={`tab-button ${activeTab === 'upcoming' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('upcoming');
              setSearchParams({ tab: 'upcoming' }, { replace: true });
            }}
          >
            <span className="tab-icon">📅</span>
            <span className="tab-label">Upcoming</span>
            <span className="tab-count">
              ({matches.filter(m => m.status === 'SCHEDULED' || m.status === 'LIVE').length})
            </span>
          </button>
          <button
            className={`tab-button ${activeTab === 'results' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('results');
              setSearchParams({ tab: 'results' }, { replace: true });
            }}
          >
            <span className="tab-icon">🏆</span>
            <span className="tab-label">Results</span>
            <span className="tab-count">
              ({matches.filter(m => m.status === 'FINISHED').length})
            </span>
          </button>
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
          Showing {filteredMatches.length} {activeTab === 'upcoming' ? 'upcoming' : 'finished'} {filteredMatches.length === 1 ? 'match' : 'matches'}
          {filteredMatches.length !== matches.filter(m => activeTab === 'upcoming' ? (m.status === 'SCHEDULED' || m.status === 'LIVE') : m.status === 'FINISHED').length && 
            ` (of ${matches.filter(m => activeTab === 'upcoming' ? (m.status === 'SCHEDULED' || m.status === 'LIVE') : m.status === 'FINISHED').length} total)`}
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
              const isFinished = match.status === 'FINISHED';
              const isExpandedDesktop = expandedFinishedMatches.has(match.id);
              // All matches are always collapsed (compact view) - same as mobile
              // Only finished matches can be expanded to show details
              const isExpanded = isFinished ? isExpandedDesktop : false;
              const isCollapsed = !isExpanded;
              
              // Determine result type for finished matches
              let resultType = null;
              if (isFinished && prediction && match.homeScore !== null && match.awayScore !== null) {
                const points = prediction.points;
                if (points === 3) {
                  resultType = 'exact';
                } else if (points === 1) {
                  resultType = 'correct-winner';
                } else if (points === 0 || points === null) {
                  resultType = 'wrong';
                }
              }

              // Check if prediction has a winner (points > 0) - only for finished matches
              const hasWinner = isFinished && prediction && prediction.points !== null && prediction.points !== undefined && prediction.points > 0;

              // Calculate time remaining for scheduled matches (for color coding)
              let timeRemainingClass = '';
              if (match.status === 'SCHEDULED' && match.matchDate) {
                // Check if already predicted
                const hasPrediction = prediction && prediction.homeScore !== undefined && prediction.awayScore !== undefined;
                
                if (hasPrediction) {
                  timeRemainingClass = 'time-predicted'; // Green for already predicted
                } else {
                  const dateStr = match.matchDate;
                  const matchTime = dateStr.endsWith('Z') 
                    ? new Date(dateStr)
                    : new Date(dateStr + 'Z');
                  const now = new Date();
                  const diffMs = matchTime - now;
                  const diffHours = diffMs / (1000 * 60 * 60);
                  
                  if (diffHours < 1) {
                    timeRemainingClass = 'time-very-soon'; // < 1 hour
                  } else if (diffHours < 12) {
                    timeRemainingClass = 'time-soon'; // < 12 hours
                  } else if (diffHours < 24) {
                    timeRemainingClass = 'time-medium'; // < 1 day
                  } else {
                    timeRemainingClass = 'time-far'; // > 1 day
                  }
                }
              }

              return (
                <div key={match.id} className={`match-card ${isFinished ? 'finished-match' : ''} ${isCollapsed ? 'collapsed' : ''} ${isMobile ? 'mobile-view' : ''} ${activeTab === 'results' ? 'results-view' : 'upcoming-view'} ${resultType ? `result-${resultType}` : ''} ${timeRemainingClass}`}>
                  <div className="match-header">
                    <div className="match-header-left">
                      <span className={`match-group ${isMobile ? 'mobile-header-text' : 'desktop-header-text'}`}>{match.group}</span>
                      {/* Timer for scheduled matches, LIVE text for live matches, Points for finished matches */}
                      {match.status === 'SCHEDULED' && (
                        <div className={isMobile ? 'mobile-header-timer' : 'desktop-header-timer'}>
                          <CountdownTimer 
                            matchDate={match.matchDate} 
                            status={match.status}
                            matchId={match.id}
                            onExpired={handleCountdownExpired}
                          />
                        </div>
                      )}
                      {match.status === 'LIVE' && (
                        <div className={isMobile ? 'mobile-header-timer' : 'desktop-header-timer'}>
                          <span className="live-indicator">LIVE</span>
                        </div>
                      )}
                      {isFinished && prediction && (
                        <div className={isMobile ? 'mobile-header-points' : 'desktop-header-points'}>
                          {prediction.points !== null && prediction.points !== undefined ? (
                            <span className={`header-points-badge points-${prediction.points}`}>
                              {prediction.points === 1 ? '1 pt' : `${prediction.points} pts`}
                            </span>
                          ) : (
                            <span className="header-points-badge points-pending">Pending</span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {/* Compact centered layout for all matches */}
                  <div className={`match-compact ${isMobile ? 'mobile-match-compact' : 'desktop-match-compact'}`}>
                    <div className={`compact-row ${isMobile ? 'mobile-compact-row' : 'desktop-compact-row'}`}>
                      {isFinished ? (
                        // Finished match: [Logo] Team1 [Score1] vs [Score2] Team2 [Logo] [Prediction]
                        <>
                          <img src={homeLogoUrl} alt={match.homeTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                            if (match.homeTeamCrest) {
                              e.target.src = getFlagUrl(match.homeTeam);
                            }
                          }} />
                          <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.homeTeam}</span>
                          {match.homeScore !== null && match.awayScore !== null && (
                            <span className={isMobile ? 'mobile-score' : 'desktop-score'}>{match.homeScore}</span>
                          )}
                          <span className={isMobile ? 'mobile-vs' : 'desktop-vs'}>vs</span>
                          {match.homeScore !== null && match.awayScore !== null && (
                            <span className={isMobile ? 'mobile-score' : 'desktop-score'}>{match.awayScore}</span>
                          )}
                          <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.awayTeam}</span>
                          <img src={awayLogoUrl} alt={match.awayTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                            if (match.awayTeamCrest) {
                              e.target.src = getFlagUrl(match.awayTeam);
                            }
                          }} />
                          {prediction && prediction.homeScore !== undefined && prediction.awayScore !== undefined ? (
                            <span className={`${isMobile ? 'mobile-prediction-result' : 'desktop-prediction-result'} ${prediction.points !== null && prediction.points !== undefined ? `points-${prediction.points}` : 'points-pending'}`}>
                              ({prediction.homeScore}-{prediction.awayScore})
                            </span>
                          ) : (
                            <span className={isMobile ? 'mobile-no-prediction' : 'desktop-no-prediction'}>No prediction</span>
                          )}
                        </>
                      ) : (
                        // Scheduled matches or desktop: original layout
                        <>
                          <div className={isMobile ? 'mobile-team' : 'desktop-team'}>
                            <img src={homeLogoUrl} alt={match.homeTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                              if (match.homeTeamCrest) {
                                e.target.src = getFlagUrl(match.homeTeam);
                              }
                            }} />
                            <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.homeTeam}</span>
                          </div>
                          {match.status === 'SCHEDULED' ? (
                            <>
                              <input
                                type="number"
                                min="0"
                                max="20"
                                className={isMobile ? 'mobile-prediction-input' : 'desktop-prediction-input'}
                                value={predictionInputs[match.id]?.homeScore ?? ''}
                                onChange={(e) => handlePredictionChange(match.id, 'homeScore', e.target.value)}
                                placeholder="-"
                              />
                              <span className={isMobile ? 'mobile-vs' : 'desktop-vs'}>-</span>
                              <input
                                type="number"
                                min="0"
                                max="20"
                                className={isMobile ? 'mobile-prediction-input' : 'desktop-prediction-input'}
                                value={predictionInputs[match.id]?.awayScore ?? ''}
                                onChange={(e) => handlePredictionChange(match.id, 'awayScore', e.target.value)}
                                placeholder="-"
                              />
                            </>
                          ) : (
                            <>
                              <span className={isMobile ? 'mobile-vs' : 'desktop-vs'}>vs</span>
                              {(match.status === 'FINISHED' || match.status === 'LIVE') && match.homeScore !== null && match.awayScore !== null && (
                                <span className={isMobile ? 'mobile-score' : 'desktop-score'}>{match.homeScore} - {match.awayScore}</span>
                              )}
                            </>
                          )}
                          <div className={isMobile ? 'mobile-team' : 'desktop-team'}>
                            <img src={awayLogoUrl} alt={match.awayTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                              if (match.awayTeamCrest) {
                                e.target.src = getFlagUrl(match.awayTeam);
                              }
                            }} />
                            <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.awayTeam}</span>
                          </div>
                          {match.status !== 'SCHEDULED' && (
                            prediction && prediction.homeScore !== undefined && prediction.awayScore !== undefined ? (
                              <span className={`${isMobile ? 'mobile-prediction' : 'desktop-prediction'} ${hasWinner ? 'has-winner' : ''}`}>
                                ({prediction.homeScore}-{prediction.awayScore})
                              </span>
                            ) : (
                              <span className={isMobile ? 'mobile-no-prediction' : 'desktop-no-prediction'}>No prediction</span>
                            )
                          )}
                        </>
                      )}
                    </div>
                    
                    {/* Prediction status for scheduled matches */}
                    {match.status === 'SCHEDULED' && (
                      <div className={isMobile ? 'mobile-match-actions' : 'desktop-match-actions'}>
                        {savingStates[match.id] === 'saving' && (
                          <span className="prediction-status saving">💾 Saving...</span>
                        )}
                        {savingStates[match.id] === 'saved' && (
                          <span className="prediction-status saved">✓ Saved</span>
                        )}
                        {savingStates[match.id] === 'error' && (
                          <span className="prediction-status error">✗ Error saving</span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Results summary for finished matches - only show when expanded on desktop */}
                  {!isMobile && match.status === 'FINISHED' && isExpanded && (
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
                          <span className={`points-badge ${userPredictions[match.id].points !== null && userPredictions[match.id].points !== undefined ? `points-${userPredictions[match.id].points}` : 'points-pending'}`}>
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
      </div>
    </div>
  );
};

export default Matches;

