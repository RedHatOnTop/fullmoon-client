/* ─────────────────────────────────────────────────────────────
   mockCore.ts — standalone implementation of the PinionCore
   contract (PLAN §4). Simulates the real Rust core with honest
   latency, staged installs, streamed game logs and device-code
   polling. Swap this file for tauri-specta bindings and the UI
   never notices.
   ───────────────────────────────────────────────────────────── */

import type {
  Account,
  AuthStatus,
  Cosmetic,
  CosmeticSlot,
  CoreEventName,
  CoreEvents,
  DeviceCodePrompt,
  GameState,
  HudConfig,
  Instance,
  InstancePatch,
  InstanceSpec,
  InstallStage,
  InstalledMod,
  JavaRuntime,
  LaunchOpts,
  Loadout,
  LogLevel,
  ModCatalog,
  NewsItem,
  PinionCore,
  ServerEntry,
  Settings,
  VersionSummary,
} from "./bindings";
import { loadState, saveState } from "./persistence";

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));
const latency = () => sleep(90 + Math.random() * 160);
const uid = () =>
  "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/* ── seed data ─────────────────────────────────────────────── */

const SEED_ACCOUNTS: Account[] = [
  {
    uuid: "8f3c2a10-7b6e-4d9a-9c1f-2e5a7d4b1a01",
    username: "Steve",
    skinHue: 212,
    source: "microsoft",
    capes: ["aero-cape"],
  },
  {
    uuid: "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
    username: "Alex",
    skinHue: 142,
    source: "imported",
    capes: [],
  },
];

const SEED_INSTANCES: Instance[] = [
  {
    id: "inst-main",
    name: "Pinion 26.1.2",
    versionId: "26.1.2",
    loader: "fabric",
    installed: true,
    installing: null,
    memoryMb: 6144,
    iconHue: 216,
    createdAt: new Date(Date.now() - 86400e3 * 21).toISOString(),
    lastPlayedAt: new Date(Date.now() - 86400e3 * 1.2).toISOString(),
    quickPlayServer: null,
  },
  {
    id: "inst-lab",
    name: "Snapshot Lab",
    versionId: "26.2-pre1",
    loader: "fabric",
    installed: false,
    installing: null,
    memoryMb: 4096,
    iconHue: 268,
    createdAt: new Date(Date.now() - 86400e3 * 4).toISOString(),
    lastPlayedAt: null,
    quickPlayServer: null,
  },
];

const VERSIONS: VersionSummary[] = [
  { id: "26.1.2", type: "release", releaseTime: "2026-06-18", isTarget: true },
  { id: "26.1.1", type: "release", releaseTime: "2026-05-29", isTarget: false },
  { id: "26.1.0", type: "release", releaseTime: "2026-05-08", isTarget: false },
  { id: "25.4.2", type: "release", releaseTime: "2026-02-12", isTarget: false },
  { id: "26.2-pre1", type: "snapshot", releaseTime: "2026-07-16", isTarget: false },
  { id: "26.2-snapshot-7", type: "snapshot", releaseTime: "2026-07-09", isTarget: false },
  { id: "26.2-snapshot-6", type: "snapshot", releaseTime: "2026-07-02", isTarget: false },
];

const MOD_CATALOG: ModCatalog = {
  mods: [
    {
      id: "pinion-hud",
      name: "Pinion HUD",
      version: "1.0.0",
      description:
        "우리 Fabric 모드. FPS·CPS·키스트로크·좌표·장비·포션·핑 HUD와 인게임 설정 GUI, 줌, 풀브라이트, 코스메틱 렌더.",
      kind: "hud",
      ours: true,
      compatible: true,
      note: null,
    },
    {
      id: "sodium",
      name: "Sodium",
      version: "0.7.2",
      description: "렌더 파이프라인 재작성. 청크 렌더링 최대 ~10배, 프레임 타임 안정화.",
      kind: "perf",
      ours: false,
      compatible: true,
      note: null,
    },
    {
      id: "lithium",
      name: "Lithium",
      version: "0.15.1",
      description: "게임 로직 최적화 — 틱·AI·물리·청크 직렬화. 시각적 변화 없이 TPS 개선.",
      kind: "perf",
      ours: false,
      compatible: true,
      note: null,
    },
    {
      id: "fabric-api",
      name: "Fabric API",
      version: "0.115.0",
      description: "Fabric 모드 공용 API. 번들 모드들의 필수 의존성.",
      kind: "lib",
      ours: false,
      compatible: true,
      note: null,
    },
  ],
};

