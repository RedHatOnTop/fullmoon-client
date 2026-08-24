# site/ — the source walkthrough (GitHub Pages)

Five static pages, no build step. Published to GitHub Pages by
`.github/workflows/site.yml` on every push to `main` that touches this
directory.

Design tokens mirror fullmoon.ink 1:1 (the cardTheme.js "달빛 밤하늘"
palette) — same navy, same gold, same Pretendard. The font files load from
fullmoon.ink rather than being duplicated here; if the site must render
standalone offline, drop the three Pretendard OTFs into `assets/` and point
the @font-face srcs at the local copies.

Pages:

    index.html    structure map + reading order
    launcher.html 01 — bindings contract, install pipeline, auth
    mod.html      02 — Ui kit, warp screen, bridge client rules
    bridge.html   03 — fullmoon:v1 wire format and server authority
    trust.html    04 — single distribution path, SHA256SUMS, signed OTA

Facts stated on these pages (line counts, file names, behavior) must be
checked against the code they describe when the code changes. A walkthrough
that lies about the code is worse than no walkthrough.
