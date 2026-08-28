"""Rewrite a cubic-outline font (CFF or CFF2) as a quadratic-outline one (glyf).

This is not a size or fidelity choice. The game's ttf font provider asks FreeType for
FT_Get_Font_Format and throws `IOException("Font is not in TTF format, was ...")` unless
the answer is the literal string "TrueType". FreeType answers "CFF" for every
OpenType/CFF font, whatever the file is named, so a cubic font cannot be loaded by the
game at all. Both faces this client ships are CFF at source, so both come through here.
"""

from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import newTable
from fontTools.ttLib.tables._g_l_y_f import table__g_l_y_f

# Curve error budget, in em. A thousandth of an em is a third of a physical pixel at the
# largest size this client sets (display, 22 px, oversampled 2x).
MAX_ERROR_EM = 0.001


def to_glyf(font) -> None:
    """Replace the font's CFF/CFF2 outlines with a glyf table, in place."""
    tolerance = font["head"].unitsPerEm * MAX_ERROR_EM
    glyph_set = font.getGlyphSet()
    order = font.getGlyphOrder()

    glyf = table__g_l_y_f()
    glyf.glyphOrder = order
    glyf.glyphs = {}
    for name in order:
        pen = TTGlyphPen(None)
        # TrueType and CFF disagree on which winding fills, so contours are reversed on
        # the way through; without this every counter comes out solid.
        glyph_set[name].draw(Cu2QuPen(pen, tolerance, reverse_direction=True))
        glyf.glyphs[name] = pen.glyph()
    for name in order:
        glyf.glyphs[name].recalcBounds(glyf)  # maxp and hhea both read these back

    for table in ("CFF ", "CFF2", "VORG"):
        if table in font:
            del font[table]

    font["glyf"] = glyf
    font["loca"] = newTable("loca")  # filled in by glyf's compiler
    font["head"].glyphDataFormat = 0
    # An OTF carries sfntVersion "OTTO", which tells FreeType to go looking for a CFF table
    # and fail the whole face when it finds glyf instead. FT_New_Memory_Face rejects the
    # file outright, before any format string is reported.
    font.sfntVersion = "\x00\x01\x00\x00"

    maxp = newTable("maxp")
    maxp.tableVersion = 0x00010000
    # The conversion emits no hinting programs, so every interpreter limit below is nil.
    maxp.maxZones = 1
    maxp.maxTwilightPoints = 0
    maxp.maxStorage = 0
    maxp.maxFunctionDefs = 0
    maxp.maxInstructionDefs = 0
    maxp.maxStackElements = 0
    maxp.maxSizeOfInstructions = 0
    font["maxp"] = maxp
    maxp.recalc(font)  # numGlyphs, point and contour ceilings, and head's bounding box
