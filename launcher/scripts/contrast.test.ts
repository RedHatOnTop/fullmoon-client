import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const css = readFileSync(new URL("../src/styles/tokens.css", import.meta.url), "utf8");

const declaration = (source: string, name: string) => {
  const match = source.match(new RegExp(`${name}:\\s*([^;]+);`));
  assert.ok(match, `${name} is declared`);
  return match[1].trim();
};

const resolve = (value: string): string => {
  const alias = value.match(/^var\((--[^)]+)\)$/);
  return alias ? resolve(declaration(css, alias[1])) : value;
};

const rgb = (hex: string) => {
  const value = Number.parseInt(hex.slice(1), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255] as const;
};

const luminance = (hex: string) => {
  const channels = rgb(hex).map((channel) => {
    const value = channel / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
};

const contrast = (foreground: string, background: string) => {
  const values = [luminance(foreground), luminance(background)].sort((a, b) => b - a);
  return (values[0] + 0.05) / (values[1] + 0.05);
};

test("dark tertiary text clears the small-text contrast floor", () => {
  const dark = css.match(/\[data-theme="dark"\]\s*\{([\s\S]*?)\n\}/)?.[1];
  assert.ok(dark, "dark theme block exists");
  const foreground = resolve(declaration(dark, "--text-3"));

  for (const backgroundName of ["--bg", "--surface"]) {
    const background = resolve(declaration(dark, backgroundName));
    assert.ok(
      contrast(foreground, background) >= 4.5,
      `${foreground} on ${background} clears 4.5:1`,
    );
  }
});
