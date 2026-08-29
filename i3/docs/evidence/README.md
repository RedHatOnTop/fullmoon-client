# Evidence

Captures are taken off the workspace, on a headless X server, from the dev client built out of
this tree. Nothing here is a mockup.

```sh
tools/capture.py /tmp/shots kit:F7 focus-1:Tab focus-2:Tab focus-3:Tab focus-4:Tab
```

Each `NAME:KEYS` taps its keys and photographs the result, cumulatively on one client: a
traversal capture only means anything if it is the same surface at every stop, so the screen is
never reopened between shots and the pointer is parked in a corner where no control is. The P0
files predate the tool and were taken by hand with the same Xvfb, the same `import` off the root
window, and XTEST for the keys.

The window is 1280×720 at GUI scale 2, so every layout number in the mod is half the pixel
position in these files.

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
  them. The committed P0 specimen shows the same bug — `SpecimenScreen` still carries its own
  copy of the masthead, and it inherits the fix in P1-D.
- A control with a request in flight still fired on Enter and still took typing, because
  `Surface` gated the pointer on `live()` and the keyboard on nothing. Tab deliberately stays
  outside the gate: a request that leaves the player unable to leave the control they fired it
  from has taken the surface away, not just the control.
- Tabbing to a button and then moving the mouse over it made the focus ring vanish, because the
  ring was drawn only for `FOCUS_VISIBLE` and a state can only name the loudest thing true about
  a control. The ring is now an orthogonal surface-owned bit, so a hovered or in-flight control
  keeps it.
