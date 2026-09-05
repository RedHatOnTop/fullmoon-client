/* viewports.mjs — fold + narrow-window checks (Hallmark 34/44/49) */
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const URL = "http://127.0.0.1:5921";
const OUT = process.env.OUT_DIR || "/tmp/fm-after";

const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--no-sandbox", "--force-device-scale-factor=1"],
});

async function shotAt(name, width, height, nav) {
  const page = await browser.newPage();
  await page.setViewport({ width, height });
  await page.goto(URL, { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(2200);
  if (nav) {
    await page.evaluate((t) => {
      const b = [...document.querySelectorAll(".sidebar-item")].find((x) => x.textContent.includes(t));
      b?.click();
    }, nav);
    await sleep(900);
  }
  await page.screenshot({ path: `${OUT}/fullmoon-launcher-${name}.png` });
  const scrollX = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  console.log("shot:", name, `${width}x${height}`, scrollX ? "H-SCROLL!" : "no-h-scroll");
  await page.close();
}

await shotAt("20-fold-1280", 1280, 800, null);
await shotAt("21-fold-1280-dash", 1280, 800, "대시보드");
await shotAt("22-narrow-1100", 1100, 800, null);
await shotAt("23-narrow-1100-dash", 1100, 800, "대시보드");

await browser.close();
console.log("VIEWPORT CAPTURES DONE");
