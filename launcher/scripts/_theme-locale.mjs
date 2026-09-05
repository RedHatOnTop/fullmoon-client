/* theme-locale.mjs — capture the launcher in light theme and in English,
   seeded through the mock core's own persistence blob. */
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const URL = "http://127.0.0.1:5921";
const OUT = process.env.OUT_DIR || "/tmp/fm-after";

const BASE_SETTINGS = {
  javaPath: null,
  javaArgs: "-XX:+UseG1GC",
  memoryMb: 4096,
  concurrency: 8,
  theme: "dark",
  accent: "#F5D06E",
  language: "ko",
  telemetry: false,
};

const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--no-sandbox"],
  defaultViewport: { width: 1600, height: 1000 },
});

async function capture(suffix, patch) {
  const page = await browser.newPage();
  await page.evaluateOnNewDocument(
    (settings) => {
      /* only the settings key — omitted keys fall back to the mock's seeds */
      localStorage.setItem("pinion.v1.state", JSON.stringify({ settings }));
    },
    { ...BASE_SETTINGS, ...patch },
  );
  await page.goto(URL, { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(2200);
  await page.screenshot({ path: `${OUT}/fullmoon-launcher-${suffix}.png` });
  console.log("shot:", suffix);
  await page.evaluate(() => {
    const b = [...document.querySelectorAll(".sidebar-item")].find(
      (x) => x.textContent.includes("Dashboard") || x.textContent.includes("대시보드"),
    );
    b?.click();
  });
  await sleep(900);
  await page.screenshot({ path: `${OUT}/fullmoon-launcher-${suffix}-dash.png` });
  console.log("shot:", suffix + "-dash");
  await page.close();
}

await capture("10-light-play", { theme: "light" });
await capture("11-en-play", { language: "en" });

await browser.close();
console.log("THEME/LOCALE CAPTURES DONE");
