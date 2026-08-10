import pngjs from "pngjs";
import { readFileSync, writeFileSync } from "node:fs";
const { PNG } = pngjs;
const [src, dst, X, Y, W, H, S] = [process.env.SRC, process.env.DST,
  +process.env.X, +process.env.Y, +process.env.W, +process.env.H, +(process.env.S ?? 4)];
const img = PNG.sync.read(readFileSync(src));
const out = new PNG({ width: W * S, height: H * S });
for (let y = 0; y < H * S; y++) for (let x = 0; x < W * S; x++) {
  const si = (((Y + (y / S | 0)) * img.width) + X + (x / S | 0)) * 4;
  const di = ((y * W * S) + x) * 4;
  out.data[di] = img.data[si]; out.data[di+1] = img.data[si+1];
  out.data[di+2] = img.data[si+2]; out.data[di+3] = 255;
}
writeFileSync(dst, PNG.sync.write(out));
console.log("crop", W * S, "x", H * S);
