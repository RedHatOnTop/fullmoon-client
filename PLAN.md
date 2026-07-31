# Pinion — Feature Plan (launcher)

> Custom Minecraft **Java** client. **런처 + 클라이언트(Fabric mod)** 2-piece.
> Feather-grade = **UI/UX 중심**: 성능(Sodium) + 예쁜 껍데기 + 인게임 HUD/코스메틱.
> 치트·게임플레이핵 **아님**. 클라UI + 클라측 표시물만.

이 문서는 **core(백엔드) 쪽 기능 중심 스펙**이다. 여기 담긴 계약·인벤토리·토큰(§4~§6)은
**core 내부용**이며 UI 경쟁자에게 넘기지 않는다. UI 바이크오프는 의도적으로 **스코프 + 레퍼런스
(Lunar/Feather)만** 준다(→ `UI-BRIEF.md`). 두 갈래(UI-A: Claude, UI-B: 타 LLM)를 무제약으로
뽑아 **품질로 겨루고**, 이긴 쪽/좋은 아이디어를 사후에 core 계약에 어댑트·융합한다.

---

## 0. 이름 / 리브랜드 (one-touch)

- 제품명 단일 출처 = `brand.json` (`name`/`slug`/`bin`/`appId`/`scheme`/`accent`).
- `npm run rebrand` → `scripts/rebrand.mjs`가 전파:
  - `src-tauri/tauri.conf.json` → `productName`, `identifier`, `mainBinaryName`
  - `src-tauri/Cargo.toml` → `[[bin]] name`
  - Vite `define` → `__BRAND__` (프론트에서 `import`)
  - `src-tauri/build.rs` → `brand.json` 읽어 `PINION_NAME` 등 `env!` 상수 emit
- **Zed/Zetile식 compile-time assert(`APP_NAME == CARGO_BIN_NAME`) 안 씀.** assert는
  이름을 두 곳에 강제로 묶어 리네임을 아프게 만든다. 여기선 **generate**한다 —
  한 출처에서 파생. 리네임 = `brand.json` 한 줄 + `npm run rebrand` 1회. 끝.

---

## 1. 스코프

**IN (v1)**
- 계정/인증 (Microsoft OAuth → Minecraft, 멀티계정, 공식 런처 계정 import)
- 버전·인스턴스 설치 (piston-meta 매니페스트 구동, 격리 인스턴스 = MultiMC식)
- Fabric 통합 (loader + intermediary + Fabric API 자동)
- 실행 + 라이브 로그 콘솔 + 크래시 감지 + Quick Play(서버 직행)
- 1st-party 모드 번들: **Pinion HUD**(우리 Fabric mod) + Sodium/Lithium(성능)
- 클라측 코스메틱 (본인에게 보이는 cape/wings; config→mod)
- 설정 (Java/메모리/동시성/테마/언어/telemetry off) + HUD 모듈 설정
- 홈/뉴스/체인지로그 + 서버 즐겨찾기 → Quick Play 연동

**OUT (후속)**
- 서버-가시 코스메틱(백엔드 필요), Modrinth 브라우징, 런처 자동업데이트, 스킨 에디터

**Target MC = `26.1.2` (고정).**
버전은 하드코딩 아님 — live version-manifest의 config 값. 그 버전의 Java 레벨과 Fabric
아티팩트는 **설치 시 metadata에서 해석**한다. 추측/날조 금지 — 매니페스트가 정본이며,
매니페스트에 없으면 런처가 런타임에 그 사실을 보고한다(가짜 성공 X).

---

## 2. 아키텍처 — 융합의 seam

```
                brand.json ──┐
                             ▼
        ┌───────────────────────────────────────┐
        │  Rust core  (Tauri v2 commands)        │   ← 모든 로직. 안정 IPC 계약.
        │  auth · install · fabric · launch · fs │      (Claude가 소유·고정)
        └───────────────────┬───────────────────┘
                            │ tauri-specta (auto TS gen)
                            ▼
                   bindings.ts  (typed commands + events)   ← 고정 계약
                            │
                 ┌──────────┴──────────┐
            [ UI-A : Claude ]     [ UI-B : 타 LLM ]        ← 같은 계약 위 skin ×2
                 └──────────┬──────────┘
                     component-level 융합
                   (screen/component 단위 최고 파츠 조합)
```