const COSMETICS: Cosmetic[] = [
  { id: "aero-cape", slot: "cape", name: "아에로 케이프", rarity: "rare", hue: 214, desc: "비행 깃털 문양을 수놓은 Pinion 기본 케이프." },
  { id: "ember-cape", slot: "cape", name: "엠버 케이프", rarity: "epic", hue: 22, desc: "불씨가 낙엽처럼 흩날리는 자수." },
  { id: "void-cape", slot: "cape", name: "보이드 케이프", rarity: "legendary", hue: 272, desc: "끝없이 가라앉는 별먼지 그라데이션." },
  { id: "mint-cape", slot: "cape", name: "민트 케이프", rarity: "common", hue: 152, desc: "산뜻한 단색. 조용한 취향." },
  { id: "regal-cape", slot: "cape", name: "리갈 케이프", rarity: "epic", hue: 46, desc: "금실 테두리의 의전용 케이프." },
  { id: "gale-wings", slot: "wings", name: "질풍 날개", rarity: "epic", hue: 200, desc: "바람을 가르는 6엽 깃털 날개." },
  { id: "dusk-wings", slot: "wings", name: "황혼 날개", rarity: "rare", hue: 318, desc: "노을빛으로 물든 겹깃." },
  { id: "sprout-wings", slot: "wings", name: "새싹 날개", rarity: "common", hue: 110, desc: "작지만 싱그럽게 파닥인다." },
  { id: "eclipse-wings", slot: "wings", name: "이클립스 날개", rarity: "legendary", hue: 258, desc: "일식의 코로나를 형상화한 광륜 날개." },
  { id: "comet-trail", slot: "trail", name: "혜성 꼬리", rarity: "rare", hue: 190, desc: "이동 경로에 푸른 입자를 남긴다." },
  { id: "petal-trail", slot: "trail", name: "꽃잎 궤적", rarity: "common", hue: 340, desc: "발걸음마다 꽃잎이 흩어진다." },
  { id: "static-trail", slot: "trail", name: "스태틱 궤적", rarity: "epic", hue: 56, desc: "전기 스파크가 튀는 잔광." },
];

const NEWS: NewsItem[] = [
  {
    id: "n1",
    tag: "update",
    title: "Pinion 1.0 — 날개를 달다",
    summary: "첫 정식 릴리스. 26.1.2 타겟, Fabric 번들, Pinion HUD 탑재. 깃털처럼 가볍게, 기어처럼 정확하게.",
    date: "2026-07-20",
    hue: 214,
    featured: true,
  },
  {
    id: "n2",
    tag: "dev",
    title: "개발 노트: 설치 파이프라인은 어떻게 3배 빨라졌나",
    summary: "병렬 다운로드 + SHA1 사전검증 + JRE 프로비전 캐시. 코어 팀의 최적화 기록.",
    date: "2026-07-17",
    hue: 268,
    featured: false,
  },
  {
    id: "n3",
    tag: "cosmetic",
    title: "코스메틱 드롭: 이클립스 날개",
    summary: "일식의 코로나에서 영감받은 레전더리 날개. 내 클라이언트에만 보이는, 나만의 과시.",
    date: "2026-07-14",
    hue: 258,
    featured: false,
  },
  {
    id: "n4",
    tag: "event",
    title: "여름 빌드 배틀 — 커뮤니티 인스턴스 콘테스트",
    summary: "나만의 인스턴스 프리셋을 공유하고 투표받자. 우승 프리셋은 다음 릴리스에 기본 탑재.",
    date: "2026-07-10",
    hue: 152,
    featured: false,
  },
  {
    id: "n5",
    tag: "update",
    title: "Pinion HUD 프리뷰: 키스트로크 & 포션 타이머",
    summary: "인게임 모듈 두 개가 런처 설정과 실시간 미러링된다. hud.json 계약 공개.",
    date: "2026-07-06",
    hue: 22,
    featured: false,
  },
];

