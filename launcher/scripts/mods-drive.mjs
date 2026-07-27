/* mods-drive.mjs — prove that a mod in the catalogue becomes a jar on disk.

   Runs the install through the app's own IPC (the same call the 설치 button
   makes), then reports what actually landed in the instance's mods directory:
   the point of the exercise is the file, not the promise. */
import { readdirSync, existsSync } from "node:fs";
import { join } from "node:path";
import puppeteer from "puppeteer-core";

const PORT = process.env.CDP_PORT ?? "9333";
const ROOT = join(process.env.APPDATA, "Pinion");

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${PORT}`,
  defaultViewport: null,
});
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
if (!page) throw new Error("launcher page not found on the debug port");

const invoke = (cmd, args = {}) =>
  page.evaluate(
    (c, a) => window.__TAURI_INTERNALS__.invoke(c, a),
    cmd,
    args,
  );

const instances = await invoke("instances_list");
const target = instances.find((i) => i.loader === "fabric" && i.versionId === "26.1.2");
if (!target) throw new Error("no fabric 26.1.2 instance to test with");
console.log("instance:", target.id, target.name);

const before = existsSync(join(ROOT, "instances", target.id, "minecraft", "mods"))
  ? readdirSync(join(ROOT, "instances", target.id, "minecraft", "mods"))
  : [];
console.log("mods before:", before);

console.time("install");
await invoke("instance_install", { id: target.id });
console.timeEnd("install");

const dir = join(ROOT, "instances", target.id, "minecraft", "mods");
console.log("mods after:", readdirSync(dir));
console.log("state:", JSON.stringify(await invoke("mods_list", { instanceId: target.id })
  .then((l) => l.map((m) => `${m.id}:${m.enabled ? "on" : "off"}`))));

browser.disconnect();
