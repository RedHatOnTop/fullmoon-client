import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";
const OUT = process.env.SHOT_DIR ?? ".";
mkdirSync(OUT, { recursive: true });
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (n) => writeFileSync(`${OUT}/${n}.png`, await page.screenshot({ type: "png" }));
const clickText = async (text) =>
  page.evaluate((t) => {
    const el = [...document.querySelectorAll("button, a")].find((b) => b.textContent?.trim().startsWith(t));
    el?.click();
    return !!el;
  }, text);

for (const [label, file] of Object.entries(JSON.parse(process.env.TOUR))) {
  const ok = await clickText(label);
  await sleep(1100);
  await shot(file);
  console.log(file, ok ? "ok" : "NAV MISS");
}
await browser.disconnect();
