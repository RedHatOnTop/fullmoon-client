/* _sitejs.mjs — the site's interaction layer, checked on the real page.
   Two things it proves that a screenshot cannot:
     1. site.js runs at all (it gates itself on html.js) and the scroll reveal
        hands the content back — a reveal that never fires leaves the page at
        opacity 0 while the DOM looks perfect.
     2. nothing overflows the viewport. html/body carry `overflow-x: clip`,
        which clamps scrollWidth, so an h-scroll probe reads clean even when a
        child sticks out. Measured per element with getBoundingClientRect
        instead. */
import puppeteer from "puppeteer-core";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const SITE = "file:///home/person/Projects/minecraft-server-project/fullmoon-client/site";
const PAGES = (process.env.PAGES || "index,launcher,mod,bridge,trust").split(",");
const WIDTHS = (process.env.WIDTHS || "1440,768,320").split(",").map(Number);

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: "new",
  args: ["--no-sandbox", "--allow-file-access-from-files"],
});
let bad = 0;
try {
  const page = await browser.newPage();
  page.on("pageerror", (e) => {
    console.log("PAGE ERROR:", e.message);
    bad++;
  });
  for (const width of WIDTHS) {
    await page.setViewport({ width, height: 900 });
    for (const name of PAGES) {
      await page.goto(`${SITE}/${name}.html`, { waitUntil: "load" });
      await new Promise((r) => setTimeout(r, 1400));
      const r = await page.evaluate((w) => {
        /* An element wider than the viewport is only a defect when nothing
           between it and <body> can be scrolled to reach it — html and body
           are `overflow-x: clip`, so anything that gets there is cut off for
           good. Decoration is allowed to bleed; text is not. */
        const scrolls = (el) => {
          const o = getComputedStyle(el).overflowX;
          return (o === "auto" || o === "scroll") && el.scrollWidth > el.clientWidth + 1;
        };
        const reachable = (el) => {
          for (let a = el.parentElement; a && a !== document.body; a = a.parentElement) {
            if (scrolls(a)) return true;
          }
          return false;
        };
        const over = [];
        for (const el of document.querySelectorAll("body *")) {
          const b = el.getBoundingClientRect();
          if (b.width === 0 && b.height === 0) continue;
          if (b.right <= w + 1 && b.left >= -1) continue;
          if (reachable(el)) continue;
          if (el.dataset.bleed !== undefined) continue;
          over.push(`${el.tagName.toLowerCase()}.${el.className || "-"} ${Math.round(b.left)}..${Math.round(b.right)}`);
        }
        const first = document.querySelector("main > section, .page-head");
        return {
          gate: document.documentElement.className,
          progress: !!document.querySelector(".progress"),
          firstOpacity: first ? getComputedStyle(first).opacity : "n/a",
          copyButtons: document.querySelectorAll(".pre-wrap .copy").length,
          over: over.slice(0, 4),
          overCount: over.length,
        };
      }, width);
      const ok = r.gate === "js" && r.progress && r.firstOpacity === "1" && r.overCount === 0;
      if (!ok) bad++;
      console.log(
        `${ok ? "ok  " : "BAD "} ${String(width).padStart(4)}px ${name.padEnd(9)} ` +
          `gate=${r.gate} progress=${r.progress} opacity=${r.firstOpacity} ` +
          `copy=${r.copyButtons} overflow=${r.overCount}${r.overCount ? " " + r.over.join(" | ") : ""}`,
      );
    }
  }
} finally {
  await browser.close();
}
console.log(bad === 0 ? "site interaction layer: all clean" : `site interaction layer: ${bad} failure(s)`);
process.exit(bad === 0 ? 0 : 1);
