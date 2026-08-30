# Evidence

Captures are taken off the workspace, on a headless X server, from the dev client built out of
this tree. Nothing here is a mockup.

```sh
tools/capture.py /tmp/shots --geometry 1920x1080 --scale 2 kit:F7 focus-1:Tab focus-2:Tab
```

Each `NAME:KEYS` taps its keys and photographs the result, cumulatively on one client: a
traversal capture only means anything if it is the same surface at every stop, so the screen is
never reopened between shots and the pointer is parked in a corner where no control is. The P0
files predate the tool and were taken by hand with the same Xvfb, the same `import` off the root
window, and XTEST for the keys.

Geometry is per capture and the filename carries it: `-640x360` is a 1280×720 window, `-960x540`
a 1920×1080 one and `-960x600` a 1920×1200 one, all at GUI scale 2, so every layout number in the
mod is half the pixel position in any of them. It became an argument in P1-C, when the kit outgrew
360 GUI px, and grew again in P1-D: the list page puts a sweep, a readout and a five-row well in
one column, and the well's floor lands at 546 where `footerY(540)` is 505. It sizes the Xvfb and
the client window off the one number, because a client smaller than its display is a photograph
with a mat around it.

## P0 — render layer and design specimen

| file | what it settles |
| --- | --- |
| `p0-specimen-640x360.png` | The specimen opens from the title screen and draws: five type roles, six shapes off one SDF pipeline, nineteen colour tokens, the figure pair. |
| `p0-specimen-type-roll-2x.png` | All three baked faces rasterise — Noto Serif CJK KR for `display`, Pretendard for the rest — with real space advances and no vanilla bitmap glyphs anywhere. |
| `p0-specimen-figures-2.5x.png` | The tabular column sets `002273` 3 px wider than the proportional one: digits are on the widest-digit cell, centred in it. |

Two defects these captures caught and the fix for each:

- Every Latin glyph and digit came from vanilla's `ascii.png`, and spaces collapsed to a
  ~0 advance, because the five font jsons declared `minecraft:include/default` first and
  `FontSet` takes the first provider that answers. `FontManager.getFontSetRaw` falls back
  silently, so there is no log line for this — only the render shows it. The intended face is
  now declared first and `include/default` is gone.
- The `display` sample lost the top-left serif of its `F` and floated above its own row,
  because the row clipped vertically and the sample was placed by its draw origin. The game
  hangs every provider off one 9 px line box, so a 22 px face draws ~8 px above its origin:
  placement now goes through `Typeset.centred`, and only the column is clipped.

## P1-B — surface event routing, button and switch

| file | what it settles |
| --- | --- |
| `p1b-kit-640x360.png` | Thirty-two matrix cells and the live band in one frame, through one `draw(Painter, State)`: a cell that lied about a state would have to lie in the band too. Eight states are eight states, not eight screenshots of the one the mouse was in. |
| `p1b-kit-traversal-band.png` | Four Tab stops, no mouse anywhere. The ring is up at every stop, the order is the registration order, and the fourth wraps to the first. The footer readout is the surface naming its own keyboard holder, not my reading of the ring. |
| `p1b-switch-in-flight-2x.png` | `loading` against `disabled` at 2×, all four rows. The button trades its label for three dots; the switch parks its knob mid-track. |

Three defects this slice caught:

- The masthead's accent bar sat under the wordmark instead of beside it, and the section heads'
  ticks were a pixel low for the same reason: both were sized against the role's nominal box
  while the baseline is 7 px below the draw origin whatever the face is. `Typeset.capTop` and
  `capHeight` now name the band a role's capitals actually occupy, and the rules are measured off
  them. The committed P0 specimen shows the same bug, because that screen carried its own copy of
  the masthead; `p1d-specimen-960x600.png` is the same screen on the shared chrome with the bar
  where it belongs.
- A control with a request in flight still fired on Enter and still took typing, because
  `Surface` gated the pointer on `live()` and the keyboard on nothing. Tab deliberately stays
  outside the gate: a request that leaves the player unable to leave the control they fired it
  from has taken the surface away, not just the control.
- Tabbing to a button and then moving the mouse over it made the focus ring vanish, because the
  ring was drawn only for `FOCUS_VISIBLE` and a state can only name the loudest thing true about
  a control. The ring is now an orthogonal surface-owned bit, so a hovered or in-flight control
  keeps it.

## P1-C — slider, select and text field

