# Fullmoon Client — Plan

> **풀문 네트워크 전용 클라이언트.** Pinion 포크. 일반 런처 용도의 가치는 낮다는 걸 전제로,
> UI 구성을 "범용 런처"에서 **"풀문 접속기"**로 재구성한다. 업스트림의 core 계약(`bindings.ts`),
> 설치·실행 파이프라인, HUD mod 자산은 그대로 출발점.
> 업스트림 원문은 [docs/pinion-plan.md](./docs/pinion-plan.md) (§4 IPC 계약 유효).

Target MC = `26.1.2` 고정 (Paper 26.1.2 + Velocity 프록시, 로비 `fullmoon_v5`).

---

## 0. 제품 정의

한 문장: **설치하면 곧바로 풀문 로비로 떨어지는, 예쁜 접속기.**

- 런처 = 계정 1개 + 관리형 인스턴스 1개 + 큰 Play 버튼. 멀티인스턴스/버즈워드 탐색 같은
  범용 기능은 고급 화면으로 강등하거나 잘라낸다.
- 인게임 mod = HUD(업스트림 자산) + **bridge**: 서버가 주는 데이터로 워프 GUI·지도 모드를
  네이티브 스크린으로 렌더. ChestGUI 때우기를 대체하는 게 이 포크의 존재 이유.
- 바닐라/타 클라 접속자가 2등 시민이 되지 않게, 편의 기능은 전부 서버 측 폴백을 갖는다
  (`/워프` 커맨드 + ChestGUI). 클라는 그걸 **더 보기 좋게** 보여줄 뿐이다.

## 1. 스코프

**IN**
- 계정 (device-code + 공식계정 import — 업스트림 M1 완료 자산)
- 관리형 단일 인스턴스: `26.1.2` + Fabric + Sodium/Lithium + `fullmoon-mod` 번들,
  손상 시 자가 복구 (업스트림 M2 파이프라인 재사용)
- 홈 = 서버 허브: Play(Quick Play → 로비), **서버 상태**(server-list ping: 온라인/접속자 수),
  뉴스(JSON 구동), 계정 칩
- bridge 클라이언트(mod): handshake, 웨이포인트 동기화, **워프 네이티브 스크린**,
  **지도 모드**(BlueMap 타일 뷰어), 타이틀 스크린 "풀문 서버 접속" 버튼
- **소식 센터**: 공지·패치노트 — 정적 JSON(GitHub Pages) 구동, `news_feed` 계약 재사용.
  디스코드 연동은 후행
- **경제 카드**: 잔액 + 입출 스탯(reason/source별). economy-api + `transactions` 테이블
  재사용. 인증 = bridge가 접속 세션에 단기 토큰 발급 → mod가 인스턴스 파일 경유 전달
  (`hud.json`과 같은 런처↔mod 파일 계약) → 런처가 API 호출. "실접속자만 조회"가 기본 보장
- HUD/설정 GUI/zoom/fullbright/cosmetic — 업스트림 M4–M5 자산 유지
- 한국어 기본 (`i18n.ko` default)
- **자동 업데이트(OTA)**: Tauri v2 updater 플러그인, 서명된 manifest. OUT→IN 확정 —
  디스코드 배포 시점부터 MC 패치마다 수동재배포는 불가능

**OUT (후속 재판단)**
- 멀티서버 즐겨찾기 관리 (풀문 하나면 된다 — 설정에 주소 필드만)
- Modrinth 브라우징, 스킨 에디터
- 제로클릭 자동접속(exe 열면 무조건 게임) — 첫 설치 수 GB·크래시 루프·계정 만료 케이스에서
  UX 역효과. 원클릭이 하한선

## 1.5 신뢰/배포 원칙 (transparency stack)

- **배포 경로 단일화**: 다운로드는 GitHub Release assets만. 제3 미러 없음.
- **무결성**: 릴리스마다 SHA256SUMS 생성 + 서명, 릴리스 노트에 게시. VirusTotal 스캔 링크 첨부.
- **번들 원칙**: 우리가 만들거나 골라 넣은 것(mod jar, 리소스팩, 뉴스 카피)은 전부 설치 exe
  하나에 번들 — 업스트림 M6에서 mod가 인스톨러에 실림을 확인했다. **유일한 예외는 Mojang
  원본 에셋**(piston-meta/CDN, 재배포 금지)이며 이 사실과 SHA1 검증 동작을 문서로 명시한다.
  숨길 것이 없다는 선언 자체가 신뢰다.
