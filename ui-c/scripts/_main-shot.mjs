import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const URL = "http://127.0.0.1:4173";
const OUT = "C:\\Users\\jin14\\AppData\\Local\\Temp\\opencode";

const clickByText = (page, sel, text) =>
  page.evaluate((s, t) => {
    const el = [...document.querySelectorAll(s)].find((e) => e.textContent.trim().includes(t));
    if (!el) throw new Error(`not found: ${s} "${t}"`);
    el.click();
  }, sel, text);

const shot = async (page, name) => {
  await page.screenshot({ path: `${OUT}\\main-${name}.png` });
  console.log("shot:", name);
};

const browser = await puppeteer.launch({
  executablePath: EDGE, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1"],
  defaultViewport: { width: 1600, height: 1000 },
});
try {
  const page = await browser.newPage();
  page.on("pageerror", (e) => console.log("PAGE ERROR:", e.message));
  await page.goto(URL, { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(1800);
  await shot(page, "00-home");
  for (const [ko, name] of [["인스턴스","01-instances"],["모드","02-mods"],["코스메틱","03-cosmetics"],["계정","04-accounts"]]) {
    await clickByText(page, ".sidebar-item", ko);
    await sleep(1000);
    await shot(page, name);
  }
  console.log("MAIN OK");
} finally { await browser.close(); }
