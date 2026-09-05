# 출석 클레임 설계 — 디스코드 전용 기능의 런처 확장 (구현 전 승인 필요)

> 상태: **설계 완료, 구현 전**. 이 문서가 승인되어야 economy-api에 쓰기 표면이
> 생긴다. 2026-07-12부로 economy-api는 읽기 전용이 아키텍처 결정(server.js 헤더
> 참조)이고, 이 설계는 그 결정의 **조건부 부분 철회**를 담고 있다.

## 목표

디스코드에서만 가능하던 `/출석`을 런처에서도 클레임하게 한다. 핵심 원칙:

1. **하루 한 번은 계정 단위로 유지** — 디스코드에서 받으면 런처에서 못 받고 그
   반대도 마찬가지. 이것은 새 설계가 아니라 기존 멱등키가 이미 보장한다.
2. **한 가지 지급 코드** — economy-api가 봇의 지급 로직을 포크하지 않는다.
   드리프트는 곧 재화 무결성 사고다.
3. **새로운 쓰기 인증은 공유 비밀이 아니라 소유 증명** — "새 토큰"이 아니라
   마인크래프트 세션 서버 핸드셰이크.

## 인증: MC 세션 증명 (bearer 금지 규칙을 통과하는 유일한 길)

2026-07-12 결정의 본질은 "유출된 키가 돈을 주조해서는 안 된다"다. 공유 토큰은
그 위협을 못 넘기지만, 마인크래프트 세션 핸드셰이크는 넘는다 — 어떤 비밀도
배포하지 않고 계정 소유를 증명한다:

```
런처                                 economy-api            Mojang
  │ 1. GET /v1/daily/challenge ────────▶ │
  │ ◀── { serverId, expiresAt } ─────── │  (serverId = 랜덤 16B hex,
  │                                      │   5분 TTL, 메모리 보관)
  │ 2. POST sessionserver.join ─────────────────────────────────────▶ │
  │    { accessToken, selectedProfile: uuid, serverId }                │
  │ 3. POST /v1/daily/claim ───────────▶ │
  │    { username, uuid, serverId }      │ 4. GET hasJoined?username─▶ │
  │                                      │ ◀── { id: uuid, name } ──── │
  │                                      │  5. id==uuid 검증 → 지급
  │ ◀── { wallet, granted } ──────────── │
```

- `join`은 런처가 보유한 Microsoft accessToken으로 호출 — 이미 `auth.rs`가
  세션을 갖고 있다 (`auth::session(uuid)`).
- economy-api는 `hasJoined` 응답의 `id`가 요청 uuid와 일치하는지 확인한다.
- 챌린지는 1회용(검증 성공/실패와 무관하게 소비), 레이트리밋은 기존 버킷 공유.
- 이 인증으로 얻는 것: **계정 소유 증명**. 잃는 것: 없다 — 배포되는 비밀이 없다.

## 지급: 포크 금지, 공유 임포트

`economy-api/src/server.js`는 이미 봇 저장소를 임포트한다
(`../../coin-bridge-bot/src/economy-breakdown.js` — 같은 박스, 같은 레이아웃).
클레임 엔드포인트도 같은 방식으로 **봇의 순수 모듈**을 가져다 쓴다:

| 필요 | 가져오는 곳 | 순수성 |
|---|---|---|
| 지급(원장 쓰기) | 봇의 `grant` — `commands.js`가 쓰는 것과 동일 함수를 `economy/` 순수 모듈로 **추출** 후 양쪽이 임포트* | 추출 필요 (아래) |
| 스텝 보너스 | `economy/streakPolicy.js` — `STREAK_MILESTONES`, `streakBonus` | 순수 확인됨 |
| 라벨 | `economy/cardTheme.js` — `txLabel` | 순수 확인됨 (이번에 이미 임포트함) |
| 설정 값 | `economy_config` 테이블 직접 SELECT (`daily.amount`, `daily.milestone_*`, `reward.multiplier`, `faucet.daily_cap`) | SQL |

\* 현재 `grant`는 Discord 결합 모듈(economy/index.js — discord.js 임포트) 안에
있다. **먼저 grant를 discord.js 없는 순수 모듈로 추출**하고 봇과 economy-api가
함께 임포트하게 한다. 이 추출이 이 설계의 유일한 봇 쪽 변경이다.

## 정책 — 봇의 `cmdDaily`와 정확히 동일

- 기본액 `daily.amount` (기본 10), `reward.multiplier` × `currentFaucetBoost` 스케일
- 스텝 보너스: `bot_daily_streaks`의 streak → 3/7/14/30일 마일스톤 (`daily.milestone_*`)
- **부스터/서버태그 보너스는 미적용** — Discord 전용 혜택으로 남긴다 (런처 클레임이
  디스코드 참여를 대체하지 않는다)
- 멱등키: **`ref_id = "daily:<UTC yyyy-mm-dd>"` 그대로** — 봇과 같은 키라서
  "한 계정 하루 한 번"이 두 표면에서 공유된다. 유니크 제약이 곧 가드.
- 한도: `faucet.daily_cap` 초과 시 `capped` 응답 (봇과 동일 문구 정책)
- 원장 쓰기는 한 트랜잭션: `UPDATE balances ... RETURNING amount` +
  `INSERT transactions (..., reason='discord.daily', source='bot', ref_id, balance_after)`
  — source를 바꾸지 않는다: 재화 대시보드(`TX_FILTERS`)와 /경제현황 숫자가
  오염되지 않아야 한다. 출처 구분이 필요해지면 metadata에 `"surface":"launcher"`를
  남긴다.
- 스텝 지속은 **지급 성공 후에만** (`bot_daily_streaks` upsert) — 재시도로 스텝이
  부풀 수 없다.

## 구현 단위

| 층 | 내용 |
|---|---|
| economy-api | `POST /v1/daily/challenge` · `POST /v1/daily/claim` — `ECONOMY_WRITE_MODE=daily` env가 **없으면 501**("쓰기 표면 비활성"), 기본값 off → 프로덕션 배포 전까지 행동 변화 없음 |
| 런처 코어 | `economy.rs`에 챌린지/클레임 클라이언트 — `auth::session`으로 join 호출 |
| 런처 UI | 재화 탭에 출석 카드 — 상태 4개: 미연결(디스코드 링크) / 오늘 이미 수령 / 수령 가능(클레임 버튼) / 한도 도달. 성공 시 잔액·장부 즉시 갱신 |
| 테스트 | economy-api: 챌린지 1회성·만료, hasJoined 불일치 거부, ref_id 중복 already-granted, capped — 전부 가짜 풀로 |

## 배포 순서 (승인 후)

1. 봇: `grant`를 순수 모듈로 추출, 봇 동작 무변화 확인 (봇 테스트)
2. economy-api: 클레임 엔드포인트 배포 — **ECONOMY_WRITE_MODE 미설정 상태로**
   (501만 반환하는 배포)
3. 런처: 코어+UI 배포 (클레임 버튼은 501을 만나면 "디스코드에서 받아주세요" 상태로
   열화 — 기능 플래그 없이 자연스럽게)
4. 운영자가 `ECONOMY_WRITE_MODE=daily` 설정 + 재시작 → 기능 ON
5. 첫 주: 원장 감시 — `SELECT * FROM transactions WHERE reason='discord.daily' AND metadata ? 'surface'`