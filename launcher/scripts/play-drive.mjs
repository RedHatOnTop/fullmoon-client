/* play-drive.mjs — add an offline profile, press 플레이, and watch the game
   come up. Evidence is the console screen filling with real game log lines and
   a java process existing, not a green promise. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const NAME = process.env.PLAYER ?? "PinionDev";
const LIMIT_MS = Number(process.env.LIMIT_MS ?? 240_000);
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

// ── offline profile ────────────────────────────────────────────
await nav("계정");
const hasAccount = await page.evaluate(() => document.querySelectorAll(".acc-card").length > 0);
if (!hasAccount) {
  await page.evaluate(() => {
    // several chrome elements say 계정 추가; the screen's own button is the one
    [...document.querySelectorAll(".screen-pad button")]
      .find((b) => b.textContent?.includes("계정 추가"))
      ?.click();
  });
  await wait(800);
  await page.type(".add-offline input.input", NAME);
  await shot("play-00-offline");
  await page.evaluate(() => {
    [...document.querySelectorAll(".add-offline-row button")].pop()?.click();
  });
  await wait(1200);
}
await shot("play-01-accounts");

// ── launch ─────────────────────────────────────────────────────
await nav("인스턴스");
const pressed = await page.evaluate(() => {
  const card = [...document.querySelectorAll(".inst-card")].find((c) =>
    c.textContent?.includes("깃털"),
  );
  const play = [...(card?.querySelectorAll("button") ?? [])].find((b) =>
    b.textContent?.includes("플레이"),
  );
  if (!play) return false;
  play.click();
  return true;
});
if (!pressed) throw new Error("play button not found");
console.log("play pressed");

await nav("콘솔");
const started = Date.now();
let lines = 0;
let shots = 0;
let state = "";
while (Date.now() - started < LIMIT_MS) {
  await wait(2000);
  const s = await page.evaluate(() => ({
    lines: document.querySelectorAll(".log-line, .console-line, .log-row").length,
    state: document.querySelector(".console-state, .game-state, .badge")?.textContent?.trim() ?? "",
    tail: [...document.querySelectorAll(".log-line, .console-line, .log-row")]
      .slice(-3)
      .map((n) => n.textContent?.replace(/\s+/g, " ").trim().slice(0, 120)),
    toasts: [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()),
  }));
  if (s.toasts.length) console.log("toast:", s.toasts.join(" | "));
  if (s.lines !== lines || s.state !== state) {
    lines = s.lines;
    state = s.state;
    console.log(`lines=${lines} state=${state}`);
    console.log("  ", s.tail.join("\n   "));
    if (shots < 6) await shot(`play-${String(++shots + 1).padStart(2, "0")}`);
  }
  if (lines > 40) break;
}
await shot("play-final");
console.log("done. lines:", lines);
browser.disconnect();
