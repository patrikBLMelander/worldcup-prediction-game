import { useMemo } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import './LeagueScoreChart.css';

// Validated categorical palette (dataviz skill, dark steps) - fixed order, never cycled.
// A 9th+ member reuses the hue with a dashed line (hue + dash = composite encoding).
const SERIES_COLORS = [
  '#3987e5', // blue
  '#199e70', // aqua
  '#c98500', // yellow
  '#008300', // green
  '#9085e9', // violet
  '#e66767', // red
  '#d55181', // magenta
  '#d95926', // orange
];

/**
 * Cumulative points over time for every member of a league - one line per member.
 *
 * Props:
 *   timeline      - LeagueScoreTimelineDTO { members, points }
 *   currentUserId - highlight this member's line (thicker)
 */
const LeagueScoreChart = ({ timeline, currentUserId }) => {
  const members = timeline?.members || [];
  const points = timeline?.points || [];

  // recharts wants an array of rows keyed by series id. Prepend a zero "Start"
  // row so every line begins at 0.
  const data = useMemo(() => {
    const zero = { label: 'Start' };
    members.forEach((m) => { zero[String(m.userId)] = 0; });
    const rows = points.map((p) => ({ label: p.label, ...p.cumulative }));
    return [zero, ...rows];
  }, [members, points]);

  if (members.length === 0 || points.length === 0) {
    return (
      <div className="lsc-empty">
        No scored matches yet — the chart fills in as matches finish.
      </div>
    );
  }

  const colorFor = (idx) => SERIES_COLORS[idx % SERIES_COLORS.length];

  return (
    <div className="lsc">
      <ResponsiveContainer width="100%" height={360}>
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: -8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
          <XAxis dataKey="label" stroke="var(--text-secondary)" fontSize={11} interval="preserveStartEnd" />
          <YAxis stroke="var(--text-secondary)" fontSize={11} allowDecimals={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'var(--surface-elevated, #0f172a)',
              border: '1px solid var(--border)',
              borderRadius: '10px',
              color: 'var(--text-primary)',
            }}
            itemSorter={(item) => -item.value}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          {members.map((m, idx) => {
            const isMe = m.userId === currentUserId;
            return (
              <Line
                key={m.userId}
                type="monotone"
                dataKey={String(m.userId)}
                name={isMe ? `${m.name} (you)` : m.name}
                stroke={colorFor(idx)}
                strokeWidth={isMe ? 3.5 : 1.8}
                strokeDasharray={idx >= SERIES_COLORS.length ? '5 4' : undefined}
                dot={false}
                activeDot={{ r: isMe ? 6 : 4 }}
                connectNulls
              />
            );
          })}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default LeagueScoreChart;
