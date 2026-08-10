/* Drive the launcher's own core to start the fabric instance on the local
   server, the same path a user takes. Prints the pid the core reports. */
import puppeteer from "puppeteer-core";
const browser = await puppeteer.connect({ browserURL: "http://127.0.0.1:9333", defaultViewport: null });
const page = (await browser.pages()).find((p) => p.url().includes("4173"));

const out = await page.evaluate(async (server) => {
  const call = (cmd, args) => window.__TAURI_INTERNALS__.invoke(cmd, args);
  const instances = await call("instances_list");
  const inst = instances.find((i) => i.loader === "fabric" && i.installed);
  if (!inst) return { error: "no installed fabric instance", instances };
  const pid = await call("launch_quickplay", { instanceId: inst.id, server });
  return { instance: inst.name, pid };
}, process.env.SERVER ?? "127.0.0.1:25565");

console.log(JSON.stringify(out, null, 2));
await browser.disconnect();
