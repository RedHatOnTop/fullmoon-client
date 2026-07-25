/* client.ts — the single import point for the core contract.

   Inside the Tauri shell the real Rust core answers over IPC; in a plain
   browser (vite dev, screenshot rig) MockCore answers instead, so the UI
   stays developable without the shell. Every consumer imports `core` from
   here and nothing else changes (PLAN §2 seam). */

import type { Account, PinionCore } from "./bindings";
import { MockCore } from "./mockCore";
import { TauriCore, hasTauri } from "./tauriCore";

export const core: PinionCore = hasTauri() ? new TauriCore() : new MockCore();

/** the active account — state on the core side, not a contract method */
export function getActiveAccountUuid(): string | null {
  return core instanceof MockCore || core instanceof TauriCore ? core.getActiveUuid() : null;
}

/** true when the real core is answering — used to gate "not implemented yet" copy */
export const isRealCore = hasTauri();

/** Offline profiles exist only in the real core; the mock has fixed accounts. */
export async function addOfflineAccount(username: string): Promise<Account> {
  if (!(core instanceof TauriCore)) {
    throw new Error("오프라인 계정은 데스크톱 빌드에서만 추가할 수 있습니다");
  }
  return core.auth_add_offline(username);
}

/** Sign-in happens in the user's own browser; in the dev browser a tab will do. */
export async function openExternal(url: string): Promise<void> {
  if (!isRealCore) {
    window.open(url, "_blank", "noopener");
    return;
  }
  const { openUrl } = await import("@tauri-apps/plugin-opener");
  await openUrl(url);
}

/** IPC rejects with a plain string, never an Error — unwrap either shape */
export function errText(e: unknown): string {
  if (typeof e === "string") return e;
  if (e instanceof Error) return e.message;
  return String(e ?? "unknown");
}
