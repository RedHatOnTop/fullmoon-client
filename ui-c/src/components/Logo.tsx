/* The Pinion mark — a flight feather drawn as a single diagonal
   blade with a quill and barbs, set on a gradient squircle.
   "Flight-feather. Gear-tight." */

export function FeatherGlyph({ size = 20 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M20.4 3.6c-6.9.5-12 5.4-13.7 12l-2.9 4.4c-.4.6-.3 1.5.3 1.9.6.4 1.5.3 1.9-.3l2.8-4.3c6.6-1.7 11.5-6.8 12-13.7l-3.6 1.8c.5-1.6.6-3.3.2-5-1 .4-2 .9-3 1.5Z"
        fill="currentColor"
        opacity="0.95"
      />
      <path
        d="M7.8 18.4 17 9.2M9.9 16.6l3.2-3.2M6.6 13.4l2.6-2.6"
        stroke="var(--on-accent)"
        strokeWidth="1.5"
        strokeLinecap="round"
        opacity="0.85"
      />
    </svg>
  );
}

export function Logo({ size = 30, withWord = true }: { size?: number; withWord?: boolean }) {
  return (
    <span className="logo" style={{ display: "inline-flex", alignItems: "center", gap: 10 }}>
      <span
        className="logo-tile"
        style={{
          width: size,
          height: size,
          borderRadius: size * 0.3,
          display: "grid",
          placeItems: "center",
          color: "var(--on-accent)",
          background: "linear-gradient(150deg, var(--accent) 0%, var(--accent-dim) 100%)",
          boxShadow: "0 4px 18px -4px var(--accent-glow), inset 0 1px 0 rgba(255,255,255,.28)",
        }}
      >
        <FeatherGlyph size={size * 0.62} />
      </span>
      {withWord && (
        <span
          style={{
            fontFamily: "var(--font-display)",
            fontWeight: 700,
            fontSize: 15,
            letterSpacing: "0.22em",
          }}
        >
          PINION
        </span>
      )}
    </span>
  );
}
