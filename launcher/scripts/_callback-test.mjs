import puppeteer from "puppeteer-core";
import http from "node:http";
import { setTimeout as sleep } from "node:timers/promises";

const CHROME = "/home/person/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome";
const server = http.createServer((req, res) => {
  res.writeHead(200, { "content-type": "text/html" });
  res.end("<html><body><h1 id=cb>CALLBACK-RECEIVED</h1></body></html>");
});
await new Promise((r) => server.listen(0, "127.0.0.1", r)); // 런처와 동일: IPv4 루프백만
const port = server.address().port;
console.log("listener on 127.0.0.1:" + port);

const browser = await puppeteer.launch({
  executablePath: CHROME, headless: "new",
  args: ["--no-sandbox", "--force-device-scale-factor=1"],
});
const page = await browser.newPage();
let err = "";
page.on("pageerror", (e) => (err = e.message));
const resp = await page.goto(`http://localhost:${port}/?code=test&state=x`, { timeout: 15000 }).catch((e) => { err = e.message; return null; });
console.log("localhost 로딩:", resp ? `HTTP ${resp.status()} — 도달함` : `실패: ${err}`);
const txt = resp ? await page.evaluate(() => document.getElementById("cb")?.textContent ?? "no-el") : "n/a";
console.log("본문:", txt);

// IPv6 직접 시도도 병행
const resp6 = await page.goto(`http://[::1]:${port}/?code=test`, { timeout: 8000 }).catch((e) => { return null; });
console.log("::1 직접:", resp6 ? "도달(예상외)" : "거부(예상대로 — 런처는 IPv4만 바인드)");

await browser.close();
server.close();
