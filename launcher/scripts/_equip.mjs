import puppeteer from "puppeteer-core";
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));
const invoke = (c, a = {}) => page.evaluate((c, a) => window.__TAURI_INTERNALS__.invoke(c, a), c, a);
const uuid = (await invoke("auth_list"))[0].uuid;
const id = process.argv[2] === "none" ? null : process.argv[2];
await invoke("cosmetics_equip", { uuid, slot: "cape", itemId: id });
console.log("equipped", JSON.stringify(await invoke("cosmetics_equipped", { uuid })));
await browser.disconnect();
