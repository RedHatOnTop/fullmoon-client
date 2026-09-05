/* The console's colour rule against the core that actually applies it.
 *
 * `src/core/logLevel.ts` is a mirror of `launch.rs::level_of`, and the only thing keeping a mirror
 * honest is a test that reads the original. So this parses the Rust module: its tag constant, and
 * every vector its own unit tests assert — a level added or a needle changed over there fails here.
 *
 * Run with `node --test scripts/`. */
import { readFileSync } from "node:fs";
import assert from "node:assert/strict";
import { test } from "node:test";

import { OUR_LOGGER_TAG, levelOf } from "../src/core/logLevel.ts";

const rust = readFileSync(new URL("../src-tauri/src/launch.rs", import.meta.url), "utf8");

test("the tag that marks a line as our client's is the core's own constant", () => {
  const tag = rust.match(/OUR_LOGGER_TAG: &str = "([^"]+)"/);
  assert.equal(tag?.[1], OUR_LOGGER_TAG);
});

test("the levels are tried in the core's order, so a broken line of ours is an error first", () => {
  const body = rust.slice(rust.indexOf("fn level_of"), rust.indexOf("\n}", rust.indexOf("fn level_of")));
  const order = [...body.matchAll(/"(ERROR|WARN|OURS|DEBUG|INFO)"/g)].map((m) => m[1]);
  assert.deepEqual(order, ["ERROR", "WARN", "OURS", "DEBUG", "INFO"]);

  // one line that matches every needle at once: each arm in turn is the one that must win
  const all = "[Render thread/ERROR] /WARN /DEBUG (Fullmoon/Hud) java.io.IOException";
  const shed = ["/ERROR", "/WARN", "(Fullmoon/Hud)", "/DEBUG"];
  let line = all;
  for (const [i, level] of order.entries()) {
    assert.equal(levelOf(line), level, line);
    if (i < shed.length) line = line.replace(shed[i], "").replace("IOException", "boom");
  }
});

test("the core's own test vectors classify the same way out here", () => {
  const vectors = [
    ...rust.matchAll(/level_of\(\s*"((?:[^"\\]|\\.)*)"\s*\)\s*,\s*\n?\s*"(\w+)"/g),
  ].map(([, line, want]) => [line, want] as const);

  assert.ok(vectors.length >= 6, `only found ${vectors.length} vectors in launch.rs`);
  for (const [raw, want] of vectors) {
    const line = raw.replace(/\\"/g, '"').replace(/\\t/g, "\t").replace(/\\\\/g, "\\");
    assert.equal(levelOf(line), want, line);
  }
});

test("a line our client logged is told apart from the game's own", () => {
  assert.equal(
    levelOf("[19:55:07] [Render thread/INFO] (Fullmoon/Channel) Sent fullmoon:v1 hello (proto 1)"),
    "OURS",
  );
  // and a broken one of ours is an error first — that is what a player has to see
  assert.equal(levelOf("[19:55:23] [Render thread/ERROR] (Fullmoon/Map) nope"), "ERROR");
  assert.equal(levelOf("[19:55:07] [Render thread/INFO]: Loading Minecraft 1.21.10"), "INFO");
});
