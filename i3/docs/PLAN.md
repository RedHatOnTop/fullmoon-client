# i3 build plan

Nine phases. Each one ends in a commit whose evidence is a capture or a test run, not a
description. Captures come off `Xvfb :9` (see `docs/evidence/README.md`); no phase is reported
done on a plan or a green compile alone.

Two rules hold across all of them: no colour, radius, duration or motion literal outside
`design/tokens.json` (`design/verify-tokens.mjs` fails the build on one), and no vanilla chrome
— no drop-shadowed text, no re-drawn vanilla widget, no bitmap font in our stacks.

## P0 — render layer and design specimen · done

Shape `VertexFormat`, `RenderPipeline` and `shape.vsh`/`shape.fsh`; one SDF covering rect,
rounded rect, hairline rule, ring and dot. `Painter` over `GuiGraphicsExtractor` with a clip
stack and the blurred stratum. `Typeset`: five roles on five baked ttf providers, tabular
figures, baseline placement. `tokens.json` → `Tokens.java` + `tokens.css` with a contrast gate.
`SpecimenScreen` on F6, reachable from the title screen without a mixin.

Evidence: `docs/evidence/p0-*.png` — the specimen rendered at 640×360 GUI px, the type roll at
2× showing all three faces, the figure pair. Nine contrast floors PASS in `generate.mjs`.

## P1 — layout, event bus, widget kit

Box layout (the left spine and the master–detail split the surfaces need), focus traversal with
a visible ring, an event bus for mouse/key/scroll/char. Widgets: button, toggle, slider, select,
text field, list row, tab rail, tooltip — each with all eight states (rest, hover, active,
focus, focus-visible, disabled, loading, error).

Evidence: one state-matrix capture per widget, all eight cells in frame. JUnit over the layout
arithmetic and the focus order. A keyboard-only traversal capture: no mouse, ring visible at
every stop.

## P2 — in-game surfaces

Mod menu, settings with search, keybind editor with conflict detection, account surface. All on
the blurred stratum, all `Esc`/`Tab` correct, all strings in `ko_kr` and `en_us`.

Evidence: captures taken while connected to the local Paper server on `:25566`, a keybind
conflict shown resolving, and a resource-pack reload that leaves the metrics correct (the
reload listener ordering against `FONTS` is what this proves).

## P3 — HUD and its editor

Elements through `HudElementRegistry`: coordinates, clock, ping, server tick, effects. A drag
editor with snapping on a 4 px grid, L-shaped corner ticks, per-element toggles, anchors that
survive a resolution change. Config as json.

Evidence: HUD in world, editor with its snap guides, a config round-trip test, and a resolution
change that keeps every element on its anchor. Frame cost is quoted from the game's own
profiler output or not claimed at all.

## P4 — launcher core

Tauri v2 with the work in Rust: version manifest fetch, asset and library download with sha1
verification and resume, Fabric loader install, instance layout, offline account, JVM argument
composition, spawn, log streaming to the front end. The Microsoft device-code path is written
against an empty `msClientId` and stays unverified until there is one — stated as unverified.

Evidence: the launcher spawning a game that reaches the title screen, with the spawn command
and the client log quoted. A test that corrupts one asset's sha1 and fails the install.

## P5 — launcher UI

The front end on the same tokens: first run, install progress, instance list, play, settings,
error surfaces. Eight states again, and every animation on transform/opacity with a
`prefers-reduced-motion` collapse.

Evidence: per-state captures, a first-run-to-play sequence, and a reduced-motion pair.

## P6 — server channel and the slop test

A `fullmoon:v1` plugin channel: server-pushed HUD values and notices, versioned payloads, a
client that degrades silently on an older server. Then the 58-gate slop test read for the first
time and answered gate by gate, and the six-axis self-critique stamp.

Evidence: a packet round trip against local Paper with both logs, a gate-by-gate pass table,
the stamp.

## P7 — native server route ledger · done

Complete the remaining `fullmoon:v1` warp contract: validated waypoint snapshots, full-snapshot
replacement, ID-only requests, matched server results, timeouts, and server-directed screen open.
The screen is a master-detail route ledger over live Paper data with one request action and no
client-owned teleport authority.

Evidence: route selection, in-flight, accepted, and compact captures from a real Paper session;
matching client and server logs for `palace_gate`; reducer and payload boundary tests; a fresh
58-gate Hallmark audit.

## P8 — in-game map

A north-up map of the terrain the client already has, and nothing else: a column outside the
client cache draws as unmapped rather than as invented ground, and the footer says what fraction
of the frame is real. Five survey scales with cursor-anchored zoom, arrow panning, one key back
to the player. The only markers are the routes the server published over `fullmoon:v1`, filtered
to the dimension the player is standing in, and clicking one centres the map on it — the map asks
for no teleport of its own. `MapCanvas` draws the plane for the screen and stays shaped for a HUD
minimap, which is not in this phase.

Evidence: captures of a real Paper session at 960×540 and 640×360 with loaded and unloaded
terrain in the same frame, the published routes marked against the ledger's own six destinations,
and a zoom pair that keeps the block under the cursor fixed. The pure core is at 100% line and
branch on every gated map class; `TerrainSampler` has no unit test, because it needs a live
`ClientLevel`, so those frames are the only evidence it has. A fresh 58-gate Hallmark audit.
