import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = (path: string) =>
  readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

const accounts = source("src/screens/Accounts.tsx");
const bindings = source("src/core/bindings.ts");
const client = source("src/core/client.ts");
const mock = source("src/core/mockCore.ts");
const verify = source("scripts/verify.mjs");
const ko = source("src/i18n/ko.ts");
const en = source("src/i18n/en.ts");

test("a clean account screen offers one-click local testing", () => {
  assert.match(accounts, /LOCAL_TEST_USERNAME/);
  assert.match(accounts, /createLocalTestAccount/);
  assert.match(accounts, /accounts\.localTestAction/);
  assert.match(ko, /localTestAction:\s*"로컬 테스트 계정 만들기"/);
  assert.match(en, /localTestAction:\s*"Create local test account"/);
});

test("offline account creation follows the same core contract in browser and desktop", () => {
  assert.match(bindings, /auth_add_offline\(username: string\): Promise<Account>/);
  assert.doesNotMatch(bindings, /auth_add_offline\?\(/);
  assert.match(mock, /async auth_add_offline\(username: string\)/);
  assert.match(client, /return core\.auth_add_offline\(username\)/);
  assert.doesNotMatch(client, /if \(!\(core instanceof TauriCore\)\)/);
});

test("the browser verification drives the clean-profile local account journey", () => {
  assert.match(verify, /accounts:\s*\[\]/);
  assert.match(verify, /activeUuid:\s*null/);
  assert.match(verify, /로컬 테스트 계정 만들기/);
  assert.match(verify, /FullmoonTest/);
  assert.match(verify, /09-offline-account/);
});
