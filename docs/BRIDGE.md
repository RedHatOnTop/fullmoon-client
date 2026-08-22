# BRIDGE.md — fullmoon:v1 프로토콜 스펙 (v0 초안)

서버(Paper `fullmoon-bridge`) ↔ 클라(`fullmoon-mod`) 간 채널. 이 문서는 공개된다
(오픈소스 전제) — 따라서 **위조를 전제**한다: 프로토콜은 UX 배분만 하고, 모든 게임플레이
효과(tp·상거래)의 권한·쿨다운 검증은 서버가 감지 결과와 무관하게 수행한다.

## 1. 채널 / 등록

- 채널명: **`fullmoon:v1`** (namespaced, Bukkit Messenger + Fabric networking 표준 형식).
- 클라는 login/config 단계에서 채널을 등록한다. 서버는 join 직후 등록 여부를 확인한다.
- 등록됨 = 후보. **handshake 완료 = 지원.** 두 단계로 나누는 이유: mod가 깔렸지만 깨졌거나
  서버가 구버전인 경우를 조용히 걸러내기 위함.

## 2. Handshake

```
C → S   {"type":"hello", "proto":1, "client":"fullmoon", "version":"0.1.0"}
S → C   {"type":"welcome", "proto":1, "waypoints":[...]}
```

- 인코딩: 채널 페이로드 = UTF-8 JSON 한 객체, 최상위에 항상 `"type"`.
- `proto`: 정수, 호환 규칙 — 서버가 더 크면 클라는 bridge 비활성(구버전 서버 보호),
  클라가 더 크면 클라가 자기 기능을 clamp. minor 기능 추가는 proto 올리지 않고 필드 추가로.
- **timeout 5s**: hello 후 welcome이 안 오면 클라는 이 세션 동안 조용히 비활성.
  서버도 등록만 되고 handshake 없는 플레이어에게 네이티브 스크린을 유도하지 않는다(폴백 사용).
- 재협상은 없다. 판단은 로그인 세션당 1회.

## 3. 운영 페이로드

| dir | type | 내용 |
|-----|------|------|
| S→C | `welcome` | handshake 응답 + 웨이포인트 전체 스냅샷 |
| S→C | `waypoint_sync` | 스냅샷 교체 (풀 스냅샷 방식 — delta 없음. POI는 수십 개 규모) |
| C→S | `tp_request` | `{"type":"tp_request","id":"<wp id>"}` |
| S→C | `tp_result` | `{"type":"tp_result","id":"...","ok":true}` 또는 `{"ok":false,"reason":"cooldown"}` |
| S→C | `screen_open` | `{"type":"screen_open","screen":"warp","data":{...}}` — 서버 주도 스크린 오픈 |

웨이포인트 객체:

```json
{
  "id": "palace_gate",
  "name": "만월궁 정문",
  "icon": "moon",
  "x": 500, "y": 72, "z": -140,
  "world": "lobby",
  "group": "palace",
  "perm": "warp.palace"
}
```

- `perm`: 서버가 tp 시점에 검사하는 권한 키. 클라는 목록에서 미허가 항목을 숨길 뿐,
  숨김 자체는 최적화일 뿐 신뢰 아님.
- 페이로드 크기: 플러그인 메시지 패킷 한계(≈32KiB) 내. v1 스케일(수십 POI)에서 청킹 불필요 —
  넘으면 그때 `waypoint_sync`를 페이지로 쪼갠다(proto bump).

## 4. 폴백 (바닐라/타 클라)

- `/워프` 커맨드: 목록 출력 + `<id>` 인자 실행. 권한·쿨다운 로직은 tp_request와 **동일 코드 경로**.
- 필요시 ChestGUI 메뉴(기존 서버 관습) — bridge는 이걸 대체하지 않고 병존한다.
- 서버는 클라 지원 여부에 따라 같은 기능을 어느 표면으로 보낼지 고를 뿐, 기능 집합이 달라지지
  않게 한다.

## 5. 서버 권위 규칙 (fullmoon-bridge 구현 요구)

1. `tp_request` 처리: perm → 쿨다운(moonportals와 동일 4000ms 글로벌) → world 일치 →
   좌표 유효성(웨이포인트 레지스트리의 것과 일치하는지만 허용 — 임의 좌표는 프로토콜에 없음).
2. 감지(등록+handshake)는 **렌더 편의 선택**에만 사용. 권한 상승·검증 생략과 무관.
3. 모든 reject는 `tp_result{ok:false}`로 응답 — 클라 UI가 추측하지 않게.
4. 로그: hello/welcome/tp_request 결과를 서버 로그에 남긴다(디버그 + 남용 추적).

## 6. 열린 항목

- **Velocity 통과 검증**: BungeeCord 채널은 증명됨(moonportals). 임의 커스텀 채널의
  player↔backend relay는 FM2에서 로컬 Velocity로 먼저 확인. 막히면 velocity 설정/포크로 풀기.
- `screen_open`의 v1 범위는 `warp` 하나. 상점·이벤트 스크린은 서버 콘텐츠 확정 후 proto 1 내
  필드 확장으로.
