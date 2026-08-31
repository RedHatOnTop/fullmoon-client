/* How a line of the game's output gets its level, mirrored from the core's own
 * `src-tauri/src/launch.rs::level_of`.
 *
 * The real core classifies every line it pumps, so the front end never has to. This mirror exists
 * for the mock core, which owns its fixtures and would otherwise decide the accent by hand — and a
 * fixture that shows the accent on a line the real rule would leave dim is a console the launcher
 * only appears to have. `scripts/log-level.test.ts` runs the Rust tests' own vectors through it. */
import type { LogLevel } from "./bindings";

/** Uppercased, because the match runs against an uppercased line. */
export const OUR_LOGGER_TAG = "(FULLMOON/";

export function levelOf(line: string): LogLevel {
  const upper = line.toUpperCase();
  if (upper.includes("/ERROR") || upper.includes("EXCEPTION") || upper.includes("\tAT ")) return "ERROR";
  if (upper.includes("/WARN")) return "WARN";
  if (upper.includes(OUR_LOGGER_TAG)) return "OURS";
  if (upper.includes("/DEBUG")) return "DEBUG";
  return "INFO";
}
