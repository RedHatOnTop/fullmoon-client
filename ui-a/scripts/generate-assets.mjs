// Generates the 3D-viewer textures the launcher ships: one player skin (64x64)
// and a cape (64x32) per cosmetic hue. Pure pixel art, no external assets.
import pngjs from "pngjs";
import { writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const { PNG } = pngjs;
const here = dirname(fileURLToPath(import.meta.url));
const pub = join(here, "..", "public");
mkdirSync(join(pub, "skins"), { recursive: true });
mkdirSync(join(pub, "capes"), { recursive: true });

const mk = (w, h) => {
  const png = new PNG({ width: w, height: h });
  png.data.fill(0);
  const set = (x, y, [r, g, b, a = 255]) => {
    if (x < 0 || y < 0 || x >= w || y >= h) return;
    const i = (y * w + x) * 4;
    png.data[i] = r;
    png.data[i + 1] = g;
    png.data[i + 2] = b;
    png.data[i + 3] = a;
  };
  const rect = (x, y, rw, rh, c) => {
    for (let j = 0; j < rh; j++) for (let i = 0; i < rw; i++) set(x + i, y + j, c);
  };
  const save = (p) => writeFileSync(p, PNG.sync.write(png));
  return { set, rect, save };
};

const hsl = (h, s, l) => {
  s /= 100;
  l /= 100;
  const k = (n) => (n + h / 30) % 12;
  const a = s * Math.min(l, 1 - l);
  const f = (n) => l - a * Math.max(-1, Math.min(k(n) - 3, 9 - k(n), 1));
  return [Math.round(f(0) * 255), Math.round(f(8) * 255), Math.round(f(4) * 255), 255];
};

/* ───────── skin: dark-hoodie gamer with brand-blue accents ───────── */
{
  const s = mk(64, 64);
  const skin = [216, 172, 130, 255];
  const skinDk = [190, 146, 108, 255];
  const hair = [38, 30, 28, 255];
  const shirt = [24, 28, 38, 255];
  const shirtDk = [18, 21, 30, 255];
  const accent = [110, 168, 255, 255];
  const denim = [44, 52, 68, 255];
  const shoe = [16, 19, 27, 255];
  const eyeW = [234, 238, 246, 255];

  const faces = (parts, c) => parts.forEach(([x, y, w, h]) => s.rect(x, y, w, h, c));

  // head — all six faces skin
  faces(
    [
      [8, 0, 8, 8], [16, 0, 8, 8], [0, 8, 8, 8], [8, 8, 8, 8], [16, 8, 8, 8], [24, 8, 8, 8],
    ],
    skin,
  );
  // hair cap
  s.rect(8, 0, 8, 8, hair);
  s.rect(8, 8, 8, 2, hair);
  s.rect(0, 8, 8, 2, hair);
  s.rect(16, 8, 8, 2, hair);
  s.rect(24, 8, 8, 2, hair);
  // eyes + brow + mouth on front face
  s.rect(9, 11, 2, 1, eyeW);
  s.rect(13, 11, 2, 1, eyeW);
  s.set(10, 11, accent);
  s.set(13, 11, accent);
  s.rect(11, 13, 2, 1, skinDk);

  // body — hoodie
  faces(
    [
      [20, 16, 8, 4], [28, 16, 8, 4], [16, 20, 4, 12], [20, 20, 8, 12], [28, 20, 4, 12], [32, 20, 8, 12],
    ],
    shirt,
  );
  s.rect(20, 24, 8, 1, accent); // chest stripe
  s.rect(23, 21, 2, 2, accent); // collar emblem
  s.rect(16, 20, 4, 12, shirtDk);
  s.rect(32, 20, 8, 12, shirtDk);

  // right arm (sleeve + hand)
  faces(
    [
      [44, 16, 4, 4], [48, 16, 4, 4], [40, 20, 4, 12], [44, 20, 4, 12], [48, 20, 4, 12], [52, 20, 4, 12],
    ],
    shirt,
  );
  [[40, 29], [44, 29], [48, 29], [52, 29]].forEach(([x, y]) => s.rect(x, y, 4, 3, skin));
  // left arm
  faces(
    [
      [36, 48, 4, 4], [40, 48, 4, 4], [32, 52, 4, 12], [36, 52, 4, 12], [40, 52, 4, 12], [44, 52, 4, 12],
    ],
    shirt,
  );
  [[32, 61], [36, 61], [40, 61], [44, 61]].forEach(([x, y]) => s.rect(x, y, 4, 3, skin));

  // right leg (denim + shoe)
  faces(
    [
      [4, 16, 4, 4], [8, 16, 4, 4], [0, 20, 4, 12], [4, 20, 4, 12], [8, 20, 4, 12], [12, 20, 4, 12],
    ],
    denim,
  );
  [[0, 29], [4, 29], [8, 29], [12, 29]].forEach(([x, y]) => s.rect(x, y, 4, 3, shoe));
  // left leg
  faces(
    [
      [20, 48, 4, 4], [24, 48, 4, 4], [16, 52, 4, 12], [20, 52, 4, 12], [24, 52, 4, 12], [28, 52, 4, 12],
    ],
    denim,
  );
  [[16, 61], [20, 61], [24, 61], [28, 61]].forEach(([x, y]) => s.rect(x, y, 4, 3, shoe));

  s.save(join(pub, "skins", "blackcow.png"));
}

/* ───────── capes: one per cosmetic hue ───────── */
const capes = [
  ["c1", 266], ["c2", 214], ["c3", 18], ["c4", 190],
  ["c5", 148], ["c6", 280], ["c7", 40], ["c8", 200],
];
for (const [id, hue] of capes) {
  const c = mk(64, 32);
  const base = hsl(hue, 68, 52);
  const dark = hsl(hue, 62, 34);
  const light = hsl(hue, 78, 68);
  const trim = hue === 40 ? [242, 210, 120, 255] : light; // Mythic gold trim on Solar Halo

  // cover every cape face region
  c.rect(0, 0, 23, 17, base);
  // front (1,1,10,16): vertical shade + emblem
  for (let y = 1; y < 17; y++) {
    const t = (y - 1) / 15;
    const col = [
      Math.round(base[0] * (1 - t) + dark[0] * t),
      Math.round(base[1] * (1 - t) + dark[1] * t),
      Math.round(base[2] * (1 - t) + dark[2] * t),
      255,
    ];
    c.rect(1, y, 10, 1, col);
  }
  // trim border on front
  c.rect(1, 1, 10, 1, trim);
  c.rect(1, 1, 1, 16, trim);
  c.rect(10, 1, 1, 16, trim);
  c.rect(1, 16, 10, 1, dark);
  // emblem diamond (feather-ish)
  const ex = 5, ey = 8;
  c.set(ex + 1, ey - 2, light);
  c.rect(ex, ey - 1, 3, 1, light);
  c.rect(ex - 1, ey, 5, 1, light);
  c.rect(ex, ey + 1, 3, 1, light);
  c.set(ex + 1, ey + 2, light);
  c.set(ex + 1, ey, trim);
  c.save(join(pub, "capes", `${id}.png`));
}

console.log("assets: skins/blackcow.png + 8 capes");
