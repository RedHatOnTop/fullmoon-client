# Fullmoon Client

풀문 네트워크 전용 Minecraft **Java** 클라이언트. launcher + in-game Fabric mod,
두 조각. 치트 클라이언트가 아니다 — 서버가 모든 권한을 가지고, 클라는 같은 기능을
더 보기 좋게 보여줄 뿐이다. Target MC `26.1.2` (서버 = Paper 26.1.2 + Velocity).

- 설치하면 곧바로 풀문 로비로 떨어지는 원클릭 접속기
- 서버가 보내는 데이터로 그리는 **네이티브 워프 GUI** — ChestGUI 때우기의 대체
- 계정(Microsoft OAuth) · 관리형 단일 인스턴스 · Sodium/Lithium · HUD/설정 GUI ·
  줌/풀브라이트/코스메틱 내장
- 서버 주소: `play.fullmoon.ink` · 웹사이트: [fullmoon.ink](https://fullmoon.ink)

## 저장소 안내

| 문서 | 내용 |
|---|---|
| [docs/BRIDGE.md](./docs/BRIDGE.md) | 서버↔클라 `fullmoon:v1` 채널 프로토콜 스펙 (공개 계약) |
| [THIRD-PARTY-NOTICES.md](./THIRD-PARTY-NOTICES.md) | 번들·런타임 서드파티 구성요소와 라이선스 |
| [site/](./site/) | 소스 해설 사이트 (GitHub Pages — 아키텍처 워크스루) |

## 구성

- **`launcher/`** — Tauri v2 런처. Rust core(계정·설치·실행·경제 조회) + React/TS UI.
  `src/core/bindings.ts`가 UI와 core의 유일한 접점(IPC 계약)이다.
- **`pinion-mod/`** — 인게임 Fabric mod. HUD·설정 GUI에 더해, 서버가
  `fullmoon:v1` 채널로 보내는 데이터로 워프 네이티브 스크린을 렌더한다.

## 빌드

```bash
# launcher (Node 20+, Rust stable)
cd launcher && npm ci && npm run build        # frontend (tsc + vite)
cd src-tauri && cargo build --release         # core + bundle (NSIS는 Windows에서)

# mod (JDK 25; 없으면 Gradle이 자가 프로비전한다)
cd pinion-mod && ./gradlew build              # build/libs/pinion-hud-*.jar
```

`.github/workflows/`가 위 두 조각을 CI로 검증하고, 태그를 올리면 서명된
인스톨러 + `SHA256SUMS`를 GitHub Release로 만든다.

## 신뢰 모델

- **감지는 편의일 뿐, 신뢰가 아니다.** 클라가 `fullmoon:v1` 채널을 열어도
  tp·상거래의 권한·쿨다운 검증은 서버가 감지와 무관하게 수행한다. 위조 클라를
  전제로 설계됐다 — 스펙 전문은 [docs/BRIDGE.md](./docs/BRIDGE.md).
- **배포 경로는 GitHub Release 하나.** 릴리스마다 SHA256SUMS가 함께 올라가고,
  OTA 업데이트는 서명된 manifest로만 이루어진다.
- **우리가 만든 것은 전부 exe 하나에 번들**된다. 유일한 예외는 Mojang 원본
  에셋(재배포 금지)이며, 런처가 다운로드해 Mojang이 공개한 SHA1으로 검증한다.

## 라이선스

GPL-3.0 — [LICENSE](./LICENSE). 서드파티 구성요소는
[THIRD-PARTY-NOTICES.md](./THIRD-PARTY-NOTICES.md)에 정리돼 있다.
Minecraft는 Mojang Synergies AB의 상표이며, 이 프로젝트는 Mojang과 무관하다.
