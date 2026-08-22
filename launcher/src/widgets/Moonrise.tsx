/* Moonrise — the hero art. A crescent over the night sky, drawn as the
   website's #sky would draw it: one warm body, a soft halo, stars that are
   dots rather than decorations. Replaces the stock voxel island — the lobby
   is a place under a moon, not a grass block. */
import type { CSSProperties } from "react";

const STARS: Array<{ x: number; y: number; r: number; o: number }> = [
  { x: 34, y: 40, r: 1.4, o: 0.9 },
  { x: 78, y: 22, r: 1.0, o: 0.6 },
  { x: 120, y: 52, r: 1.6, o: 0.8 },
  { x: 168, y: 30, r: 1.0, o: 0.5 },
  { x: 210, y: 64, r: 1.3, o: 0.7 },
  { x: 248, y: 38, r: 1.0, o: 0.45 },
  { x: 58, y: 96, r: 1.0, o: 0.5 },
  { x: 150, y: 108, r: 1.2, o: 0.55 },
  { x: 236, y: 112, r: 1.5, o: 0.75 },
  { x: 100, y: 140, r: 1.0, o: 0.4 },
  { x: 196, y: 148, r: 1.1, o: 0.5 },
];

export default function Moonrise({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      className={className}
      style={style}
      width="284"
      height="200"
      viewBox="0 0 284 200"
      fill="none"
      aria-hidden
    >
      <defs>
        <radialGradient id="moonrise-halo" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FFE9B0" stopOpacity="0.32" />
          <stop offset="55%" stopColor="#F5D06E" stopOpacity="0.10" />
          <stop offset="100%" stopColor="#F5D06E" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="moonrise-body" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#FFE9B0" />
          <stop offset="100%" stopColor="#F5D06E" />
        </linearGradient>
        <mask id="moonrise-cut">
          <circle cx="150" cy="100" r="52" fill="#fff" />
          <circle cx="172" cy="86" r="46" fill="#000" />
        </mask>
      </defs>

      {/* halo — wide enough to wash the panel corner, faint enough to stay weather */}
      <circle cx="150" cy="100" r="96" fill="url(#moonrise-halo)" />

      {/* the crescent itself */}
      <circle cx="150" cy="100" r="52" fill="url(#moonrise-body)" mask="url(#moonrise-cut)" />

      {/* one hairline orbit — the only line, so the mark stays quiet */}
      <circle
        cx="150"
        cy="100"
        r="74"
        stroke="#F5D06E"
        strokeOpacity="0.18"
        strokeWidth="1"
        strokeDasharray="2 6"
      />

      {/* stars: dots, never sparkles */}
      {STARS.map((s, i) => (
        <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#F4F6FB" fillOpacity={s.o * 0.55} />
      ))}
    </svg>
  );
}
