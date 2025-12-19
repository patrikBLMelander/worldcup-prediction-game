import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import { useNotifications } from '../context/NotificationContext';
import './Leagues.css';

const Leagues = () => {
  const navigate = useNavigate();
  const { markSectionAsRead } = useNotifications();
  const [leagues, setLeagues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form states
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showJoinForm, setShowJoinForm] = useState(false);
  const [createFormData, setCreateFormData] = useState({
    name: '',
    startDate: '',
    endDate: '',
    bettingType: 'FLAT_STAKES',
    entryPrice: '',
    payoutStructure: 'WINNER_TAKES_ALL',
    rankedPercentages: { 1: 0.60, 2: 0.30, 3: 0.10 }
  });
  const [joinCode, setJoinCode] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [joinLoading, setJoinLoading] = useState(false);
  
  // Members modal state
  const [selectedLeagueId, setSelectedLeagueId] = useState(null);
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [showMembersModal, setShowMembersModal] = useState(false);
  // Invite modal state
  const [inviteLeagueId, setInviteLeagueId] = useState(null);
  const [showInviteModal, setShowInviteModal] = useState(false);

  useEffect(() => {
    fetchLeagues();
  }, []);

  // Clear any notifications that belong to the Leagues section when this page is viewed
  useEffect(() => {
    markSectionAsRead('/leagues');
  }, [markSectionAsRead]);

  const fetchLeagues = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get('/leagues/mine');
      setLeagues(response.data);
      setError('');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load leagues');
      console.error('Failed to fetch leagues:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateLeague = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setError('');
    setSuccess('');

    try {
      // Frontend validation
      if (createFormData.bettingType === 'FLAT_STAKES') {
        // Validate entry price
        if (!createFormData.entryPrice || parseFloat(createFormData.entryPrice) <= 0) {
          setError('Entry price is required and must be greater than $0.00');
          setCreateLoading(false);
          return;
        }

        // Validate ranked percentages if using RANKED payout
        if (createFormData.payoutStructure === 'RANKED') {
          const total = Object.values(createFormData.rankedPercentages)
            .reduce((a, b) => a + (b || 0), 0);
          if (Math.abs(total - 1) > 0.001) {
            setError('Ranked percentages must sum to exactly 100%');
            setCreateLoading(false);
            return;
          }
        }
      }

      const payload = {
        name: createFormData.name.trim(),
        startDate: createFormData.startDate,
        endDate: createFormData.endDate,
        bettingType: createFormData.bettingType
      };

      // Add Flat Stakes fields if betting type is FLAT_STAKES
      if (createFormData.bettingType === 'FLAT_STAKES') {
        payload.entryPrice = parseFloat(createFormData.entryPrice);
        payload.payoutStructure = createFormData.payoutStructure;
        
        if (createFormData.payoutStructure === 'RANKED') {
          payload.rankedPercentages = createFormData.rankedPercentages;
        }
      }

      const response = await apiClient.post('/leagues', payload);

      setSuccess(`League "${response.data.name}" created successfully! Join code: ${response.data.joinCode}`);
      setCreateFormData({ 
        name: '', 
        startDate: '', 
        endDate: '',
        bettingType: 'FLAT_STAKES',
        entryPrice: '',
        payoutStructure: 'WINNER_TAKES_ALL',
        rankedPercentages: { 1: 0.60, 2: 0.30, 3: 0.10 }
      });
      setShowCreateForm(false);
      await fetchLeagues();
    } catch (err) {
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Failed to create league';
      setError(errorMsg);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleJoinLeague = async (e) => {
    e.preventDefault();
    setJoinLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await apiClient.post('/leagues/join', {
        joinCode: joinCode.trim().toUpperCase()
      });

      const league = response.data;
      let successMsg = `Successfully joined league "${league.name}"!`;
      
      // Add betting info to success message
      if (league.bettingType === 'FLAT_STAKES' && league.entryPrice) {
        successMsg += ` Entry price: $${parseFloat(league.entryPrice).toFixed(2)}`;
        if (league.payoutStructure === 'WINNER_TAKES_ALL') {
          successMsg += ' (Winner Takes All)';
        } else if (league.payoutStructure === 'RANKED') {
          successMsg += ' (Ranked Payout)';
        }
      }
      
      setSuccess(successMsg);
      setJoinCode('');
      setShowJoinForm(false);
      await fetchLeagues();
    } catch (err) {
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Failed to join league';
      setError(errorMsg);
    } finally {
      setJoinLoading(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const isLocked = (league) => {
    const now = new Date();
    const startDate = new Date(league.startDate);
    return now >= startDate;
  };

  const copyJoinCode = (code) => {
    navigator.clipboard.writeText(code);
    setSuccess(`Join code "${code}" copied to clipboard!`);
    setTimeout(() => setSuccess(''), 3000);
  };

  const fetchLeagueMembers = async (leagueId) => {
    try {
      setMembersLoading(true);
      const response = await apiClient.get(`/leagues/${leagueId}/members`);
      setMembers(response.data);
      setSelectedLeagueId(leagueId);
      setShowMembersModal(true);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load league members');
      console.error('Failed to fetch league members:', err);
    } finally {
      setMembersLoading(false);
    }
  };

  const closeMembersModal = () => {
    setShowMembersModal(false);
    setSelectedLeagueId(null);
    setMembers([]);
  };

  const openInviteModal = (leagueId) => {
    setInviteLeagueId(leagueId);
    setShowInviteModal(true);
  };

  const closeInviteModal = () => {
    setInviteLeagueId(null);
    setShowInviteModal(false);
  };

  const getInviteUrlForLeague = (league) => {
    if (!league) return '';
    const origin = window.location.origin;
    return `${origin}/invite/${league.joinCode}`;
  };

  if (loading) {
    return (
      <div className="profile-container">
        <Navigation />
        <div className="profile-content">
          <div className="loading">Loading leagues...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <Navigation />
      <div className="profile-content">
        <div className="leagues-header">
          <h1>My Leagues</h1>
          <p>Create or join private leagues to compete with friends</p>
        </div>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        {/* Action Buttons */}
        <div className="leagues-actions">
          <button
            onClick={() => {
              setShowCreateForm(!showCreateForm);
              setShowJoinForm(false);
              setError('');
            }}
            className="btn-primary"
          >
            {showCreateForm ? 'Cancel' : '+ Create League'}
          </button>
          <button
            onClick={() => {
              setShowJoinForm(!showJoinForm);
              setShowCreateForm(false);
              setError('');
            }}
            className="btn-secondary"
          >
            {showJoinForm ? 'Cancel' : '+ Join League'}
          </button>
        </div>

        {/* Create League Form */}
        {showCreateForm && (
          <section className="profile-section">
            <h2>Create New League</h2>
            <form onSubmit={handleCreateLeague} className="profile-form">
              <div className="form-group">
                <label htmlFor="leagueName">League Name</label>
                <input
                  type="text"
                  id="leagueName"
                  value={createFormData.name}
                  onChange={(e) => setCreateFormData({ ...createFormData, name: e.target.value })}
                  placeholder="Enter league name"
                  minLength={2}
                  maxLength={100}
                  required
                  disabled={createLoading}
                />
              </div>
              <div className="form-group">
                <label htmlFor="startDate">Start Date</label>
                <input
                  type="datetime-local"
                  id="startDate"
                  value={createFormData.startDate}
                  onChange={(e) => setCreateFormData({ ...createFormData, startDate: e.target.value })}
                  required
                  disabled={createLoading}
                />
              </div>
              <div className="form-group">
                <label htmlFor="endDate">End Date</label>
                <input
                  type="datetime-local"
                  id="endDate"
                  value={createFormData.endDate}
                  onChange={(e) => setCreateFormData({ ...createFormData, endDate: e.target.value })}
                  required
                  disabled={createLoading}
                />
              </div>

              {/* Betting Type Toggle */}
              <div className="form-group">
                <label>Betting Type</label>
                <div className="toggle-group">
                  <button
                    type="button"
                    className={`toggle-option ${createFormData.bettingType === 'FLAT_STAKES' ? 'active' : ''}`}
                    onClick={() => setCreateFormData({ ...createFormData, bettingType: 'FLAT_STAKES' })}
                    disabled={createLoading}
                  >
                    Flat Stakes
                  </button>
                  <button
                    type="button"
                    className={`toggle-option ${createFormData.bettingType === 'CUSTOM_STAKES' ? 'active' : ''}`}
                    onClick={() => setCreateFormData({ ...createFormData, bettingType: 'CUSTOM_STAKES' })}
                    disabled={createLoading}
                  >
                    Custom Stakes
                    {createFormData.bettingType === 'CUSTOM_STAKES' && (
                      <span className="coming-soon-badge">Coming Soon</span>
                    )}
                  </button>
                </div>
                <p className="form-hint">
                  {createFormData.bettingType === 'FLAT_STAKES' 
                    ? 'Flat Stakes: Everyone pays the same entry price'
                    : 'Custom Stakes: Coming soon - Each player sets their own stake'}
                </p>
              </div>

              {/* Entry Price (always shown - admin is part of league) */}
              <div className="form-group">
                <label htmlFor="entryPrice">Entry Price ($)</label>
                <input
                  type="number"
                  id="entryPrice"
                  min="0.01"
                  step="0.01"
                  value={createFormData.entryPrice}
                  onChange={(e) => setCreateFormData({ ...createFormData, entryPrice: e.target.value })}
                  placeholder="10.00"
                  required
                  disabled={createLoading}
                />
                <p className="form-hint">
                  {createFormData.bettingType === 'FLAT_STAKES' 
                    ? 'Amount each player must pay to join'
                    : 'Your entry price (you are part of the league)'}
                </p>
              </div>

              {/* Payout Structure (only for Flat Stakes) */}
              {createFormData.bettingType === 'FLAT_STAKES' && (
                <>
                  <div className="form-group">
                    <label>Payout Structure</label>
                    <div className="toggle-group">
                      <button
                        type="button"
                        className={`toggle-option ${createFormData.payoutStructure === 'WINNER_TAKES_ALL' ? 'active' : ''}`}
                        onClick={() => setCreateFormData({ ...createFormData, payoutStructure: 'WINNER_TAKES_ALL' })}
                        disabled={createLoading}
                      >
                        Winner Takes All
                      </button>
                      <button
                        type="button"
                        className={`toggle-option ${createFormData.payoutStructure === 'RANKED' ? 'active' : ''}`}
                        onClick={() => setCreateFormData({ ...createFormData, payoutStructure: 'RANKED' })}
                        disabled={createLoading}
                      >
                        Ranked Distribution
                      </button>
                    </div>
                    <p className="form-hint">
                      {createFormData.payoutStructure === 'WINNER_TAKES_ALL' 
                        ? '100% of the pot goes to 1st place'
                        : 'Prize is distributed across top ranks'}
                    </p>
                  </div>

                  {/* Ranked Percentages (if Ranked payout) */}
                  {createFormData.payoutStructure === 'RANKED' && (
                    <div className="form-group">
                      <label>Ranked Payout Percentages</label>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        {[1, 2, 3].map(rank => (
                          <div key={rank} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <label style={{ minWidth: '80px' }}>{rank}{rank === 1 ? 'st' : rank === 2 ? 'nd' : 'rd'} Place:</label>
                            <input
                              type="number"
                              min="0"
                              max="100"
                              step="1"
                              value={((createFormData.rankedPercentages[rank] || 0) * 100).toFixed(0)}
                              onChange={(e) => {
                                const newPercentages = { ...createFormData.rankedPercentages };
                                const percentValue = parseFloat(e.target.value) || 0;
                                newPercentages[rank] = percentValue / 100; // Convert percentage to decimal
                                setCreateFormData({ ...createFormData, rankedPercentages: newPercentages });
                              }}
                              style={{ flex: 1 }}
                              disabled={createLoading}
                            />
                            <span>%</span>
                          </div>
                        ))}
                      </div>
                      <p className="form-hint">
                        Total: {(Object.values(createFormData.rankedPercentages).reduce((a, b) => a + (b || 0), 0) * 100).toFixed(0)}%
                        {Math.abs(Object.values(createFormData.rankedPercentages).reduce((a, b) => a + (b || 0), 0) - 1) > 0.001 && (
                          <span style={{ color: 'var(--error)', marginLeft: '0.5rem' }}>
                            (Must equal 100%)
                          </span>
                        )}
                        {Math.abs(Object.values(createFormData.rankedPercentages).reduce((a, b) => a + (b || 0), 0) - 1) <= 0.001 && (
                          <span style={{ color: 'var(--success)', marginLeft: '0.5rem' }}>
                            ✓ Valid
                          </span>
                        )}
                      </p>
                    </div>
                  )}
                </>
              )}

              {/* Warning message for Custom Stakes */}
              {createFormData.bettingType === 'CUSTOM_STAKES' && (
                <div className="form-group" style={{ 
                  padding: '1rem', 
                  backgroundColor: 'rgba(245, 158, 11, 0.1)', 
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid rgba(245, 158, 11, 0.3)'
                }}>
                  <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--warning)' }}>
                    <strong>⚠️ Custom Stakes Coming Soon</strong><br />
                    This feature is not yet available. Please select "Flat Stakes" to create a league.
                  </p>
                </div>
              )}

              <button
                type="submit"
                className="btn-primary"
                disabled={createLoading || createFormData.bettingType === 'CUSTOM_STAKES'}
              >
                {createLoading ? 'Creating...' : 'Create League'}
              </button>
            </form>
          </section>
        )}

        {/* Join League Form */}
        {showJoinForm && (
          <section className="profile-section">
            <h2>Join League</h2>
            <form onSubmit={handleJoinLeague} className="profile-form">
              <div className="form-group">
                <label htmlFor="joinCode">Join Code</label>
                <input
                  type="text"
                  id="joinCode"
                  value={joinCode}
                  onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                  placeholder="Enter league join code"
                  maxLength={32}
                  required
                  disabled={joinLoading}
                  style={{ textTransform: 'uppercase' }}
                />
                <p className="form-hint">Enter the 8-character join code provided by the league owner</p>
              </div>
              <div className="form-group" style={{ 
                padding: '1rem', 
                backgroundColor: 'var(--surface-elevated)', 
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border)'
              }}>
                <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                  <strong>Note:</strong> If this is a Flat Stakes league, you'll be required to pay the entry price when joining.
                  The entry price and payout structure will be shown after you enter a valid join code.
                </p>
              </div>
              <button
                type="submit"
                className="btn-primary"
                disabled={joinLoading}
              >
                {joinLoading ? 'Joining...' : 'Join League'}
              </button>
            </form>
          </section>
        )}

        {/* Leagues List */}
        <section className="profile-section">
          <h2>Your Leagues</h2>
          {leagues.length === 0 ? (
            <p className="section-description">
              You haven't joined any leagues yet. Create a new league or join one with a join code!
            </p>
          ) : (
            <div className="leagues-list">
              {leagues.map((league) => (
                <div key={league.id} className="league-card">
                  <div className="league-card-header">
                    <h3>{league.name}</h3>
                    {isLocked(league) && (
                      <span className="league-badge locked">Locked</span>
                    )}
                    {!isLocked(league) && (
                      <span className="league-badge open">Open</span>
                    )}
                  </div>
                  <div className="league-card-body">
                    <div className="league-info">
                      <div className="league-info-item">
                        <span className="league-info-label">Start:</span>
                        <span className="league-info-value">{formatDate(league.startDate)}</span>
                      </div>
                      <div className="league-info-item">
                        <span className="league-info-label">End:</span>
                        <span className="league-info-value">{formatDate(league.endDate)}</span>
                      </div>
                      {league.ownerId && (
                        <div className="league-info-item">
                          <span className="league-info-label">Owner:</span>
                          <span className="league-info-value">{league.ownerScreenName || 'Unknown'}</span>
                        </div>
                      )}
                      {league.bettingType === 'FLAT_STAKES' && league.entryPrice && (
                        <>
                          <div className="league-info-item">
                            <span className="league-info-label">Entry Price:</span>
                            <span className="league-info-value" style={{ color: 'var(--primary-500)', fontWeight: 600 }}>
                              ${parseFloat(league.entryPrice).toFixed(2)}
                            </span>
                          </div>
                          <div className="league-info-item">
                            <span className="league-info-label">Payout:</span>
                            <span className="league-info-value">
                              {league.payoutStructure === 'WINNER_TAKES_ALL' 
                                ? 'Winner Takes All' 
                                : league.payoutStructure === 'RANKED' 
                                  ? 'Ranked Distribution' 
                                  : 'N/A'}
                            </span>
                          </div>
                        </>
                      )}
                    </div>
                    <div className="league-actions">
                      <div className="league-action-buttons">
                        <button
                          onClick={() => openInviteModal(league.id)}
                          className="btn-secondary btn-small"
                        >
                          Invite Friends
                        </button>
                        <button
                          onClick={() => fetchLeagueMembers(league.id)}
                          className="btn-secondary btn-small"
                        >
                          View Members
                        </button>
                        <button
                          onClick={() => navigate(`/leaderboard?league=${league.id}`)}
                          className="btn-secondary btn-small"
                        >
                          View Leaderboard
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Members Modal */}
        {showMembersModal && (
          <>
            <div className="modal-overlay" onClick={closeMembersModal} />
            <div className="members-modal">
              <div className="members-modal-header">
                <h2>
                  {leagues.find(l => l.id === selectedLeagueId)?.name || 'League'} Members
                </h2>
                <button
                  onClick={closeMembersModal}
                  className="modal-close-button"
                  aria-label="Close"
                >
                  ×
                </button>
              </div>
              <div className="members-modal-content">
                {membersLoading ? (
                  <div className="loading">Loading members...</div>
                ) : members.length === 0 ? (
                  <p className="section-description">No members found.</p>
                ) : (
                  <div className="members-list">
                    {members.map((member) => (
                      <div key={member.userId} className="member-item">
                        <div className="member-info">
                          <div className="member-name">
                            {member.screenName || member.email}
                            {member.role === 'OWNER' && (
                              <span className="member-role-badge owner">Owner</span>
                            )}
                          </div>
                          <div className="member-email">{member.email}</div>
                          <div className="member-joined">
                            Joined: {formatDate(member.joinedAt)}
                          </div>
                        </div>
                        <button
                          onClick={() => navigate(`/user/${member.userId}`)}
                          className="btn-icon-small"
                          title="View profile"
                        >
                          👤
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* Invite Modal */}
        {showInviteModal && (
          <>
            <div className="modal-overlay" onClick={closeInviteModal} />
            <div className="members-modal">
              <div className="members-modal-header">
                <h2>
                  Invite to{' '}
                  {leagues.find(l => l.id === inviteLeagueId)?.name || 'League'}
                </h2>
                <button
                  onClick={closeInviteModal}
                  className="modal-close-button"
                  aria-label="Close"
                >
                  ×
                </button>
              </div>
              <div className="members-modal-content">
                {(() => {
                  const league = leagues.find(l => l.id === inviteLeagueId);
                  const inviteUrl = getInviteUrlForLeague(league);
                  if (!league) {
                    return <p className="section-description">League not found.</p>;
                  }

                  const handleCopyLink = () => {
                    navigator.clipboard.writeText(inviteUrl);
                  };

                  return (
                    <div className="invite-content">
                      <p className="section-description">
                        Share this link or QR code so friends can easily join your league.
                      </p>
                      <div className="invite-link-section">
                        <label className="league-info-label">Invite Link</label>
                        <div className="invite-link-row">
                          <input
                            type="text"
                            readOnly
                            value={inviteUrl}
                            className="invite-link-input"
                            onFocus={(e) => e.target.select()}
                          />
                          <button
                            onClick={handleCopyLink}
                            className="btn-secondary btn-small"
                            type="button"
                          >
                            Copy
                          </button>
                        </div>
                      </div>
                      <div className="invite-qr-section">
                        <label className="league-info-label">QR Code</label>
                        <div className="invite-qr-wrapper">
                          <img
                            src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(
                              inviteUrl
                            )}`}
                            alt="League invite QR code"
                          />
                        </div>
                        <p className="form-hint">
                          Friends can scan this QR code with their phone camera to open the invite
                          link.
                        </p>
                      </div>
                      <div className="invite-code-section">
                        <details className="invite-code-details">
                          <summary className="invite-code-summary">Show Join Code (for manual entry)</summary>
                          <div className="invite-code-content">
                            <p className="form-hint" style={{ marginBottom: 'var(--spacing-sm)' }}>
                              Share this code if someone prefers to enter it manually:
                            </p>
                            <div className="invite-link-row">
                              <code className="invite-code-value">{league.joinCode}</code>
                              <button
                                onClick={() => copyJoinCode(league.joinCode)}
                                className="btn-secondary btn-small"
                                type="button"
                              >
                                Copy Code
                              </button>
                            </div>
                          </div>
                        </details>
                      </div>
                    </div>
                  );
                })()}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default Leagues;

