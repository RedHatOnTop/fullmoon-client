// Hand-built isometric voxel island — the "it's Minecraft" cue, no game assets.
import type { CSSProperties } from "react";

type Kind = "grass" | "dirt" | "wood" | "leaf" | "brand" | "stone";
type Block = { i: number; j: number; k: number; t: Kind };

const PAL: Record<Kind, [string, string, string]> = {
  grass: ["#7cc46e", "#6b4a35", "#573c2a"],
  dirt: ["#8a5e3c", "#6f4a30", "#573a26"],
  wood: ["#7d5c39", "#5f4630", "#4c391f"],
  leaf: ["#6fbf72", "#4f9a56", "#3d7a45"],
  brand: ["#93c1ff", "#5f8fe6", "#4a72c8"],
  stone: ["#8b93a3", "#6b7280", "#535964"],
};

const TW = 20,
  TH = 10,
  CH = 20,
  OX = 160,
  OY = 96;

const blocks: Block[] = [];
// island body: two dirt layers + a tapered bottom point
for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) blocks.push({ i, j, k: 0, t: "dirt" });
for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) blocks.push({ i, j, k: 1, t: "grass" });
blocks.push({ i: 1, j: 1, k: -1, t: "stone" });
// tree
blocks.push({ i: 1, j: 1, k: 2, t: "wood" }, { i: 1, j: 1, k: 3, t: "wood" });
blocks.push(
  { i: 0, j: 1, k: 3, t: "leaf" },
  { i: 2, j: 1, k: 3, t: "leaf" },
  { i: 1, j: 0, k: 3, t: "leaf" },
  { i: 1, j: 2, k: 3, t: "leaf" },
  { i: 1, j: 1, k: 4, t: "leaf" },
);
// floating brand block + a drifting stone
blocks.push({ i: 3, j: -1, k: 2, t: "brand" });
blocks.push({ i: -1, j: 3, k: 1, t: "stone" });

blocks.sort((a, b) => a.i + a.j - (b.i + b.j) || a.k - b.k);

const poly = (pts: number[][], fill: string) => (
  <polygon points={pts.map((p) => p.join(",")).join(" ")} fill={fill} />
);

export default function VoxelIsland({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg viewBox="0 0 340 320" className={className} style={style} fill="none">
      {blocks.map((b, n) => {
        const sx = OX + (b.i - b.j) * TW;
        const sy = OY + (b.i + b.j) * TH - b.k * CH;
        const [top, left, right] = PAL[b.t];
        const T = [sx, sy],
          R = [sx + TW, sy + TH],
          B = [sx, sy + 2 * TH],
          L = [sx - TW, sy + TH];
        const Bd = [sx, sy + 2 * TH + CH],
          Ld = [sx - TW, sy + TH + CH],
          Rd = [sx + TW, sy + TH + CH];
        return (
          <g key={n}>
            {poly([L, B, Bd, Ld], left)}
            {poly([R, B, Bd, Rd], right)}
            {poly([T, R, B, L], top)}
          </g>
        );
      })}
    </svg>
  );
}
