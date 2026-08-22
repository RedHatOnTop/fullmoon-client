# Fullmoon Client

풀문 네트워크 전용 Minecraft **Java** 클라이언트 — [Pinion](https://github.com/RedHatOnTop/pinion)
포크. launcher + in-game Fabric mod 두 조각 구조와 core 계약을 그대로 이어받되, 스코프를
**풀문 서버 접속** 하나로 좁히고 UI를 서버 중심으로 재구성한다. 치트 클라이언트가 아니다.
Target MC `26.1.2` (서버 = Paper 26.1.2 + Velocity).

- **[PLAN.md](./PLAN.md)** — 풀문 클라이언트 플랜 (source of truth).
- **[docs/BRIDGE.md](./docs/BRIDGE.md)** — 서버↔클라 bridge payload 프로토콜 스펙.
- **[docs/pinion-plan.md](./docs/pinion-plan.md)** — 업스트림 Pinion 플랜 (IPC 계약 §4, 마일스톤 증거 포함).
- **[brand.json](./brand.json)** — 제품명 단일 출처; `npm run rebrand`로 전파.

Two-piece:
- `launcher/` — 런처 (Tauri v2 + Rust core + React/TS skin).
- `pinion-mod/` — 인게임 클라 (Fabric mod). HUD·설정 GUI에 bridge 클라이언트(워프 GUI, 지도)를 얹는다.

Fork 관례:
- 업스트림은 `upstream` remote로 남긴다 (`git remote rename origin upstream` 완료).
- 공개 시 이 레포만 큐레이션해 올린다. GPL-3.0 계열 적용 예정.
