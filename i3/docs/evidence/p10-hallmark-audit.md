/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 */

# P10 Hallmark anti-slop audit

The six-axis critique covers the launcher's HUD editor and the shell visible around it. Philosophy is
5 because the surface edits the client's own anchor contract instead of inventing a launcher-only
position model; hierarchy is 5 because the settings rail, game plane, element ledger and inspector
remain distinct at a glance; execution is 5 because the same file was driven through the launcher
and two running client resolutions; specificity is 5 because the stage is the client's exact
640×360 GUI coordinate plane and the limitations are printed beside it; restraint is 5 because the
surface adds no marketing content, decorative card grid or success toast; variety is 4 because it
deliberately reuses the established desktop settings shell.

Audit basis: the [Hallmark 58-gate slop test](https://raw.githubusercontent.com/Nutlope/hallmark/main/skills/hallmark/references/slop-test.md).
The scope is the P10 HUD editor and the shared launcher shell visible in
`p10-launcher-hud-editor.png`, not unrelated legacy screens in `screens.css`. This is a Tauri desktop
application whose declared minimum window is 1040×680, so phone and browser-navigation gates are
N/A. Every applicable answer is **No**.

| Gate | Answer | Observed or inspected evidence |
| --- | --- | --- |
| 1 | No | Pretendard is self-hosted for interface copy and the platform monospace stack is reserved for coordinates and versions. |
| 2 | No | The HUD surface has no gradient text or purple-blue wash. The shell has one fixed gold atmospheric bloom over navy. |
| 3 | No | The macrostructure is a game plane beside one element ledger, not a grid of interchangeable cards. |
| 4 | No | The plane is not wrapped in nested cards; one inset hairline states its boundary. |
| 5 | No | Selection uses a gold outline and row wash. No coloured side-stripe is applied to cards. |
| 6 | N/A | This settings utility has no hero. |
| 7 | No | Navy surfaces use warm, chromatic neutrals; the plane deliberately uses the client's own near-neutral HUD tokens. |
| 8 | No | A measured HUD instrument is the page's subject. There is no marketing template or landing-page structure. |
| 9 | N/A | There is no sequence of page sections. |
| 10 | No | P10 selectors use property-specific transitions. None uses `transition: all`. |
| 11 | No | Hover and selection do not scale controls. |
| 12 | No | The surface uses the launcher's standard non-overshooting easing. |
| 13 | No | A row changes ground, an anchor changes line and fill, and a toggle changes state; no target stacks transform, shadow and colour effects. |
| 14 | No | No P10 interaction animates width, height, top, left, margin or padding. Dragging follows the pointer directly. |
| 15 | No | Global `:focus-visible` draws an immediate 2 px accent ring. A HUD element has its own immediate 1 px ring with 4 px offset. |
| 16 | No | Reset changes the visible layout in place and emits no success toast. Read, write and reset failures still use error feedback. |
| 17 | N/A | This surface has no tooltip. Anchor names are native `title` labels rather than delayed custom hover content. |
| 18 | N/A | There is no carousel, rotating banner or rotating statistic. |
| 19 | No | The copy names the actual managed instance, file, dimensions and current implementation limits. No fictional user or company appears. |
| 20 | No | `screens.css` carries the P10 macrostructure stamp immediately above the HUD editor block. |
| 21 | No | The layout is a desktop settings instrument, not a catalogue, portfolio or landing page. |
| 22 | No | The shell's neutral ramp is night navy rather than a zero-chroma grey ramp. |
| 23 | No | Gold is limited to selection, active controls and one shell bloom; the black game plane and navy shell dominate the frame. |
| 24 | No | P10 launcher spacing uses `--space-1` through `--space-5`. Exact GUI-pixel sizes remain only where the launcher mirrors client geometry. |
| 25 | No | At the declared 1040×680 minimum, the editor stacks to a 510×287 stage and the remaining ledger is reachable by the content scroller. |
| 26 | No | Rows, anchor cells, step buttons, toggles and the reset action expose rest, hover, focus-visible, active/selected and disabled states where applicable. Save failures have an error state. |
| 27 | No | `prefers-reduced-motion: reduce` collapses launcher animation and transition durations to 0.01 ms. |
| 28 | N/A | There is no video. |
| 29 | No | The shell uses one non-animating gold bloom. Stars twinkle but neither translate nor form a mesh gradient. |
| 30 | No | Icons come from the launcher's icon component and the HUD preview's own geometric marks. There are no emoji. |
| 31 | N/A | There is no generated illustration or Lottie asset. |
| 32 | No | A draggable 640×360 client-coordinate plane with anchor transfer and a ledger is specific to this product contract. |
| 33 | N/A | There is no decorative SVG figure or hand-authored illustration. |
| 34 | No | Browser runs at 1040×680, 1280×820 and 1920×1080 all reported document `scrollWidth === clientWidth`. |
| 35 | No | Hairlines define the title bar, stage and inspector. Text is not underlined decoratively. |
| 36 | No | Ledger names and numeric offsets occupy separate columns; coordinates use tabular figures and the 3×3 anchor cells align on one grid. |
| 37 | No | The surface uses one primary family plus one data face, not three or more families. |
| 38 | No | The display family is used for the settings heading; body and control copy keep compact UI roles. |
| 38a | No | No display copy is italic. |
| 39 | N/A | P10 adds no text input, textarea or select. Its 3×3 choice is a labelled button grid. |
| 40 | No | The dark tertiary text token is `#8B97B6`, measuring 6.50:1 on `#0B101F` and 5.86:1 on `#151B2E`; a regression test enforces the 4.5:1 floor. |
| 41 | No | Game-plane ink comes from the client's own HUD token block; selected gold, primary ink and secondary ink remain legible on the near-black chip ground in the captured frame. |
| 42 | N/A | Tauri owns one desktop window; the HUD tab does not implement browser-page navigation. |
| 43 | No | The persistent bottom dock exposes account, instance and the primary play action; the HUD's own instructions stay in the content area. |
| 44 | N/A | There is no hero or fold. |
| 45 | No | The grid, centre rules, vanilla HUD outline and anchor brackets all report placement constraints rather than decorate empty space. |
| 46 | No | The stage says that its readings are examples, preserves unknown element IDs, and explicitly discloses browser font-width and unimplemented scale limits. |
| 47 | No | `p10-launcher-hud-editor.png` is the actual Chromium-rendered launcher surface, not a fake browser or device frame. |
| 48 | No | The P10 plane palette and spacing are named in `tokens.css`; component CSS consumes variables rather than defining anonymous colour literals. |
| 49 | No | Korean instructions wrap in their content column; element names ellipsize in the ledger without widening it. |
| 50 | N/A | The Tauri window cannot be resized below 1040 px and has no phone layout. |
| 51 | N/A | The surface has no display headline whose mobile wrapping must be controlled. |
| 52 | N/A | There is no themed marketing section heading. |
| 53 | N/A | The settings navigation is a tab list, but it is not implemented as hidden radio inputs. |
| 54 | No | `HUD 배치` appears once as the surface heading; the top subtitle names it only as one setting category. |
| 55 | No | Only the `FULLMOON` wordmark and compact in-plane keys use uppercase. Korean interface copy does not shout. |
| 56 | N/A | The desktop shell has a fixed application dock by design, not a phone sticky CTA. |
| 57 | N/A | No approved studied-DNA diagnosis was replaced with a catalogue theme. |

## Visual and runtime evidence

- `p10-launcher-hud-editor.png` shows the real launcher DOM at 1280×820 with all eight element rows,
  the selected coordinate chip, its 3×3 anchor inspector and the 640×360 stage.
- `p10-launcher-browser.log` records the minimum, default and wide window probes, zero console errors,
  zero horizontal overflow, reset without a toast and the focused row moving from `(16,56)` to
  `(20,56)` after `ArrowRight`.
- `p10-hud-boot-640x360.png`, `p10-hud-xyz-moved-640x360.png` and
  `p10-hud-fps-moved-640x360.png` show the same running client before and after two external edits.
- `p10-hud-carried-960x540.png`, `p10-hud-tps-off-960x540.png` and
  `p10-hud-time-moved-960x540.png` repeat the contract at a larger frame.
- `p10-live-client-640x360.log` and `p10-live-client-960x540.log` contain four
  `Adopted hud.json edited outside the game` events between those frames.
- `p10-final-hud.json` is the exact eight-element file left by the live run.

Honesty notes.

The browser run uses the launcher's Vite mock core because Tauri IPC is unavailable in Chromium.
It proves rendered layout, keyboard interaction, reset feedback, responsive behaviour and console
health. The actual disk contract and hot adoption are proved separately by the live Fabric captures,
the persisted json and the client logs.

The Hallmark scope does not certify unrelated launcher screens that share the historical
`screens.css` file. P10 selectors and the shell visible in the committed frame were inspected; legacy
play and dashboard rules were not silently claimed clean.

Result: **PASS — 58 gates answered; no applicable gate answered Yes.**