- **소스 해설**: 별도 사이트 대신 GitHub Pages 한 벌 — 아키텍처 개요, BRIDGE.md(공개 스펙),
  핵심 파일 워크스루. 1차 해설은 코드 자체(GPL-3.0) + CI 태그 빌드로 "이 바이너리 == 이 커밋"
  성립. Sodium/Lithium 번들의 재배포 조건은 FM6 전에 각 라이선스 확인.

## 2. 런처 UI 재구성 (Pinion component inventory → Fullmoon)

| Upstream screen | Fullmoon 처리 |
|---|---|
| `HomeScreen` | **서버 허브로 재설계.** 워드마크 + 달 무드, `PlayButton`(로비 직행), `ServerStatusCard`(ping), `NewsFeed`(공지·패치노트 탭), `EconomyCard`(잔액·입출 스탯), `AccountChip`. 인스턴스 선택 개념 노출 안 함. 첫 설치 완료 후엔 홈 스킵 원클릭 접속(설정 해제 가능) |
| `InstancesScreen` | **제거** (사용자 피드백: 단일 버전만 유지보수). 인스턴스는 선택지가 아니라 상태 — PlayDock에 정적 칩으로 표시, 설치·복구는 큰 버튼과 Settings가 담당. 코어가 관리형 인스턴스를 자가 프로비전 |
| `AccountsScreen` | 유지 (멀티계정은 가족 공유 PC 케이스가 있어 남긴다) |
| `ModsScreen` | 유지, 카탈로그 = Sodium/Lithium/fullmoon-mod 고정 3종 |
| `CosmeticsScreen` | 유지 |
| `SettingsScreen` | 유지 + 서버 주소(기본 고정, override 가능) |
| `Console`/shell | 유지 |

디자인 언어: **fullmoon.ink("달빛 밤하늘")가 단일 출처다.** 런처 tokens.css와 mod Ui.java
팔레트는 사이트(cardTheme.js 유래)의 밤하늘 네이비 + 문라이트 골드 + Pretendard를 값 그대로
미러링한다 — 폰트도 자체 호스팅(Pretendard 3 weights, font CDN 없음). 홈 배경의 달/별
무드는 FM1에서 사이트의 #sky 캔버스 모티프를 따라 눈으로 보면서 조절한다.
품질 바 동일: **스캐폴드처럼 보이면 실패.**

## 3. 인게임 mod 트랙 (pinion-mod → fullmoon-mod)

- 기반: 업스트림 HUD 모듈 + `PinionSettingsScreen`(런처 디자인 언어 Ui.java) 그대로.
- 신규: **bridge 클라이언트** ([docs/BRIDGE.md](./docs/BRIDGE.md))
  - login/config phase에서 `fullmoon:v1` 채널 등록 → 서버 handshake 응답 대기(5s timeout,
    실패 시 조용히 비활성 — mod만 깔렸고 서버가 구버전인 경우 등)
  - `waypoint_sync` 수신 → 워프 목록 상태 보관
  - `screen_open` 수신 → 네이티브 스크린 렌더 (v1: `warp`)
  - 워프 스크린에서 선택 → `tp_request` 전송. 실행/거절은 서버 몫, 클라는 결과 toast만.
  - **지도 모드**: fullscreen map screen. v1 = BlueMap flat 타일 HTTP fetch + pan/zoom +
    자기 마커(서버가 `waypoint_sync`에 self 좌표 포함 or 로컬 coords 모듈 재사용).
    Xaero식 로컬 미니맵은 OUT — bluemap-serve가 이미 돌고 있으니 재투자.
  - **타이틀 스크린 버튼**: 바닐라 타이틀에 "풀문 서버 접속" 추가(Quick Play connect).
    싱글월드에서 놀다 돌아오는 경로용. Feather(현 Dawn) 패턴.
  - **세션 토큰 릴레이**: 서버가 발급한 단기 economy 토큰을 인스턴스 파일로 write →
    런처가 읽어 economy-api 호출(§1 경제 카드).
