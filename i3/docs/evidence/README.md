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

Geometry is per capture and the filename carries it: `-640x360` is a 1280×720 window and
`-960x540` a 1920×1080 one, both at GUI scale 2, so every layout number in the mod is half the
pixel position in either. It became an argument in P1-C, when the kit outgrew 360 GUI px. It sizes
the Xvfb and the client window off the one number, because a client smaller than its display is a
photograph with a mat around it.

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