const SERVERS: ServerEntry[] = [
  { id: "s1", name: "Ember SMP", address: "play.embersmp.net", motd: "시즌 4 — 불꽃의 섬", players: 842, maxPlayers: 1000, pingMs: 23, hue: 22 },
  { id: "s2", name: "Crystal PvP", address: "crystal.gg", motd: "랭크 시즌 12 · 듀얼 오픈", players: 3210, maxPlayers: 5000, pingMs: 41, hue: 190 },
  { id: "s3", name: "Moonrise", address: "moonrise.network", motd: "스카이블록 · 신규 차원 업데이트", players: 12764, maxPlayers: 20000, pingMs: 57, hue: 268 },
  { id: "s4", name: "Skyfall", address: "skyfall.gg", motd: "베드워즈 · 듀오 토너먼트", players: 534, maxPlayers: 800, pingMs: 12, hue: 330 },
];

const JAVA_RUNTIMES: JavaRuntime[] = [
  { path: "C:\\Program Files\\Java\\jdk-25", version: "25.0.3", vendor: "Oracle JDK", arch: "x64", recommended: true },
  { path: "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5", version: "21.0.5", vendor: "Temurin", arch: "x64", recommended: false },
  { path: "C:\\Program Files\\Zulu\\zulu-17", version: "17.0.11", vendor: "Azul Zulu", arch: "x64", recommended: false },
];

const DEFAULT_SETTINGS: Settings = {
  javaPath: JAVA_RUNTIMES[0].path,
  javaArgs: "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",
  memoryMb: 6144,
  concurrency: 8,
  theme: "dark",
  accent: "#6ea8ff",
  language: "ko",
  telemetry: false,
};

const DEFAULT_HUD: HudConfig = {
  modules: [
    { id: "fps", enabled: true, x: 4, y: 6, scale: 1 },
    { id: "cps", enabled: true, x: 4, y: 14, scale: 1 },
    { id: "coords", enabled: true, x: 5.5, y: 22, scale: 1 },
    { id: "ping", enabled: true, x: 93, y: 6, scale: 1 },
    { id: "keystrokes", enabled: true, x: 86, y: 74, scale: 1 },
    { id: "gear", enabled: true, x: 93, y: 40, scale: 1 },
    { id: "potion", enabled: false, x: 93, y: 24, scale: 1 },
  ],
};

const DEFAULT_LOADOUT: Loadout = { cape: "aero-cape", wings: null, trail: null };

/* ── install simulation tables ─────────────────────────────── */

const STAGE_WEIGHTS: Record<Exclude<InstallStage, "done">, number> = {
  manifest: 4,
  libraries: 26,
  assets: 30,
  jre: 18,
  fabric: 12,
  mods: 10,
};

const STAGE_FILES: Record<Exclude<InstallStage, "done">, string[]> = {
  manifest: ["version_manifest_v2.json", "26.1.2.json"],
  libraries: [
    "client.jar",
    "net/fabricmc/fabric-loader/0.17.2/fabric-loader-0.17.2.jar",
    "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows.jar",
    "com/google/guava/guava/33.2.1/guava-33.2.1.jar",
    "net/minecraft/minecraft-merged-26.1.2.jar",
    "org/ow2/asm/asm-9.7.1.jar",
  ],
  assets: [
    "indexes/26.json",
    "objects/4f/4f9a…c1.lang",
    "objects/9b/9b2e…a7.ogg",
    "objects/d1/d1cc…3f.png",
    "objects/77/77aa…e9.ogg",
    "objects/30/30fe…b2.png",
  ],
  jre: ["jre-25+7-windows-x64.tar.gz", "bin/java.exe", "lib/modules"],
  fabric: [
    "intermediary/26.1.2/mappings.tiny",
    "fabric-loader-0.17.2.jar (install)",
    "fabric-api-0.115.0.jar",
  ],
  mods: ["pinion-hud-1.0.0.jar", "sodium-0.7.2.jar", "lithium-0.15.1.jar"],
};

