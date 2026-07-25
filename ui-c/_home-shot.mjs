import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const DIST = process.argv[2];
const OUTNAME = process.argv[3];
const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUT = "C:\\Users\\jin14\\AppData\\Local\\Temp\\opencode";
const MIME = { ".html":"text/html", ".js":"text/javascript", ".css":"text/css", ".png":"image/png", ".svg":"image/svg+xml", ".json":"application/json", ".woff2":"font/woff2" };

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
  await page.screenshot({ path: `${OUT}\\${OUTNAME}.png` });
  console.log("shot:", OUTNAME);
} finally { await browser.close(); srv.close(); }
