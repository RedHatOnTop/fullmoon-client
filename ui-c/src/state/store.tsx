/* ─────────────────────────────────────────────────────────────
   store.tsx — app state over the core contract.

   The store owns NOTHING the core owns. It hydrates from core
   commands, mirrors core events, and exposes intent-level actions
   to the views. Views stay pure (PLAN §5).
   ───────────────────────────────────────────────────────────── */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { flushSync } from "react-dom";
import { core, getActiveAccountUuid } from "../core/client";
import type {
  Account,
  Cosmetic,
  GameState,
  Instance,
  InstanceSpec,
  Loadout,
  LogLevel,
  ModCatalog,
  NewsItem,
  ServerEntry,
  Settings,
  VersionSummary,
} from "../core/bindings";
import { useT } from "../i18n";

export type Screen = "home" | "instances" | "mods" | "cosmetics" | "accounts" | "settings" | "console";

export interface Toast {
  id: number;
  kind: "info" | "success" | "error";
  text: string;
}

export interface DownloadInfo {
  taskId: string;
  file: string;
  pct: number;
  bytesPerSec: number;
  at: number;
}

export interface LogEntry {
  id: number;
  level: LogLevel;
  line: string;
  ts: string;
}

interface Store {
  ready: boolean;
  screen: Screen;
  setScreen: (s: Screen) => void;

  accounts: Account[];
  activeAccount: Account | null;
  selectAccount: (uuid: string) => Promise<void>;
  removeAccount: (uuid: string) => Promise<void>;
  refreshAccount: (uuid: string) => Promise<void>;
  importOfficial: () => Promise<void>;
  syncAccounts: () => Promise<void>;

  versions: VersionSummary[];
  instances: Instance[];
  selectedInstanceId: string | null;
  selectInstance: (id: string) => void;
  selectedInstance: Instance | null;
  createInstance: (spec: InstanceSpec) => Promise<void>;
  deleteInstance: (id: string) => Promise<void>;
  installInstance: (id: string) => Promise<void>;

  modCatalog: ModCatalog | null;
  cosmetics: Cosmetic[];

  settings: Settings | null;
  patchSettings: (patch: Partial<Settings>) => Promise<void>;

  news: NewsItem[];
  servers: ServerEntry[];
  removeServer: (id: string) => Promise<void>;

  game: GameState;
  logs: LogEntry[];
  launch: (instanceId: string, server?: string) => Promise<void>;
  killGame: () => Promise<void>;
  clearLogs: () => void;

  loadout: Loadout | null;
  equip: (slot: keyof Loadout, itemId: string | null) => Promise<void>;

  downloads: DownloadInfo[];
  toasts: Toast[];
  toast: (kind: Toast["kind"], text: string) => void;
  dismissToast: (id: number) => void;
}

const Ctx = createContext<Store | null>(null);
let toastSeq = 0;
let logSeq = 0;

const now = () =>
  new Date().toLocaleTimeString("en-GB", { hour12: false }) +
  "." +
  String(new Date().getMilliseconds()).padStart(3, "0");

