/* persistence — localStorage hydration for the mock core.
   In the Tauri build this whole layer is the Rust core's job
   (%APPDATA%/pinion + OS keychain). Here: one JSON blob. */

const KEY = "pinion.v1.state";

export interface PersistedState {
  accounts: unknown[];
  activeUuid: string | null;
  instances: unknown[];
  modEnabled: Record<string, Record<string, boolean>>;
  modFavorite: Record<string, Record<string, boolean>>;
  settings: unknown | null;
  loadouts: Record<string, unknown>;
  hudConfigs: Record<string, unknown>;
  servers: unknown[] | null;
}

export function loadState(): Partial<PersistedState> {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return {};
    return JSON.parse(raw) as Partial<PersistedState>;
  } catch {
    return {};
  }
}

export function saveState(state: PersistedState): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    /* quota / private mode — non-fatal in the mock */
  }
}

export function clearState(): void {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
