/* design-sweep.mjs — every remaining state that hasn't been
   visually verified: settings tabs, dialogs, dock menus,
   AMOLED, English, empty states, trail render. */

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
  await page.screenshot({ path: `${OUT}\\ds-${name}.png` });
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
  await page.goto(URL, { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(1600);

  // console idle (empty state) — before any launch
  await clickByText(page, ".sidebar-item", "콘솔");
  await sleep(600);
  await shot(page, "01-console-idle");

  // settings: java / perf / appearance / privacy / about
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(1000);
  await shot(page, "02-set-java");
  await clickByText(page, ".set-nav button", "성능");
  await sleep(400);
  await shot(page, "03-set-perf");
  await clickByText(page, ".set-nav button", "외관");
  await sleep(400);
  await shot(page, "04-set-look");
  await clickByText(page, ".set-nav button", "프라이버시");
  await sleep(400);
  await shot(page, "05-set-privacy");
  await clickByText(page, ".set-nav button", "정보");
  await sleep(400);
  await shot(page, "06-set-about");

  // AMOLED theme → home
  await clickByText(page, ".set-nav button", "외관");
  await sleep(300);
  await clickByText(page, ".segmented-item", "AMOLED");
  await sleep(400);
  await clickByText(page, ".sidebar-item", "홈");
  await sleep(700);
  await shot(page, "07-home-amoled");
  // back to dark
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(400);
  await clickByText(page, ".set-nav button", "외관");
  await sleep(300);
  await clickByText(page, ".segmented-item", "다크");
  await sleep(300);

  // create instance dialog
  await clickByText(page, ".sidebar-item", "인스턴스");
  await sleep(500);
  await clickByText(page, ".page-head button", "새 인스턴스");
  await sleep(600);
  await shot(page, "08-create-dialog");
  await page.keyboard.press("Escape");
  await sleep(300);

  // add-account chooser
  await clickByText(page, ".sidebar-item", "계정");
  await sleep(500);
  await clickByText(page, ".page-head button", "계정 추가");
  await sleep(500);
  await shot(page, "09-add-account");
  await page.keyboard.press("Escape");
  await sleep(300);

  // delete confirm
  await page.evaluate(() => {
    const card = [...document.querySelectorAll(".acc-card")][1];
    card.querySelector(".iconbtn-danger").click();
  });
  await sleep(500);
  await shot(page, "10-confirm");
  await page.keyboard.press("Escape");
  await sleep(300);

  // dock menus
  await page.evaluate(() => document.querySelectorAll(".dock-chip")[0].click());
  await sleep(400);
  await shot(page, "11-dock-accounts");
  await page.evaluate(() => document.querySelectorAll(".dock-chip")[1].click());
  await sleep(400);
  await shot(page, "12-dock-instances");

  // cosmetics: trail equip
  await clickByText(page, ".sidebar-item", "코스메틱");
  await sleep(600);
  await clickByText(page, ".segmented-item", "궤적");
  await sleep(400);
  await page.evaluate(() => document.querySelector(".cos-item")?.click());
  await sleep(1200);
  await shot(page, "13-trail");

  // English locale
  await clickByText(page, ".sidebar-item", "설정");
  await sleep(400);
  await clickByText(page, ".set-nav button", "외관");
  await sleep(300);
  await clickByText(page, ".segmented-item", "English");
  await sleep(500);
  await clickByText(page, ".sidebar-item", "Home");
  await sleep(700);
  await shot(page, "14-home-en");

  console.log("SWEEP OK");
} finally {
  await browser.close();
}
