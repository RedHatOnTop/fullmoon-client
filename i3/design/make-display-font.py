#!/usr/bin/env python3
"""Bake the Fullmoon display face out of the system Noto Serif CJK variable font.

The display role is a serif on purpose. A UI that sets every string in one sans is a
template UI; the counter-tell is a real pairing, so headings and numerals get a Korean
serif and everything else stays on Pretendard.

Minecraft's ttf font provider cannot instance a variable font and will not apply an
OpenType weight axis, so the weight has to be frozen here, at build time. The subset is
Latin plus the Korean blocks; anything outside it falls through to the Pretendard
provider listed after this one in the font json.
"""

import pathlib
import sys

from fontTools import subset
from fontTools.ttLib import TTCollection
from fontTools.varLib import instancer

from _glyf import to_glyf

SOURCE = pathlib.Path("/usr/share/fonts/google-noto-serif-cjk-vf-fonts/NotoSerifCJK-VF.ttc")
KR_FACE = 1
WEIGHT = 600
OUT = pathlib.Path(__file__).resolve().parents[1] / "mod/src/main/resources/assets/fullmoon/font/display.ttf"

UNICODE_RANGES = [
    (0x0020, 0x007E),  # basic latin
    (0x00A0, 0x00FF),  # latin-1 supplement
    (0x2000, 0x206F),  # general punctuation: real dashes, real quotes, the ellipsis
    (0x20A0, 0x20BF),  # currency, for the store
    (0x2190, 0x21FF),  # arrows
    (0x2200, 0x22FF),  # maths operators
    (0x3000, 0x303F),  # cjk symbols and punctuation
    (0x3130, 0x318F),  # hangul compatibility jamo: standalone letters, as in chat
]


def ks_x_1001_syllables() -> set[int]:
    """The 2350 precomposed syllables of KS X 1001, derived rather than tabulated.

    Carrying all 11172 modern syllables costs 5 MB of jar for glyphs that authored
    display copy never reaches. KS X 1001 is exactly the set EUC-KR can encode, and it
    covers running Korean prose; a syllable outside it falls through to Pretendard
    SemiBold, which is the provider listed after this one.
    """
    out = set()
    for cp in range(0xAC00, 0xD7A4):
        try:
            encoded = chr(cp).encode("euc_kr")
        except UnicodeEncodeError:
            continue
        # Python's euc_kr codec is the UHC superset and encodes all 11172 syllables, so
        # the wansung lead-byte range is what actually selects KS X 1001.
        if len(encoded) == 2 and 0xB0 <= encoded[0] <= 0xC8:
            out.add(cp)
    return out


def main() -> int:
    if not SOURCE.exists():
        print(f"missing source font: {SOURCE}", file=sys.stderr)
        return 1

    collection = TTCollection(str(SOURCE), lazy=False)
    font = collection.fonts[KR_FACE]
    family = font["name"].getDebugName(16) or font["name"].getDebugName(1)
    if "KR" not in family:
        print(f"face {KR_FACE} is {family!r}, not the Korean face", file=sys.stderr)
        return 1

    codepoints = {cp for lo, hi in UNICODE_RANGES for cp in range(lo, hi + 1)}
    codepoints |= ks_x_1001_syllables()
    options = subset.Options()
    # The game maps a codepoint straight to a glyph and advances by hmtx; there is no
    # shaping engine anywhere in its text path, so every layout table is dead weight.
    options.layout_features = []
    # Vertical metrics have no reader here and fontTools warns that it cannot keep VORG
    # consistent through instancing, so drop the vertical tables outright.
    options.drop_tables += ["BASE", "DSIG", "VORG", "vhea", "vmtx", "VVAR"]
    options.name_IDs = ["*"]
    options.name_legacy = True
    options.notdef_outline = True
    options.recalc_bounds = True
    subsetter = subset.Subsetter(options=options)
    subsetter.populate(unicodes=codepoints)
    subsetter.subset(font)

    frozen = instancer.instantiateVariableFont(font, {"wght": WEIGHT}, inplace=True, updateFontNames=True)
    frozen["name"].setName("Fullmoon Serif", 1, 3, 1, 0x409)
    frozen["name"].setName("Regular", 2, 3, 1, 0x409)
    frozen["name"].setName("Fullmoon Serif", 4, 3, 1, 0x409)
    frozen["name"].setName("FullmoonSerif-Display", 6, 3, 1, 0x409)

    to_glyf(frozen)
    for table in ("STAT", "fvar", "avar", "HVAR", "MVAR"):
        if table in frozen:
            del frozen[table]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    frozen.save(str(OUT))
    kb = OUT.stat().st_size / 1024
    glyphs = len(frozen.getGlyphOrder())
    print(f"wrote {OUT.relative_to(OUT.parents[7])}")
    print(f"  {glyphs} glyphs, {kb:.0f} KiB, wght frozen at {WEIGHT}, outlines in glyf")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
