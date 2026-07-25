/* import-session-drive.mjs — re-import the official launcher's profiles and
   prove what an imported account can and cannot do: it appears as a real
   account, and pressing 플레이 with it selected fails before anything spawns,
   with the reason, because the Store launcher hands over no session. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const WHO = process.env.WHO ?? "Person1010";
mkdirSync(OUT, { recursive: true });

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${process.env.CDP_PORT ?? "9333"}`,
  defaultViewport: null,
});
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (n) => writeFileSync(`${OUT}/${n}.png`, await page.screenshot({ type: "png" }));
const toasts = () =>
  page.evaluate(() => [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()));
const nav = async (label) => {
  await page.evaluate((want) => {
    [...document.querySelectorAll("aside button, nav button")]
      .find((b) => b.textContent?.trim() === want)
      ?.click();
  }, label);
  await wait(700);
};
const names = () =>
  page.evaluate(() =>
    [...document.querySelectorAll(".acc-card")].map((c) => c.querySelector("strong")?.textContent?.trim()),
  );

await nav("계정");

// ── drop the earlier import so the fixed path runs from scratch ──
const removed = await page.evaluate((who) => {
  const card = [...document.querySelectorAll(".acc-card")].find((c) => c.textContent?.includes(who));
  const trash = [...(card?.querySelectorAll("button") ?? [])].find((b) =>
    b.getAttribute("aria-label")?.includes("삭제"),
  );
  trash?.click();
  return !!trash;
}, WHO);
if (removed) {
  await wait(500);
  await page.evaluate(() => {
    const modal = document.querySelector(".modal, [role=dialog]");
    [...(modal?.querySelectorAll("button") ?? [])].filter((b) => b.textContent?.includes("삭제")).pop()?.click();
  });
  await wait(900);
}
console.log("after remove:", JSON.stringify(await names()));

// ── import again ────────────────────────────────────────────────
await page.evaluate(() => {
  [...document.querySelectorAll(".screen-pad button")].find((b) => b.textContent?.includes("계정 추가"))?.click();
});
await wait(700);
await page.evaluate(() => {
  [...document.querySelectorAll(".add-option")].find((b) => b.textContent?.includes("공식 런처"))?.click();
});
await wait(2200);
console.log("import toast:", JSON.stringify(await toasts()));
console.log("after import:", JSON.stringify(await names()));
await shot("import-00-accounts");

// ── make it active, then try to play ────────────────────────────
await page.evaluate((who) => {
  const card = [...document.querySelectorAll(".acc-card")].find((c) => c.textContent?.includes(who));
  [...(card?.querySelectorAll("button") ?? [])].find((b) => b.textContent?.includes("전환"))?.click();
}, WHO);
await wait(1000);

await nav("인스턴스");
await page.evaluate(() => {
  const card = [...document.querySelectorAll(".inst-card")].find((c) => c.textContent?.includes("깃털"));
  [...(card?.querySelectorAll("button") ?? [])].find((b) => b.textContent?.includes("플레이"))?.click();
});
await wait(3000);
console.log("play toast:", JSON.stringify(await toasts()));
await shot("import-01-play-blocked");

// ── back to the offline profile so the box is left as found ─────
await nav("계정");
await page.evaluate(() => {
  const card = [...document.querySelectorAll(".acc-card")].find((c) => c.textContent?.includes("PinionDev"));
  [...(card?.querySelectorAll("button") ?? [])].find((b) => b.textContent?.includes("전환"))?.click();
});
await wait(800);
await shot("import-02-restored");
browser.disconnect();
