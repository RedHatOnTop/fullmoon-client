package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import org.junit.jupiter.api.Test;

/**
 * Where a hint lands. The text inside it needs a font and the box around it does not, which is
 * why {@link Tooltip#place} takes the width already measured — the part worth checking is what it
 * does at the four edges, and none of that depends on the words.
 *
 * <p>Those edges belong to the region the surface hands the hint and not to the window. Most of
 * these pass the whole screen, which is the case where the two are the same thing; the inset one is
 * the case a surface with chrome around it actually asks for.
 */
class TooltipTest {
    private static final int GAP = Tokens.Space.SNUG;
    private static final int W = 80;
    private static final int H = Tooltip.height();
    private static final int SCREEN_W = 400;
    private static final int SCREEN_H = 300;
    private static final Box SCREEN = screen(SCREEN_H);

    private static final Box NEAR = new Box(100, 100, 60, Tokens.Space.SECTION);

    /** The window, as the region: the whole of it is the hint's to use. */
    private static Box screen(int h) {
        return new Box(0, 0, SCREEN_W, h);
    }

    @Test
    void sitsBelowTheControlOnItsLeftEdge() {
        Box box = Tooltip.place(NEAR, W, H, SCREEN);
        assertEquals(NEAR.x(), box.x(), "the eye is already at the left edge of the control");
        assertEquals(NEAR.bottom() + GAP, box.y());
    }

    /** A control near the bottom would push its hint off the screen, so the hint goes over it. */
    @Test
    void flipsAboveWhenThereIsNoRoomBelow() {
        int screenH = NEAR.bottom() + GAP + H;
        assertEquals(NEAR.y() - GAP - H, Tooltip.place(NEAR, W, H, screen(screenH - 1)).y());
        assertEquals(NEAR.bottom() + GAP, Tooltip.place(NEAR, W, H, screen(screenH + GAP)).y(),
            "room for the hint and the gap under it is room");
    }

    /** Against the right edge it slides along rather than off: a nudged hint is still readable. */
    @Test
    void slidesAlongTheEdgeRatherThanOffIt() {
        Box far = NEAR.at(SCREEN_W - NEAR.w(), NEAR.y());
        assertEquals(SCREEN_W - W, Tooltip.place(far, W, H, SCREEN).x());
        assertEquals(0, Tooltip.place(NEAR.at(-40, NEAR.y()), W, H, SCREEN).x(),
            "off the left of the screen is the left of the screen");
    }

    /**
     * The nudge stops at the side, not a gap short of it. A page is laid out from its region's left
     * edge, so most controls sit on it — inset the clamp and every one of their hints comes away
     * from the line the section heads, the rows and the masthead are all aligned to.
     */
    @Test
    void aControlOnTheRegionsLeftEdgeKeepsItsAlignment() {
        Box column = Box.between(220, 24, 740, 565);
        Box flush = new Box(column.x(), 300, 60, Tokens.Space.SECTION);
        assertEquals(column.x(), Tooltip.place(flush, W, H, column).x());

        Box last = new Box(column.right() - 60, 300, 60, Tokens.Space.SECTION);
        assertEquals(column.right() - W, Tooltip.place(last, W, H, column).x(),
            "and the far side is the far side, where the masthead's rule ends");
    }

    /** A hint wider than the screen has no good x. It takes the left edge and is cut on the right. */
    @Test
    void aHintTooWideForTheScreenStaysAtTheLeftEdge() {
        assertEquals(0, Tooltip.place(NEAR, SCREEN_W * 2, H, SCREEN).x());
    }

    /** Nowhere below and nowhere above: it stays on screen and covers the control instead. */
    @Test
    void aControlWithRoomOnNeitherSideKeepsTheHintOnScreen() {
        Box tight = NEAR.at(NEAR.x(), 0);
        assertEquals(GAP, Tooltip.place(tight, W, H, screen(tight.bottom() + GAP)).y());
    }

    /**
     * A surface's chrome is not the hint's ground. The window has room under the last control of a
     * page and room to the right of the last one in a column, and taking either is how a hint ends
     * up over the footer rule or hanging off the page into the blur.
     */
    @Test
    void theRegionsEdgesAreTheOnesThatCount() {
        Box column = Box.between(40, 20, 360, 260);

        Box low = new Box(60, 230, 60, Tokens.Space.SECTION);
        assertEquals(low.bottom() + GAP, Tooltip.place(low, W, H, SCREEN).y(),
            "the window has room below it");
        assertEquals(low.y() - GAP - H, Tooltip.place(low, W, H, column).y(),
            "the column does not, so the hint goes above rather than through the floor");

        Box late = new Box(300, 100, 60, Tokens.Space.SECTION);
        assertEquals(late.x(), Tooltip.place(late, W, H, SCREEN).x(), "inside the window");
        assertEquals(column.right() - W, Tooltip.place(late, W, H, column).x(),
            "and pulled back inside the column");
    }
}