**원칙: core 계약을 먼저 못박는다**(안정 seam). 단 이 계약은 **경쟁 UI엔 주지 않는다** —
경쟁자는 스코프 + Lunar/Feather 레퍼런스만 받고 자유롭게 뽑는다(품질 비교가 목적).
바이크오프 뒤, 이긴/융합된 UI를 아래 3개에 어댑트해 배선한다:
1. `bindings.ts` — 타입된 command/event 계약 (§4)
2. **Component inventory** — core가 기대하는 뷰 표면 (§5, 어댑트 참조)
3. **Design tokens** — `tokens.css` CSS 변수 (§6, 어댑트 참조)

Stack: **Tauri v2** + **Rust** core, **React + TS + Vite** front, **tauri-specta** 바인딩,
`keyring`(토큰 보관), `reqwest`(다운로드), `sha1`(무결성). 인게임 클라 = 별도 Fabric mod(§7).

---

## 3. 기능 (domain별)

### 3.1 Accounts / Auth
- Microsoft OAuth: **device-code** (헤드리스/편함) + **auth-code PKCE** (임베드 웹뷰) 둘 다.
- 체인: MSA token → Xbox Live → XSTS → Minecraft token → profile(uuid/name/skin/capes).
- 멀티계정, 전환, 추가/삭제, 토큰 자동 refresh, **OS keychain 보관**(평문 파일 금지).
- **Fast-path**: 공식 런처 `launcher_accounts.json` import (이미 로그인된 계정 흡수).
- **Offline/dev 모드**: 인증 없이 UI 개발 가능(타 LLM이 UI-B 만들 때 계정 불필요).

### 3.2 Versions / Instances
- Version manifest fetch(piston-meta) → release/snapshot 목록, `26.1.2` 타겟.
- 설치 파이프라인: client jar + libraries + asset-index + assets + native 추출 +
  **매칭 JRE 프로비저닝**(없으면 다운로드). 병렬 다운로드, SHA1 검증, resume.
- Instances = 격리 게임 디렉터리(버전/모드팩별 own mods·config·saves·resourcepacks).
- 무결성: 파일 해시 검증, 손상 감지 → 재다운.

### 3.3 Fabric
- fabric-meta에서 loader + intermediary mappings 해석·설치, `26.1.2` 호환 조합 선택.
- Fabric API 및 1st-party 모드 번들을 인스턴스 mods/에 배치.

### 3.4 Mods
- 1st-party: **Pinion HUD**(§7), Sodium, Lithium — 번들, 버전호환 체크.
- 인스턴스별 enable/disable, 충돌/버전 경고.

### 3.5 Launch
- JVM args 빌드(메모리 슬라이더 + GC 플래그), game args, auth 주입, Quick Play(서버 직행).
- 프로세스 관리: 실행, **라이브 로그 콘솔(event stream)**, 크래시 감지, "running" 상태.
- Pre-launch 체크: Java 존재/버전, 파일 무결성.

### 3.6 Cosmetics (클라측)
- Cape / wings / cosmetic item — **본인 클라에 렌더**(우리 mod가 그림).
- 런처 picker → HUD/cosmetic config 파일 write → mod가 read.
- (서버-가시 코스메틱은 백엔드 필요 = OUT.)

### 3.7 Settings + HUD config
- Java 경로/args, 메모리, 다운로드 동시성, 테마, 언어, telemetry OFF.
- HUD 모듈 설정(런처 ↔ 인게임 모드메뉴 미러): FPS/CPS/keystrokes/coords/장비/포션/핑
  각 toggle + 위치/스케일. 인스턴스별 `hud.json`으로 저장, mod가 읽음.

### 3.8 Home / Shell
- 뉴스/체인지로그(JSON 구동), Play 버튼, 선택 인스턴스/계정, 상태.
- 서버 즐겨찾기/최근 → Quick Play 직행.

---

## 4. IPC 계약 (융합 seam — 두 UI 공통, Rust가 tauri-specta로 생성)

