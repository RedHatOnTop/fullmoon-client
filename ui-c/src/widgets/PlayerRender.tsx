/* PlayerRender — stylized side-profile figure with live cape
   sway, wing flap and trail particles. Pure SVG + CSS, driven
   by the equipped cosmetics' hues. */

import type { Cosmetic, Loadout } from "../core/bindings";

export function PlayerRender({
  loadout,
  catalog,
  skinHue,
}: {
  loadout: Loadout | null;
  catalog: Cosmetic[];
  skinHue: number;
}) {
  const find = (id: string | null) => catalog.find((c) => c.id === id) ?? null;
  const cape = find(loadout?.cape ?? null);
  const wings = find(loadout?.wings ?? null);
  const trail = find(loadout?.trail ?? null);

  const skin = `hsl(26 42% 60%)`;
  const skinDark = `hsl(26 40% 48%)`;
  const shirt = `hsl(${skinHue} 40% 42%)`;
  const pants = `hsl(222 30% 34%)`;

  return (
    <svg viewBox="0 0 200 150" className="player-render" aria-hidden>
      {/* ground */}
      <ellipse cx="118" cy="136" rx="52" ry="7" fill="rgba(0,0,0,.45)" />
      <ellipse cx="118" cy="136" rx="52" ry="7" fill="none" stroke="var(--border-strong)" strokeWidth="1" strokeDasharray="3 4" />

      {/* trail particles (behind the figure) */}
      {trail && (
        <g fill={`hsl(${trail.hue} 85% 68%)`}>
          {[0, 1, 2, 3, 4].map((i) => (
            <circle
              key={i}
              cx={86 - i * 6}
              cy={118 + (i % 2) * 5}
              r={2.6 - i * 0.35}
              className="trail-p"
              style={{ animationDelay: `${i * 0.42}s` }}
            />
          ))}
        </g>
      )}

      {/* cape — anchored at the shoulder, sways */}
      {cape && (
        <g className="cape" style={{ transformOrigin: "108px 52px" }}>
          <path
            d="M108 50 C 88 52 72 66 64 100 L 78 112 C 86 84 96 68 112 62 Z"
            fill={`hsl(${cape.hue} 62% 46%)`}
          />
          <path
            d="M108 50 C 92 54 80 66 74 92"
            fill="none"
            stroke={`hsl(${cape.hue} 70% 66%)`}
            strokeWidth="2"
            strokeLinecap="round"
            opacity="0.8"
          />
          <path
            d="M103 58 C 93 64 85 76 80 96"
            fill="none"
            stroke={`hsl(${cape.hue} 70% 66%)`}
            strokeWidth="1.4"
            strokeLinecap="round"
            opacity="0.5"
          />
        </g>
      )}

      {/* wings — layered feathers, flap */}
      {wings && (
        <g className="wings" style={{ transformOrigin: "106px 56px" }}>
          <path
            d="M106 56 C 88 44 70 42 54 48 C 68 50 82 56 92 66 Z"
            fill={`hsl(${wings.hue} 60% 52%)`}
          />
          <path
            d="M106 58 C 90 52 74 52 60 58 C 74 60 86 64 96 70 Z"
            fill={`hsl(${wings.hue} 64% 42%)`}
          />
          <path
            d="M105 60 C 92 58 80 60 70 66 C 82 66 92 69 99 72 Z"
            fill={`hsl(${wings.hue} 66% 34%)`}
          />
        </g>
      )}

      {/* legs */}
      <rect x="112" y="102" width="11" height="32" rx="3.5" fill={pants} />
      <rect x="126" y="102" width="11" height="32" rx="3.5" fill={pants} opacity="0.85" />
      <rect x="112" y="128" width="12" height="7" rx="2.5" fill={skinDark} />
      <rect x="126" y="128" width="12" height="7" rx="2.5" fill={skinDark} opacity="0.85" />

      {/* body */}
      <rect x="108" y="52" width="28" height="52" rx="6" fill={shirt} />
      <rect x="108" y="52" width="28" height="10" rx="5" fill={shirt} opacity="0.75" />

      {/* near arm */}
      <rect x="134" y="56" width="10" height="34" rx="4" fill={shirt} opacity="0.9" />
      <rect x="134" y="86" width="10" height="9" rx="4" fill={skin} />

      {/* head */}
      <rect x="106" y="14" width="34" height="34" rx="7" fill={skin} />
      <rect x="106" y="14" width="34" height="12" rx="6" fill={`hsl(${skinHue} 45% 34%)`} />
      <rect x="106" y="22" width="6" height="10" rx="2" fill={`hsl(${skinHue} 45% 34%)`} />
      {/* eye */}
      <rect x="130" y="28" width="5" height="4" rx="1" fill="#fff" />
      <rect x="133" y="28" width="3" height="4" rx="1" fill={`hsl(${skinHue} 60% 40%)`} />
      {/* nose + mouth */}
      <rect x="138" y="32" width="3" height="3" rx="1" fill={skinDark} />
      <rect x="132" y="40" width="6" height="2" rx="1" fill={skinDark} />
    </svg>
  );
}