/* ── boot log script ───────────────────────────────────────── */

interface LogLine { delay: number; level: LogLevel; thread: string; text: string }

function bootScript(version: string, server: string | null): LogLine[] {
  const L = (delay: number, level: LogLevel, thread: string, text: string): LogLine => ({ delay, level, thread, text });
  const lines: LogLine[] = [
    L(120, "INFO", "main", `Loading Minecraft ${version} with Fabric Loader 0.17.2`),
    L(180, "INFO", "main", "Loading 4 mods:"),
    L(60, "INFO", "main", "\t- fabric-api 0.115.0"),
    L(40, "INFO", "main", "\t- lithium 0.15.1"),
    L(40, "INFO", "main", "\t- pinion-hud 1.0.0"),
    L(40, "INFO", "main", "\t- sodium 0.7.2"),
    L(210, "INFO", "main", "SpongePowered MIXIN Subsystem Version=0.8.7"),
    L(140, "INFO", "main", "Compatibility level: JAVA_21"),
    L(330, "INFO", "main", "Applying 214 mixins... done (812 ms)"),
    L(420, "PINION", "main", "pinion-hud: 인스턴스 구성 로드 — hud.json (7 modules)"),
    L(260, "INFO", "Render thread", "Backend library: LWJGL version 3.3.3"),
    L(190, "INFO", "Render thread", "OpenGL Vendor: NVIDIA Corporation"),
    L(90, "INFO", "Render thread", "OpenGL Renderer: NVIDIA GeForce RTX 4070/PCIe/SSE2"),
    L(90, "INFO", "Render thread", "OpenGL Version: 4.6.0 NVIDIA 561.09"),
    L(520, "INFO", "Render thread", "Reloading ResourceManager: Default, Fabric Mods, pinion:cosmetics"),
    L(610, "INFO", "Worker-Main-3", "Baking models: 21468 models in 748 ms"),
    L(280, "WARN", "Render thread", "Ambiguity between arguments [teleport, location] and [teleport, targets]"),
    L(340, "INFO", "Render thread", "Created: 1024x512x4 minecraft:textures/atlas/blocks.png-atlas"),
    L(150, "INFO", "Render thread", "Created: 256x128x4 minecraft:textures/atlas/signs.png-atlas"),
    L(430, "PINION", "Render thread", "cosmetics: cape=aero-cape 렌더 활성 (client-side only)"),
    L(290, "INFO", "Render thread", "Sound engine started (OpenAL Soft 1.23.1)"),
    L(220, "INFO", "Render thread", "Narrator library successfully loaded"),
    L(510, "INFO", "Server thread", `Starting integrated minecraft server version ${version}`),
    L(380, "INFO", "Server thread", 'Preparing level "world"'),
    L(300, "INFO", "Server thread", "Preparing spawn area: 34%"),
    L(300, "INFO", "Server thread", "Preparing spawn area: 78%"),
    L(300, "INFO", "Server thread", "Preparing spawn area: 100%"),
    L(240, "INFO", "Server thread", "Time elapsed: 2401 ms"),
  ];
  if (server) {
    lines.push(
      L(360, "PINION", "Render thread", `Quick Play → ${server} 로 직행`),
      L(420, "INFO", "Render thread", `Connecting to ${server}, 25565`),
      L(380, "INFO", "Netty Client IO #1", "Handshake complete — protocol 773"),
      L(320, "INFO", "Render thread", "Joined server hub. 40 chunks loaded."),
    );
  } else {
    lines.push(
      L(360, "INFO", "Render thread", 'Loaded 0 advancements, world "world" ready'),
      L(300, "INFO", "Render thread", "Singleplayer session started."),
    );
  }
  return lines;
}

