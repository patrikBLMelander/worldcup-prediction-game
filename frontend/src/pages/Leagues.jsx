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
    endDate: ''
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
      const response = await apiClient.post('/leagues', {
        name: createFormData.name.trim(),
        startDate: createFormData.startDate,
        endDate: createFormData.endDate
      });

      setSuccess(`League "${response.data.name}" created successfully! Join code: ${response.data.joinCode}`);
      setCreateFormData({ name: '', startDate: '', endDate: '' });
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

      setSuccess(`Successfully joined league "${response.data.name}"!`);
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
              <button
                type="submit"
                className="btn-primary"
                disabled={createLoading}
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

