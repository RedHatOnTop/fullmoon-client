/* install-drive.mjs — press 설치 in the real app and watch the core work.

   Drives the shipped UI rather than invoking the command directly: the point
   is to prove the button, the event stream and the progress bar, not just the
   Rust function. Shots land at each stage change. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const PORT = process.env.CDP_PORT ?? "9333";
const LIMIT_MS = Number(process.env.LIMIT_MS ?? 900_000);
mkdirSync(OUT, { recursive: true });

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${PORT}`,
  defaultViewport: null,
});
const pages = await browser.pages();
const page = pages.find((p) => p.url().includes("4173")) ?? pages[0];
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (name) => {
  writeFileSync(`${OUT}/${name}.png`, await page.screenshot({ type: "png" }));
  console.log("shot", name);
};

/* Observation is the rendered card, not the event stream: if the bar does not
   move on screen the feature is not working, whatever the core emits. */
const readStage = () =>
  page.evaluate(() => {
    const card = document.querySelector(".inst-card");
    const busy = document.querySelector(".inst-installing-row");
    return {
      busy: busy?.textContent?.trim() ?? null,
      badge: card?.querySelector(".badge")?.textContent?.trim() ?? null,
      card: card?.textContent?.replace(/\s+/g, " ").trim().slice(0, 120) ?? null,
      toasts: [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()),
    };
  });

await page.evaluate(() => {
  const b = [...document.querySelectorAll("aside button, nav button")].find(
    (x) => x.textContent?.trim() === "인스턴스",
  );
  b?.click();
});
await wait(600);

const pressed = await page.evaluate(() => {
  const b = [...document.querySelectorAll(".inst-actions button")].find((x) =>
    x.textContent?.includes("설치"),
  );
  if (!b) return false;
  b.click();
  return true;
});
if (!pressed) throw new Error("install button not found");
console.log("install pressed");

const started = Date.now();
const seen = [];
let last = "";
let shots = 0;
while (Date.now() - started < LIMIT_MS) {
  await wait(800);
  const s = await readStage();
  const key = s.busy ?? s.badge ?? "";
  if (key && key !== last) {
    last = key;
    seen.push({ ...s, t: Date.now() - started });
    console.log(`ui → ${key}`);
    if (shots < 8) {
      await shot(`install-${String(++shots).padStart(2, "0")}`);
    }
  }
  if (s.toasts.some((t) => t?.includes("not implemented") || t?.includes("HTTP"))) {
    await shot("install-error");
    console.log("ERROR TOAST:", s.toasts.join(" | "));
    break;
  }
  if (!s.busy && s.badge && !s.badge.includes("미설치")) break;
}

await wait(1200);
await shot("install-final");
writeFileSync(`${OUT}/install-ui.json`, JSON.stringify(seen, null, 1));
console.log("ui states seen:", seen.length, "| final:", JSON.stringify(await readStage()));

browser.disconnect();
