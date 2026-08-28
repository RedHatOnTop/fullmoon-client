package dev.fullmoon.client.ui;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

/**
 * The frame this client's development surfaces share: a masthead with an accent bar, a section
 * head, and a footer rule that names the keys out of the screen.
 *
 * <p>It exists so the specimen and the kit read as two pages of one document rather than two
 * screens that happen to draw from the same tokens. The heights are published separately from
 * the drawing because a screen has to lay its widgets out in {@code init}, before there is a
 * painter to ask.
 *
 * <p>{@link SpecimenScreen} still carries its own copy of this. Moving it over means re-capturing
 * it, and that belongs with P1-D, where the two screens get a shared tab rail anyway.
 */
public final class DevChrome {
    private DevChrome() {}

    public static int headerHeight() {
        return Tokens.Type.DISPLAY.leading() + Tokens.Type.LABEL.leading() + Tokens.Space.GUTTER;
    }

    public static int sectionHeadHeight() {
        return Tokens.Type.LABEL.leading() + Tokens.Space.COZY;
    }

    /** The masthead. Returns the y its content starts at. */
    public static int header(Painter painter, int x, int y, int w, String subtitle) {
        painter.fill(x, y + Tokens.Space.SNUG, Tokens.Stroke.FOCUS,
            Tokens.Type.DISPLAY.px() - Tokens.Space.BASE, Tokens.Color.ACCENT);
        int textX = x + Tokens.Stroke.FOCUS + Tokens.Space.COZY;

        Typeset.draw(painter, Tokens.Type.DISPLAY, "Fullmoon", textX, y, Tokens.Color.INK_PRIMARY);
        Typeset.draw(painter, Tokens.Type.LABEL, subtitle, textX,
            y + Tokens.Type.DISPLAY.leading(), Tokens.Color.INK_TERTIARY);
        Typeset.tabularRight(painter, Tokens.Type.LABEL,
            painter.width() + " × " + painter.height() + " gui px",
            x + w, y + Tokens.Space.TIGHT, Tokens.Color.INK_TERTIARY);

        painter.hRule(x, y + Tokens.Type.DISPLAY.leading() + Tokens.Type.LABEL.leading(), w,
            Tokens.Color.LINE_STRONG);
        return y + headerHeight();
    }

    /** A section head is a label with an accent tick, never a tag left and a value right. */
    public static int sectionHead(Painter painter, String name, int x, int y) {
        painter.fill(x, y + Tokens.Space.TIGHT, Tokens.Stroke.HAIR,
            Tokens.Type.LABEL.px() - Tokens.Space.TIGHT, Tokens.Color.ACCENT);
        Typeset.draw(painter, Tokens.Type.LABEL, name, x + Tokens.Space.BASE, y,
            Tokens.Color.INK_SECONDARY);
        return y + sectionHeadHeight();
    }

    public static void footer(Painter painter, int x, int y, int w, String keys, String status) {
        painter.hRule(x, y, w, Tokens.Color.LINE_HAIRLINE);
        int textY = y + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.LABEL, keys, x, textY, Tokens.Color.INK_TERTIARY);
        Typeset.tabularRight(painter, Tokens.Type.LABEL, status, x + w, textY,
            Tokens.Color.INK_TERTIARY);
    }

    /** Where the footer rule goes on a screen {@code height} tall. */
    public static int footerY(int height) {
        return height - Tokens.Space.SECTION - Tokens.Type.LABEL.leading();
    }
}
