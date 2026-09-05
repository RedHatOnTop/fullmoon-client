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
await sleep(2000);
await page.evaluate(() => {
  const b = [...document.querySelectorAll(".sidebar-item")].find((x) => x.textContent.includes("모드"));
  b?.click();
});
await sleep(900);
await page.screenshot({ path: "/tmp/shader-easy-live.png" });
console.log("shot ok");
await browser.close();
