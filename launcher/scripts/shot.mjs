/* shot.mjs — serve dist/ + screenshot a list of UI states.
   Usage: node scripts/shot.mjs [prefix]                       */

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const here = join(fileURLToPath(import.meta.url), "..");
const DIST = normalize(join(here, "..", "dist"));
const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUT = "C:\\Users\\jin14\\AppData\\Local\\Temp\\opencode";
const PREFIX = process.argv[2] ?? "fz";
const MIME = {
  ".html": "text/html", ".js": "text/javascript", ".css": "text/css",
  ".png": "image/png", ".svg": "image/svg+xml", ".json": "application/json",
};

const srv = createServer(async (req, res) => {
  try {
    let p = decodeURIComponent(req.url.split("?")[0]);
    if (p === "/") p = "/index.html";
    let fp = normalize(join(DIST, p));
    let buf;
    try { buf = await readFile(fp); }
    catch { buf = await readFile(join(DIST, "index.html")); fp = "index.html"; }
    res.writeHead(200, { "content-type": MIME[extname(fp)] || "application/octet-stream" });
    res.end(buf);
  } catch (e) { res.writeHead(500); res.end(String(e)); }
});
await new Promise((r) => srv.listen(4173, "127.0.0.1", r));

export const clickByText = (page, sel, text) =>
  page.evaluate((s, t) => {
    const el = [...document.querySelectorAll(s)].find((e) => e.textContent.trim().includes(t));
    if (!el) throw new Error(`not found: ${s} "${t}"`);
    el.click();
  }, sel, text);

const browser = await puppeteer.launch({
  executablePath: EDGE, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--use-gl=angle", "--use-angle=swiftshader"],
  defaultViewport: { width: 1600, height: 1000 },
});

try {
  const page = await browser.newPage();
  page.on("pageerror", (e) => console.log("PAGE ERROR:", e.message));
  await page.goto("http://127.0.0.1:4173", { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(2200);

  const shot = async (name) => {
    await page.screenshot({ path: `${OUT}\\${PREFIX}-${name}.png` });
    console.log("shot:", name);
  };

  await shot("00-home");

  // command palette
  await page.keyboard.down("Control");
  await page.keyboard.press("k");
  await page.keyboard.up("Control");
  await sleep(600);
  await shot("01-palette");
  await page.keyboard.press("Escape");
  await sleep(300);

  // account chip dropdown
  await page.evaluate(() => document.querySelector(".acctchip")?.click());
  await sleep(500);
  await shot("02-acct-menu");
  await page.keyboard.press("Escape");
  await page.evaluate(() => document.body.click());
  await sleep(300);

  // bell dropdown
  await page.evaluate(() => document.querySelector(".bellbtn")?.click());
  await sleep(500);
  await shot("03-bell");
  await page.keyboard.press("Escape");
  await sleep(300);

  // other screens with the new topbar
  for (const [ko, name] of [
    ["인스턴스", "04-instances"],
    ["모드", "05-mods"],
    ["코스메틱", "06-cosmetics"],
    ["계정", "07-accounts"],
    ["설정", "08-settings"],
    ["콘솔", "09-console"],
  ]) {
    await clickByText(page, ".sidebar-item", ko);
    await sleep(900);
    await shot(name);
  }

  console.log("SHELL SWEEP OK");
} finally {
  await browser.close();
  srv.close();
}
