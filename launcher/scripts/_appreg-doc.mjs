import puppeteer from "puppeteer-core";
const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const browser = await puppeteer.launch({ executablePath: CHROME, headless: "new", args: ["--no-sandbox", "--disable-http2"] });
const page = await browser.newPage();
await page.goto("https://help.minecraft.net/hc/en-us/articles/16254801392141", { waitUntil: "domcontentloaded", timeout: 45000 });
await new Promise((r) => setTimeout(r, 6000));
const text = await page.evaluate(() => document.querySelector("article")?.innerText ?? document.body.innerText);
console.log(text.slice(0, 3500));
await browser.close();