| file | what it settles |
| --- | --- |
| `p1c-kit-960x540.png` | Seven widget rows against eight states, fifty-six cells, and a live band of five controls under them — one frame, one `draw(Painter, State)`. The slider, the select and the text field are drawn by the same call the button and the switch are. |
| `p1c-select-open-960x540.png` | The open list paints over the slider line and the button line below it and is clickable there. Drawing in front and being hit first are one fact, and neither of them is Tab order: the select is the second control the keyboard reaches, the slider under its list the third. |
| `p1c-band-in-flight-960x540.png` | 적용 pressed by keyboard, and every control in the band is in flight at once. The field keeps what the player typed and takes dots beside it, the select trades its value for dots, the slider holds its knob and drops its readout, and 적용 keeps its focus ring while it stops answering — 취소 stays live, because a request the player cannot get out of has taken the surface away. |
| `p1c-field-caret-2x.png` | The caret is the band a capital occupies plus a hairline above and below, so it ends flush with nothing. It is up because the keyboard is in this field, not because the state reads `focus`. |
| `p1c-field-error-2x.png` | One typed space fails the field's rule and the state under it becomes `error` with the caret still live: a field that does not validate is still a field being typed in. |
| `p1c-field-row-1.5x.png` | Eight states of the text field, side by side. `loading` keeps the text and adds the dots, and the caret is in the two focus columns and nowhere else. |

Three defects this slice caught:

- A text field nobody was typing in scrolled its own text out of view. `view()` keeps the caret
  in sight, `loading` narrows the area to make room for the dots, and a blurred field still has a
  caret index sitting at the end of its text — so the `loading` cell read `빛 •••` with the
  `달` shifted off the left edge. What a field nobody is typing in has to show is the head of its
  text: the scroll is now `typing ? view(area) : 0`. Those 177 pixels are the only difference
  between the pre-fix and post-fix runs of all five captures, which is also the rig repeating
  itself to the pixel.
- Paint order and hit order were two different orders. `Surface.at` walked the registration list
  once, back to front, so the slider registered after the select won every click inside the open
  list while the list painted over it. Widgets that are over the surface now get a pass of their
  own on the way in, and Tab order is left out of it. Flipping `Select.overlaying()` back to
  `false` fails `anOpenListIsHitBeforeWhateverItCovers`, so that test is not green by accident.
- The kit did not fit its own capture, and the first 1920×1080 attempt came back letterboxed:
  a 1280×720 client on a black mat, still 640×360 GUI px, footer text over the last matrix row.
  `--geometry` sized the Xvfb, but the client window came from a hard-coded
  `programArgs("--width", "1280", ...)` in `build.gradle.kts`. The size is now `client_width` and
  `client_height` in `gradle.properties`, and the rig passes both.

## P1-D — list, tab rail, tooltip and the dev surface chrome

| file | what it settles |
| --- | --- |
| `p1d-list-960x600.png` | Eight states against chosen and unchosen, sixteen row cells, and no two of them alike. Selection outlives all eight, so a row cannot use a state to say it: the wide accent tick does, and the sweep is where that claim is either kept or broken. |
| `p1d-rail-mark-4x.png` | The rail's mark, in the only two frames that can show it. Top, the mark is on the tab already open and draws nothing — rehearsing an underline that is already there would be a lie. Bottom, one Left later: a strong line under 위젯 at the accent underline's own y, a hairline shy of its weight, 위젯's label brightened, 목록 still underlined and still the page on screen. |
| `p1d-well-960x600.png` | The keyboard in the well, the mark on its first row, and nothing chosen: the readout reads 고른 항목 없음 and 값 복사 stays off. Walking a list is not choosing from it. The hint has flipped above the well, because below it is the footer. |
| `p1d-list-chosen-and-marked-960x600.png` | The chosen row and the marked row in one frame, four rows apart, after the view scrolled to keep the mark in sight. Wash ground and a two-pixel tick against raised ground and a hairline one — the pair the sweep enumerates, here on one live list. |
| `p1d-copy-960x600.png` | A hint at the far side of the column, ending where the masthead's rule ends. The ring is on 값 복사, live now that something is chosen, and the row chosen four keys ago is still wearing its tick with the keyboard gone from the well. |
| `p1d-specimen-960x600.png` | The specimen drawing the shared masthead: accent bar at GUI x 220..222, y 14..31, the same two pixels on the same cap band as every other page. The P0 capture of this screen still shows the bar under the wordmark; this is the P1-B fix reaching it. |

Four defects these captures caught:

- The hint on 값 복사 ran from GUI x 689 to 862 with the page's column ending at 740, so a third of
  it hung over the blurred void outside the surface. `Tooltip.place` was clamping to the window,
  which is the one rectangle that is never the ground being drawn on. It now takes the region the
  surface owns.
- The well's hint sat at GUI y 548..570 with `footerY(600)` at 565, crossing the footer hairline and
  crowding the line that tells the player how to leave. Same root cause, same fix: with the column
  as the region there is no room below the well, so the hint flips above it and covers the section
  head of the list it belongs to — which is the group it is describing, and transient.
