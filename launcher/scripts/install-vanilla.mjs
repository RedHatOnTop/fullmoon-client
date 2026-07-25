/* install-vanilla.mjs — create a vanilla instance of an uncached version and
   watch the progress UI while it downloads for real. Proves the event path
   (core → install://stage → store → card) and the no-Fabric branch. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const VERSION = process.env.VERSION ?? "1.21.11";
const LIMIT_MS = Number(process.env.LIMIT_MS ?? 840_000);
mkdirSync(OUT, { recursive: true });

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${process.env.CDP_PORT ?? "9333"}`,
  defaultViewport: null,
});
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (n) => writeFileSync(`${OUT}/${n}.png`, await page.screenshot({ type: "png" }));

await page.evaluate(() => {
  [...document.querySelectorAll("aside button, nav button")]
    .find((x) => x.textContent?.trim() === "인스턴스")
    ?.click();
});
await wait(600);
await page.evaluate(() => {
  [...document.querySelectorAll("button")].find((x) => x.textContent?.includes("새 인스턴스"))?.click();
});
await wait(500);
await page.type(".modal input.input", `바닐라 ${VERSION}`);
await page.evaluate((v) => {
  [...document.querySelectorAll(".version-chip")]
    .find((c) => c.textContent?.trim().startsWith(v))
    ?.click();
  [...document.querySelectorAll(".segmented button")].find((b) => b.textContent === "Vanilla")?.click();
}, VERSION);
await wait(300);
await shot("van-00-dialog");
await page.evaluate(() => [...document.querySelectorAll(".modal-actions button")].pop()?.click());
console.log("create pressed");

const read = () =>
  page.evaluate(() => {
    const cards = [...document.querySelectorAll(".inst-card")];
    const busy = cards.find((c) => c.querySelector(".inst-installing-row"));
    const row = busy?.querySelector(".inst-installing-row")?.textContent?.replace(/\s+/g, " ").trim();
    const bar = busy?.querySelector(".progress-fill, .bar-fill, [style*='width']");
    return {
      row: row ?? null,
      width: bar?.getAttribute("style") ?? null,
      toasts: [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()),
      cards: cards.length,
    };
  });

const seen = [];
const started = Date.now();
let last = "";
let shots = 0;
while (Date.now() - started < LIMIT_MS) {
  await wait(1000);
  const s = await read();
  if (s.row && s.row !== last) {
    last = s.row;
    seen.push({ ...s, t: Math.round((Date.now() - started) / 1000) });
    console.log(`ui → ${s.row}  ${s.width ?? ""}`);
    if (shots < 10) await shot(`van-${String(++shots).padStart(2, "0")}`);
  }
  const bad = s.toasts.find((t) => t && /HTTP|checksum|not implemented|없|실패/.test(t));
  if (bad) {
    await shot("van-error");
    console.log("ERROR TOAST:", bad);
    break;
  }
  if (!s.row && seen.length > 0) break;
}
await wait(1000);
await shot("van-final");
writeFileSync(`${OUT}/van-ui.json`, JSON.stringify(seen, null, 1));
console.log("progress rows seen:", seen.length);
browser.disconnect();
