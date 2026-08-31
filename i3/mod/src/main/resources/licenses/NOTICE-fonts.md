# Fonts

Both faces this client ships are Modified Versions of OFL-1.1 fonts, subset and rewritten
from cubic to quadratic outlines by the scripts under `i3/design/`. Both upstreams carry a
Reserved Font Name, so both derivatives are renamed, as the licence requires.

## Fullmoon Sans — `assets/fullmoon/font/sans-{regular,semibold}.ttf`

Derived from **Pretendard** 1.309, © 2023 Kil Hyung-jin, RFN `Pretendard`.
<https://github.com/orioncactus/pretendard> — licence in `OFL-Pretendard.txt`.

Baked by `i3/design/make-ui-font.py`: subset to Latin, punctuation, currency, arrows, box
drawing, CJK punctuation, compatibility jamo and all 11172 modern Hangul syllables; layout
tables dropped; outlines converted to `glyf`.

## Fullmoon Serif — `assets/fullmoon/font/display.ttf`

Derived from **Noto Serif CJK KR** (variable), © 2017-2024 Adobe, RFN `Source`.
<https://github.com/notofonts/noto-cjk> — licence in `OFL-NotoSerifCJK.txt`.

Baked by `i3/design/make-display-font.py`: `wght` instanced at 600, subset to the same
Latin and punctuation plus the 2350 syllables of KS X 1001, layout tables dropped, outlines
converted to `glyf`.

## Why the outlines are converted

`TrueTypeGlyphProviderDefinition` asks FreeType for `FT_Get_Font_Format` and refuses
anything that does not answer `TrueType`. Both upstreams are OpenType/CFF, which answers
`CFF`. `i3/design/ftcheck.sh` runs that same check against the shipped files.
