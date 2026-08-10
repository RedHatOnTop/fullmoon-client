import { writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
await new Promise((r) => setTimeout(r, 1500));
writeFileSync(process.env.OUT, await page.screenshot({ type: "png" }));
console.log("title:", await page.title());
console.log("brand:", await page.evaluate(() => __BRAND__));
await browser.disconnect();
