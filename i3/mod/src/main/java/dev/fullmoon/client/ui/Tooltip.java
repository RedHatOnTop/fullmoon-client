package dev.fullmoon.client.ui;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

/**
 * What a control has to add about itself, in a box beside it.
 *
 * <p>Not a {@link Widget}, and it has no eight states. Nothing can hover a tooltip or put the
 * keyboard on one — it is the only thing on a surface a player cannot reach, which is exactly why
 * it must never be the only place something is said. It is drawn from another control's hint, and
 * {@link Surface#tipped} decides whose.
 *
 * <p>One line, because a hint that needs two is documentation and belongs on the surface itself.
 *
 * <p>No delay and no fade in this phase: both are motion, and motion arrives with the tokens that
 * describe it. What is here is the box, the words and the flip that keeps them inside the region the
 * surface hands them.
 */
public final class Tooltip {
    private static final int PAD_X = Tokens.Space.COZY;
    private static final int PAD_Y = Tokens.Space.SNUG;

    /**
     * How far a hint stands off the control it belongs to, and off the two lines that close the
     * region above and below it. Not off the region's sides: those are edges the whole page is
     * already aligned to, and a hint held four pixels clear of them lines up with nothing.
     */
    private static final int GAP = Tokens.Space.SNUG;

    private Tooltip() {}

    public static int height() {
        return Tokens.Type.BODY.leading() + PAD_Y * 2;
    }

    /**
     * Draws {@code hint} beside {@code near} and inside {@code within}, which is the ground the
     * surface owns rather than the whole window: a hint that leaves the column hangs over nothing,
     * and one that reaches the footer covers the line telling the player how to leave.
     */
    public static void draw(Painter painter, String hint, Box near, Box within) {
        if (hint.isEmpty()) {
            return;
        }
        int w = Typeset.width(Tokens.Type.BODY, hint) + PAD_X * 2;
        Box box = place(near, w, height(), within);
        painter.fill(box.x(), box.y(), box.w(), box.h(), Tokens.Radius.SM,
            Tokens.Color.SURFACE_OVERLAY);
        painter.border(box.x(), box.y(), box.w(), box.h(), Tokens.Radius.SM, Tokens.Stroke.HAIR,
            Tokens.Color.LINE_STRONG);
        Typeset.draw(painter, Tokens.Type.BODY, hint, box.x() + PAD_X,
            Typeset.centred(Tokens.Type.BODY, box.y(), box.h()), Tokens.Color.INK_PRIMARY);
    }

    /**
     * Below the control and aligned to its left edge, which is where the eye already is. It flips
     * above when there is no room below, and slides along a side rather than off it — a tooltip
     * that has to be nudged is still readable, and one that runs off the ground it is drawn on is
     * not.
     *
     * <p>Nudged as far as the side itself and no further. Most controls on a page sit on the
     * region's left edge, so holding the hint a gap clear of it would push every one of those hints
     * off the line the rest of the page is aligned to, to buy room against nothing: the sides carry
     * no rule, and the masthead's own runs the full width of them.
     */
    static Box place(Box near, int w, int h, Box within) {
        int x = Math.clamp(near.x(), within.x(), Math.max(within.x(), within.right() - w));
        int below = near.bottom() + GAP;
        int above = near.y() - GAP - h;
        int y = below + h + GAP <= within.bottom() ? below : Math.max(within.y() + GAP, above);
        return new Box(x, y, w, h);
    }
}
