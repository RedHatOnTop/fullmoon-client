/* appshot.mjs — drive the REAL Tauri build and shoot each screen.

   The launcher must already be running with
   WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS=--remote-debugging-port=9333
   so the WebView2 page is reachable over CDP. Unlike devshot.mjs (which
   shoots the browser build on MockCore) every number in these shots comes
   from the Rust core. */
import { mkdirSync, writeFileSync } from "node:fs";
import puppeteer from "puppeteer-core";

const OUT = process.env.SHOT_DIR ?? ".";
const PORT = process.env.CDP_PORT ?? "9333";
mkdirSync(OUT, { recursive: true });

const browser = await puppeteer.connect({
  browserURL: `http://127.0.0.1:${PORT}`,
  defaultViewport: null,
});

const pages = await browser.pages();
const page = pages.find((p) => p.url().includes("4173")) ?? pages[0];
const shot = async (name) => {
  const buf = await page.screenshot({ type: "png" });
  writeFileSync(`${OUT}/${name}.png`, buf);
  console.log("shot", name);
};
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

/* the sidebar is the only navigation the shots need; click by its label */
const nav = async (label) => {
  const ok = await page.evaluate((want) => {
    const el = [...document.querySelectorAll(".nav-item, .navitem, nav button, aside button")].find(
      (b) => b.textContent?.trim() === want,
    );
    if (!el) return false;
    el.click();
    return true;
  }, label);
  if (!ok) throw new Error(`nav target not found: ${label}`);
  await wait(700);
};

const probe = async (cmd, args = {}) =>
  page.evaluate(
    async (c, a) => {
      try {
        return { ok: true, value: await window.__TAURI_INTERNALS__.invoke(c, a) };
      } catch (e) {
        return { ok: false, error: String(e) };
      }
    },
    cmd,
    args,
  );

await wait(1200);
await shot("app-00-home");

for (const [label, name] of [
  ["인스턴스", "app-01-instances"],
  ["모드", "app-02-mods"],
  ["코스메틱", "app-03-cosmetics"],
  ["계정", "app-04-accounts"],
  ["콘솔", "app-05-console"],
]) {
  await nav(label);
  await shot(name);
}

/* create an instance through the UI it ships with — the round trip has to
   land on disk, not in React state */
if (process.env.MAKE_INSTANCE) {
  await nav("인스턴스");
  await page.evaluate(() => {
    const b = [...document.querySelectorAll("button")].find((x) =>
      x.textContent?.includes("새 인스턴스"),
    );
    b?.click();
  });
  await wait(500);
  await page.type(".modal input.input", process.env.MAKE_INSTANCE);
  await wait(200);
  await shot("app-06-create-dialog");
  await page.evaluate(() => {
    const b = [...document.querySelectorAll(".modal-actions button")].pop();
    b?.click();
  });
  await wait(1200);
  await shot("app-07-instance-made");
}

/* the core answers these directly — proof the shots above are not mock data */
const facts = {
  java: await probe("java_detect"),
  versions: await probe("versions_manifest"),
  settings: await probe("settings_get"),
  instances: await probe("instances_list"),
  install: await probe("instance_install", { id: "nope" }),
};
writeFileSync(`${OUT}/core-facts.json`, JSON.stringify(facts, null, 2));
console.log(
  "java:",
  facts.java.ok ? facts.java.value.length : facts.java.error,
  "| versions:",
  facts.versions.ok ? facts.versions.value.length : facts.versions.error,
  "| install:",
  facts.install.ok ? "ok" : facts.install.error,
);

browser.disconnect();
