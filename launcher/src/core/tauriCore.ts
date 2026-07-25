/* tauriCore.ts — the contract, spoken over IPC.

   Every method is a thin `invoke` of the identically-named Rust command;
   the arguments are camelCase because that is what the Rust side declares.
   Commands the core has not implemented yet reject with the core's own
   message ("not implemented yet: …") — the UI surfaces that rather than
   pretending the call worked. */

import type {
  Account,
  AuthStatus,
  Cosmetic,
  CosmeticSlot,
  DeviceCodePrompt,
  GameState,
  HudConfig,
  InstalledMod,
  Instance,
  InstancePatch,
  InstanceSpec,
  JavaRuntime,
  Loadout,
  ModCatalog,
  NewsItem,
  PinionCore,
  ServerEntry,
  Settings,
  VersionSummary,
  CoreEventName,
  CoreEvents,
  LaunchOpts,
} from "./bindings";

type Invoke = <T>(cmd: string, args?: Record<string, unknown>) => Promise<T>;
type Listen = <T>(
  event: string,
  handler: (e: { payload: T }) => void,
) => Promise<() => void>;

interface TauriInternals {
  invoke: Invoke;
  event?: { listen: Listen };
}

export function hasTauri(): boolean {
  return typeof window !== "undefined" && "__TAURI_INTERNALS__" in window;
}

export class TauriCore implements PinionCore {
  private readonly api: TauriInternals;

  constructor() {
    this.api = (window as unknown as { __TAURI_INTERNALS__: TauriInternals }).__TAURI_INTERNALS__;
  }

  private call<T>(cmd: string, args?: Record<string, unknown>): Promise<T> {
    return this.api.invoke<T>(cmd, args ?? {});
  }

  // auth
  auth_begin_device_code = () => this.call<DeviceCodePrompt>("auth_begin_device_code");
  auth_poll = (session: string) => this.call<AuthStatus>("auth_poll", { session });
  auth_login_authcode = () => this.call<Account>("auth_login_authcode");
  auth_import_official = () => this.call<Account[]>("auth_import_official");
  /* the active account lives in Rust state; cache it here so the store can
     read it synchronously right after the list it always awaits first */
  private activeUuid: string | null = null;

  auth_list = async () => {
    const [list, active] = await Promise.all([
      this.call<Account[]>("auth_list"),
      this.call<string | null>("auth_active"),
    ]);
    this.activeUuid = active;
    return list;
  };
  getActiveUuid = () => this.activeUuid;
  auth_select = async (uuid: string) => {
    await this.call<void>("auth_select", { uuid });
    this.activeUuid = uuid;
  };
  auth_remove = (uuid: string) => this.call<void>("auth_remove", { uuid });
  auth_refresh = (uuid: string) => this.call<Account>("auth_refresh", { uuid });

  // versions / instances
  versions_manifest = () => this.call<VersionSummary[]>("versions_manifest");
  instances_list = () => this.call<Instance[]>("instances_list");
  instance_create = (spec: InstanceSpec) => this.call<Instance>("instance_create", { spec });
  instance_update = (id: string, patch: InstancePatch) =>
    this.call<Instance>("instance_update", { id, patch });
  instance_delete = (id: string) => this.call<void>("instance_delete", { id });
  instance_install = (id: string) => this.call<string>("instance_install", { id });

  // mods
  mods_available = () => this.call<ModCatalog>("mods_available");
  mods_list = (instanceId: string) => this.call<InstalledMod[]>("mods_list", { instanceId });
  mod_toggle = (instanceId: string, modId: string, enabled: boolean) =>
    this.call<void>("mod_toggle", { instanceId, modId, enabled });
  mod_favorite = (instanceId: string, modId: string, favorite: boolean) =>
    this.call<void>("mod_favorite", { instanceId, modId, favorite });

  // launch
  launch = (instanceId: string, opts?: LaunchOpts) =>
    this.call<string>("launch", { instanceId, opts: opts ?? null });
  launch_quickplay = (instanceId: string, server: string) =>
    this.call<string>("launch_quickplay", { instanceId, server });
  game_kill = (sessionId: string) => this.call<void>("game_kill", { sessionId });
  game_status = () => this.call<GameState>("game_status");

  // cosmetics / hud
  cosmetics_catalog = () => this.call<Cosmetic[]>("cosmetics_catalog");
  cosmetics_equipped = (uuid: string) => this.call<Loadout>("cosmetics_equipped", { uuid });
  cosmetics_equip = (uuid: string, slot: CosmeticSlot, itemId: string | null) =>
    this.call<void>("cosmetics_equip", { uuid, slot, itemId });
  hud_get = (instanceId: string) => this.call<HudConfig>("hud_get", { instanceId });
  hud_set = (instanceId: string, cfg: HudConfig) => this.call<void>("hud_set", { instanceId, cfg });

  // settings
  settings_get = () => this.call<Settings>("settings_get");
  settings_set = (patch: Partial<Settings>) => this.call<Settings>("settings_set", { patch });
  java_detect = () => this.call<JavaRuntime[]>("java_detect");

  // home
  news_feed = () => this.call<NewsItem[]>("news_feed");
  servers_list = () => this.call<ServerEntry[]>("servers_list");
  servers_save = (list: ServerEntry[]) => this.call<void>("servers_save", { list });

  /* Tauri's listen is async but the store's subscribe API is not; unsubscribing
     before the listener registers has to still take effect, hence the flag. */
  on<E extends CoreEventName>(event: E, cb: (payload: CoreEvents[E]) => void): () => void {
    let stop: (() => void) | null = null;
    let cancelled = false;
    void this.api.event
      ?.listen<CoreEvents[E]>(event, (e) => cb(e.payload))
      .then((un) => {
        if (cancelled) un();
        else stop = un;
      });
    return () => {
      cancelled = true;
      stop?.();
    };
  }
}
