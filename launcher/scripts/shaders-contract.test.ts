import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = (path: string) =>
  readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

const mods = source("src/screens/Mods.tsx");
const bindings = source("src/core/bindings.ts");
const client = source("src/core/tauriCore.ts");
const mock = source("src/core/mockCore.ts");
const ko = source("src/i18n/ko.ts");
const en = source("src/i18n/en.ts");

test("the mods screen offers one-click shader install", () => {
  assert.match(mods, /shaders_install/);
  assert.match(mods, /mods\.shaderInstall/);
  assert.match(ko, /shaderInstall:\s*"쉐이더 설치"/);
  assert.match(en, /shaderInstall:\s*"Install shaders"/);
});

test("shader commands exist on the core contract in browser and desktop", () => {
  assert.match(bindings, /shaders_install\(instanceId: string\): Promise<ShaderStatus>/);
  assert.match(client, /shaders_install = \(instanceId: string\)/);
  assert.match(mock, /async shaders_install\(instanceId: string\)/);
});