- The masthead moved when the tab changed. `SpecimenScreen.maxContent()` returns 420, and the
  chrome was measured with it, so the wordmark, the rail and the footer rule all sat at GUI x 270
  on the specimen and 220 on the other two pages: tabbing 표본 ↔ 위젯 slid the whole frame 50 px
  sideways and read as the screen having reloaded. `maxContent()` now governs the page's own body
  only. The body keeps its measure and gives up its right edge instead of the left one every other
  thing on the surface is aligned to.
- Then the fix for the first two put every hint 4 px off that same left edge. Most controls on a
  page sit on the column's left edge, and clamping to `within.x() + GAP` pushed their hints inward
  to buy room against nothing — the rail's hint came away from the line the section head ticks and
  the masthead bar are on, and covered the tick beside it in the bargain. Caught by diffing the
  run against the previous one: the only pixels that moved were hints, all of them by four. The
  sides of a region are edges to align to, not walls to stand off; the gap now belongs to the
  control and to the two rules that close the column above and below.

## P2 — in-game surfaces

| file | what it settles |
| --- | --- |
| `p2-settings-960x540.png` | Settings screen opened via F9: 7 Minecraft options, search bar, toggle switch on detail pane, unified masthead with connection status dot, footer rules. |
| `p2-keybinds-960x540.png` | Key bindings surface: 64 bindings in scrollable well, category grouping, bound key readout, conflict detection with warning indicators, rebind and reset actions. |
| `p2-mods-960x540.png` | Active mods browser: 51 loaded Fabric mods with versions, author credits, descriptions, and environment flags. |
| `p2-account-960x540.png` | Player identity and network session: profile username, UUID, offline/online auth badge, connection status, and clipboard copy actions. |

Two defects these captures caught:

- The connection status string used separate unlocalized keys across screens (`fullmoon.keybinds.server.disconnected`, etc.), leaving raw resource keys in the masthead. Unified into `HubChrome.connection` using the canonical localized string.
- Keybinding category headers rendered raw Identifier paths (e.g. `minecraft:gameplay`). Added `HubChrome.categoryLabel` to resolve against vanilla language keys (`key.category.minecraft.gameplay` → `게임플레이`).

## P3 — HUD and its editor

| file | what it settles |
| --- | --- |
| `p3-hud-editor-960x540.png` | In-game HUD editor opened via F10: 4px grid snapping, gold L-shaped corner ticks on the active selection (Coordinates), FPS, Ping (live dot), Clock, Keystrokes chips, top toolbar with enable toggle and action buttons. |

One defect this capture caught:

- `ClockHud`, `CoordinatesHud`, and `FpsHud` measured only their value strings without accounting for the leading key tag ("TIME", "XYZ", "FPS"), causing the right edge of wider numerals to clip against the chip border. Updated `measureWidth` to measure the compound label + value footprint.



## P6 — versioned Paper channel and legacy fallback

| file | what it settles |
| --- | --- |
| `p6-live-channel-960x540.png` | A real protocol-1 Paper session completed hello/welcome, rendered measured `TPS 20.0 · 2.2 ms`, and displayed the Korean two-line server notice over the live world. |
| `p6-live-channel-320x180.png` | The same event at 320×180 GUI pixels remains within the viewport; title and body retain their hierarchy without an oversized panel. |
| `p6-legacy-fallback-960x540.png` | A protocol-0 bridge deliberately stayed quiet; after five seconds the mod retained vanilla play, omitted the notice, and rendered unsupported metrics as `TPS —`. |
| `p6-live-client.log` / `p6-live-server.log` | Matching hello, welcome, first HUD revision, and notice identifiers prove the server-to-client round trip. |
| `p6-legacy-client.log` / `p6-legacy-server.log` | The server records the version mismatch and the client records its five-second fallback. |
| `p6-hallmark-audit.md` | The six-axis critique and all 58 Hallmark anti-slop gates, with native-GUI scope called out explicitly. |

The Paper fixture was compiled in a temporary directory and installed only for this run. After the
captures, the original `FullmoonBridge.jar` was restored byte-for-byte and the local server was
stopped. The fixture held the overworld at dusk tick 13000 with clear weather for every committed
P6 frame. These images are direct client captures, not mockups.

Two visual decisions came from the rendered frames:

- The server event is a flat, top-centred strip with one two-pixel severity rule. It uses no icon,
  gradient, blur bloom, animation, or redundant action. The world remains the visual subject.
- Missing support never masquerades as healthy data. The legacy frame visibly says `TPS —` rather
  than keeping the editor's sample or inventing a nominal 20 TPS reading.
