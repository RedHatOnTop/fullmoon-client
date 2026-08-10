import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";
const OUT = process.env.SHOT_DIR ?? ".";
mkdirSync(OUT, { recursive: true });
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = async (n) => writeFileSync(`${OUT}/${n}.png`, await page.screenshot({ type: "png" }));
const clickText = async (text) =>
  page.evaluate((t) => {
    const el = [...document.querySelectorAll("button, a")].find((b) => b.textContent?.trim() === t);
    el?.click();
    return !!el;
  }, text);
await clickText("코스메틱");
await sleep(1200);
await shot("cos-cape");
console.log("wings tab:", await clickText("날개"));
await sleep(900);
await shot("cos-wings");
await browser.disconnect();