const AMBIENT_LOGS: Array<[LogLevel, string, string]> = [
  ["DEBUG", "Worker-Main-5", "Chunk system: updated 12 chunk tickets"],
  ["INFO", "Render thread", "[CHAT] <Steve> o/"],
  ["PINION", "Render thread", "hud: 243 fps · 41 ms ping · 모듈 7개 렌더 중"],
  ["DEBUG", "Server thread", "Autosave started"],
  ["INFO", "Render thread", "Loaded 214 advancements"],
  ["WARN", "Render thread", "Can't keep up! Is the server overloaded? Running 2037ms behind"],
  ["DEBUG", "Netty Epoll IO #2", "Keep-alive RTT 22 ms"],
];

/* ── the mock core ─────────────────────────────────────────── */

type Handler<E extends CoreEventName> = (payload: CoreEvents[E]) => void;

export class MockCore implements PinionCore {
  private accounts: Account[];
  private activeUuid: string | null;
  private instances: Instance[];
  private modEnabled: Record<string, Record<string, boolean>>;
  private settings: Settings;
  private loadouts: Record<string, Loadout>;
  private hudConfigs: Record<string, HudConfig>;
  private servers: ServerEntry[];
  private game: GameState = { state: "idle", sessionId: null, instanceId: null, server: null, startedAt: null, exitCode: null };

  private handlers = new Map<CoreEventName, Set<Handler<CoreEventName>>>();
  private timers = new Set<ReturnType<typeof setTimeout>>();
  private authPolls = new Map<string, number>();

  constructor() {
    const saved = loadState();
    this.accounts = (saved.accounts as Account[] | undefined) ?? SEED_ACCOUNTS;
    this.activeUuid = saved.activeUuid !== undefined ? saved.activeUuid : this.accounts[0]?.uuid ?? null;
    this.instances = ((saved.instances as Instance[] | undefined) ?? SEED_INSTANCES).map((i) => ({
      ...i,
      installing: null, // transient — never persisted mid-flight
    }));
    this.modEnabled = saved.modEnabled ?? {};
    this.settings = { ...DEFAULT_SETTINGS, ...(saved.settings as Partial<Settings> | null) };
    this.loadouts = (saved.loadouts as Record<string, Loadout> | undefined) ?? {};
    this.hudConfigs = (saved.hudConfigs as Record<string, HudConfig> | undefined) ?? {};
    this.servers = (saved.servers as ServerEntry[] | undefined) ?? SERVERS;
  }

  private persist() {
    saveState({
      accounts: this.accounts,
      activeUuid: this.activeUuid,
      instances: this.instances,
      modEnabled: this.modEnabled,
      settings: this.settings,
      loadouts: this.loadouts,
      hudConfigs: this.hudConfigs,
      servers: this.servers,
    });
  }

  private emit<E extends CoreEventName>(event: E, payload: CoreEvents[E]) {
    const set = this.handlers.get(event);
    if (!set) return;
    set.forEach((cb) => (cb as Handler<E>)(payload));
  }

  on<E extends CoreEventName>(event: E, cb: Handler<E>): () => void {
    let set = this.handlers.get(event);
    if (!set) {
      set = new Set();
      this.handlers.set(event, set);
    }
    set.add(cb as Handler<CoreEventName>);
    return () => set.delete(cb as Handler<CoreEventName>);
  }

  private after(ms: number, fn: () => void) {
    const t = setTimeout(() => {
      this.timers.delete(t);
      fn();
    }, ms);
    this.timers.add(t);
  }

  /* ── auth ── */

