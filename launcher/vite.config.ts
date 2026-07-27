import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import brand from "../brand.json";

// brand.json is the single source of the product name (PLAN §0).
// It is injected at build time as __BRAND__ and also imported at runtime.
export default defineConfig({
  plugins: [
    react(),
    {
      // index.html is served before any of our code runs, so the title it
      // ships with is the one the taskbar shows during startup.
      name: "brand-html",
      transformIndexHtml: (html) => html.replaceAll("%BRAND_NAME%", brand.name),
    },
  ],
  define: {
    __BRAND__: JSON.stringify(brand),
  },
  server: {
    port: 5173,
    strictPort: true,
    fs: { allow: [".."] },
  },
});
