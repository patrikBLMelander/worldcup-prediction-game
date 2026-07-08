import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { useWebSocket } from '../hooks/useWebSocket';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import CountdownTimer from '../components/CountdownTimer';
import StandingsModal from '../components/StandingsModal';
import { getFlagUrl, hasKnownFlag } from '../utils/countryFlags';
import './Matches.css';

const Matches = () => {
  const { user } = useAuth();
  const { markSectionAsRead } = useNotifications();
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
  const [standingsGroup, setStandingsGroup] = useState(null); // Selected group for standings modal

  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(() => {
    // Initialize from URL parameter if present
    const tabParam = searchParams.get('tab');
    return tabParam === 'results' ? 'results' : 'upcoming';
  });

  // Clear any notifications that belong to the Matches section when this page is viewed
  useEffect(() => {
    markSectionAsRead('/matches');
  }, [markSectionAsRead]);

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
          outcome: pred.predictedOutcome,
          points: pred.points,
        };
        // Initialize selected outcome with existing predictions
        inputsMap[pred.matchId] = {
          outcome: pred.predictedOutcome,
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

  // Save an outcome pick immediately (one click = one prediction)
  const selectOutcome = useCallback(async (matchId, outcome) => {
    // Optimistically reflect the selection right away
    setPredictionInputs(prev => ({ ...prev, [matchId]: { outcome } }));
    setSavingStates(prev => ({ ...prev, [matchId]: 'saving' }));

    try {
      await apiClient.post('/predictions', {
        matchId,
        predictedOutcome: outcome,
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

  // Currently selected outcome for a match (pending selection or saved prediction)
  const selectedOutcome = (matchId) =>
    predictionInputs[matchId]?.outcome ?? userPredictions[matchId]?.outcome ?? null;

  // Map points to an existing colour class (green for any score, red for 0)
  const pointsClass = (points) => {
    if (points === null || points === undefined) return 'points-pending';
    if (points === 0) return 'points-0';
    return 'points-3';
  };

  // Human label for a predicted outcome
  const outcomeLabel = (match, outcome) => {
    if (outcome === 'DRAW') return 'Draw';
    if (outcome === 'HOME_WIN') return match.homeTeam;
    if (outcome === 'AWAY_WIN') return match.awayTeam;
    return null;
  };

  // Small superscript shown next to a team's regulation score for knockout
  // matches decided after 90 minutes (display only - the big number is the
  // regulation score that actually counts). Penalties show as "+N".
  const scoreSuffix = (match, side) => {
    if (match.duration === 'PENALTY_SHOOTOUT') {
      const p = side === 'home' ? match.penaltiesHome : match.penaltiesAway;
      return p !== null && p !== undefined ? `+${p}` : null;
    }
    if (match.duration === 'EXTRA_TIME') {
      const et = side === 'home' ? match.extraTimeHome : match.extraTimeAway;
      return et !== null && et !== undefined ? `${et}` : null;
    }
    return null;
  };

  // Short label for how a knockout was decided, or null for a normal result.
  const deciderLabel = (match) => {
    if (match.duration === 'PENALTY_SHOOTOUT') return 'pens';
    if (match.duration === 'EXTRA_TIME') return 'a.e.t.';
    return null;
  };

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

        <div className="scoring-rules" aria-label="Scoring rules">
          <div className="scoring-rules-title">Scoring</div>
          <div className="scoring-rules-tiers">
            <div className="scoring-tier scoring-tier-exact">
              <span className="scoring-tier-points">Pick the result</span>
              <span className="scoring-tier-label">Home win, draw or away win</span>
            </div>
            <div className="scoring-tier scoring-tier-winner">
              <span className="scoring-tier-points">Rarer = more</span>
              <span className="scoring-tier-label">Alone with the right call earns the most; less as more get it right</span>
            </div>
            <div className="scoring-tier scoring-tier-wrong">
              <span className="scoring-tier-points">100 → 200</span>
              <span className="scoring-tier-label">Group games worth 100, rising to 200 for the final</span>
            </div>
          </div>
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

        {uniqueGroups.length > 1 && (
          <div className="group-pills" role="group" aria-label="Filter by group or stage">
            {uniqueGroups.map(g => (
              <button
                key={g}
                type="button"
                className={`group-pill ${groupFilter === g ? 'active' : ''}`}
                onClick={() => setGroupFilter(g)}
                title={g === 'ALL' ? 'All groups & stages' : g}
              >
                {g === 'ALL' ? 'All' : (g.startsWith('Group ') ? g.replace('Group ', '') : g)}
              </button>
            ))}
          </div>
        )}

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
              
              // Determine result type for finished matches (correct outcome vs wrong)
              let resultType = null;
              if (isFinished && prediction && match.homeScore !== null && match.awayScore !== null) {
                const points = prediction.points;
                if (points === null || points === undefined) {
                  resultType = null;
                } else if (points > 0) {
                  resultType = 'correct-winner';
                } else {
                  resultType = 'wrong';
                }
              }

              // Calculate time remaining for scheduled matches (for color coding)
              let timeRemainingClass = '';
              if (match.status === 'SCHEDULED' && match.matchDate) {
                // Check if already predicted
                const hasPrediction = !!selectedOutcome(match.id);
                
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
                      {match.group && match.group.startsWith('Group ') ? (
                        <button
                          type="button"
                          className={`match-group match-group-clickable ${isMobile ? 'mobile-header-text' : 'desktop-header-text'}`}
                          onClick={() => setStandingsGroup(match.group)}
                          title="View standings"
                        >
                          {match.group}
                        </button>
                      ) : (
                        <span className={`match-group ${isMobile ? 'mobile-header-text' : 'desktop-header-text'}`}>{match.group}</span>
                      )}
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
                            <span className={`header-points-badge ${pointsClass(prediction.points)}`}>
                              {prediction.points === 1 ? '1 pt' : `${prediction.points} pts`}
                            </span>
                          ) : (
                            <span className="header-points-badge points-pending">Pending</span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {isFinished ? (
                    /* Finished match: compact result row with the user's pick */
                    <div className={`match-compact ${isMobile ? 'mobile-match-compact' : 'desktop-match-compact'}`}>
                      <div className={`compact-row ${isMobile ? 'mobile-compact-row' : 'desktop-compact-row'}`}>
                        <img src={homeLogoUrl} alt={match.homeTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                          if (match.homeTeamCrest) {
                            e.target.src = getFlagUrl(match.homeTeam);
                          }
                        }} />
                        <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.homeTeam}</span>
                        {match.homeScore !== null && match.awayScore !== null && (
                          <span className={isMobile ? 'mobile-score' : 'desktop-score'}>
                            {match.homeScore}
                            {scoreSuffix(match, 'home') && <sup className="score-extra">{scoreSuffix(match, 'home')}</sup>}
                          </span>
                        )}
                        <span className={isMobile ? 'mobile-vs' : 'desktop-vs'}>vs</span>
                        {match.homeScore !== null && match.awayScore !== null && (
                          <span className={isMobile ? 'mobile-score' : 'desktop-score'}>
                            {match.awayScore}
                            {scoreSuffix(match, 'away') && <sup className="score-extra">{scoreSuffix(match, 'away')}</sup>}
                          </span>
                        )}
                        <span className={isMobile ? 'mobile-team-name' : 'desktop-team-name'}>{match.awayTeam}</span>
                        <img src={awayLogoUrl} alt={match.awayTeam} className={isMobile ? 'mobile-team-logo' : 'desktop-team-logo'} onError={(e) => {
                          if (match.awayTeamCrest) {
                            e.target.src = getFlagUrl(match.awayTeam);
                          }
                        }} />
                        {deciderLabel(match) && (
                          <span className="score-decider">{deciderLabel(match)}</span>
                        )}
                        {prediction && prediction.outcome ? (
                          <span className={`${isMobile ? 'mobile-prediction-result' : 'desktop-prediction-result'} ${pointsClass(prediction.points)}`}>
                            {outcomeLabel(match, prediction.outcome)}
                          </span>
                        ) : (
                          <span className={isMobile ? 'mobile-no-prediction' : 'desktop-no-prediction'}>No prediction</span>
                        )}
                      </div>
                    </div>
                  ) : (
                    /* Scheduled / Live: Copabet-style pick layout (names on top, 3 big cells) */
                    <div className="predict-body">
                      <div className="predict-teams">
                        <span className="predict-team-name home">{match.homeTeam}</span>
                        <span className="predict-team-name away">{match.awayTeam}</span>
                      </div>
                      {(() => {
                        const locked = match.status !== 'SCHEDULED';
                        const sel = selectedOutcome(match.id);
                        const cell = (outcome, content, label) => (
                          <button
                            type="button"
                            className={`outcome-cell ${outcome === 'DRAW' ? 'outcome-draw' : ''} ${sel === outcome ? 'selected' : ''} ${locked ? 'locked' : ''}`}
                            onClick={locked ? undefined : () => selectOutcome(match.id, outcome)}
                            disabled={locked}
                            title={label}
                            aria-label={label}
                          >
                            {content}
                          </button>
                        );
                        const flag = (crest, team) => {
                          // No crest from the feed and no known country -> neutral placeholder box
                          if (!crest && !hasKnownFlag(team)) {
                            return <span className="hex-frame"><span className="outcome-flag outcome-flag-unknown" aria-hidden="true" /></span>;
                          }
                          const url = crest || getFlagUrl(team);
                          return (
                            <span className="hex-frame">
                              <img src={url} alt="" className="outcome-flag" onError={(e) => { if (crest) e.target.src = getFlagUrl(team); }} />
                            </span>
                          );
                        };
                        const drawHex = (
                          <span className="outcome-draw-hex">
                            <svg className="outcome-draw-svg" viewBox="0 0 56 56" aria-hidden="true">
                              <polygon points="15,3 41,3 54,28 41,53 15,53 2,28" fill="none" stroke="currentColor" strokeWidth="3" strokeLinejoin="round" />
                            </svg>
                            <span className="outcome-draw-x">X</span>
                          </span>
                        );
                        return (
                          <div className="outcome-picker" role="group" aria-label="Pick the result">
                            {cell('HOME_WIN', flag(match.homeTeamCrest, match.homeTeam), `${match.homeTeam} win`)}
                            {cell('DRAW', drawHex, 'Draw')}
                            {cell('AWAY_WIN', flag(match.awayTeamCrest, match.awayTeam), `${match.awayTeam} win`)}
                          </div>
                        );
                      })()}
                      {match.status === 'LIVE' && match.homeScore !== null && match.awayScore !== null && (
                        <div className="predict-live-score">
                          <span className="live-indicator">LIVE</span> {match.homeScore} - {match.awayScore}
                        </div>
                      )}
                      {match.status === 'SCHEDULED' && (
                        <div className="predict-saving">
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
                  )}

                  {/* Results summary for finished matches - only show when expanded on desktop */}
                  {!isMobile && match.status === 'FINISHED' && isExpanded && (
                    <div className="match-result-summary">
                      <div className="result-comparison">
                        <div className="result-row">
                          <span className="result-label">Your Prediction</span>
                          <span className="result-prediction-score">
                            {userPredictions[match.id]?.outcome
                              ? outcomeLabel(match, userPredictions[match.id].outcome)
                              : 'No prediction'}
                          </span>
                        </div>
                        <div className="result-row">
                          <span className="result-label">Final Score</span>
                          <span className="result-actual-score">
                            {match.homeScore}
                            {scoreSuffix(match, 'home') && <sup className="score-extra">{scoreSuffix(match, 'home')}</sup>}
                            {' - '}
                            {match.awayScore}
                            {scoreSuffix(match, 'away') && <sup className="score-extra">{scoreSuffix(match, 'away')}</sup>}
                            {deciderLabel(match) && <span className="score-decider">{deciderLabel(match)}</span>}
                          </span>
                        </div>
                      </div>
                      {userPredictions[match.id] && (
                        <div className="points-display">
                          <span className="points-label">Points Earned</span>
                          <span className={`points-badge ${pointsClass(userPredictions[match.id].points)}`}>
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
      <StandingsModal
        isOpen={standingsGroup !== null}
        onClose={() => setStandingsGroup(null)}
        group={standingsGroup}
      />
    </div>
  );
};

export default Matches;

