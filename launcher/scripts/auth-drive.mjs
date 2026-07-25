/* auth-drive.mjs — exercise every sign-in route the accounts screen offers and
   report what the UI actually said. Evidence, not promises: the imported
   profiles have to appear as cards, and the Microsoft routes have to fail with
   the configuration sentence rather than a silent nothing. */
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
const toasts = () =>
  page.evaluate(() => [...document.querySelectorAll(".toast")].map((t) => t.textContent?.trim()));
const cards = () =>
  page.evaluate(() =>
    [...document.querySelectorAll(".acc-card")].map((c) => ({
      name: c.querySelector("strong")?.textContent?.trim(),
      badges: [...c.querySelectorAll(".badge")].map((b) => b.textContent?.trim()),
    })),
  );

await page.evaluate(() => {
  [...document.querySelectorAll("aside button, nav button")]
    .find((b) => b.textContent?.trim() === "계정")
    ?.click();
});
await wait(700);
console.log("before:", JSON.stringify(await cards()));

const openAdd = async () => {
  await page.evaluate(() => {
    [...document.querySelectorAll(".screen-pad button")]
      .find((b) => b.textContent?.includes("계정 추가"))
      ?.click();
  });
  await wait(700);
};
const pickOption = async (label) => {
  const hit = await page.evaluate((want) => {
    const btn = [...document.querySelectorAll(".add-option")].find((b) =>
      b.textContent?.includes(want),
    );
    btn?.click();
    return !!btn;
  }, label);
  if (!hit) throw new Error(`add-option not found: ${label}`);
};

// ── 1. import from the official launcher ───────────────────────
await openAdd();
await shot("auth-00-chooser");
await pickOption("공식 런처");
await wait(2500);
console.log("import toast:", JSON.stringify(await toasts()));
console.log("after import:", JSON.stringify(await cards()));
await shot("auth-01-imported");

// ── 2. browser sign-in (auth code) ─────────────────────────────
await openAdd();
await pickOption("브라우저");
await wait(2500);
console.log("browser toast:", JSON.stringify(await toasts()));
await shot("auth-02-browser");
await page.keyboard.press("Escape");
await wait(600);

// ── 3. device code ─────────────────────────────────────────────
await openAdd();
await pickOption("코드");
await wait(3000);
console.log("device toast:", JSON.stringify(await toasts()));
console.log(
  "device code shown:",
  await page.evaluate(() => document.querySelector(".device-code")?.textContent?.trim() ?? null),
);
await shot("auth-03-device");
await page.keyboard.press("Escape");
await wait(600);
await shot("auth-04-final");

browser.disconnect();
