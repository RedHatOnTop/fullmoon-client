# Evidence

Captures are taken off the workspace, on a headless X server, from the dev client built out of
this tree. Nothing here is a mockup.

```sh
Xvfb :9 -screen 0 1280x720x24 +extension GLX +extension RANDR +extension XTEST -nolisten tcp &
: > /tmp/i3-xauth9                     # python-xlib insists an authority file exists
env -u WAYLAND_DISPLAY -u XAUTHORITY -u XDG_SESSION_TYPE DISPLAY=:9 \
  ./gradlew -p mod runClient --console=plain
# F6 through XTEST, then:
XAUTHORITY=/tmp/i3-xauth9 import -display :9 -window root out.png
```

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
