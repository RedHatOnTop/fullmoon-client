/* hud-drive.mjs — launch into a world through the launcher and photograph the
   in-game HUD.

   A local 26.1.2 server stands in for "a world": quick play joins it without a
   menu click, so the shot is of the mod drawing over a real running game with
   a real player entity, not of a title screen. */
import { execFileSync } from "node:child_process";
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const PORT = process.env.CDP_PORT ?? "9333";
const SERVER = process.env.MC_SERVER ?? "127.0.0.1";
const OUT = process.env.SHOT ?? "hud.png";
const JOIN_WAIT = Number(process.env.JOIN_WAIT ?? 150_000);

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${PORT}`,
  defaultViewport: null,
});
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
if (!page) throw new Error("launcher page not found on the debug port");

const invoke = (cmd, args = {}) =>
  page.evaluate((c, a) => window.__TAURI_INTERNALS__.invoke(c, a), cmd, args);

const accounts = await invoke("auth_list");
if (!accounts.length) throw new Error("no account to launch with");
await invoke("auth_select", { uuid: accounts[0].uuid });
console.log("account:", accounts[0].username, `(${accounts[0].source})`);

const instances = await invoke("instances_list");
const inst = instances.find((i) => i.loader === "fabric" && i.versionId === "26.1.2");
console.log("instance:", inst.name);

const session = await invoke("launch_quickplay", { instanceId: inst.id, server: SERVER });
console.log("session:", session);

/* Hide the window the moment it appears: the probe must not take the screen
   away from whoever is using the machine. The game keeps rendering, and
   PrintWindow still reads the surface. */
const hide = () => {
  try {
    return execFileSync(
      "powershell",
      ["-ExecutionPolicy", "Bypass", "-File", "scripts/shot-game.ps1", "-HideOnly"],
      { encoding: "utf8", stdio: "pipe" },
    ).trim();
  } catch {
    return null;
  }
};
let hidden = null;
for (let i = 0; i < 30 && !(hidden = hide()); i++) await sleep(2000);
console.log("window", hidden ?? "NOT FOUND");

/* The window exists long before the player does, so the first frames are the
   loading screen. Wait out the join, then take a short burst — the HUD only
   draws once there is a level and a player to draw it over. */
await sleep(Number(process.env.WARMUP ?? 75_000));

for (let i = 0; i < Number(process.env.SHOTS ?? 3); i++) {
  const state = await invoke("game_status");
  if (state.state === "crashed" || state.state === "closed") {
    console.log("game ended before the shot:", JSON.stringify(state));
    break;
  }
  try {
    const out = execFileSync(
      "powershell",
      [
        "-ExecutionPolicy", "Bypass", "-File", "scripts/shot-game.ps1",
        "-Out", OUT.replace(/\.png$/, `-${i}.png`), "-Hide",
      ],
      { encoding: "utf8" },
    );
    console.log(out.trim());
  } catch (e) {
    console.log("shot failed:", String(e.stderr ?? e).slice(0, 300));
  }
  await sleep(8000);
}

console.log("final state:", JSON.stringify(await invoke("game_status")));
browser.disconnect();