  async auth_begin_device_code(): Promise<DeviceCodePrompt> {
    await latency();
    const code = Array.from({ length: 8 }, () => "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[(Math.random() * 32) | 0]).join("");
    const prompt: DeviceCodePrompt = {
      session: uid(),
      userCode: `${code.slice(0, 4)}-${code.slice(4)}`,
      verificationUri: "https://microsoft.com/link",
      expiresIn: 900,
    };
    this.authPolls.set(prompt.session, 0);
    this.emit("auth://device", {
      userCode: prompt.userCode,
      verificationUri: prompt.verificationUri,
      expiresIn: prompt.expiresIn,
    });
    return prompt;
  }

  async auth_poll(session: string): Promise<AuthStatus> {
    await sleep(620);
    const polls = (this.authPolls.get(session) ?? 0) + 1;
    this.authPolls.set(session, polls);
    if (polls < 6) return { state: "pending" };
    this.authPolls.delete(session);
    const account: Account = {
      uuid: uid(),
      username: "EnderKnight",
      skinHue: 262,
      source: "microsoft",
      capes: [],
    };
    this.accounts.push(account);
    this.activeUuid = account.uuid;
    this.persist();
    return { state: "done", account };
  }

  async auth_login_authcode(): Promise<Account> {
    await sleep(2400); // embedded webview round-trip
    const account: Account = {
      uuid: uid(),
      username: "Vex_07",
      skinHue: 8,
      source: "microsoft",
      capes: ["ember-cape"],
    };
    this.accounts.push(account);
    this.activeUuid = account.uuid;
    this.persist();
    return account;
  }

  async auth_import_official(): Promise<Account[]> {
    await latency();
    const candidates: Account[] = [
      { uuid: uid(), username: "CraftMaster", skinHue: 96, source: "imported", capes: [] },
      { uuid: uid(), username: "MinnieKim", skinHue: 330, source: "imported", capes: [] },
    ];
    const added = candidates.filter((c) => !this.accounts.some((a) => a.username === c.username));
    this.accounts.push(...added);
    if (added.length > 0) this.persist();
    return added;
  }

  async auth_list(): Promise<Account[]> {
    await latency();
    return this.accounts.map((a) => ({ ...a }));
  }

  async auth_select(uuidSel: string): Promise<void> {
    await sleep(60);
    if (this.accounts.some((a) => a.uuid === uuidSel)) {
      this.activeUuid = uuidSel;
      this.persist();
    }
  }

  async auth_remove(uuidDel: string): Promise<void> {
    await sleep(80);
    this.accounts = this.accounts.filter((a) => a.uuid !== uuidDel);
    delete this.loadouts[uuidDel];
    if (this.activeUuid === uuidDel) this.activeUuid = this.accounts[0]?.uuid ?? null;
    this.persist();
  }

  async auth_refresh(uuidRef: string): Promise<Account> {
    await sleep(420);
    const found = this.accounts.find((a) => a.uuid === uuidRef);
    if (!found) throw new Error("account not found");
    return { ...found };
  }

  getActiveUuid(): string | null {
    return this.activeUuid;
  }

  /* ── versions / instances ── */

  async versions_manifest(): Promise<VersionSummary[]> {
    await latency();
    return VERSIONS.map((v) => ({ ...v }));
  }

  async instances_list(): Promise<Instance[]> {
    await sleep(60);
    return this.instances.map((i) => ({ ...i }));
  }

  async instance_create(spec: InstanceSpec): Promise<Instance> {
    await latency();
    const inst: Instance = {
      id: uid().slice(0, 8),
      name: spec.name,
      versionId: spec.versionId,
      loader: spec.loader,
      installed: false,
      installing: null,
      memoryMb: spec.memoryMb ?? 4096,
      iconHue: spec.iconHue ?? Math.floor(Math.random() * 360),
      createdAt: new Date().toISOString(),
      lastPlayedAt: null,
      quickPlayServer: null,
    };
    this.instances.push(inst);
    this.persist();
    return { ...inst };
  }

  async instance_update(id: string, patch: InstancePatch): Promise<Instance> {
    await sleep(70);
    const inst = this.instances.find((i) => i.id === id);
    if (!inst) throw new Error("instance not found");
    Object.assign(inst, patch);
    this.persist();
    return { ...inst };
  }

  async instance_delete(id: string): Promise<void> {
    await sleep(120);
    this.instances = this.instances.filter((i) => i.id !== id);
    delete this.modEnabled[id];
    delete this.hudConfigs[id];
    this.persist();
  }

  async instance_install(id: string): Promise<string> {
    const inst = this.instances.find((i) => i.id === id);
    if (!inst) throw new Error("instance not found");
    if (inst.installing) return id; // already running
    const taskId = `task-${id}`;
    inst.installing = { stage: "manifest", pct: 0 };

    const stages = Object.keys(STAGE_WEIGHTS) as Array<Exclude<InstallStage, "done">>;
    const stageStart: Record<string, number> = {};
    let acc = 0;
    for (const s of stages) {
      stageStart[s] = acc;
      acc += STAGE_WEIGHTS[s];
    }

    const tickMs = 120;
    const totalMs = 11_000 + Math.random() * 2_500;
    let elapsed = 0;
    let fileIdx = 0;

    const timer = setInterval(() => {
      const current = this.instances.find((i) => i.id === id);
      if (!current) {
        clearInterval(timer);
        return;
      }
      elapsed += tickMs * (0.75 + Math.random() * 0.6);
      const overall = clamp((elapsed / totalMs) * 100, 0, 100);

      let stage: Exclude<InstallStage, "done"> = "mods";
      for (const s of stages) {
        if (overall >= stageStart[s]) stage = s;
      }
      current.installing = { stage, pct: overall };
      this.emit("install://stage", { instanceId: id, stage, pct: overall });

      // file-level download events during the heavy stages
      const files = STAGE_FILES[stage];
      if (Math.random() < 0.8) {
        const file = files[fileIdx % files.length];
        fileIdx += 1;
        const done = clamp(elapsed / totalMs, 0.02, 1);
        this.emit("download://progress", {
          taskId,
          file,
          done,
          total: 1,
          bytesPerSec: (6 + Math.random() * 20) * 1_048_576,
        });
      }

      if (overall >= 100) {
        clearInterval(timer);
        current.installed = true;
        current.installing = null;
        // freshly installed instances get the full bundle enabled
        this.modEnabled[id] = Object.fromEntries(MOD_CATALOG.mods.map((m) => [m.id, true]));
        this.emit("install://stage", { instanceId: id, stage: "done", pct: 100 });
        this.persist();
      }
    }, tickMs);
    this.timers.add(timer as unknown as ReturnType<typeof setTimeout>);
    return taskId;
  }

  /* ── mods ── */

  async mods_available(): Promise<ModCatalog> {
    await latency();
    return MOD_CATALOG;
  }

  async mods_list(instanceId: string): Promise<InstalledMod[]> {
    await sleep(70);
    const enabledMap = this.modEnabled[instanceId] ?? {};
    return MOD_CATALOG.mods.map((m) => ({ ...m, enabled: enabledMap[m.id] ?? true }));
  }

  async mod_toggle(instanceId: string, modId: string, enabled: boolean): Promise<void> {
    await sleep(60);
    if (!this.modEnabled[instanceId]) this.modEnabled[instanceId] = {};
    this.modEnabled[instanceId][modId] = enabled;
    this.persist();
  }

  /* ── launch ── */

  async launch(instanceId: string, opts?: LaunchOpts): Promise<string> {
    const inst = this.instances.find((i) => i.id === instanceId);
    if (!inst || !inst.installed) throw new Error("instance not installed");
    if (this.game.state === "running" || this.game.state === "starting") return this.game.sessionId!;
    await sleep(300);

    const sessionId = uid();
    this.game = {
      state: "starting",
      sessionId,
      instanceId,
      server: opts?.server ?? null,
      startedAt: Date.now(),
      exitCode: null,
    };
    this.emit("game://state", { sessionId, state: "starting" });

    // stream the boot log, then flip to running
    const script = bootScript(inst.versionId, opts?.server ?? null);
    let t = 0;
    script.forEach((line) => {
      t += line.delay;
      this.after(t, () => {
        this.emit("game://log", {
          sessionId,
          level: line.level,
          line: `[${line.thread}/${line.level}]: ${line.text}`,
        });
      });
    });
    this.after(t + 350, () => {
      if (this.game.sessionId !== sessionId) return;
      this.game = { ...this.game, state: "running" };
      this.emit("game://state", { sessionId, state: "running" });
      inst.lastPlayedAt = new Date().toISOString();
      this.persist();
      // ambient chatter while "in game"
      let n = 0;
      const ambient = setInterval(() => {
        if (this.game.sessionId !== sessionId || this.game.state !== "running") {
          clearInterval(ambient);
          return;
        }
        const [level, , text] = AMBIENT_LOGS[n % AMBIENT_LOGS.length];
        n += 1;
        this.emit("game://log", { sessionId, level, line: `[${level}]: ${text}` });
      }, 3400);
      this.timers.add(ambient as unknown as ReturnType<typeof setTimeout>);
    });
    return sessionId;
  }

  async launch_quickplay(instanceId: string, server: string): Promise<string> {
    return this.launch(instanceId, { server });
  }

  async game_kill(sessionId: string): Promise<void> {
    if (this.game.sessionId !== sessionId) return;
    this.emit("game://log", { sessionId, level: "WARN", line: "[pinion]: 사용자가 프로세스를 종료했습니다 (SIGTERM)" });
    this.after(500, () => {
      this.game = { ...this.game, state: "closed", exitCode: 0 };
      this.emit("game://state", { sessionId, state: "closed", exitCode: 0 });
    });
  }

  async game_status(): Promise<GameState> {
    return { ...this.game };
  }

  /* ── cosmetics / hud ── */

  async cosmetics_catalog(): Promise<Cosmetic[]> {
    await latency();
    return COSMETICS.map((c) => ({ ...c }));
  }

  async cosmetics_equipped(uuid: string): Promise<Loadout> {
    await sleep(60);
    return { ...(this.loadouts[uuid] ?? DEFAULT_LOADOUT) };
  }

  async cosmetics_equip(uuid: string, slot: CosmeticSlot, itemId: string | null): Promise<void> {
    await sleep(70);
    this.loadouts[uuid] = { ...(this.loadouts[uuid] ?? DEFAULT_LOADOUT), [slot]: itemId };
    this.persist();
  }

  async hud_get(instanceId: string): Promise<HudConfig> {
    await sleep(60);
    const cfg = this.hudConfigs[instanceId] ?? DEFAULT_HUD;
    return { modules: cfg.modules.map((m) => ({ ...m })) };
  }

  async hud_set(instanceId: string, cfg: HudConfig): Promise<void> {
    await sleep(70);
    this.hudConfigs[instanceId] = { modules: cfg.modules.map((m) => ({ ...m })) };
    this.persist();
  }

  /* ── settings ── */

  async settings_get(): Promise<Settings> {
    await sleep(50);
    return { ...this.settings };
  }

  async settings_set(patch: Partial<Settings>): Promise<Settings> {
    await sleep(70);
    this.settings = { ...this.settings, ...patch };
    this.persist();
    return { ...this.settings };
  }

  async java_detect(): Promise<JavaRuntime[]> {
    await sleep(700); // scanning PATH / registry
    return JAVA_RUNTIMES.map((j) => ({ ...j }));
  }

  /* ── home ── */

  async news_feed(): Promise<NewsItem[]> {
    await latency();
    return NEWS.map((n) => ({ ...n }));
  }

  async servers_list(): Promise<ServerEntry[]> {
    await sleep(120);
    // live-ish ping jitter
    return this.servers.map((s) => ({
      ...s,
      pingMs: Math.max(6, Math.round(s.pingMs + (Math.random() * 14 - 7))),
      players: Math.max(0, Math.round(s.players * (0.94 + Math.random() * 0.12))),
    }));
  }

  async servers_save(list: ServerEntry[]): Promise<void> {
    await sleep(70);
    this.servers = list.map((s) => ({ ...s }));
    this.persist();
  }
}