```ts
// ── commands (invoke) ──────────────────────────────────────────
// auth
auth_begin_device_code(): DeviceCodePrompt          // { userCode, verificationUri, expiresIn }
auth_poll(session: string): AuthStatus              // pending | Account
auth_login_authcode(): Account
auth_import_official(): Account[]                    // launcher_accounts.json fast-path
auth_list(): Account[]
auth_select(uuid: string): void
auth_remove(uuid: string): void
auth_refresh(uuid: string): Account

// versions / instances
versions_manifest(): VersionSummary[]
instances_list(): Instance[]
instance_create(spec: InstanceSpec): Instance
instance_update(id: string, patch: InstancePatch): Instance
instance_delete(id: string): void
instance_install(id: string): string                // -> taskId; 진행은 event로

// mods
mods_available(): ModCatalog                         // 1st-party bundle
mods_list(instanceId: string): Mod[]
mod_toggle(instanceId: string, modId: string, enabled: boolean): void

// launch
launch(instanceId: string, opts?: LaunchOpts): string     // -> sessionId
launch_quickplay(instanceId: string, server: string): string
game_kill(sessionId: string): void
game_status(): GameState

// cosmetics / hud
cosmetics_catalog(): Cosmetic[]
cosmetics_equipped(uuid: string): Loadout
cosmetics_equip(uuid: string, slot: string, itemId: string | null): void
hud_get(instanceId: string): HudConfig
hud_set(instanceId: string, cfg: HudConfig): void

// settings
settings_get(): Settings
settings_set(patch: Partial<Settings>): Settings
java_detect(): JavaRuntime[]

// home
news_feed(): NewsItem[]
servers_list(): ServerEntry[]
servers_save(list: ServerEntry[]): void

// ── events (emit) ──────────────────────────────────────────────
"auth://device"     { userCode, verificationUri, expiresIn }
"download://progress" { taskId, file, done, total, bytesPerSec }
"install://stage"   { instanceId, stage, pct }        // manifest|libraries|assets|jre|fabric|mods|done
"game://log"        { sessionId, level, line }
"game://state"      { sessionId, state, exitCode? }   // starting|running|crashed|closed
```

이 `bindings.ts`가 계약이다. UI는 **여기 정의된 것만** 호출/구독한다. 새 니즈 →
Claude가 Rust core에 command/event 추가 → 바인딩 재생성 → 두 UI 모두 즉시 사용.

---

## 5. Component inventory (core 어댑트 참조 — 경쟁 UI엔 미제공)

core가 기대하는 뷰 표면. **경쟁자에겐 주지 않는다** — 바이크오프 승자/융합본을 계약에
배선할 때 이 표면으로 매핑하는 용도. 경쟁 UI가 다른 이름/구성을 써도 무방, 어댑터가 흡수.

| 화면 (Screen)        | 핵심 컴포넌트                                             |
|----------------------|----------------------------------------------------------|
| `HomeScreen`         | `PlayButton`, `AccountChip`, `InstancePicker`, `NewsFeed`, `ServerFavorites` |
| `AccountsScreen`     | `AccountCard`, `AddAccountFlow(device-code)`, `SkinPreview` |
| `InstancesScreen`    | `InstanceCard`, `CreateInstanceDialog`, `InstallProgress`  |
| `ModsScreen`         | `ModRow`, `ModToggle`, `PerfBadge`                        |
| `CosmeticsScreen`    | `CosmeticGrid`, `LoadoutSlots`, `PlayerRender`            |
| `SettingsScreen`     | `JavaPicker`, `MemorySlider`, `HudModuleEditor`, `ThemeToggle` |
| `LaunchConsole`      | `LogStream`, `StateBadge`, `KillButton`                  |
| shell                | `Sidebar`, `TitleBar`, `Toast`, `ProgressDock`            |

배선 시 각 컴포넌트는 §4 계약만 소비하는 순수 view로 어댑트한다(로직은 core에).

---

## 6. Design tokens (core 어댑트 레이어 — 경쟁 UI엔 미제공)

`src/styles/tokens.css` — CSS 변수 한 파일, accent는 `brand.json`에서 주입. 융합본을 브랜드에
맞출 때의 스왑 레이어. **경쟁자에겐 토큰을 강제하지 않는다** — 각자 디자인 언어대로 뽑고,
융합 단계에서 이 토큰으로 정규화한다.

```
--bg, --bg-elev, --surface, --border,
--text, --text-dim, --accent, --accent-dim,
--radius, --radius-lg, --shadow-1, --shadow-2,
--font-ui, --font-mono, --ease, --dur
```

품질 바(글로벌 output style): 프레임워크 기본 스캐폴드처럼 보이면 실패.
Feather/Lunar급으로 **의도적으로 디자인된, 눈에 띄게 구별되는** 껍데기여야 한다.

---

## 7. 인게임 클라 = Pinion HUD (Fabric mod, 별도 트랙)

