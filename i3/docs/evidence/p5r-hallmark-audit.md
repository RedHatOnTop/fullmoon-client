/* Hallmark · pre-emit critique: P5 H5 E5 S4 R5 V4 */

# P5-R Hallmark anti-slop audit — launcher UI overhaul

Scope: the desktop launcher front end (`launcher/src`), every surface — shell (titlebar,
sidebar, topbar, dock, palette, toasts, launch overlay) and screens (play, dashboard, mods,
cosmetics, accounts, settings + HUD editor). Theme dark (canonical) and light, locales ko
and en.

Audit basis: the [Hallmark 58-gate slop test](https://raw.githubusercontent.com/Nutlope/hallmark/main/skills/hallmark/references/slop-test.md).
Every answer below is **no** unless marked. Genre: **atmospheric-leaning desktop utility** —
the night sky lives on the backdrop as weather; panels are hairline-and-air.

## What this pass actually changed (the before state, for the record)

The launcher carried two design languages at war: the token-driven "Imprint" system and a
later `game-*`/`play-*` glassmorphism layer. Evidence of the war, all fixed this pass:

1. **The backdrop ate the chrome.** `.game-backdrop` was `position: fixed; z-index: 0` while
   `.shell`, `.main` and `.topbar` were `position: static` — so every non-positioned element
   (topbar headings, the search button, the titlebar's own controls, sidebar group labels,
   the settings body, the cosmetics slot list, the mods toolbar) painted *under* the sky and
   disappeared from captures while staying in the hit-test tree. Fixed by making `.titlebar`
   and `.shell` a `z-index: var(--z-shell)` stacking layer above `--z-backdrop`.
   Before/after: the settings screen went from an empty sky with a tab rail to a full Java
   runtime ledger (compare `05-settings-general.png` against the pre-pass capture).
2. Raw off-palette hexes: Chakra green `#68D391`, Chakra red `#FC8181`, Discord blurple
   `#5865F2`, Tailwind `#3B82F6`/`#06B6D4` nebulae, gradient text
   (`background-clip: text` on the hero headline), 14× `transition: all`, undefined tokens
   (`--radius-pill`, `--text-1`, `--font-ui`), ~1,300 lines of dead CSS from a previous Home.
3. Home/Play hardcoded Korean copy, bypassing the `t()` layer entirely — the English locale
   rendered Korean. Both screens are rebuilt on `home.*` / `play.*` keys.

## Six-axis self-critique (pre-emit)

| axis | score | note |
|---|---|---|
| Philosophy | 5 | one position: the sky is weather, the chrome is architecture, the accent is the moon — spent on the play button, the balance figure, the active tab. |
| Hierarchy | 5 | topbar names the screen; dashboard reads status band → tabs → deck; play reads word → button → deck. |
| Execution | 5 | spacing on the 4px scale, states written out per control, focus rings instant outlines. |
| Specificity | 4 | night-navy + moon gold + Pretendard + the rabbit wink; the skin figure is Minecraft itself. |
| Restraint | 5 | twinkle animation, indigo/cyan nebulae, backdrop grid, gradient text, glass blur — all removed. |
| Variety | 4 | dashboard ≠ play macrostructure; both differ from the previous build's centred hero. |

## Gate-by-gate

**Visual**
1. Display font is Pretendard, not Inter/Roboto/etc. No.
2. No gradient anywhere on text; the hero headline's second line is solid `var(--accent)`.
   The two surviving gradients are the sky backdrop and the moon bloom — background weather,
   the atmospheric carve-out. No.
3. No 3-equal-column icon-above-heading grid. No.
4. No card-in-card: wallet stats are hairline-divided cells, not boxes in a box. No.
5. No thick coloured side-stripe on any card (news rows carry a 3px data-hue swatch — a
   ledger marker, not a card stripe). No.
6. Play stage is two columns — word left, figure right; eyebrow, title, sub and CTA sit on
   the left axis, none centred. No.
7. No pure `#000`/`#fff` as a base colour; the one white ink is the named `--fill-ink`
   token on chromatic fills. No.

**Structural**
8. Macrostructure differs from the previous build (centred hero → asymmetric stage; marketing
   banner → status band + tab deck). Stamps at the head of `screens.css`. No.
9. Sections are separated by hairline rules and tab rails, not equal whitespace. No.

**Microinteractions**
10. `transition: all` count in `src/styles`: 0 (was 14). No.
11. No uniform hover-scale anywhere; hovers are single-channel (colour *or* background *or*
    a 1px press). No.
12. No bouncy easing on UI state changes — the rabbit's peek rides `--ease-out`. No.
13. No element combines translate+scale+shadow+colour on hover. No.
14. No animated width/height/top/left/margin/padding (progress bars animate `width` only as
    fill painting — the exception the gate's spirit allows; transform/opacity elsewhere). No.
15. Focus rings are instant `outline` — no fade-in. No.
16. The telemetry toggle no longer fires a success toast; toasts report failures and
    invisible effects only. No.
17. No custom tooltip delay system in the launcher shell (native `title` only). N/A.
18. No auto-rotating content. No.
19. No Jane Doe / Acme copy; names come from the mock account ledger (BlackCow, Alex). No.

**Variety**
20. Stamp present at the head of `screens.css`. No.
21. Not the Specimen macrostructure. No.

**Implementation**
22. Neutrals are tinted navy (the sky ramp), never zero-chroma greys. No.
23. Accent footprint per viewport: the play button + balance figure + active tab underline
    ≲ 3% of the canvas; the backdrop bloom is the atmospheric 20% carve-out, fixed and
    unanimated. No.
24. New CSS uses the named spacing scale (`--space-2xs … --space-3xl`, multiples of 4px);
    legacy sections keep their audited values. No.
25. Prose measures: play sub 52ch cap, hints 62–68ch caps. No.
26. Interactive elements carry default + hover + `:focus-visible` (global) + `:active` +
    `:disabled` (opacity 0.55 + `cursor: not-allowed` + native attribute). No.
27. All keyframes sit under the global `prefers-reduced-motion` collapse; the starfield no
    longer animates at all. No.

**Hero enrichment**
28. No demo video. N/A.
29. Abstract background: one gold bloom + one cool breath, fixed, no animation. No.
30. One bespoke SVG icon set (`components/Icon.tsx`), no emoji glyphs, no mixed libraries. No.
31. No Lottie; hero art is the hand-drawn `Moonrise` SVG and the skinview3d figure. No.

**Diversification**
32. Different macrostructure and different accent knobs from the previous launcher pass. No.
33. Decorative SVGs (`Moonrise`, rabbit, backdrop, icons) are `aria-hidden`; the news swatch
    is presentational. No.

**Layout safety**
34. `document.documentElement` h-scroll probe at 1600/1280/1100px widths: all negative
    (`_viewports.mjs` prints `no-h-scroll` four times); body is `overflow: hidden` by shell
    design. No.
35. No highlighter bands; underlines are 2px inset box-shadows on the tab rail. No.
36. Bars (topbar, dock, tabs, rows) declare `align-items: center` / baseline explicitly. No.

**Typography**
37. Two families in play: Pretendard (display + UI) and the mono stack for data. No third. No.
38. Mono is the data register — numerals, addresses, version strings — not a display face. No.
38a. No italic headings anywhere; `<em>` is reset to roman globally. No.

**Input states**
39. `.input` keeps 1px borders in every state, focus is an outline ring, input and adjacent
    button share `--control-h` (32px), and helper slots hold their line. No.

**Contrast**
40. Text/background pairs ride the audited token pairs (`--text` on `--bg`, `--text-2` on
    cards); the accent-on-gold pair is `--on-accent` on `--accent-fill`. No.
41. Button text ≠ fill (dark ink on gold); `--on-accent` exists and is used; dark panels
    carry light ink by token. No.

**Nav / footer / hero fingerprints**
42. No marketing nav; the shell is a desktop sidebar + topbar (application chrome). N/A.
43. No marketing footer. N/A.
44. 1280×800 fold check (`20-fold-1280.png`): eyebrow, headline, sub, the play CTA and the
    deck's header all sit above the fold. No.
45. Every ornament is motivated: the moon bloom is the brand's namesake, the stars are the
    sky, the rabbit surfaces only on launch intent, the dais shadow grounds the figure. No.

**Honest copy**
46. No invented metrics — the status band reads live store values (worlds online, players,
    balance, instance); the hero makes no quantitative claim at all. No.

**Re-drawn chrome**
47. No fake browser/phone/terminal frames. The HUD editor's 16:9 plane is a functional
    simulator — it positions real modules against the horizon the mod draws in game, and its
    colours are now the named `--hud-sim-*` tokens. No.

**Token discipline**
48. Zero raw hex/rgb colour literals in `styles/` outside `tokens.css` (grep-verified); the
    accent-picker swatches in `Settings.tsx` are stored *data* (the value persisted to
    `settings.accent`), annotated as such. No.

**Responsive**
49. No two-line clickable text at 1100–1600px; tabs, buttons and links are `white-space:
    nowrap`. No.

**Mobile** (a desktop client; the floor is the desktop window, checked anyway)
50. Image-bearing grid tracks use `minmax(0, 1fr)`. No.
51. The display headline carries `overflow-wrap: anywhere; min-width: 0`. No.
52. No theme-specific multi-column section-head overrides. No.
53. No CSS-only radio tabs; tabs are real buttons. No.
54. No eyebrow-beside-heading pattern; eyebrows stack above headings in one column. No.
55. No all-caps display heads (caps appear only at overline size with tracking). No.
56. One sticky-at-top element exists per surface at most (settings rail is sticky inside a
    scrolling column, not at `top: 0` against a sticky page nav). No.
57. No studied-DNA diversion; the palette is the project's own token file. No.

## Evidence

| file | what it settles |
| --- | --- |
| `fullmoon-launcher-01-play.png` | the asymmetric launch stage: word left, figure right, one gold button, meta instrument line |
| `fullmoon-launcher-01-play-rabbit-hover.png` | the moon rabbit surfaces on launch hover only |
| `fullmoon-launcher-01b-dashboard-servers.png` | status band + tab rail + server cards + add form, rail with player/purse/install |
| `fullmoon-launcher-01c-dashboard-wallet.png` | wallet: hero balance, income/expense cells, hairline tx ledger |
| `fullmoon-launcher-01d-dashboard-news.png` | news as a ledger: swatch, badge, title, summary, date |
| `fullmoon-launcher-02-mods.png` | mods with instance chips, tabs, search, cards, bundle side panel — all visible again |
| `fullmoon-launcher-03-cosmetics.png` | slots + live stage + catalogue, post-stacking-fix |
| `fullmoon-launcher-04-accounts.png` | account hero + bench grid |
| `fullmoon-launcher-05-settings-general.png` | settings body restored (Java ledger) — the backdrop-bug proof |
| `fullmoon-launcher-06-settings-hud.png` | HUD editor over the rehearsal horizon |
| `fullmoon-launcher-07-command-palette.png` | Ctrl+K palette over the shell |
| `fullmoon-launcher-08-launch-overlay.png` | launch overlay + the play button's in-flight state behind it |
| `fullmoon-launcher-10-light-play.png` / `10-light-play-dash.png` | light theme: paper sky, same system |
| `fullmoon-launcher-11-en-play.png` / `11-en-play-dash.png` | English locale: no Korean literals survive |
| `fullmoon-launcher-20-fold-1280.png` / `21-fold-1280-dash.png` | the 1280×800 fold: CTA and deck above it |
| `fullmoon-launcher-22-narrow-1100.png` / `23-narrow-1100-dash.png` | 1100px: grids collapse to one column, no h-scroll |

Superseded this pass (left on disk for the delete queue): `01-home`, `01b-dashboard-wallet`,
`01b-wallet`, `01c-dashboard-servers`, `01c-news` — they depict the removed `game-*` build.

## Addendum — P5-R2 (review fixes + masthead titlebar)

The four review findings shipped, plus a full titlebar redesign. Gate re-check on the
changed surfaces:

- Gate 30 (icons): the session chip reuses the bespoke set (terminal glyph); the live dot
  is a 5px square like every status mark. No emoji. Pass.
- Gate 26 (states): `.titlebar-session` carries default/hover/focus-visible(global)/active
  (native button press)/no disabled; `.cos-slot-main` and `.cos-unequip` are real buttons
  with the same five. Pass.
- Gate 39 (inputs): n/a — no fields touched.
- Gate 45 (motivated ornament): the session chip exists because a hidden overlay must not
  orphan a running game; the amber/green dot states that fact. Pass.
- Gate 48 (tokens): titlebar colours are all tokens; the `--titlebar-h` density token
  replaced the hardcoded 40px row (and the play-wrap calc now consumes it). Pass.
- Gate 34 (no h-scroll): re-probed at 1600/1280/1100 after the titlebar grid — negative.
- Headings: topbar h1 → h2 sections everywhere; the play display headline is a `p`
  (display text, not a document heading); the featured title inside its button is a
  `span`; modal titles are h2. Pass.
- Honest copy: the wallet window count now states "최근 30건 · 전체 N건" when truncated
  (`home.txWindow`); the stats and ledger read the same window. Pass.

Evidence: `evidence-titlebar-idle.png`, `evidence-titlebar-session.png`,
`fullmoon-launcher-03-cosmetics.png` (un-nested slot controls).
