import { useState } from 'react';
import './PredictionSplitBar.css';

/**
 * Shows how a league's members predicted one locked match: a stacked bar of the
 * Home / Draw / Away split, expandable to reveal who picked what.
 *
 * Props:
 *   split           - LeaguePredictionSplitDTO from the backend
 *   defaultExpanded - start with the name lists shown (default false)
 */
const PredictionSplitBar = ({ split, defaultExpanded = false }) => {
  const [expanded, setExpanded] = useState(defaultExpanded);

  if (!split) return null;

  const home = split.homeWin || [];
  const draw = split.draw || [];
  const away = split.awayWin || [];
  const total = home.length + draw.length + away.length;

  if (total === 0) {
    return (
      <div className="psb">
        <div className="psb-empty">No league picks for this match.</div>
      </div>
    );
  }

  const pct = (n) => Math.round((n / total) * 100);
  const homePct = pct(home.length);
  const drawPct = pct(draw.length);
  // Give the last segment the remainder so the three always sum to 100.
  const awayPct = 100 - homePct - drawPct;

  const segments = [
    { key: 'home', label: split.homeTeam, cls: 'psb-home', count: home.length, pct: homePct, voters: home },
    { key: 'draw', label: 'Draw', cls: 'psb-draw', count: draw.length, pct: drawPct, voters: draw },
    { key: 'away', label: split.awayTeam, cls: 'psb-away', count: away.length, pct: awayPct, voters: away },
  ];

  const renderNames = (voters) => {
    if (voters.length === 0) return <span className="psb-none">—</span>;
    return voters.map((v, i) => (
      <span key={v.userId} className={`psb-name ${v.isMe ? 'psb-me' : ''}`}>
        {v.screenName}{v.isMe ? ' (you)' : ''}{i < voters.length - 1 ? ', ' : ''}
      </span>
    ));
  };

  return (
    <div className="psb">
      <button
        type="button"
        className="psb-bar-row"
        onClick={() => setExpanded((e) => !e)}
        aria-expanded={expanded}
      >
        <div className="psb-bar" role="img" aria-label="League prediction split">
          {segments.map((s) => (
            s.pct > 0 && (
              <div key={s.key} className={`psb-seg ${s.cls}`} style={{ width: `${s.pct}%` }} title={`${s.label}: ${s.count}`} />
            )
          ))}
        </div>
        <span className="psb-toggle">{expanded ? 'hide ▴' : 'who ▾'}</span>
      </button>

      <div className="psb-legend">
        {segments.map((s) => (
          <span key={s.key} className="psb-legend-item">
            <span className={`psb-dot ${s.cls}`} />
            {s.label} {s.pct}% ({s.count})
          </span>
        ))}
      </div>

      {expanded && (
        <div className="psb-details">
          {segments.map((s) => (
            <div key={s.key} className="psb-detail-row">
              <span className={`psb-detail-label ${s.cls}-text`}>{s.label}</span>
              <span className="psb-detail-names">{renderNames(s.voters)}</span>
            </div>
          ))}
          {split.noPickCount > 0 && (
            <div className="psb-nopick">{split.noPickCount} didn&apos;t pick</div>
          )}
        </div>
      )}
    </div>
  );
};

export default PredictionSplitBar;
