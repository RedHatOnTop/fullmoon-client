import pngjs from "pngjs";
import { readFileSync, writeFileSync } from "node:fs";
const { PNG } = pngjs;
const ids = ["aero-cape","ember-cape","void-cape","mint-cape","regal-cape"];
const S = 14, W = ids.length * (10 * S + 12) + 12, H = 16 * S + 24;
const out = new PNG({ width: W, height: H });
for (let i = 0; i < out.data.length; i += 4) { out.data[i]=18; out.data[i+1]=18; out.data[i+2]=22; out.data[i+3]=255; }
ids.forEach((id, n) => {
  const src = PNG.sync.read(readFileSync(`public/capes/${id}.png`));
  const ox = 12 + n * (10 * S + 12), oy = 12;
  for (let v = 0; v < 16; v++) for (let u = 0; u < 10; u++) {
    const si = (((1 + v) * 64) + (1 + u)) * 4;
    for (let dy = 0; dy < S; dy++) for (let dx = 0; dx < S; dx++) {
      const di = (((oy + v * S + dy) * W) + ox + u * S + dx) * 4;
      out.data[di] = src.data[si]; out.data[di+1] = src.data[si+1];
      out.data[di+2] = src.data[si+2]; out.data[di+3] = 255;
    }
  }
});
writeFileSync(process.env.OUT, PNG.sync.write(out));
console.log("ok");
