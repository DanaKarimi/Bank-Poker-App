import React, { useState, useMemo } from 'react';
import { Trophy, TrendingUp, Award, Calendar, Sparkles } from 'lucide-react';
import { PokerAvatar } from './AvatarSystem';

const PLAYER_COLORS = [
  '#f5c542', // Gold
  '#34d399', // Emerald
  '#60a5fa', // Sky Blue
  '#f472b6', // Rose Pink
  '#fb923c', // Tangerine
  '#a78bfa', // Violet
  '#2dd4bf', // Teal
  '#f87171', // Coral Red
  '#a3e635', // Lime
  '#38bdf8', // Cyan
  '#e879f9', // Fuchsia
  '#fbbf24', // Amber
];

const formatSignedMoney = (value) => {
  const num = Number(value) || 0;
  if (num > 0) return `+$${num.toLocaleString('en-US')}`;
  if (num < 0) return `-$${Math.abs(num).toLocaleString('en-US')}`;
  return '$0';
};

const buildSmoothPath = (points) => {
  if (!points || points.length === 0) return '';
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`;
  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1];
    const curr = points[i];
    const midX = (prev.x + curr.x) / 2;
    d += ` C ${midX} ${prev.y}, ${midX} ${curr.y}, ${curr.x} ${curr.y}`;
  }
  return d;
};

const LeaderboardTab = ({ data, loading = false }) => {
  const [filterMode, setFilterMode] = useState('top10'); // 'top5' | 'top10' | 'all'
  const [selectedPlayerKey, setSelectedPlayerKey] = useState(null);
  const [selectedPoint, setSelectedPoint] = useState(null); // { playerKey, snapshotIndex }

  const snapshots = data?.snapshots || [];
  const fullLeaderboard = data?.leaderboard || [];

  const colorMap = useMemo(() => {
    const map = new Map();
    fullLeaderboard.forEach((p, idx) => {
      const key = p.userId || p.playerId || p.name;
      map.set(key, PLAYER_COLORS[idx % PLAYER_COLORS.length]);
    });
    return map;
  }, [fullLeaderboard]);

  const visiblePlayers = useMemo(() => {
    if (filterMode === 'top5') return fullLeaderboard.slice(0, 5);
    if (filterMode === 'top10') return fullLeaderboard.slice(0, 10);
    return fullLeaderboard;
  }, [fullLeaderboard, filterMode]);

  const maxRankInChart = useMemo(() => {
    let maxR = Math.max(visiblePlayers.length, 1);
    visiblePlayers.forEach((p) => {
      (p.history || []).forEach((h) => {
        if (h.rank > maxR) maxR = h.rank;
      });
    });
    return maxR;
  }, [visiblePlayers]);

  // SVG Layout geometry
  const leftPad = 42;
  const rightPad = 128;
  const topPad = 24;
  const bottomPad = 48;
  const colStep = snapshots.length <= 1 ? 120 : Math.max(78, Math.min(120, Math.floor(620 / Math.max(snapshots.length - 1, 1))));
  const rowStep = maxRankInChart <= 6 ? 42 : maxRankInChart <= 10 ? 34 : 28;
  const svgWidth = Math.max(460, leftPad + Math.max(snapshots.length - 1, 1) * colStep + rightPad);
  const svgHeight = Math.max(220, topPad + Math.max(maxRankInChart - 1, 1) * rowStep + bottomPad);

  const getX = (snapshotIdx) => {
    if (snapshots.length <= 1) return leftPad + 60;
    return leftPad + snapshotIdx * colStep;
  };
  const getY = (rank) => topPad + (Math.max(rank, 1) - 1) * rowStep;

  const activePlayerObj = useMemo(() => {
    if (!selectedPlayerKey) return null;
    return fullLeaderboard.find((p) => (p.userId || p.playerId || p.name) === selectedPlayerKey) || null;
  }, [fullLeaderboard, selectedPlayerKey]);

  const activePointDetails = useMemo(() => {
    if (!activePlayerObj) return null;
    const hist = activePlayerObj.history || [];
    if (hist.length === 0) return null;
    if (selectedPoint && selectedPoint.playerKey === selectedPlayerKey) {
      const exact = hist.find((h) => h.snapshotIndex === selectedPoint.snapshotIndex);
      if (exact) return { player: activePlayerObj, point: exact };
    }
    return { player: activePlayerObj, point: hist[hist.length - 1] };
  }, [activePlayerObj, selectedPoint, selectedPlayerKey]);

  const handleSelectPlayer = (key, snapshotIndex = null) => {
    if (snapshotIndex !== null) {
      setSelectedPlayerKey(key);
      setSelectedPoint({ playerKey: key, snapshotIndex });
      return;
    }
    if (selectedPlayerKey === key && !selectedPoint) {
      setSelectedPlayerKey(null);
      setSelectedPoint(null);
    } else {
      setSelectedPlayerKey(key);
      setSelectedPoint(null);
    }
  };

  if (loading && fullLeaderboard.length === 0) {
    return (
      <div className="p-8 bg-felt-card/60 rounded-2xl border border-gold-accent/20 text-center text-xs text-cream-text/60">
        Loading leaderboard & rank progression...
      </div>
    );
  }

  if (snapshots.length === 0 || fullLeaderboard.length === 0) {
    return (
      <div className="p-10 bg-felt-card/60 rounded-2xl border border-gold-accent/25 text-center space-y-2">
        <Trophy className="w-10 h-10 text-gold-accent/40 mx-auto" />
        <h3 className="text-sm font-bold text-cream-text">No Closed Tables Yet</h3>
        <p className="text-xs text-cream-text/60 max-w-sm mx-auto">
          Once at least one table in this group is closed, all-time standings and historical rank progression will appear here.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 w-full min-w-0 max-w-full animate-in fade-in duration-200">
      {/* SECTION 1: RANK PROGRESSION BUMP CHART */}
      <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-4 sm:p-5 shadow-xl w-full min-w-0 max-w-full">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div className="min-w-0">
            <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent flex items-center gap-2">
              <TrendingUp className="w-4 h-4 shrink-0" />
              <span>Rank Progression</span>
            </h2>
            <p className="text-[11px] text-cream-text/60 mt-0.5">
              Rank trajectory across {snapshots.length} closed {snapshots.length === 1 ? 'table' : 'tables'} (tap a player or node to inspect)
            </p>
          </div>

          <div className="flex items-center gap-1 p-1 bg-felt-dark/90 border border-gold-accent/30 rounded-xl shrink-0">
            {[
              { id: 'top5', label: 'Top 5' },
              { id: 'top10', label: 'Top 10' },
              { id: 'all', label: `All (${fullLeaderboard.length})` },
            ].map((opt) => (
              <button
                key={opt.id}
                type="button"
                onClick={() => setFilterMode(opt.id)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition cursor-pointer ${
                  filterMode === opt.id
                    ? 'bg-gold-accent text-black shadow'
                    : 'text-cream-text/70 hover:text-cream-text'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Interactive Player Filter Pills */}
        <div className="flex flex-wrap items-center gap-1.5 mb-3">
          {visiblePlayers.map((p) => {
            const pKey = p.userId || p.playerId || p.name;
            const color = colorMap.get(pKey) || '#f5c542';
            const isSelected = selectedPlayerKey === pKey;
            const isDimmed = selectedPlayerKey && !isSelected;

            return (
              <button
                key={pKey}
                type="button"
                onClick={() => handleSelectPlayer(pKey)}
                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold border transition cursor-pointer ${
                  isSelected
                    ? 'bg-felt-dark border-gold-accent text-cream-text shadow-md scale-[1.02]'
                    : 'bg-felt-dark/60 border-gold-accent/25 text-cream-text/80 hover:border-gold-accent/60'
                } ${isDimmed ? 'opacity-45' : 'opacity-100'}`}
              >
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: color }}
                />
                <span className="truncate max-w-[110px]">#{p.rank} {p.name}</span>
              </button>
            );
          })}
          {selectedPlayerKey && (
            <button
              type="button"
              onClick={() => {
                setSelectedPlayerKey(null);
                setSelectedPoint(null);
              }}
              className="px-2 py-1 rounded-full text-[11px] font-semibold text-gold-accent hover:underline cursor-pointer"
            >
              Clear focus
            </button>
          )}
        </div>

        {/* Strictly Contained Horizontal Scroll Wrapper for SVG */}
        <div className="w-full min-w-0 max-w-full overflow-x-auto pb-2 rounded-xl bg-felt-dark/55 border border-gold-accent/20">
          <svg
            width={svgWidth}
            height={svgHeight}
            viewBox={`0 0 ${svgWidth} ${svgHeight}`}
            className="block select-none"
            role="img"
            aria-label="Group Leaderboard Rank Progression Bump Chart"
          >
            {/* Horizontal Rank Gridlines & Y-Axis Labels */}
            {Array.from({ length: maxRankInChart }, (_, i) => i + 1).map((rankNum) => {
              const y = getY(rankNum);
              return (
                <g key={`grid-rank-${rankNum}`}>
                  <line
                    x1={leftPad}
                    y1={y}
                    x2={svgWidth - rightPad + 8}
                    y2={y}
                    stroke="rgba(212, 175, 55, 0.14)"
                    strokeDasharray="3 4"
                    strokeWidth="1"
                  />
                  <text
                    x={leftPad - 10}
                    y={y + 4}
                    textAnchor="end"
                    fill={rankNum === 1 ? '#f5c542' : rankNum === 2 ? '#c0c0c0' : rankNum === 3 ? '#cd7f32' : 'rgba(245, 245, 220, 0.55)'}
                    fontSize="11"
                    fontWeight="700"
                    fontFamily="ui-monospace, SFMono-Regular, Menlo, monospace"
                  >
                    #{rankNum}
                  </text>
                </g>
              );
            })}

            {/* Vertical Snapshot Ticks & X-Axis Labels */}
            {snapshots.map((snap) => {
              const x = getX(snap.index);
              return (
                <g key={`snap-axis-${snap.tableId}`}>
                  <line
                    x1={x}
                    y1={topPad - 6}
                    x2={x}
                    y2={svgHeight - bottomPad + 6}
                    stroke="rgba(212, 175, 55, 0.08)"
                    strokeWidth="1"
                  />
                  <text
                    x={x}
                    y={svgHeight - bottomPad + 20}
                    textAnchor="middle"
                    fill="rgba(245, 245, 220, 0.78)"
                    fontSize="10"
                    fontWeight="700"
                  >
                    {snap.dateLabel || `T${snap.index + 1}`}
                  </text>
                  <text
                    x={x}
                    y={svgHeight - bottomPad + 33}
                    textAnchor="middle"
                    fill="rgba(212, 175, 55, 0.55)"
                    fontSize="9"
                  >
                    #{snap.index + 1}
                  </text>
                </g>
              );
            })}

            {/* Player Trajectory Curves & Nodes */}
            {visiblePlayers.map((p) => {
              const pKey = p.userId || p.playerId || p.name;
              const color = colorMap.get(pKey) || '#f5c542';
              const isSelected = selectedPlayerKey === pKey;
              const isDimmed = selectedPlayerKey && !isSelected;
              const pts = (p.history || []).map((h) => ({
                ...h,
                x: getX(h.snapshotIndex),
                y: getY(h.rank),
              }));
              if (pts.length === 0) return null;

              const pathD = buildSmoothPath(pts);
              const lastPt = pts[pts.length - 1];
              const shortName = p.name.length > 11 ? `${p.name.slice(0, 10)}…` : p.name;

              return (
                <g
                  key={`curve-${pKey}`}
                  style={{ opacity: isDimmed ? 0.18 : 1, transition: 'opacity 180ms ease' }}
                >
                  <path
                    d={pathD}
                    fill="none"
                    stroke={color}
                    strokeWidth={isSelected ? 4 : 2.6}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="cursor-pointer"
                    onClick={() => handleSelectPlayer(pKey)}
                  />

                  {pts.map((pt) => {
                    const isNodeSelected =
                      selectedPoint?.playerKey === pKey &&
                      selectedPoint?.snapshotIndex === pt.snapshotIndex;

                    return (
                      <circle
                        key={`node-${pKey}-${pt.snapshotIndex}`}
                        cx={pt.x}
                        cy={pt.y}
                        r={isNodeSelected ? 6.5 : isSelected ? 5 : 4}
                        fill={pt.sessionNet !== null ? color : '#062815'}
                        stroke={isNodeSelected ? '#ffffff' : color}
                        strokeWidth={isNodeSelected ? 2.5 : 2}
                        className="cursor-pointer"
                        onClick={() => handleSelectPlayer(pKey, pt.snapshotIndex)}
                        onMouseEnter={() => {
                          if (!selectedPlayerKey || selectedPlayerKey === pKey) {
                            setSelectedPlayerKey(pKey);
                            setSelectedPoint({ playerKey: pKey, snapshotIndex: pt.snapshotIndex });
                          }
                        }}
                      />
                    );
                  })}

                  {/* End-of-line label pill */}
                  <g
                    className="cursor-pointer"
                    onClick={() => handleSelectPlayer(pKey, lastPt.snapshotIndex)}
                  >
                    <rect
                      x={lastPt.x + 10}
                      y={lastPt.y - 10}
                      width={108}
                      height={20}
                      rx={10}
                      fill="rgba(6, 40, 21, 0.92)"
                      stroke={color}
                      strokeWidth={isSelected ? 1.8 : 1}
                    />
                    <text
                      x={lastPt.x + 18}
                      y={lastPt.y + 3.5}
                      fill="#f5f5dc"
                      fontSize="10"
                      fontWeight="700"
                    >
                      #{p.rank} {shortName}
                    </text>
                  </g>
                </g>
              );
            })}
          </svg>
        </div>

        {/* Interactive Selected Snapshot Summary Card */}
        {activePointDetails && (
          <div className="mt-3 p-3.5 rounded-xl bg-felt-dark/90 border border-gold-accent/45 flex flex-wrap items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2.5 min-w-0">
              <PokerAvatar
                avatarId={activePointDetails.player.avatarId}
                name={activePointDetails.player.name}
                size={34}
              />
              <div className="min-w-0">
                <div className="font-bold text-cream-text flex items-center gap-2">
                  <span className="truncate">{activePointDetails.player.name}</span>
                  <span className="px-2 py-0.5 rounded-full bg-gold-accent/20 border border-gold-accent/50 text-gold-accent text-[10px] font-mono">
                    Rank #{activePointDetails.point.rank}
                  </span>
                </div>
                <div className="text-[11px] text-cream-text/60 flex items-center gap-1.5 mt-0.5 truncate">
                  <Calendar className="w-3 h-3 text-gold-accent/70 shrink-0" />
                  <span>{activePointDetails.point.dateLabel}</span>
                  <span>•</span>
                  <span className="truncate">{activePointDetails.point.tableName}</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-4 shrink-0">
              <div className="text-right">
                <div className="text-[10px] uppercase text-cream-text/50 font-semibold">Session Net</div>
                <div
                  className={`font-mono font-bold text-xs ${
                    activePointDetails.point.sessionNet === null
                      ? 'text-cream-text/50'
                      : activePointDetails.point.sessionNet > 0
                      ? 'text-emerald-400'
                      : activePointDetails.point.sessionNet < 0
                      ? 'text-red-400'
                      : 'text-cream-text'
                  }`}
                >
                  {activePointDetails.point.sessionNet === null
                    ? 'Did not play'
                    : formatSignedMoney(activePointDetails.point.sessionNet)}
                </div>
              </div>
              <div className="text-right pl-3 border-l border-gold-accent/20">
                <div className="text-[10px] uppercase text-cream-text/50 font-semibold">Cumulative Net</div>
                <div
                  className={`font-mono font-extrabold text-sm ${
                    activePointDetails.point.cumulativeNet > 0
                      ? 'text-emerald-400'
                      : activePointDetails.point.cumulativeNet < 0
                      ? 'text-red-400'
                      : 'text-cream-text'
                  }`}
                >
                  {formatSignedMoney(activePointDetails.point.cumulativeNet)}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* SECTION 2: ALL-TIME GROUP STANDINGS */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-black uppercase tracking-wider text-gold-accent flex items-center gap-2">
            <Award className="w-4 h-4" />
            <span>All-Time Standings ({fullLeaderboard.length})</span>
          </h2>
          <span className="text-[11px] text-cream-text/50">Tap a player to highlight curve</span>
        </div>

        <div className="space-y-2.5">
          {fullLeaderboard.map((p) => {
            const pKey = p.userId || p.playerId || p.name;
            const color = colorMap.get(pKey) || '#f5c542';
            const isSelected = selectedPlayerKey === pKey;

            const rankStyle =
              p.rank === 1
                ? 'bg-gold-accent text-black border-gold-light'
                : p.rank === 2
                ? 'bg-[#c0c0c0] text-black border-white/60'
                : p.rank === 3
                ? 'bg-[#cd7f32] text-black border-amber-300/60'
                : 'bg-felt-dark text-gold-accent border-gold-accent/40';

            return (
              <button
                key={pKey}
                type="button"
                onClick={() => handleSelectPlayer(pKey)}
                className={`w-full text-left p-3.5 sm:p-4 rounded-2xl bg-felt-card border transition shadow-md flex flex-wrap sm:flex-nowrap items-center justify-between gap-3 cursor-pointer ${
                  isSelected
                    ? 'border-gold-accent ring-1 ring-gold-accent/60 bg-felt-card'
                    : 'border-gold-accent/30 hover:border-gold-accent/70'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  {/* Rank Badge + Delta */}
                  <div className="flex flex-col items-center shrink-0 w-10">
                    <span className={`px-2 py-0.5 rounded-lg text-xs font-black border ${rankStyle}`}>
                      #{p.rank}
                    </span>
                    <span
                      className={`text-[10px] font-bold mt-0.5 ${
                        p.rankDelta > 0
                          ? 'text-emerald-400'
                          : p.rankDelta < 0
                          ? 'text-rose-400'
                          : 'text-cream-text/40'
                      }`}
                    >
                      {p.rankDelta > 0 ? `▲${p.rankDelta}` : p.rankDelta < 0 ? `▼${Math.abs(p.rankDelta)}` : '—'}
                    </span>
                  </div>

                  {/* Color Dot + Avatar */}
                  <div className="relative shrink-0">
                    <PokerAvatar avatarId={p.avatarId} name={p.name} size={38} />
                    <span
                      className="w-3 h-3 rounded-full border-2 border-felt-dark absolute -bottom-0.5 -right-0.5"
                      style={{ backgroundColor: color }}
                    />
                  </div>

                  {/* Player Info */}
                  <div className="min-w-0 flex-1">
                    <div className="font-bold text-sm text-cream-text truncate">{p.name}</div>
                    <div className="text-[11px] text-cream-text/60 flex flex-wrap items-center gap-x-2 gap-y-0.5 mt-0.5">
                      <span>{p.tablesPlayed} {p.tablesPlayed === 1 ? 'table' : 'tables'}</span>
                      <span>•</span>
                      <span>Win Rate: <strong className="text-cream-text/85">{p.winRate}%</strong></span>
                      <span>•</span>
                      <span className="inline-flex items-center gap-0.5">
                        <Sparkles className="w-3 h-3 text-gold-accent/70" />
                        Best: <strong className="font-mono text-gold-light">{formatSignedMoney(p.bestSessionNet)}</strong>
                      </span>
                    </div>
                  </div>
                </div>

                {/* Cumulative Net */}
                <div className="text-right shrink-0 ml-auto">
                  <div className="text-[10px] uppercase text-cream-text/50 font-semibold">Cumulative Net</div>
                  <div
                    className={`text-base sm:text-lg font-black font-mono ${
                      p.cumulativeNet > 0
                        ? 'text-emerald-400'
                        : p.cumulativeNet < 0
                        ? 'text-red-400'
                        : 'text-cream-text'
                    }`}
                  >
                    {formatSignedMoney(p.cumulativeNet)}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default LeaderboardTab;
