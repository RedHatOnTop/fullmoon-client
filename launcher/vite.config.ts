import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import brand from "../brand.json";

// brand.json is the single source of the product name (PLAN §0).
// It is injected at build time as __BRAND__ and also imported at runtime.
export default defineConfig({
  plugins: [react()],
  define: {
    __BRAND__: JSON.stringify(brand),
  },
  server: {
    port: 5173,
    strictPort: true,
    fs: { allow: [".."] },
  },
});
