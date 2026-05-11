import { useEffect, useState } from 'react';
import apiClient from '../config/api';
import { getFlagUrl } from '../utils/countryFlags';
import './StandingsModal.css';

const StandingsModal = ({ isOpen, onClose, group }) => {
  const [standings, setStandings] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;

    const scrollY = window.scrollY;
    document.body.style.position = 'fixed';
    document.body.style.top = `-${scrollY}px`;
    document.body.style.width = '100%';
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.width = '';
      document.body.style.overflow = '';
      window.scrollTo(0, scrollY);
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    let cancelled = false;
    setLoading(true);
    setError('');
    apiClient
      .get('/standings')
      .then((res) => {
        if (cancelled) return;
        setStandings(res.data || []);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err.response?.data?.message || 'Failed to load standings');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const groupStanding = standings?.find((s) => s.group === group);
  const rows = groupStanding?.table || [];

  return (
    <div className="standings-modal-overlay" onClick={onClose}>
      <div className="standings-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="standings-modal-header">
          <h2>{group || 'Standings'}</h2>
          <button className="standings-modal-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        {loading && <div className="standings-loading">Loading standings…</div>}
        {error && <div className="standings-error">{error}</div>}

        {!loading && !error && rows.length === 0 && (
          <div className="standings-empty">No standings available yet for this group.</div>
        )}

        {!loading && !error && rows.length > 0 && (
          <div className="standings-table-wrapper">
            <table className="standings-table">
              <thead>
                <tr>
                  <th className="col-pos">#</th>
                  <th className="col-team">Team</th>
                  <th className="col-num">P</th>
                  <th className="col-num">W</th>
                  <th className="col-num">D</th>
                  <th className="col-num">L</th>
                  <th className="col-num">GF</th>
                  <th className="col-num">GA</th>
                  <th className="col-num">GD</th>
                  <th className="col-num col-points">Pts</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const teamName = row.team?.name || '';
                  const flag = getFlagUrl(teamName);
                  return (
                    <tr key={row.team?.id || row.position}>
                      <td className="col-pos">{row.position}</td>
                      <td className="col-team">
                        {flag && <img src={flag} alt="" className="standings-flag" />}
                        <span>{teamName}</span>
                      </td>
                      <td className="col-num">{row.playedGames}</td>
                      <td className="col-num">{row.won}</td>
                      <td className="col-num">{row.draw}</td>
                      <td className="col-num">{row.lost}</td>
                      <td className="col-num">{row.goalsFor}</td>
                      <td className="col-num">{row.goalsAgainst}</td>
                      <td className="col-num">
                        {row.goalDifference > 0 ? `+${row.goalDifference}` : row.goalDifference}
                      </td>
                      <td className="col-num col-points">{row.points}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default StandingsModal;
