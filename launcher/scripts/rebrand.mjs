/* rebrand.mjs — push brand.json into the two files that cannot read it.

   Everything else already derives the brand at build time: build.rs emits it as
   env constants for Rust, vite injects it as __BRAND__ for the front end. But
   Cargo.toml and tauri.conf.json are parsed before any of our code runs, so the
   binary name, the product name, the bundle identifier and the window title
   have to be written down. This writes them, and nothing else — a rename stays
   one edit to brand.json plus one run of this. */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..", "..");
const brand = JSON.parse(readFileSync(join(root, "brand.json"), "utf8"));

for (const key of ["name", "slug", "bin", "appId"]) {
  if (typeof brand[key] !== "string" || !brand[key].trim()) {
    throw new Error(`brand.json needs a non-empty ${key}`);
  }
}

const changes = [];
const note = (file, what, from, to) => {
  if (from !== to) changes.push(`${file}  ${what}: ${from} -> ${to}`);
};

// ── tauri.conf.json ────────────────────────────────────────────
/* Edited as text, not parsed and re-serialised: a rename must not reflow the
   file's hand-kept formatting into a diff nobody asked to review. `title` is
   a window key and appears nowhere else in this schema. */
const confPath = join(here, "..", "src-tauri", "tauri.conf.json");
let conf = readFileSync(confPath, "utf8");
const field = (key, value) => {
  const re = new RegExp(`("${key}"\\s*:\\s*")([^"]*)(")`, "g");
  const seen = [...conf.matchAll(re)];
  if (!seen.length) throw new Error(`tauri.conf.json has no "${key}" to rewrite`);
  for (const m of seen) note("tauri.conf.json", key, m[2], value);
  conf = conf.replace(re, `$1${value}$3`);
};
field("productName", brand.name);
field("identifier", brand.appId);
field("mainBinaryName", brand.bin);
field("title", brand.name);
writeFileSync(confPath, conf);

// ── Cargo.toml ─────────────────────────────────────────────────
/* Only the [[bin]] name follows the brand. The package name is the crate's own
   identity — renaming it would churn every path in the build directory for no
   user-visible gain. */
const cargoPath = join(here, "..", "src-tauri", "Cargo.toml");
const cargo = readFileSync(cargoPath, "utf8");
const binSection = /(\[\[bin\]\][^[]*?name\s*=\s*")([^"]+)(")/;
const found = cargo.match(binSection);
if (!found) throw new Error("Cargo.toml has no [[bin]] name to rewrite");
note("Cargo.toml", "[[bin]] name", found[2], brand.bin);
writeFileSync(cargoPath, cargo.replace(binSection, `$1${brand.bin}$3`));

console.log(changes.length ? changes.join("\n") : `already ${brand.name} (${brand.bin})`);
console.log("rebuild to pick it up: cargo build (build.rs re-emits the constants)");
