import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import Navigation from '../components/Navigation';
import './Admin.css';

const Admin = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('matches'); // 'matches', 'users', 'overview'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Matches state
  const [matches, setMatches] = useState([]);
  const [showMatchForm, setShowMatchForm] = useState(false);
  const [editingMatch, setEditingMatch] = useState(null);
  const [matchForm, setMatchForm] = useState({
    homeTeam: '',
    awayTeam: '',
    matchDate: '',
    venue: '',
    group: '',
  });

  // Users state
  const [users, setUsers] = useState([]);

  // Overview state
  const [stats, setStats] = useState({
    totalMatches: 0,
    totalUsers: 0,
    totalPredictions: 0,
    scheduledMatches: 0,
    liveMatches: 0,
    finishedMatches: 0,
  });

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      setError('Access denied. Admin privileges required.');
      return;
    }
    fetchData();
  }, [user, activeTab]);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      if (activeTab === 'matches') {
        const response = await apiClient.get('/matches');
        setMatches(response.data);
      } else if (activeTab === 'users') {
        const response = await apiClient.get('/admin/users');
        setUsers(response.data);
      } else if (activeTab === 'overview') {
        const [matchesRes, usersRes, predictionsRes] = await Promise.all([
          apiClient.get('/matches'),
          apiClient.get('/admin/users'),
          apiClient.get('/predictions/my-predictions').catch(() => ({ data: [] })),
        ]);
        const allMatches = matchesRes.data;
        setStats({
          totalMatches: allMatches.length,
          totalUsers: usersRes.data.length,
          totalPredictions: predictionsRes.data.length,
          scheduledMatches: allMatches.filter(m => m.status === 'SCHEDULED').length,
          liveMatches: allMatches.filter(m => m.status === 'LIVE').length,
          finishedMatches: allMatches.filter(m => m.status === 'FINISHED').length,
        });
      }
    } catch (err) {
      console.error('Failed to fetch data:', err);
      setError(err.response?.data?.error || 'Failed to fetch data');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateMatch = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      await apiClient.post('/admin/matches', {
        ...matchForm,
        matchDate: new Date(matchForm.matchDate).toISOString(),
      });
      setSuccess('Match created successfully!');
      setShowMatchForm(false);
      setMatchForm({
        homeTeam: '',
        awayTeam: '',
        matchDate: '',
        venue: '',
        group: '',
      });
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create match');
    }
  };

  const handleUpdateMatchResult = async (matchId, homeScore, awayScore) => {
    setError('');
    setSuccess('');

    try {
      await apiClient.put(`/admin/matches/${matchId}/result`, {
        homeScore: parseInt(homeScore),
        awayScore: parseInt(awayScore),
      });
      setSuccess('Match result updated successfully!');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update match result');
    }
  };

  const handleUpdateMatchStatus = async (matchId, status) => {
    setError('');
    setSuccess('');

    try {
      await apiClient.put(`/admin/matches/${matchId}/status?status=${status}`);
      setSuccess('Match status updated successfully!');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update match status');
    }
  };

  const handleDeleteMatch = async (matchId) => {
    if (!window.confirm('Are you sure you want to delete this match?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      await apiClient.delete(`/admin/matches/${matchId}`);
      setSuccess('Match deleted successfully!');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete match');
    }
  };

  const handleUpdateUserRole = async (userId, role) => {
    setError('');
    setSuccess('');

    try {
      await apiClient.put(`/admin/users/${userId}/role?role=${role}`);
      setSuccess('User role updated successfully!');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update user role');
    }
  };

  const handleUpdateUserEnabled = async (userId, enabled) => {
    setError('');
    setSuccess('');

    try {
      await apiClient.put(`/admin/users/${userId}/enabled?enabled=${enabled}`);
      setSuccess('User status updated successfully!');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update user status');
    }
  };

  if (user?.role !== 'ADMIN') {
    return (
      <div className="admin-container">
        <Navigation />
        <div className="admin-error">Access denied. Admin privileges required.</div>
      </div>
    );
  }

  return (
    <div className="admin-container">
      <Navigation />
      <div className="admin-content">
        <div className="admin-header">
          <h1>Admin Dashboard</h1>
        </div>

        {error && <div className="admin-message error">{error}</div>}
        {success && <div className="admin-message success">{success}</div>}

        <div className="admin-tabs">
          <button
            className={activeTab === 'overview' ? 'active' : ''}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button
            className={activeTab === 'matches' ? 'active' : ''}
            onClick={() => setActiveTab('matches')}
          >
            Matches
          </button>
          <button
            className={activeTab === 'users' ? 'active' : ''}
            onClick={() => setActiveTab('users')}
          >
            Users
          </button>
        </div>

        {loading && <div className="admin-loading">Loading...</div>}

        {activeTab === 'overview' && (
          <div className="admin-overview">
            <div className="stats-grid">
              <div className="stat-card">
                <h3>Total Matches</h3>
                <p className="stat-value">{stats.totalMatches}</p>
              </div>
              <div className="stat-card">
                <h3>Total Users</h3>
                <p className="stat-value">{stats.totalUsers}</p>
              </div>
              <div className="stat-card">
                <h3>Total Predictions</h3>
                <p className="stat-value">{stats.totalPredictions}</p>
              </div>
              <div className="stat-card">
                <h3>Scheduled</h3>
                <p className="stat-value">{stats.scheduledMatches}</p>
              </div>
              <div className="stat-card">
                <h3>Live</h3>
                <p className="stat-value">{stats.liveMatches}</p>
              </div>
              <div className="stat-card">
                <h3>Finished</h3>
                <p className="stat-value">{stats.finishedMatches}</p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'matches' && (
          <div className="admin-matches">
            <div className="admin-section-header">
              <h2>Match Management</h2>
              <button
                className="btn-primary"
                onClick={() => {
                  setShowMatchForm(true);
                  setEditingMatch(null);
                  setMatchForm({
                    homeTeam: '',
                    awayTeam: '',
                    matchDate: '',
                    venue: '',
                    group: '',
                  });
                }}
              >
                + Create Match
              </button>
            </div>

            {showMatchForm && (
              <div className="match-form-modal">
                <div className="modal-content">
                  <h3>{editingMatch ? 'Edit Match' : 'Create New Match'}</h3>
                  <form onSubmit={handleCreateMatch}>
                    <div className="form-group">
                      <label>Home Team</label>
                      <input
                        type="text"
                        value={matchForm.homeTeam}
                        onChange={(e) =>
                          setMatchForm({ ...matchForm, homeTeam: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Away Team</label>
                      <input
                        type="text"
                        value={matchForm.awayTeam}
                        onChange={(e) =>
                          setMatchForm({ ...matchForm, awayTeam: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Match Date & Time</label>
                      <input
                        type="datetime-local"
                        value={matchForm.matchDate}
                        onChange={(e) =>
                          setMatchForm({ ...matchForm, matchDate: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Venue</label>
                      <input
                        type="text"
                        value={matchForm.venue}
                        onChange={(e) =>
                          setMatchForm({ ...matchForm, venue: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Group/Stage</label>
                      <input
                        type="text"
                        value={matchForm.group}
                        onChange={(e) =>
                          setMatchForm({ ...matchForm, group: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="form-actions">
                      <button type="submit" className="btn-primary">
                        {editingMatch ? 'Update' : 'Create'}
                      </button>
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => {
                          setShowMatchForm(false);
                          setEditingMatch(null);
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}

            <div className="matches-table">
              <table>
                <thead>
                  <tr>
                    <th>Teams</th>
                    <th>Date</th>
                    <th>Venue</th>
                    <th>Group</th>
                    <th>Status</th>
                    <th>Result</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {matches.map((match) => (
                    <MatchRow
                      key={match.id}
                      match={match}
                      onUpdateResult={handleUpdateMatchResult}
                      onUpdateStatus={handleUpdateMatchStatus}
                      onDelete={handleDeleteMatch}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'users' && (
          <div className="admin-users">
            <div className="admin-section-header">
              <h2>User Management</h2>
            </div>
            <div className="users-table">
              <table>
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Screen Name</th>
                    <th>Role</th>
                    <th>Enabled</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <UserRow
                      key={user.id}
                      user={user}
                      onUpdateRole={handleUpdateUserRole}
                      onUpdateEnabled={handleUpdateUserEnabled}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const MatchRow = ({ match, onUpdateResult, onUpdateStatus, onDelete }) => {
  const [homeScore, setHomeScore] = useState(match.homeScore?.toString() || '');
  const [awayScore, setAwayScore] = useState(match.awayScore?.toString() || '');
  const [showResultInput, setShowResultInput] = useState(false);

  const handleSaveResult = () => {
    if (homeScore && awayScore) {
      onUpdateResult(match.id, homeScore, awayScore);
      setShowResultInput(false);
    }
  };

  return (
    <tr>
      <td>
        {match.homeTeam} vs {match.awayTeam}
      </td>
      <td>{(() => {
        const dateStr = match.matchDate;
        const utcDate = dateStr.endsWith('Z') 
          ? new Date(dateStr)
          : new Date(dateStr + 'Z');
        return utcDate.toLocaleString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          timeZone: 'UTC'
        });
      })()}</td>
      <td>{match.venue}</td>
      <td>{match.group}</td>
      <td>
        <select
          value={match.status}
          onChange={(e) => onUpdateStatus(match.id, e.target.value)}
        >
          <option value="SCHEDULED">Scheduled</option>
          <option value="LIVE">Live</option>
          <option value="FINISHED">Finished</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </td>
      <td>
        {showResultInput ? (
          <div className="result-input-group">
            <input
              type="number"
              value={homeScore}
              onChange={(e) => setHomeScore(e.target.value)}
              min="0"
              style={{ width: '50px' }}
            />
            <span> - </span>
            <input
              type="number"
              value={awayScore}
              onChange={(e) => setAwayScore(e.target.value)}
              min="0"
              style={{ width: '50px' }}
            />
            <button onClick={handleSaveResult} className="btn-small">Save</button>
            <button
              onClick={() => {
                setShowResultInput(false);
                setHomeScore(match.homeScore?.toString() || '');
                setAwayScore(match.awayScore?.toString() || '');
              }}
              className="btn-small btn-secondary"
            >
              Cancel
            </button>
          </div>
        ) : (
          <div>
            {match.homeScore !== null && match.awayScore !== null ? (
              <span>
                {match.homeScore} - {match.awayScore}
              </span>
            ) : (
              <span className="no-result">No result</span>
            )}
            <button
              onClick={() => setShowResultInput(true)}
              className="btn-small"
              style={{ marginLeft: '10px' }}
            >
              {match.homeScore !== null ? 'Edit' : 'Set'}
            </button>
          </div>
        )}
      </td>
      <td>
        <button
          onClick={() => onDelete(match.id)}
          className="btn-small btn-danger"
        >
          Delete
        </button>
      </td>
    </tr>
  );
};

const UserRow = ({ user, onUpdateRole, onUpdateEnabled }) => {
  return (
    <tr>
      <td>{user.email}</td>
      <td>{user.screenName || 'N/A'}</td>
      <td>
        <select
          value={user.role}
          onChange={(e) => onUpdateRole(user.id, e.target.value)}
        >
          <option value="USER">User</option>
          <option value="ADMIN">Admin</option>
        </select>
      </td>
      <td>
        <select
          value={user.enabled.toString()}
          onChange={(e) => onUpdateEnabled(user.id, e.target.value === 'true')}
        >
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </select>
      </td>
      <td>{new Date(user.createdAt).toLocaleDateString()}</td>
      <td>-</td>
    </tr>
  );
};

export default Admin;


