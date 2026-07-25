/* make-icon.mjs — rasterises the brand mark to a 1024px PNG that
   `cargo tauri icon` then expands into the platform icon set.
   The mark is the same feather-in-a-square the favicon uses; brand.json
   supplies the colour so a rebrand does not need a new asset. */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import puppeteer from "puppeteer-core";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const here = dirname(fileURLToPath(import.meta.url));
const brand = JSON.parse(readFileSync(resolve(here, "../../brand.json"), "utf8"));
const out = resolve(here, "../src-tauri/icon-src.png");

const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="1024" height="1024">
  <rect width="32" height="32" rx="6" fill="${brand.accent}"/>
  <path d="M22.5 6.5c-4.8.4-8.4 3.8-9.6 8.4l-2 3.1 2.4 2.4 3.1-2c4.6-1.2 8-4.8 8.4-9.6l-2.5 1.3c.3-1.2.4-2.4.1-3.6z" fill="#F7F8F5"/>
</svg>`;

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: "new",
  args: ["--force-device-scale-factor=1"],
  defaultViewport: { width: 1024, height: 1024, deviceScaleFactor: 1 },
});
try {
  const page = await browser.newPage();
  await page.setContent(
    `<body style="margin:0;background:transparent">${svg}</body>`,
    { waitUntil: "load" },
  );
  const buf = await page.screenshot({ omitBackground: true, type: "png" });
  writeFileSync(out, buf);
  console.log("wrote", out);
} finally {
  await browser.close();
}
