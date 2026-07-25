/* kill-drive.mjs — press 프로세스 종료 on the console screen, confirm, and
   report what the UI says afterwards. The real proof is the java process being
   gone, which the caller checks. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
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
    .find((b) => b.textContent?.trim() === "콘솔")
    ?.click();
});
await wait(700);
await shot("kill-00-before");

const opened = await page.evaluate(() =>
  !![...document.querySelectorAll(".console-actions button")]
    .find((b) => b.textContent?.includes("프로세스 종료"))
    ?.click() ?? true,
);
console.log("kill button clicked:", opened);
await wait(500);
await shot("kill-01-confirm");

await page.evaluate(() => {
  const modal = document.querySelector(".modal, [role=dialog]");
  [...(modal?.querySelectorAll("button") ?? [])]
    .filter((b) => b.textContent?.includes("종료"))
    .pop()
    ?.click();
});
console.log("confirm clicked");

for (let i = 0; i < 12; i++) {
  await wait(1000);
  const s = await page.evaluate(() => ({
    stats: [...document.querySelectorAll(".console-stat")].map((n) => n.textContent?.replace(/\s+/g, " ").trim()),
    badge: document.querySelector(".console-head .badge, .console-state")?.textContent?.trim() ?? "",
    toasts: [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()),
    overlay: !!document.querySelector(".launch-overlay"),
  }));
  console.log(`t+${i + 1}s badge=${s.badge} overlay=${s.overlay} :: ${s.stats.join(" | ")}`);
  if (s.toasts.length) console.log("  toast:", s.toasts.join(" | "));
  if (/종료|완료|중지|Exited|Stopped/.test(s.badge) || s.stats.some((x) => x?.includes("종료 코드"))) break;
}
await shot("kill-02-after");
browser.disconnect();
