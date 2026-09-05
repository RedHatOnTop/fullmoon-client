import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--no-sandbox"],
  defaultViewport: { width: 1600, height: 1000 },
});
const page = await browser.newPage();
await page.goto("http://127.0.0.1:5921", { waitUntil: "networkidle0", timeout: 20000 });
await sleep(1800);

const probe = await page.evaluate(() => {
  const out = {};
  const r = (el) => { if (!el) return null; const b = el.getBoundingClientRect(); return { x: Math.round(b.x), y: Math.round(b.y), w: Math.round(b.width), h: Math.round(b.height) }; };
  const tb = document.querySelector(".topbar");
  const h = tb?.querySelector(".topbar-heading");
  const h1 = h?.querySelector("h1");
  out.topbar = r(tb); out.heading = r(h); out.h1 = r(h1);
  if (h1) { const cs = getComputedStyle(h1); out.h1cs = { color: cs.color, opacity: cs.opacity, visibility: cs.visibility, fontSize: cs.fontSize }; }
  out.searchbtn = r(document.querySelector(".searchbtn"));
  if (h1) { const b = h1.getBoundingClientRect(); const el = document.elementFromPoint(b.x + b.width / 2, b.y + b.height / 2); out.hit = el ? (el.className || el.tagName) : null; }
  const gb = document.querySelector(".game-backdrop");
  out.backdrop = r(gb);
  if (gb) { const cs = getComputedStyle(gb); out.backdropCs = { zIndex: cs.zIndex, position: cs.position }; }
  const tbcs = getComputedStyle(tb); out.topbarCs = { zIndex: tbcs.zIndex, position: tbcs.position, bg: tbcs.backgroundColor };
  const m = document.querySelector(".main"); const mcs = getComputedStyle(m);
  out.mainCs = { position: mcs.position, zIndex: mcs.zIndex };
  const shell = document.querySelector(".shell"); const scs = getComputedStyle(shell);
  out.shellCs = { position: scs.position, zIndex: scs.zIndex };
  return out;
});
console.log(JSON.stringify(probe, null, 2));
await browser.close();
