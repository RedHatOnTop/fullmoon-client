# Pinion

Custom Minecraft **Java** client — launcher + client(Fabric mod). Feather/Lunar-grade,
**UI/UX-focused**: performance (Sodium) + a polished shell + in-game HUD/cosmetics.
Not a cheat client. Target MC `26.1.2`.

- **[PLAN.md](./PLAN.md)** — feature-centric plan, IPC contract, milestones (source of truth).
- **[UI-BRIEF.md](./UI-BRIEF.md)** — paste-ready brief to hand any LLM for a UI variant.
- **[brand.json](./brand.json)** — single source of the product name; `npm run rebrand` propagates.

Two-piece:
- `pinion/` — launcher (Tauri v2 + Rust core + React/TS skin). Core logic + fixed contract.
- `pinion-mod/` — in-game client (Fabric mod, Java/Kotlin). Starts after launcher M3.

Fusion model: Rust core exposes a fixed typed contract (tauri-specta → `bindings.ts`). Two UI
variants are built over the same contract + component inventory + design tokens, then fused
component-by-component.
