import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";
const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--no-sandbox"],
  defaultViewport: { width: 1600, height: 1000 },
});
const page = await browser.newPage();
await page.goto("http://127.0.0.1:5921", { waitUntil: "networkidle0", timeout: 20000 });
await sleep(2500);
// scroll content to top just in case, then crop the topbar strip
await page.screenshot({ path: "/tmp/fm-before/probe-topbar-only.png", clip: { x: 216, y: 40, width: 700, height: 90 } });
// also check: is the h1 painted? sample pixels
const px = await page.evaluate(() => {
  const h1 = document.querySelector(".topbar-heading h1");
  const b = h1.getBoundingClientRect();
  const el = document.elementFromPoint(b.x + 5, b.y + 15);
  return { hit: el?.tagName + "." + el?.className, rect: { x: b.x, y: b.y } };
});
console.log(JSON.stringify(px));
await browser.close();
