import { useMemo } from "react";

interface Star {
  id: number;
  top: string;
  left: string;
  size: number;
  opacity: number;
  duration: string;
  delay: string;
}

export function AtmosphericBackdrop() {
  const stars: Star[] = useMemo(() => {
    return Array.from({ length: 42 }, (_, i) => ({
      id: i,
      top: `${(i * 19.7) % 94}%`,
      left: `${(i * 23.3) % 98}%`,
      size: (i % 3 === 0 ? 2.5 : (i % 2 === 0 ? 1.8 : 1.2)),
      opacity: 0.25 + (i % 5) * 0.15,
      duration: `${3 + (i % 4) * 1.5}s`,
      delay: `${(i % 7) * 0.8}s`,
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
              animationDuration: s.duration,
              animationDelay: s.delay,
            }}
          />
        ))}
      </div>

      <div className="backdrop-vignette" />
      <div className="backdrop-grid" />
    </div>
  );
}
export default AtmosphericBackdrop;
