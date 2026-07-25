/* flow-shot.mjs — cosmetics 3D + the launch overlay flow. */
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
const MIME = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".png": "image/png", ".svg": "image/svg+xml" };

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

const clickByText = (page, sel, text) =>
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
  await sleep(2000);
  const shot = async (n) => { await page.screenshot({ path: `${OUT}\\flow-${n}.png` }); console.log("shot:", n); };

  // cosmetics: 3D stage + cape swap + walk
  await clickByText(page, ".sidebar-item", "코스메틱");
  await sleep(1400);
  await shot("01-cosmetics");
  await clickByText(page, ".cos-item", "보이드 케이프");
  await sleep(1200);
  await clickByText(page, ".cos-stage-anim .segmented-item", "걷기");
  await sleep(900);
  await shot("02-cos-void-walk");

  // launch flow
  await clickByText(page, ".sidebar-item", "홈");
  await sleep(800);
  await page.evaluate(() => document.querySelector(".playbtn")?.click());
  await sleep(1500);
  await shot("03-overlay-starting");
  // wait for running state (mock core streams logs then flips state)
  await page.waitForFunction(
    () => !!document.querySelector(".lov-badge.ok"),
    { timeout: 30000 },
  );
  await sleep(600);
  await shot("04-overlay-running");
  await clickByText(page, ".lov-actions button", "숨기기");
  await sleep(700);
  await shot("05-home-running");

  console.log("FLOW OK");
} finally {
  await browser.close();
  srv.close();
}
