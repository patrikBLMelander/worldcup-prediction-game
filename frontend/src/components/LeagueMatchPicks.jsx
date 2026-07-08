import { useState } from 'react';
import apiClient from '../config/api';
import PredictionSplitBar from './PredictionSplitBar';
import './LeagueMatchPicks.css';

/**
 * Collapsible "League picks" reveal for a single match on the Matches page.
 * Lazily loads the split from the backend the first time it's opened, so the
 * match list stays light until the user actually asks.
 *
 * Only render this for LOCKED matches (kickoff passed) and when a league is
 * selected - the backend also enforces both.
 */
const LeagueMatchPicks = ({ leagueId, matchId }) => {
  const [open, setOpen] = useState(false);
  const [split, setSplit] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!leagueId) return null;

  const toggle = async () => {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    if (split || loading) return; // already loaded / loading
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get(`/leagues/${leagueId}/match-predictions/${matchId}`);
      setSplit(res.data);
    } catch (e) {
      setError('Could not load league picks');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="lmp">
      <button type="button" className="lmp-toggle" onClick={toggle} aria-expanded={open}>
        👥 League picks {open ? '▴' : '▾'}
      </button>
      {open && (
        <div className="lmp-body">
          {loading && <div className="lmp-status">Loading…</div>}
          {error && <div className="lmp-status lmp-error">{error}</div>}
          {split && <PredictionSplitBar split={split} defaultExpanded />}
        </div>
      )}
    </div>
  );
};

export default LeagueMatchPicks;
