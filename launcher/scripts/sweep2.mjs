/* sweep2.mjs — theme (light) + locale (en) variants. */
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
  await sleep(2200);
  const shot = async (n) => { await page.screenshot({ path: `${OUT}\\v-${n}.png` }); console.log("shot:", n); };

  // light theme
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(800);
  await clickByText(page, ".set-nav button", "외관");
  await sleep(500);
  await clickByText(page, ".segmented-item", "라이트");
  await sleep(600);
  await shot("01-settings-light");
  await clickByText(page, ".sidebar-item", "홈");
  await sleep(1400);
  await shot("02-home-light");

  // back to dark, switch to English
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(700);
  await clickByText(page, ".set-nav button", "외관");
  await sleep(400);
  await clickByText(page, ".segmented-item", "다크");
  await sleep(400);
  await clickByText(page, ".segmented-item", "English");
  await sleep(700);
  await shot("03-settings-en");
  await clickByText(page, ".sidebar-item", "Home");
  await sleep(1400);
  await shot("04-home-en");
  await clickByText(page, ".sidebar-item", "Instances");
  await sleep(900);
  await shot("05-instances-en");
  await clickByText(page, ".sidebar-item", "Cosmetics");
  await sleep(1400);
  await shot("06-cosmetics-en");

  console.log("VARIANT SWEEP OK");
} finally {
  await browser.close();
  srv.close();
}
