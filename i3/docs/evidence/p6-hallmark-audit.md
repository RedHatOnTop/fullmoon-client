/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 */

# P6 Hallmark anti-slop audit

The six-axis critique scores the in-world notice and server metric treatment, not the Minecraft
world behind it. Philosophy is 5 because the client presents only server-owned facts; hierarchy is
5 because title, detail, and metric separate immediately; execution is 4 because the notice must
truncate rather than reflow on a narrow game window; specificity is 5 because this is a Fullmoon
network event, not a generic toast; restraint is 5 because one rule and two text lines do all the
work; variety is 4 because the treatment deliberately reuses the established i3 HUD ground and
type roles while introducing a distinct transient overlay.

Audit basis: the [Hallmark 58-gate slop test](https://raw.githubusercontent.com/Nutlope/hallmark/main/skills/hallmark/references/slop-test.md)
at commit-visible `main`, applied to native Minecraft GUI rather than pretending web-only DOM,
CSS, SEO, nav, footer, or hero gates exist here. Every applicable answer is **No**. Web-only gates
are **N/A**, not silently claimed as browser tests.

| Gate | Answer | Observed or inspected evidence |
| --- | --- | --- |
| 1 | No | Noto Serif CJK KR and Pretendard are baked roles; no system-default display face is used. |
| 2 | No | The client overlay has no gradient and introduces no purple or blue accent. |
| 3 | No | P6 adds one notification strip and one existing HUD chip, not a three-card grid. |
| 4 | No | Neither element contains a nested card. |
| 5 | No | The two-pixel severity rule is a semantic state indicator, not a thick decorative card stripe. |
| 6 | N/A | Native in-world HUD; there is no hero. |
| 7 | No | The ground is tinted `surface.void`, not pure black or white. |
| 8 | No | The structure is a transient server notice over play, not a marketing-page macrostructure. |
| 9 | N/A | Native HUD has no sequence of page sections. |
| 10 | No | P6 contains no transition declaration. |
| 11 | No | P6 contains no hover scaling. |
| 12 | No | P6 contains no overshoot easing. |
| 13 | No | P6 adds no hover effects. |
| 14 | No | P6 animates no layout property. |
| 15 | N/A | P6 adds no focusable control. |
| 16 | No | The notice reports an otherwise invisible server event; visible user actions do not trigger it. |
| 17 | N/A | P6 adds no tooltip. |
| 18 | N/A | P6 adds no carousel, rotating banner, or rotating statistic. |
| 19 | No | No placeholder person, company, or startup cliché appears. |
| 20 | N/A | Native Java rendering has no CSS file; this audit carries the required pre-emit stamp. |
| 21 | No | The HUD does not use the Specimen editorial macrostructure. |
| 22 | No | Every neutral token has warm hue 89 and chroma from 0.006 through 0.013. |
| 23 | No | Accent occupies only a two-pixel rule; it is visibly below five percent of both captures. |
| 24 | No | Placement and spacing consume named `Tokens.Space` and `Tokens.Stroke` values. |
| 25 | N/A | Two short operational lines are fitted to a HUD strip, not a prose container. |
| 26 | N/A | P6 adds no interactive element. |
| 27 | No | P6 adds no transform or keyframe animation. |
| 28 | N/A | There is no video. |
| 29 | N/A | There is no abstract background. |
| 30 | No | There is no icon or emoji in the notice or TPS chip. |
| 31 | N/A | There is no illustration or Lottie asset. |
| 32 | No | The transient in-world strip is not a repeated launcher or prior surface archetype. |
| 33 | N/A | There is no SVG, canvas art, or decorative figure. |
| 34 | No | The 320×180 GUI capture shows the notice contained within the viewport without clipping its panel. |
| 35 | No | The only accent stroke is a visually inspected two-pixel vertical severity rule, not text decoration. |
| 36 | No | Both lines use the shared type roles and their measured baselines; the live captures show vertical centring. |
| 37 | No | The project uses two families: Noto Serif CJK KR and Pretendard. |
| 38 | No | The display face is not used by this notice or metric chip. |
| 38a | No | The title and all other P6 text are roman. |
| 39 | N/A | P6 adds no input, textarea, or select field. |
| 40 | No | Effective notice contrast is 13.56:1 or higher for primary text, 7.41:1 or higher for secondary text, and 8.30:1 or higher for the live rule across black/white world extremes. |
| 41 | N/A | P6 adds no button or text-bearing accent fill; dark-ground text is explicitly light. |
| 42 | N/A | There is no page navigation. |
| 43 | N/A | There is no footer. |
| 44 | N/A | There is no hero or fold. |
| 45 | No | The severity rule carries message state; there is no purposeless decoration. |
| 46 | No | TPS and tick time come from Paper measurements; unsupported data renders as `—`. |
| 47 | No | The client uses the real Minecraft viewport and draws no fake browser, phone, terminal, or IDE chrome. |
| 48 | No | All P6 colours, radii, strokes, and spacing reference named tokens; the 88-file token scan found no literal. |
| 49 | N/A | P6 adds no clickable label. |
| 50 | N/A | Native immediate-mode HUD has no CSS grid or intrinsic image track. |
| 51 | N/A | There is no web display header; native text is width-fitted before drawing. |
| 52 | N/A | There is no themed CSS section head. |
| 53 | N/A | There is no radio-tab implementation. |
| 54 | N/A | There is no section eyebrow or page heading pair. |
| 55 | No | P6 adds no uppercase display heading. |
| 56 | N/A | There are no sticky web elements. |
| 57 | N/A | No studied-DNA diagnosis was discarded for a catalogue theme. |

## Visual and numeric evidence

- `p6-live-channel-960x540.png`: a real protocol-1 session shows the notice and `TPS 20.0 · 2.2 ms`.
- `p6-live-channel-320x180.png`: the same server event remains contained at the narrow GUI width.
- `p6-legacy-fallback-960x540.png`: a protocol-0 bridge stays silent and the client shows `TPS —`.
- `p6-live-client.log` and `p6-live-server.log`: matching hello, welcome, HUD revision, and notice.
- `p6-legacy-client.log` and `p6-legacy-server.log`: incompatible server silence and the five-second fallback.
- `node i3/design/verify-tokens.mjs`: 88 files scanned, zero colour or motion literals outside the token block.
- Generated contrast floors pass all nine declared design-token pairs. The notice-specific effective
  composites were also calculated over both black and white world backgrounds.

Result: **PASS — 58 gates answered; no applicable gate answered Yes.**