- 감지 규칙(중요): 채널 등록·handshake 성공 = **UX 스위치일 뿐 신뢰 아님.** tp/상거래 검증은
  서버가 감지 결과와 무관하게 수행. 위조 클라가 payload를 흉내 내도 서버 권위가 전부 흡수.

## 4. 서버 측 컴포넌트 (minecraft-server-project/plugins-src/fullmoon-bridge)

- Paper plugin, moonportals 형식 준수. v1 기능:
  - join 시 채널 등록 감지 → handshake → `waypoint_sync`(POI 스냅샷: 로비 프로그램
    zones 2–15 확정 좌표에서 생성)
  - `tp_request` 검증(권한·쿨다운·같은 월드·거리) 후 teleport, 결과 응답
  - 접속 세션에 단기 economy 토큰 발급 → bridge 채널로 mod 전달 (§1 경제 카드 인증)
  - 미지원 클라 폴백: `/워프` 커맨드 (+ 필요시 ChestGUI)
- 열린 검증 항목: **custom plugin channel의 Velocity 통과.** BungeeCord 채널(moonportals)은
  증명됐으나 임의 채널 relay는 FM2에서 로컬로 먼저 확인한다.

## 5. Bridge 프로토콜

스펙 본문: [docs/BRIDGE.md](./docs/BRIDGE.md). 요약:
`fullmoon:v1` 등록 → C→S `hello{proto}` → S→C `welcome{proto, waypoints[]}` (5s 내) →
운영 페이로드(`waypoint_sync`, `screen_open`, `tp_request`, `tp_result`). 버전 필드로
양방향 하위호환. 전부 서버 권위, 프로토콜 문서는 공개(오픈소스 전제).

## 6. 마일스톤

| M   | 내용 | 검증 (launch & look) | 상태 |
|-----|------|----------------------|------|
| FM0 | 포크 + 리브랜드 + 빌드 green | `npm run rebrand` diff + tsc/vite/cargo 통과 | wip (rebrand 완료, 빌드 미검증) |
| FM1 | 런처 홈 = 서버 허브 재설계 | 홈 스크린샷 — 인스턴스 개념 노출 0, ping 표시 | |
| FM2 | fullmoon-bridge(Paper) v1 | 바닐라 클라 폴백(`/워프`) + mod 클라 handshake 로그 | |
| FM3 | mod 워프 네이티브 스크린 | 인게임 스크린샷 + 실제 tp 이동 | |
| FM4 | 지도 모드 (BlueMap tiles) | 인게임 맵에서 만월 궁 식별 스샷 | |
| FM5 | 런처 폴리시: 소식 센터 + 경제 카드 + 원클릭 접속 플로우 | 전체 플로우 스샷 세트 — 잔액·공지가 홈에 실제 데이터로 렌더 | wip (UI 착상·mock 완료: 재화/서버 상태 카드, 콘솔 화면 제거하고 오버레이가 실세계면. 실데이터는 bridge 세션 토큰 후) |
| FM6 | 배포: NSIS + OTA(Tauri updater, 서명 manifest) + 신뢰 스택(§1.5) | 클린 PC 설치 → 로비 접속까지 + SHA256SUMS·VT 링크 게시 | |

FM0–FM3가 최소 제품. FM4부터가 차별화.

## 7. 검증 바 / 법적

- 업스트림과 동일: **green 테스트 = 렌더 증거 아님.** 각 마일스톤 띄워서 눈으로 보고 스샷 남긴다.
- 라이선스: GPL-3.0 계열 (launcher + mod). 에셋은 별도 고지. 릴리스는 CI 태그 빌드 + 산출물
  해시 공개로 "이 바이너리 == 이 커밋" 성립시킨다.
- Mojang piston-meta/CDN 다운로드(재배포 금지) 준수 — 업스트림 파이프라인이 이미 그렇게 동작.
- 공개 레포는 이 히스토리에서 큐레이션해 분리. distribution-project 루트의 키 파일들이
  섞이지 않게 한다.
