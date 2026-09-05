/* backview.mjs — cosmetics back view with cape equipped AND unequipped:
   the torso back is only visible without the cape (review gate #1). */
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const OUT = process.env.OUT_DIR || "/tmp/fm-backview";
const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--no-sandbox"],
  defaultViewport: { width: 1600, height: 1000 },
});
const page = await browser.newPage();
await page.goto("http://127.0.0.1:5921", { waitUntil: "networkidle0", timeout: 20000 });
await sleep(2200);
await page.evaluate(() => {
  const b = [...document.querySelectorAll(".sidebar-item")].find((x) => x.textContent.includes("코스메틱"));
  b?.click();
});
await sleep(1200);
await page.screenshot({ path: `${OUT}/backview-equipped.png` });
await page.evaluate(() => document.querySelector(".cos-unequip")?.click());
await sleep(700);
await page.screenshot({ path: `${OUT}/backview-unequipped.png` });
console.log("shots done");
await browser.close();
