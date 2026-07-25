/* client.ts — the single import point for the core contract.

   Standalone build: MockCore. Tauri build: replace this file with
   tauri-specta generated bindings — every consumer imports `core`
   from here and nothing else changes (PLAN §2 seam). */

import type { PinionCore } from "./bindings";
import { MockCore } from "./mockCore";

export const core: PinionCore = new MockCore();

/** mock-core extras that the real core will expose via state, not methods */
export function getActiveAccountUuid(): string | null {
  return core instanceof MockCore ? core.getActiveUuid() : null;
}
