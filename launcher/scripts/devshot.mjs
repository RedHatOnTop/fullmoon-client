/* devshot.mjs — shoot the LIVE dev server on :4173 (HMR) instead of
   spinning a second static server, so it can't fight shot.mjs for the port. */
import puppeteer from "puppeteer-core";
import { setTimeout as sleep } from "node:timers/promises";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUT = "C:\\Users\\jin14\\AppData\\Local\\Temp\\opencode";

const clickByText = (page, sel, text) =>
  page.evaluate((s, t) => {
    const el = [...document.querySelectorAll(s)].find((e) => e.textContent.trim().includes(t));
    if (!el) throw new Error(`not found: ${s} "${t}"`);
    el.click();
  }, sel, text);

const browser = await puppeteer.launch({
  executablePath: EDGE, headless: "new",
  args: ["--window-size=1600,1000", "--force-device-scale-factor=1", "--use-gl=angle", "--use-angle=swiftshader"],
  defaultViewport: { width: 1600, height: 1000 },
});
try {
  const page = await browser.newPage();
  page.on("pageerror", (e) => console.log("PAGE ERROR:", e.message));
  await page.goto("http://127.0.0.1:4173", { waitUntil: "networkidle0", timeout: 20000 });
  await sleep(2600);
  const shot = async (n) => { await page.screenshot({ path: `${OUT}\\dev-${n}.png` }); console.log("shot:", n); };
  await shot("00-home");

  await clickByText(page, ".sidebar-item", "모드");
  await sleep(1000);
  await shot("01-mods");

  /* a fresh puppeteer profile has no favourites, so the starred state never
     appears unless we star one first */
  await page.evaluate(() => document.querySelectorAll(".mod-fav")[1]?.click());
  await sleep(700);
  await clickByText(page, ".mod-tab", "즐겨찾기");
  await sleep(600);
  await shot("01b-mods-fav");
  await clickByText(page, ".mod-tab", "전체");
  await sleep(400);

  await clickByText(page, ".sidebar-item", "인스턴스");
  await sleep(800);
  await shot("02-instances");

  await clickByText(page, ".sidebar-item", "코스메틱");
  await sleep(1600);
  await shot("03-cosmetics");

  await clickByText(page, ".sidebar-item", "설정");
  await sleep(800);
  await clickByText(page, ".set-nav button", "외관");
  await sleep(500);
  await shot("04-appearance");

  await clickByText(page, ".segmented-item", "라이트");
  await sleep(600);
  await clickByText(page, ".sidebar-item", "홈");
  await sleep(1500);
  await shot("05-home-light");

  console.log("DEVSHOT OK");
} finally {
  await browser.close();
}
