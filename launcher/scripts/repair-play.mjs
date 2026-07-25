/* repair-play.mjs — press 파일 검증·복구 on the fabric instance (which
   re-extracts natives), then 플레이, and read the console until the game is
   either up or dead. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const LIMIT_MS = Number(process.env.LIMIT_MS ?? 300_000);
mkdirSync(OUT, { recursive: true });

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${process.env.CDP_PORT ?? "9333"}`,
  defaultViewport: null,
});
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (n) => writeFileSync(`${OUT}/${n}.png`, await page.screenshot({ type: "png" }));
const nav = async (label) => {
  await page.evaluate((want) => {
    [...document.querySelectorAll("aside button, nav button")]
      .find((b) => b.textContent?.trim() === want)
      ?.click();
  }, label);
  await wait(600);
};

await nav("인스턴스");
await page.evaluate(() => {
  const card = [...document.querySelectorAll(".inst-card")].find((c) => c.textContent?.includes("깃털"));
  [...(card?.querySelectorAll("button") ?? [])]
    .find((b) => b.getAttribute("aria-label")?.includes("검증") || b.title?.includes("검증"))
    ?.click();
});
console.log("repair pressed");

// wait for the card to stop showing a progress row
for (let i = 0; i < 120; i++) {
  await wait(1000);
  const busy = await page.evaluate(() => !!document.querySelector(".inst-installing-row"));
  if (i > 2 && !busy) break;
}
await shot("repair-00-done");

await page.evaluate(() => {
  const card = [...document.querySelectorAll(".inst-card")].find((c) => c.textContent?.includes("깃털"));
  [...(card?.querySelectorAll("button") ?? [])].find((b) => b.textContent?.includes("플레이"))?.click();
});
console.log("play pressed");

await nav("콘솔");
const started = Date.now();
let seen = 0;
let shots = 0;
while (Date.now() - started < LIMIT_MS) {
  await wait(2500);
  const s = await page.evaluate(() => {
    const rows = [...document.querySelectorAll(".log-line, .console-line, .log-row")];
    return {
      n: rows.length,
      tail: rows.slice(-2).map((r) => r.textContent?.replace(/\s+/g, " ").trim().slice(0, 130)),
      crashed: rows.some((r) => r.textContent?.includes("Minecraft has crashed")),
      window: rows.some((r) => /OpenAL initialized|Created:.*minecraft:textures|Sound engine started/.test(r.textContent ?? "")),
      toasts: [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()),
    };
  });
  if (s.n !== seen) {
    seen = s.n;
    console.log(`lines=${s.n} :: ${s.tail.join(" || ")}`);
    if (shots < 5) await shot(`repair-${String(++shots).padStart(2, "0")}`);
  }
  if (s.toasts.length) console.log("toast:", s.toasts.join(" | "));
  if (s.crashed) {
    console.log("CRASHED");
    break;
  }
  if (s.window) {
    console.log("GAME WINDOW UP");
    break;
  }
}
await shot("repair-final");
browser.disconnect();