- 별 디렉터리 `pinion-mod/` (Java/Kotlin, Fabric Loom, target `26.1.2`).
- v1 모듈: FPS·CPS·keystrokes·coords·장비·포션·핑 HUD + **인게임 모드설정 GUI**(런처 톤 일치)
  + zoom + fullbright + 클라측 cosmetic 렌더.
- 런처 ↔ mod 계약 = 인스턴스 dir의 `pinion/hud.json` + `pinion/cosmetics.json`(런처 write, mod read).
- 런처 마일스톤 뒤(M4)에 시작 — 지금은 계약(파일 스키마)만 예약.

---

## 8. 마일스톤

| M  | 내용                                                              | 검증 (launch & look)                    | 상태 |
|----|-------------------------------------------------------------------|-----------------------------------------|------|
| M0 | repo + brand + rebrand + tauri-specta 배선 + `versions_manifest` live | `26.1.2` 매니페스트가 실제로 떠온다      | done |
| M1 | Auth: device-code + 공식계정 import, refresh                       | 6년차 실계정 로그인 성공, 프로필 표시    | done |
| M2 | 설치 파이프라인: `26.1.2` + Fabric, 해시검증, JRE 프로비전          | 인스턴스 파일 디스크에 무결성 통과       | done |
| M3 | **Launch** — 우리 런처가 진짜 MC 창을 띄운다 (auth 주입)            | **MC 창 뜸. 스크린샷.** ← 핵심 증명      | done |
| M4 | Mods + Pinion HUD 첫 컷(FPS/coords/keystrokes) 인게임               | 인게임 HUD 렌더 스샷                     | done |
| M5 | Cosmetics + 폴리시 + rebrand 테스트                                | cape 본인 렌더 + 리네임 1회 동작         | done |
| M6 | 배포 산출물 — 릴리스 빌드 + NSIS 인스톨러 + 클린 첫 실행           | 설치된 exe가 계정 없는 상태에서 뜬다     | wip  |

M4 = 로컬 26.1.2 서버에 quick play로 접속한 실게임에서 FPS·CPS·XYZ·PING·키스트로크가
전부 렌더된 스샷. 인게임 모드설정 GUI·zoom·fullbright(§7 v1 잔여)는 M5로 넘어간다.

M5 증거 (전부 실행 후 확인, 로컬 26.1.2 서버):
- 인게임 모드설정 GUI 렌더 + 행 토글이 `pinion/hud.json`을 쓰고 HUD에서 모듈이 사라짐.
- zoom = C 홀드 fov 30(바닐라 하한. 20은 `Illegal option value`로 되돌아간다).
- fullbright = `LightmapMixin`이 ambient를 흰색으로. 자정 프레임 48% → 97% 밝기.
- cape = 런처 `cosmetics_equip` → 인스턴스 `pinion/cosmetics.json` → 본인 등에 렌더,
  해제하면 사라짐. 재시작 없음. 케이프 PNG는 런처 `public/capes/`에서 빌드 때 복사되므로
  미리보기와 인게임이 갈라질 수 없다.
- rebrand = `brand.json`을 Talon으로 바꾸고 `npm run rebrand` + 빌드 → `talon.exe`,
  창 제목·워드마크·뉴스 카피·데이터 루트(`%APPDATA%/Talon`)까지 Talon. 되돌리면 Pinion.
  숨은 하드코딩 4곳(워드마크, index.html title, 번들 카탈로그, pledge 문구)을 이 테스트가
  잡아냈다.
- 촬영 리그는 게임 자체 F2를 쓴다. 창을 화면 밖으로 파킹하면 DWM이 리다이렉션 표면 갱신을
  멈춰 `PrintWindow`가 옛 프레임을 돌려준다(측정: 켠 뒤 온스크린 97% vs 파킹 48%).

M6 증거 (2026-07-28, 전부 실행 후 확인):
- `npm run bundle` EXIT=0, 14m47s → `target/release/pinion.exe` 13.4 MB +
  `bundle/nsis/Pinion_1.0.0_x64-setup.exe` 3.5 MB.
