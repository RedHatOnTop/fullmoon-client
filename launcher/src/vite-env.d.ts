/// <reference types="vite/client" />

/* allow CSS custom properties (--*) in style objects */
declare module "csstype" {
  interface Properties {
    [index: `--${string}`]: string | number | undefined;
  }
}
