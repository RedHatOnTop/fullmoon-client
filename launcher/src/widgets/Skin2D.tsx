/* Skin2D — the player figure as pure CSS sprite regions of the skin texture.
   No canvas, no WebGL, no render loop: the compositor draws it once and the
   UI thread never hears about it again. A 3D viewer costs a per-viewer rAF
   and a GL context; this costs twelve divs.

   Figure grid: 16×32 texture pixels (arm 4 + body 8 + arm 4; head 8, body 12,
   legs 12), scaled up with `image-rendering: pixelated`. */

import { useEffect, useState } from "react";

type View = "front" | "back";

type Part = {
  /** source rect in the 64×64 texture: x, y, w, h */
  s: [number, number, number, number];
  /** destination in the 16×32 figure grid: x, y */
  d: [number, number];
  /** legacy 64×32 skins mirror the right limb for the left */
  mirror?: boolean;
};

const FRONT: Array<(mirror: boolean) => Part[]> = [
  (m) => [
    { s: [8, 8, 8, 8], d: [4, 0] },
    { s: [40, 8, 8, 8], d: [4, 0] },
    { s: [20, 20, 8, 12], d: [4, 8] },
    { s: [20, 36, 8, 12], d: [4, 8] },
    { s: [44, 20, 4, 12], d: [0, 8] },
    { s: [44, 36, 4, 12], d: [0, 8] },
    { s: [36, 52, 4, 12], d: [12, 8] },
    { s: [52, 52, 4, 12], d: [12, 8] },
    { s: [4, 20, 4, 12], d: [4, 20] },
    { s: [4, 36, 4, 12], d: [4, 20] },
    { s: [20, 52, 4, 12], d: [8, 20] },
    { s: [4, 52, 4, 12], d: [8, 20] },
  ],
  (m) => [
    { s: [8, 8, 8, 8], d: [4, 0] },
    { s: [20, 20, 8, 12], d: [4, 8] },
    { s: [44, 20, 4, 12], d: [0, 8] },
    { s: [44, 20, 4, 12], d: [12, 8], mirror: m },
    { s: [4, 20, 4, 12], d: [4, 20] },
    { s: [4, 20, 4, 12], d: [8, 20], mirror: m },
  ],
];

const BACK: Array<(mirror: boolean) => Part[]> = [
  (m) => [
    { s: [24, 8, 8, 8], d: [4, 0] },
    { s: [56, 8, 8, 8], d: [4, 0] },
    { s: [32, 20, 8, 12], d: [4, 8] },
    { s: [32, 36, 8, 12], d: [4, 8] },
    { s: [52, 20, 4, 12], d: [12, 8] },
    { s: [52, 36, 4, 12], d: [12, 8] },
    { s: [44, 52, 4, 12], d: [0, 8] },
    { s: [60, 52, 4, 12], d: [0, 8] },
    { s: [12, 20, 4, 12], d: [8, 20] },
    { s: [12, 36, 4, 12], d: [8, 20] },
    { s: [28, 52, 4, 12], d: [4, 20] },
    { s: [12, 52, 4, 12], d: [4, 20] },
  ],
  (m) => [
    { s: [24, 8, 8, 8], d: [4, 0] },
    { s: [32, 20, 8, 12], d: [4, 8] },
    { s: [52, 20, 4, 12], d: [12, 8] },
    { s: [52, 20, 4, 12], d: [0, 8], mirror: m },
    { s: [12, 20, 4, 12], d: [8, 20] },
    { s: [12, 20, 4, 12], d: [4, 20], mirror: m },
  ],
];

export default function Skin2D({
  skin = "/skins/blackcow.png",
  cape = null,
  view = "front",
  scale = 8,
  label,
}: {
  skin?: string;
  cape?: string | null;
  /** front = the player's face; back = the cape side */
  view?: View;
  /** texture pixel → screen pixel; integers keep the pixels crisp */
  scale?: number;
  /** accessible name — the figure is content, not decoration */
  label?: string;
}) {
  /* legacy 64×32 skins have no overlay layer and mirrored left limbs */
  const [legacy, setLegacy] = useState(false);
  useEffect(() => {
    let alive = true;
    const img = new Image();
    img.onload = () => {
      if (alive) setLegacy(img.naturalWidth === img.naturalHeight * 2);
    };
    img.src = skin;
    return () => {
      alive = false;
    };
  }, [skin]);

  const parts = (view === "front" ? FRONT : BACK)[legacy ? 1 : 0](legacy);
  const bg = (image: string, w: number, h: number) => ({
    backgroundImage: `url("${image}")`,
    backgroundSize: `${w * scale}px ${h * scale}px`,
    imageRendering: "pixelated" as const,
  });

  return (
    <div
      role={label ? "img" : undefined}
      aria-label={label}
      style={{
        position: "relative",
        width: 16 * scale,
        height: 32 * scale,
        flex: "none",
      }}
    >
      {parts.map((p, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: p.d[0] * scale,
            top: p.d[1] * scale,
            width: p.s[2] * scale,
            height: p.s[3] * scale,
            ...bg(skin, 64, 64),
            backgroundPosition: `${-p.s[0] * scale}px ${-p.s[1] * scale}px`,
            transform: p.mirror ? "scaleX(-1)" : undefined,
          }}
        />
      ))}
      {/* a cape drapes OVER the back of the body — drawing it first would
          hide ten of its ten units behind torso and arms */}
      {cape && view === "back" && (
        <div
          style={{
            position: "absolute",
            left: 3 * scale,
            top: 7 * scale,
            width: 10 * scale,
            height: 16 * scale,
            ...bg(cape, 64, 32),
            backgroundPosition: `${-1 * scale}px ${-1 * scale}px`,
          }}
        />
      )}
    </div>
  );
}