- 무인 설치 `/S` exit 0 → `%LOCALAPPDATA%\Pinion\`에 exe + uninstall.exe +
  `resources\mods\pinion-hud.jar`(번들 모드가 인스톨러에 실제로 들어간다).
  제어판 항목 등록됨(DisplayName Pinion, DisplayVersion 1.0.0, UninstallString).
- 클린 첫 실행 = `PINION_DATA_ROOT`를 빈 디렉터리로 지정하고 설치본 실행. 계정 0·인스턴스 0
  상태로 렌더(우상단·독 CTA가 전부 "계정 추가"), 루트에 `settings.json`과 라이브
  `version_manifest_v2.json`(272 KB)이 생성 = 네트워크까지 산 채로 동작.
  `dirs`는 Known Folder API를 쓰므로 `%APPDATA%` 오버라이드로는 이 테스트가 불가능하다.

인게임 GUI 패스 (2026-07-31, 전부 로컬 26.1.2 서버에서 실행 후 확인):
- 모드 GUI를 런처와 같은 언어로 다시 그림. 새 `Ui.java` = 팔레트(런처 `tokens.css`
  dark 값 그대로) + 모서리 깎은 rect/border + 그림자 + 가로 그라디언트 + 자간 준 대문자
  + 애니메이션 pill. 배경을 모르는 채 떠 있는 표면이라 둥근 모서리는 "덮어 그리기"가
  불가능 — 픽셀을 **버려서** 만든다.
- `PinionSettingsScreen` = 좌측 레일(MODULES/VISUALS/KEYS) + 페이지 행. 열릴 때
  9px 상승 + 행 stagger, hover/토글은 프레임 독립 approach. 휠로 모듈 크기 조절,
  TAB/←→로 페이지 이동(키로 연 패널은 키로 걸을 수 있어야 한다).
- 새 페이지 VISUALS/KEYS. VISUALS(fullbright·zoom fov·zoom 감도)는 모드 소유 파일
  `pinion/client.json`에 저장 — 런처가 `hud.json`을 통째로 다시 쓰기 때문에 거기 넣으면
  지워진다. fullbright가 재시작을 넘겨 살아남는 게 요점.
- HUD 모듈 2개 완성(§7 v1 잔여): **gear**(투구·갑옷·주손·오프핸드 아이콘 + 바닐라
  내구 표시), **potion**(효과별 고유 색 눈금 + 남은 시간, 200틱 미만은 poppy).
  FPS/PING은 값에 따라 bone→ochre→poppy. 키캡은 뗀 뒤 잔광이 사라진다.
- 촬영 리그: `scripts/_rcon.mjs`(서버 콘솔이 없어 장비·효과를 줄 방법이 없었다) +
  `scripts/panel-drive.mjs`(합류를 `list`로 확인 → 레이아웃 write → 장비/효과 →
  HUD·키홀드·패널 3페이지 촬영). `shot-game.ps1`에 `-HoldKey/-HoldMs` 추가 —
  키스트로크 모듈은 키가 실제로 눌린 동안에만 보여줄 게 있다.
- 함정: 데브 빌드의 `paths::resources`는 `target/debug/resources`로 풀린다. 런처가
  실행 때마다 그 jar를 인스턴스로 복사하므로, 거기 갱신하지 않으면 새 jar를 넣어도
  **옛 GUI가 뜬다**(한 번 당함).

M6 착수 상태:
- 개발 내내 `cargo build` + 수동 vite였고 tauri CLI가 없어 인스톨러를 만들 수 없었다.
  `@tauri-apps/cli`를 devDependency로 넣고 `npm run bundle`로 nsis 타깃을 돈다.
- `brand.json.msClientId`가 비어 있어 클린 머신에서 device-code/브라우저 로그인은
  Azure 앱 등록 전까지 `no Microsoft application id is configured`로 막힌다.
  공식 런처 계정 import와 오프라인 프로필은 영향 없음.

UI 바이크오프(UI-A/UI-B)는 **스코프 + Lunar/Feather 레퍼런스만** 주고 무제약 병렬 →
품질로 겨뤄 승자/융합본을 M3 이후 core 계약에 어댑트·배선.

---

## 9. 검증 바 (project CLAUDE.md)

시각 프로젝트. **green 테스트 = 렌더 증거 아님.** 각 마일스톤은 띄워서 눈으로 본다.
M3에서 실제 Minecraft 창이 우리 런처 auth로 떠야 "런처 됨"이라 부른다. 스샷 남긴다.

## 10. 툴체인 / 법적 메모

- 박스 현황: **JDK 25.0.3**(2026 MC의 Java 21+ 요구 커버), Node v22.12, Tauri v2.
- 법적: 런처는 Mojang piston-meta/CDN에서 에셋 다운(재배포 X). 우리 mod = 클라UI/표시물만.
  이름 `Pinion` = Feather/Minecraft 상표 회피. Mojang EULA·브랜드 가이드라인 준수.
