/* brand bridge — brand.json lives at the repo root and is the
   single source of the product name/accent (PLAN §0). Vite also
   injects it at build time as __BRAND__ (vite.config.ts). */

import brand from "../../brand.json";

declare const __BRAND__: typeof brand | undefined;

/** build-time injected when available, else the direct import */
const BRAND: typeof brand =
  typeof __BRAND__ !== "undefined" ? __BRAND__ : brand;

export default BRAND;
