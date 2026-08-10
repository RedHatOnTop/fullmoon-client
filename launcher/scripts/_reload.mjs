import puppeteer from "puppeteer-core";
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
await page.reload({ waitUntil: "networkidle2" });
console.log("reloaded", await page.title());
await browser.disconnect();
