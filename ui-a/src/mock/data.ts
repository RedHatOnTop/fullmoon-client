// Mock stand-ins for the eventual Rust core contract (auth/versions/mods/...).
// UI renders entirely from these — no backend required to view or compare.

export const account = {
  name: "BlackCow",
  uuid: "8b1d…f42a",
  state: "Authenticated",
  // inline data-uri avatar so it renders offline (steve-ish head)
  avatar:
    "data:image/svg+xml;utf8," +
    encodeURIComponent(
      `<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64'>
        <rect width='64' height='64' fill='#1b1f2b'/>
        <rect x='14' y='12' width='36' height='34' rx='4' fill='#6ea8ff'/>
        <rect x='20' y='24' width='8' height='8' fill='#0a0b10'/>
        <rect x='36' y='24' width='8' height='8' fill='#0a0b10'/>
        <rect x='24' y='38' width='16' height='4' fill='#0a0b10' opacity='.5'/>
      </svg>`,
    ),
};

export const version = { mc: "26.1.2", fabric: "0.17.2", loader: "Fabric" };

export type NewsItem = { id: string; title: string; body: string; date: string; tag: string; hue: number };
export const news: NewsItem[] = [
  {
    id: "n1",
    title: "26.1.2 support is live",
    body: "Fabric loader synced, Sodium rebuilt. Instances update on next launch.",
    date: "07-22",
    tag: "P",
    hue: 214,
  },
  {
    id: "n2",
    title: "Pinion HUD — keystrokes & CPS",
    body: "New in-game module: draggable keystrokes, CPS meter, reach display.",
    date: "07-19",
    tag: "H",
    hue: 266,
  },
  {
    id: "n3",
    title: "Cosmetic drop: Aurora capes",
    body: "Six animated capes added to the vault. Equip from the Cosmetics tab.",
    date: "07-14",
    tag: "C",
    hue: 158,
  },
];

export type Server = {
  id: string;
  name: string;
  addr: string;
  hue: number;
  ping: number;
  players: number;
  max: number;
  motd: string;
  version: string;
};
export const servers: Server[] = [
  { id: "s1", name: "Hypixel", addr: "mc.hypixel.net", hue: 214, ping: 24, players: 41208, max: 100000, motd: "Skyblock · Bedwars · Duels", version: "1.8-26.1" },
  { id: "s2", name: "Lobby / SMP", addr: "play.blackcow.gg", hue: 266, ping: 61, players: 38, max: 120, motd: "Private survival — season 4", version: "26.1.2" },
  { id: "s3", name: "Practice PvP", addr: "eu.pvp.land", hue: 158, ping: 138, players: 892, max: 2000, motd: "Ranked crystal & sword", version: "26.1.2" },
  { id: "s4", name: "MineCore FFA", addr: "ffa.minecore.io", hue: 24, ping: 47, players: 611, max: 1500, motd: "Kit FFA · NoDebuff · Sumo", version: "1.8-26.1" },
  { id: "s5", name: "Aether Anarchy", addr: "join.aether.gg", hue: 300, ping: 92, players: 204, max: 500, motd: "No rules. No resets. 3y old.", version: "26.1.2" },
];

export type World = { id: string; name: string; mode: string; last: string; hue: number };
export const worlds: World[] = [
  { id: "w1", name: "Skyhaven", mode: "Survival · Hard", last: "2h ago", hue: 158 },
  { id: "w2", name: "Redstone Lab", mode: "Creative · Flat", last: "yesterday", hue: 8 },
  { id: "w3", name: "Frostpeak", mode: "Survival · Normal", last: "3d ago", hue: 200 },
];

export type Mod = {
  id: string;
  name: string;
  desc: string;
  ver: string;
  tag: "perf" | "core" | null;
  letter: string;
  hue: number;
  dl: string;
  on: boolean;
};
export const mods: Mod[] = [
  { id: "pinion", name: "Pinion HUD", desc: "FPS · CPS · keystrokes · coords · armor · potions", ver: "1.0.0", tag: "core", letter: "P", hue: 214, dl: "first-party", on: true },
  { id: "sodium", name: "Sodium", desc: "Rendering engine rewrite — massive FPS uplift", ver: "0.6.3", tag: "perf", letter: "S", hue: 190, dl: "48.2M", on: true },
  { id: "lithium", name: "Lithium", desc: "Game-logic & tick optimizations, zero visual change", ver: "0.13.1", tag: "perf", letter: "L", hue: 158, dl: "31.6M", on: true },
  { id: "iris", name: "Iris Shaders", desc: "Shaderpack loader, compatible with Sodium", ver: "1.8.4", tag: null, letter: "I", hue: 266, dl: "22.9M", on: false },
  { id: "modmenu", name: "Mod Menu", desc: "Configure every installed mod from one screen", ver: "11.0.1", tag: null, letter: "M", hue: 30, dl: "40.1M", on: true },
  { id: "zoom", name: "Zoomify", desc: "Smooth configurable zoom bound to a hotkey", ver: "2.14.0", tag: null, letter: "Z", hue: 340, dl: "6.4M", on: false },
];

export type Cosmetic = { id: string; name: string; rarity: string; hue: number; on: boolean };
export const cosmetics: Cosmetic[] = [
  { id: "c1", name: "Aurora", rarity: "Legendary", hue: 266, on: true },
  { id: "c2", name: "Nimbus", rarity: "Epic", hue: 214, on: false },
  { id: "c3", name: "Ember Wings", rarity: "Legendary", hue: 18, on: false },
  { id: "c4", name: "Tidecloak", rarity: "Rare", hue: 190, on: false },
  { id: "c5", name: "Verdant", rarity: "Rare", hue: 148, on: false },
  { id: "c6", name: "Obsidian", rarity: "Epic", hue: 280, on: false },
  { id: "c7", name: "Solar Halo", rarity: "Mythic", hue: 40, on: false },
  { id: "c8", name: "Frostwing", rarity: "Epic", hue: 200, on: false },
];

export const rarityColor = (r: string) =>
  r === "Mythic" ? "#f2c14e" : r === "Legendary" ? "#c084fc" : r === "Epic" ? "#6ea8ff" : "#56d98a";
