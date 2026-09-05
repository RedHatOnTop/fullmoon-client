import { useMemo } from "react";

interface Star {
  id: number;
  top: string;
  left: string;
  size: number;
  opacity: number;
}

/* The night sky the client is named after: one navy depth, one moonlit
   bloom, and stars that hold still. Nothing here moves — the wallpaper's
   job is to make the foreground readable, never to ask for a glance. */
export function AtmosphericBackdrop() {
  const stars: Star[] = useMemo(() => {
    return Array.from({ length: 56 }, (_, i) => ({
      id: i,
      top: `${(i * 19.7) % 94}%`,
      left: `${(i * 23.3) % 98}%`,
      size: i % 3 === 0 ? 2 : i % 2 === 0 ? 1.5 : 1,
      opacity: 0.18 + (i % 5) * 0.11,
    }));
  }, []);

  return (
    <div className="game-backdrop" aria-hidden="true">
      <div className="nebula-layer nebula-gold" />

      <div className="starfield">
        {stars.map((s) => (
          <span
            key={s.id}
            className="twinkle-star"
            style={{
              top: s.top,
              left: s.left,
              width: `${s.size}px`,
              height: `${s.size}px`,
              opacity: s.opacity,
            }}
          />
        ))}
      </div>

      <div className="backdrop-vignette" />
    </div>
  );
}
export default AtmosphericBackdrop;
