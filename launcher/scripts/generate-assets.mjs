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

/* ───────── capes: one per cape-slot cosmetic (ids match the catalogue) ─────────
   Each cape draws its own motif. The catalogue promises a feather, an ember
   embroidery, a starfield and a gold crest; five recolours of one diamond would
   make every line of that copy a lie. The visible panel is the 10x16 at (1,1);
   the rest of the 23x17 block only has to be covered so no face renders
   transparent. */
const mix = (a, b, t) => [
  Math.round(a[0] * (1 - t) + b[0] * t),
  Math.round(a[1] * (1 - t) + b[1] * t),
  Math.round(a[2] * (1 - t) + b[2] * t),
  255,
];

const capes = {
  "aero-cape": (p, { base, dark, light, pale }) => {
    for (let v = 0; v < 16; v++) p.row(v, mix(base, dark, v / 15));
    p.border(mix(light, base, 0.35));
    /* one flight feather. The vane is a silhouette, not a stack of diagonals —
       at ten pixels wide, drawn barbs merge into a blob; notching the edge is
       what makes it read as a feather. */
    const vane = (v) => (v <= 3 ? 1 : v <= 6 ? 2 : v <= 10 ? 3 : v <= 12 ? 2 : 1);
    for (let v = 2; v < 14; v++) {
      const w = vane(v);
      for (let k = 1; k <= w; k++) {
        const shade = mix(light, base, 0.1 + k * 0.14);
        p.px(4 - k, v, shade);
        p.px(4 + k, v, shade);
      }
      if (v % 3 === 0 && w > 1) {
        p.px(4 - w, v, mix(base, dark, 0.5));
        p.px(4 + w, v, mix(base, dark, 0.5));
      }
    }
    for (let v = 2; v < 14; v++) p.px(4, v, v > 11 ? mix(pale, light, 0.5) : pale);
  },

  "ember-cape": (p, { base, dark, light, pale }) => {
    // charcoal at the shoulders, live coals at the hem
    for (let v = 0; v < 16; v++) p.row(v, mix(mix(dark, [26, 18, 16, 255], 0.55), base, v / 15));
    p.border(mix(dark, base, 0.5));
    const sparks = [
      [2, 3], [6, 2], [4, 5], [7, 6], [1, 7], [5, 8], [8, 9], [3, 9],
      [6, 11], [2, 11], [7, 13], [4, 12], [1, 13], [5, 14],
    ];
    for (const [u, v] of sparks) {
      p.px(u, v, v > 8 ? pale : light);
      if (v > 10) p.px(u, v - 1, mix(light, base, 0.6)); // the ones near the fire trail
    }
  },

  "void-cape": (p, { base, dark, light, pale }) => {
    // sinks past black rather than to it, so the hem keeps a trace of hue
    const deep = mix(dark, [8, 6, 14, 255], 0.8);
    for (let v = 0; v < 16; v++) p.row(v, mix(mix(base, light, 0.2), deep, (v / 15) ** 0.75));
    p.border(mix(deep, base, 0.35));
    const stars = [
      [2, 1, 1], [6, 2, 0], [8, 1, 1], [4, 3, 0], [1, 4, 1], [7, 5, 0],
      [3, 6, 0], [5, 7, 1], [8, 8, 0], [2, 9, 0], [6, 10, 0], [4, 12, 0],
    ];
    for (const [u, v, bright] of stars) p.px(u, v, bright ? pale : mix(pale, base, 0.55));
  },

  "mint-cape": (p, { base, dark, light }) => {
    // the quiet one: flat colour, one seam, a hem. Restraint is the design.
    p.fill(base);
    p.border(mix(base, light, 0.4));
    for (let v = 1; v < 15; v++) p.px(7, v, mix(base, light, 0.55));
    p.row(14, mix(base, dark, 0.45));
    p.row(15, mix(base, dark, 0.7));
  },

  "regal-cape": (p) => {
    /* the catalogue sells the gold thread, so the cloth has to be dark enough
       for gold to be thread and not just more cloth */
    const cloth = hsl(46, 34, 17);
    const clothLo = hsl(46, 30, 11);
    const gold = [242, 210, 120, 255];
    const goldDk = [178, 142, 62, 255];
    for (let v = 0; v < 16; v++) p.row(v, mix(cloth, clothLo, v / 15));
    for (let v = 1; v < 15; v++)
      for (let u = 0; u < 10; u++)
        if ((u + v) % 4 === 0) p.px(u, v, mix(cloth, goldDk, 0.3));
    p.border(gold);
    p.row(15, goldDk);
    const crest = [
      [4, 5], [5, 5],
      [3, 6], [4, 6], [5, 6], [6, 6],
      [2, 7], [7, 7],
      [3, 8], [4, 8], [5, 8], [6, 8],
      [4, 9], [5, 9],
      [4, 10], [5, 10],
    ];
    for (const [u, v] of crest) p.px(u, v, gold);
    p.px(4, 7, goldDk);
    p.px(5, 7, goldDk);
  },
};

const CAPE_HUE = {
  "aero-cape": 214, "ember-cape": 22, "void-cape": 272, "mint-cape": 152, "regal-cape": 46,
};

for (const [id, draw] of Object.entries(capes)) {
  const c = mk(64, 32);
  const hue = CAPE_HUE[id];
  const palette = {
    base: hsl(hue, 68, 52),
    dark: hsl(hue, 62, 30),
    light: hsl(hue, 78, 68),
    pale: hsl(hue, 90, 86),
  };
  c.rect(0, 0, 23, 17, palette.base);

  const p = {
    px: (u, v, col) => c.set(1 + u, 1 + v, col),
    row: (v, col) => c.rect(1, 1 + v, 10, 1, col),
    fill: (col) => c.rect(1, 1, 10, 16, col),
    border: (col) => {
      c.rect(1, 1, 10, 1, col);
      c.rect(1, 1, 1, 16, col);
      c.rect(10, 1, 1, 16, col);
    },
  };
  draw(p, palette);
  c.save(join(pub, "capes", `${id}.png`));
}

console.log(`assets: skins/blackcow.png + ${Object.keys(capes).length} capes`);
