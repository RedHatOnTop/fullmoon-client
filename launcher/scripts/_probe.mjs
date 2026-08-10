import puppeteer from "puppeteer-core";
const b = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const p = (await b.pages()).find((x) => x.url().includes("4173"));
console.log("page:", p ? p.url() : "NONE");
if (p) console.log(await p.evaluate(() => ({
  screens: document.querySelectorAll("aside button, nav button").length,
  accounts: [...document.querySelectorAll(".acc-card")].map((c) => c.querySelector("strong")?.textContent),
  err: document.body.innerText.slice(0, 60),
})));
b.disconnect();
