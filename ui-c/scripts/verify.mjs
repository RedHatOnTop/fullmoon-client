/* verify.mjs — drives the standalone build in headless Edge and
   screenshots the full user journey: home → instances → install →
   launch → console → settings/HUD → cosmetics → mods → accounts. */

import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const URL = "http://127.0.0.1:4173";
const OUT = "C:\\Users\\jin14\\AppData\\Local\\Temp\\opencode";

const clickByText = (page, selector, text) =>
  page.evaluate(
    (sel, t) => {
      const el = [...document.querySelectorAll(sel)].find((e) => e.textContent.trim().includes(t));
      if (!el) throw new Error(`not found: ${sel} "${t}"`);
      el.click();
    },
    selector,
    text,
  );

const shot = async (page, name) => {
  await page.screenshot({ path: `${OUT}\\pinion-${name}.png` });
  console.log("shot:", name);
};

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1"],
  defaultViewport: { width: 1600, height: 1000 },
});

try {
  const page = await browser.newPage();
  page.on("pageerror", (e) => console.log("PAGE ERROR:", e.message));
  page.on("console", (m) => {
    if (m.type() === "error") console.log("CONSOLE ERROR:", m.text());
  });

  await page.goto(URL, { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(1800);
  await shot(page, "01-home");

  // instances
  await clickByText(page, ".sidebar-item", "인스턴스");
  await sleep(700);
  await shot(page, "02-instances");

  // install the Snapshot Lab instance
  await page.evaluate(() => {
    const card = [...document.querySelectorAll(".inst-card")].find((c) =>
      c.textContent.includes("Snapshot Lab"),
    );
    const btn = [...card.querySelectorAll("button")].find((b) => b.textContent.includes("설치"));
    btn.click();
  });
  await sleep(3400);
  await shot(page, "03-installing");
  await sleep(9500); // let the install finish

  // back home, PLAY
  await clickByText(page, ".sidebar-item", "홈");
  await sleep(600);
  await page.click(".playbtn");
  await sleep(6500);
  await shot(page, "04-launching");

  // console while running
  await clickByText(page, ".sidebar-item", "콘솔");
  await sleep(5500);
  await shot(page, "05-console-running");

  // settings → HUD editor
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(500);
  await clickByText(page, ".set-nav button", "HUD 모듈");
  await sleep(900);
  await shot(page, "06-settings-hud");

  // cosmetics
  await clickByText(page, ".sidebar-item", "코스메틱");
  await sleep(700);
  // equip a cape + wings for the render
  await page.evaluate(() => {
    const items = [...document.querySelectorAll(".cos-item")];
    items[1]?.click(); // ember cape
  });
  await sleep(500);
  await clickByText(page, ".segmented-item", "날개");
  await sleep(400);
  await page.evaluate(() => {
    const items = [...document.querySelectorAll(".cos-item")];
    items[0]?.click(); // gale wings
  });
  await sleep(900);
  await shot(page, "07-cosmetics");

  // mods
  await clickByText(page, ".sidebar-item", "모드");
  await sleep(800);
  await shot(page, "08-mods");

  // accounts
  await clickByText(page, ".sidebar-item", "계정");
  await sleep(700);
  await shot(page, "09-accounts");

  console.log("VERIFY OK");
} finally {
  await browser.close();
}
