# FUSION — the three-way bake-off verdict, piece by piece

The launcher is the first impression; it can matter more than the client itself.
This is the decision record for fusing `ui-a` (mine), `ui-b` (anon model 1) and
`ui-c` (anon model 2) into **`launcher/`** — the real product UI.

## Verdict in one line

**ui-c is the skeleton, ui-b is the chrome, ui-a is the game-feel.**

## Base = ui-c (architecture wins)

Kept wholesale, because it is the only one built like a product:

- `core/bindings.ts` — the PLAN §4 IPC contract as a typed seam; mockCore
  implements it, the Tauri core will replace it. **This is the fusion's spine.**
- `state/store.tsx` — event-driven store (install://stage, game://log,
  game://state, download://progress), optimistic launch, persistence.
- `i18n/` ko+en, `styles/` token system with dark/AMOLED themes, accent
  propagation, `ui.tsx` kit (Button/Modal/Segmented/Slider/Badge/…).
- Screens kept as-is: **Instances** (MultiMC-style isolation), **Accounts**
  (multi-account, keychain note), **Settings** (JDK auto-detect — the single
  most "real launcher" feature of the three), **Console**, **Mods**.
- `PlayDock` — the bottom dock with the full state machine
  (no-account → no-instance → installing(inline bar) → install → starting →
  running). Best play button of the three; stays.
- `brand.json` wiring via vite `__BRAND__` define + runtime import.

## From ui-b (shell chrome + hero voice)

- **Top bar** above the content: screen title + subtitle, search button,
  notification bell, **account chip with dropdown** (add account / import
  official / manage). ui-c had only a titlebar; this is what makes it read
  as a product, not a page.
- **Command palette (Ctrl-K)** — launch, navigate, quick-join. No mainstream
  launcher ships this; it is the "engineered" tell.
- **Typographic hero voice** — "Built lighter. Fly further." eyebrow +
  two-line display type + meta chips (files verified · N mods · N GB).
  Replaces ui-c's news-carousel-as-hero; news moves below.
- **Toast copy style** ("All set / …") — merged into ui-c's toast system.

Not taken: monolith structure (30KB App.tsx), hardcoded nav badge, console as
side-dock (ui-c's console screen is deeper).

## From ui-a (game-feel)

- **`Skin3D`** — real skinview3d player render (skin + cape + idle/walk).
  Goes into: Home player card, Cosmetics preview stage (replaces the
  low-poly `PlayerRender`).
- **Skin/cape PNG assets** + `generate-assets.mjs` (pngjs) — regenerated to
  match ui-c's cosmetic catalog ids.
- **VoxelIsland** SVG — the "it's Minecraft" cue, floats in the hero.
- **Launch overlay** — the cinematic staged-progress + color-coded live
  console moment, rewired to the real store (game://log stream) instead of
  a canned script. "Hide" collapses into the PlayDock running state.

Not taken: hash routing (store screen state is richer), mock/data.ts
(bindings + mockCore replace it).

## Contract deltas (PLAN §4 amendments)

- `Account.skinUrl: string | null` — mock serves `/skins/*.png`; real core
  sends the session skin URL. `skinHue` stays as the fallback for faces.
- `Cosmetic.capeUrl: string | null` — cape-slot items point at a real
  64×32 texture so the 3D preview renders truthfully; wings/trail stay
  card-art only (they are client-side renders in-game, not cape textures).

## Layout of the fused Home

1. **Flight-deck hero** — ui-b voice + VoxelIsland art + real store meta.
   No play button here: the PlayDock is *the* launch surface (one launch
   surface, not two).
2. **Grid**: news (featured + list, ui-c) | right rail = **player card**
   (Skin3D, username, cape name) + **launcher status** (core/target/java/
   accounts, ui-c).
3. **Favorite servers** row (ui-c cards, quick-join).

## Where things run

- `launcher/` — Vite/React 18/TS, standalone on mock core, port 4173.
- Verification: headless Edge sweep (`scripts/`), every screen + dialogs +
  AMOLED + en shot at 1600×1000 before "done" is said.