export function StoreProvider({ children }: { children: ReactNode }) {
  const { t } = useT();
  const [ready, setReady] = useState(false);
  const [screen, setScreenState] = useState<Screen>("home");

  /* screen switches ride the View Transitions API when available */
  const setScreen = useCallback((s: Screen) => {
    const doc = document as Document & { startViewTransition?: (cb: () => void) => void };
    if (doc.startViewTransition) {
      doc.startViewTransition(() => flushSync(() => setScreenState(s)));
    } else {
      setScreenState(s);
    }
  }, []);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [activeUuid, setActiveUuid] = useState<string | null>(null);
  const [versions, setVersions] = useState<VersionSummary[]>([]);
  const [instances, setInstances] = useState<Instance[]>([]);
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(
    () => localStorage.getItem("pinion.v1.sel"),
  );
  const [modCatalog, setModCatalog] = useState<ModCatalog | null>(null);
  const [cosmetics, setCosmetics] = useState<Cosmetic[]>([]);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [news, setNews] = useState<NewsItem[]>([]);
  const [servers, setServers] = useState<ServerEntry[]>([]);
  const [game, setGame] = useState<GameState>({
    state: "idle", sessionId: null, instanceId: null, server: null, startedAt: null, exitCode: null,
  });
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loadout, setLoadout] = useState<Loadout | null>(null);
  const [downloads, setDownloads] = useState<DownloadInfo[]>([]);
  const [toasts, setToasts] = useState<Toast[]>([]);
  const settingsRef = useRef<Settings | null>(null);

  const dismissToast = useCallback((id: number) => {
    setToasts((ts) => ts.filter((x) => x.id !== id));
  }, []);

  const toast = useCallback(
    (kind: Toast["kind"], text: string) => {
      const id = ++toastSeq;
      setToasts((ts) => [...ts.slice(-3), { id, kind, text }]);
      window.setTimeout(() => dismissToast(id), 4200);
    },
    [dismissToast],
  );

  /* ── hydrate + subscribe ── */

  useEffect(() => {
    let alive = true;
    (async () => {
      const [accs, vers, insts, mods, cos, st, nw, sv, gs] = await Promise.all([
        core.auth_list(),
        core.versions_manifest(),
        core.instances_list(),
        core.mods_available(),
        core.cosmetics_catalog(),
        core.settings_get(),
        core.news_feed(),
        core.servers_list(),
        core.game_status(),
      ]);
      if (!alive) return;
      setAccounts(accs);
      setActiveUuid(getActiveAccountUuid());
      setVersions(vers);
      setInstances(insts);
      setModCatalog(mods);
      setCosmetics(cos);
      setSettings(st);
      settingsRef.current = st;
      setNews(nw);
      setServers(sv);
      setGame(gs);
      setSelectedInstanceId((sel) =>
        sel && insts.some((i) => i.id === sel) ? sel : insts[0]?.id ?? null,
      );
      setReady(true);
    })();

    const offInstall = core.on("install://stage", ({ instanceId, stage, pct }) => {
      setInstances((list) =>
        list.map((i) =>
          i.id === instanceId
            ? stage === "done"
              ? { ...i, installed: true, installing: null }
              : { ...i, installing: { stage, pct } }
            : i,
        ),
      );
      if (stage === "done") {
        setDownloads((d) => d.filter((x) => x.taskId !== `task-${instanceId}`));
        toast("success", t("instances.installDone"));
      }
    });

    const offDl = core.on("download://progress", ({ taskId, file, done, bytesPerSec }) => {
      setDownloads((d) => {
        const rest = d.filter((x) => x.taskId !== taskId);
        return [...rest, { taskId, file, pct: done * 100, bytesPerSec, at: Date.now() }];
      });
    });

    const offLog = core.on("game://log", ({ level, line }) => {
      setLogs((l) => [...l.slice(-900), { id: ++logSeq, level, line, ts: now() }]);
    });

    const offState = core.on("game://state", ({ sessionId, state, exitCode }) => {
      setGame((g) => (g.sessionId === sessionId ? { ...g, state, exitCode: exitCode ?? null } : g));
      if (state === "crashed") toast("error", t("console.crashedToast", { code: exitCode ?? -1 }));
      if (state === "closed") toast("info", t("console.closedToast"));
    });

    return () => {
      alive = false;
      offInstall();
      offDl();
      offLog();
      offState();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* loadout follows the active account */
  useEffect(() => {
    if (!activeUuid) {
      setLoadout(null);
      return;
    }
    core.cosmetics_equipped(activeUuid).then(setLoadout);
  }, [activeUuid]);

  /* persist selection */
  useEffect(() => {
    if (selectedInstanceId) localStorage.setItem("pinion.v1.sel", selectedInstanceId);
  }, [selectedInstanceId]);

  /* apply theme + accent to the document */
  useEffect(() => {
    if (!settings) return;
    const root = document.documentElement;
    root.dataset.theme = settings.theme;
    root.style.setProperty("--accent", settings.accent);
    root.style.setProperty("--accent-dim", shade(settings.accent, -28));
    root.style.setProperty("--accent-soft", alpha(settings.accent, 0.12));
    root.style.setProperty("--accent-glow", alpha(settings.accent, 0.35));
  }, [settings]);

  /* ── actions ── */

  const selectAccount = useCallback(async (uuid: string) => {
    await core.auth_select(uuid);
    setActiveUuid(uuid);
  }, []);

  const removeAccount = useCallback(
    async (uuid: string) => {
      await core.auth_remove(uuid);
      const list = await core.auth_list();
      setAccounts(list);
      setActiveUuid(getActiveAccountUuid());
      toast("info", t("accounts.removed"));
    },
    [toast, t],
  );

  const refreshAccount = useCallback(
    async (uuid: string) => {
      await core.auth_refresh(uuid);
      toast("success", t("accounts.refreshed"));
    },
    [toast, t],
  );

  const importOfficial = useCallback(async () => {
    const added = await core.auth_import_official();
    if (added.length === 0) toast("info", t("accounts.importNone"));
    else {
      toast("success", t("accounts.importSome", { n: added.length }));
      setAccounts(await core.auth_list());
      setActiveUuid(getActiveAccountUuid());
    }
  }, [toast, t]);

  const syncAccounts = useCallback(async () => {
    setAccounts(await core.auth_list());
    setActiveUuid(getActiveAccountUuid());
  }, []);

  const selectInstance = useCallback((id: string) => setSelectedInstanceId(id), []);

  const createInstance = useCallback(
    async (spec: InstanceSpec) => {
      const inst = await core.instance_create(spec);
      setInstances((l) => [...l, inst]);
      setSelectedInstanceId(inst.id);
      toast("success", t("instances.created"));
      void core.instance_install(inst.id);
    },
    [toast, t],
  );

  const deleteInstance = useCallback(
    async (id: string) => {
      await core.instance_delete(id);
      setInstances((l) => l.filter((i) => i.id !== id));
      setSelectedInstanceId((sel) => (sel === id ? null : sel));
      toast("info", t("instances.deleted"));
    },
    [toast, t],
  );

  const installInstance = useCallback(
    async (id: string) => {
      toast("info", t("instances.installStarted"));
      await core.instance_install(id);
    },
    [toast, t],
  );

  const patchSettings = useCallback(async (patch: Partial<Settings>) => {
    const next = await core.settings_set(patch);
    settingsRef.current = next;
    setSettings(next);
  }, []);

  const removeServer = useCallback(async (id: string) => {
    setServers((s) => {
      const next = s.filter((x) => x.id !== id);
      void core.servers_save(next);
      return next;
    });
  }, []);

  const launch = useCallback(
    async (instanceId: string, server?: string) => {
      try {
        const sessionId = server
          ? await core.launch_quickplay(instanceId, server)
          : await core.launch(instanceId);
        if (server) toast("info", t("toast.quickPlay", { server }));
        setGame((g) => ({
          ...g,
          state: "starting",
          sessionId,
          instanceId,
          server: server ?? null,
          startedAt: Date.now(),
        }));
      } catch (e) {
        toast("error", t("toast.launchFail", { reason: e instanceof Error ? e.message : "unknown" }));
      }
    },
    [toast, t],
  );

  const killGame = useCallback(async () => {
    if (game.sessionId) {
      await core.game_kill(game.sessionId);
      toast("info", t("console.killed"));
    }
  }, [game.sessionId, toast, t]);

  const clearLogs = useCallback(() => setLogs([]), []);

  const equip = useCallback(
    async (slot: keyof Loadout, itemId: string | null) => {
      if (!activeUuid) return;
      await core.cosmetics_equip(activeUuid, slot, itemId);
      setLoadout(await core.cosmetics_equipped(activeUuid));
      const item = cosmetics.find((c) => c.id === itemId);
      if (item) toast("success", t("cosmetics.equippedToast", { name: item.name }));
    },
    [activeUuid, cosmetics, toast, t],
  );

  const activeAccount = useMemo(
    () => accounts.find((a) => a.uuid === activeUuid) ?? null,
    [accounts, activeUuid],
  );
  const selectedInstance = useMemo(
    () => instances.find((i) => i.id === selectedInstanceId) ?? null,
    [instances, selectedInstanceId],
  );

  const value: Store = {
    ready, screen, setScreen,
    accounts, activeAccount, selectAccount, removeAccount, refreshAccount, importOfficial, syncAccounts,
    versions, instances, selectedInstanceId, selectInstance, selectedInstance,
    createInstance, deleteInstance, installInstance,
    modCatalog, cosmetics,
    settings, patchSettings,
    news, servers, removeServer,
    game, logs, launch, killGame, clearLogs,
    loadout, equip,
    downloads, toasts, toast, dismissToast,
  };

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useStore(): Store {
  const s = useContext(Ctx);
  if (!s) throw new Error("useStore outside provider");
  return s;
}

/* color helpers for runtime accent injection */
function hexToRgb(hex: string): [number, number, number] {
  const h = hex.replace("#", "");
  const v = h.length === 3 ? h.split("").map((c) => c + c).join("") : h;
  return [parseInt(v.slice(0, 2), 16), parseInt(v.slice(2, 4), 16), parseInt(v.slice(4, 6), 16)];
}
function alpha(hex: string, a: number): string {
  const [r, g, b] = hexToRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}
function shade(hex: string, pct: number): string {
  const [r, g, b] = hexToRgb(hex);
  const f = (c: number) => Math.max(0, Math.min(255, Math.round(c + (pct / 100) * 255)));
  return `#${[f(r), f(g), f(b)].map((c) => c.toString(16).padStart(2, "0")).join("")}`;
}
