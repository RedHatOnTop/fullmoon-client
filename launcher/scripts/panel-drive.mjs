/* panel-drive.mjs — photograph the in-game client: the HUD, then every page of
   the settings panel.

   Joins the local 26.1.2 server through the launcher (quick play, so no menu
   click), then dresses the probe player over RCON — the gear and potion
   modules have nothing to draw on a naked player standing in an empty field,
   and an empty case is not evidence that they render.

   The panel is walked with the keyboard: RIGHT SHIFT opens it, TAB cycles the
   rail. The window stays parked off the desktop the whole time and the frames
   come out of the game's own F2. */
import { execFileSync } from "node:child_process";
import { setTimeout as sleep } from "node:timers/promises";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import puppeteer from "puppeteer-core";
import { rcon } from "./_rcon.mjs";

/* Every module on, and placed so nothing lands under the toast column in the
   top right. Written after the join, not before: the launcher owns this file
   at launch time and would overwrite it. The mod re-reads on mtime, so the
   layout lands live. */
const LAYOUT = [
  ["fps", 4, 5],
  ["cps", 4, 12],
  ["coords", 4, 19],
  ["ping", 88, 5],
  ["potion", 88, 13],
  ["gear", 4, 84],
  ["keystrokes", 86, 68],
];

const PORT = process.env.CDP_PORT ?? "9333";
const SERVER = process.env.MC_SERVER ?? "127.0.0.1";
const OUT = process.env.OUT ?? "shots";

const VK_RSHIFT = 161;
const VK_TAB = 9;
const VK_W = 87;
const VK_SPACE = 32;

const ps = (args) =>
  execFileSync("powershell", ["-ExecutionPolicy", "Bypass", "-File", "scripts/shot-game.ps1", ...args], {
    encoding: "utf8",
    stdio: "pipe",
  }).trim();

const shot = (name, opts = {}) => {
  const args = ["-Out", `${OUT}/${name}.png`, "-Park"];
  if (opts.send) args.push("-SendKey", opts.send);
  if (opts.hold) args.push("-HoldKey", opts.hold);
  if (opts.holdMs) args.push("-HoldMs", String(opts.holdMs));
  try {
    console.log(ps(args));
  } catch (e) {
    console.log(`shot ${name} failed:`, String(e.stderr ?? e).slice(0, 300));
  }
};

const browser = await puppeteer.connect({ browserURL: `http://127.0.0.1:${PORT}`, defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
if (!page) throw new Error("launcher page not found on the debug port");
const invoke = (cmd, args = {}) =>
  page.evaluate((c, a) => window.__TAURI_INTERNALS__.invoke(c, a), cmd, args);

const instances = await invoke("instances_list");
const inst = instances.find((i) => i.loader === "fabric" && i.versionId === "26.1.2");
const gameDir = path.join(process.env.APPDATA, "Pinion", "instances", inst.id, "minecraft");

/* The "Move with W A S D" toast outlives any amount of walking and sits on top
   of the right-hand modules. It is a startup option, so it has to be off
   before the launch, not after the join. */
const optionsFile = path.join(gameDir, "options.txt");
if (existsSync(optionsFile)) {
  const opts = readFileSync(optionsFile, "utf8");
  if (!/^tutorialStep:none$/m.test(opts)) {
    writeFileSync(
      optionsFile,
      /^tutorialStep:/m.test(opts)
        ? opts.replace(/^tutorialStep:.*$/m, "tutorialStep:none")
        : `${opts.trimEnd()}\ntutorialStep:none\n`,
    );
    console.log("tutorial toasts disabled in options.txt");
  }
}

const status = await invoke("game_status");
if (status.state !== "running") {
  const accounts = await invoke("auth_list");
  await invoke("auth_select", { uuid: accounts[0].uuid });
  console.log("account:", accounts[0].username, "· instance:", inst.name);
  console.log("session:", await invoke("launch_quickplay", { instanceId: inst.id, server: SERVER }));
} else {
  console.log("game already running, reusing it");
}

/* park it the moment it exists — the probe must not take the screen away from
   whoever is using the machine */
for (let i = 0; i < 40; i++) {
  try {
    console.log("window", ps(["-Park", "-HideOnly"]));
    break;
  } catch {
    await sleep(2000);
  }
}

/* the window is up long before the player is; ask the server who is on rather
   than guessing at a sleep */
let joined = false;
for (let i = 0; i < 90 && !joined; i++) {
  const [list] = await rcon(["list"]);
  joined = /There are [1-9]/.test(list);
  if (!joined) await sleep(2000);
}
console.log(joined ? "player joined" : "player never joined — shooting anyway");
await sleep(4000);

const hudFile = path.join(gameDir, "pinion", "hud.json");
writeFileSync(
  hudFile,
  JSON.stringify({
    modules: LAYOUT.map(([id, x, y]) => ({ id, enabled: true, x, y, scale: 1 })),
  }),
);
console.log("layout written:", hudFile);

const name = process.env.PLAYER ?? "PinionDev";
console.log(
  await rcon([
    "time set noon",
    "weather clear",
    // rcon feedback is broadcast to ops, and a chat column of "Replaced a slot"
    // is the loudest thing in a screenshot of a HUD
    "gamerule sendCommandFeedback false",
    `item replace entity ${name} armor.head with minecraft:diamond_helmet`,
    `item replace entity ${name} armor.chest with minecraft:iron_chestplate`,
    `item replace entity ${name} armor.legs with minecraft:diamond_leggings`,
    `item replace entity ${name} armor.feet with minecraft:golden_boots`,
    `item replace entity ${name} weapon.mainhand with minecraft:netherite_pickaxe`,
    `item replace entity ${name} weapon.offhand with minecraft:shield`,
    `effect give ${name} minecraft:speed 600 1`,
    `effect give ${name} minecraft:night_vision 900 0`,
    `effect give ${name} minecraft:haste 240 2`,
  ]),
);
/* The "Move with W A S D" tutorial toast does not time out — it hangs over the
   HUD until the player actually walks, and it is the loudest thing in any
   capture. Walking for three seconds retires it and its Jump twin. */
shot("warmup", { hold: `${VK_W},${VK_SPACE}`, holdMs: 3000 });

/* chat lines and advancement toasts sit on top of the HUD for about ten
   seconds after a join; the shot has to outlast them */
await sleep(Number(process.env.SETTLE ?? 14_000));

shot("hud");
shot("hud-keys", { hold: `${VK_W},${VK_SPACE}` });
shot("panel-modules", { send: `${VK_RSHIFT}` });
shot("panel-visuals", { send: `${VK_TAB}` });
shot("panel-keys", { send: `${VK_TAB}` });
// leave the game as it was found: panel closed, player back in the world
ps(["-Park", "-SendKey", `${VK_TAB},${VK_RSHIFT}`]);

console.log("final state:", JSON.stringify(await invoke("game_status")));
browser.disconnect();
