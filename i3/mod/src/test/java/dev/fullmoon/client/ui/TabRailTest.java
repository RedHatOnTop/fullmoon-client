package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import dev.fullmoon.client.design.Tokens;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The rail's two halves: which tab a pointer is on, and the walk-then-commit the arrows do.
 *
 * <p>The tabs are as wide as their names, so the hit test takes the widths as a table and can be
 * checked without a font. The keyboard half needs nothing measured at all — a tab here replaces the
 * whole screen, so what the arrows move is a mark, and the thing worth proving is that walking the
 * rail loads no pages.
 */
class TabRailTest {
    private static final int LEFT = 100;
    private static final int GAP = Tokens.Space.SNUG;
    private static final int[] WIDTHS = {40, 60, 30};

    private final List<Integer> picked = new ArrayList<>();

    private TabRail rail() {
        return new TabRail("페이지", List.of("표본", "위젯", "목록"), 0, picked::add);
    }

    @Test
    void picksTheTabUnderThePointer() {
        assertEquals(0, TabRail.pick(LEFT, LEFT, WIDTHS, GAP));
        assertEquals(0, TabRail.pick(LEFT + 39, LEFT, WIDTHS, GAP));
        assertEquals(1, TabRail.pick(LEFT + 44, LEFT, WIDTHS, GAP));
        assertEquals(2, TabRail.pick(LEFT + 108, LEFT, WIDTHS, GAP));
        assertEquals(2, TabRail.pick(LEFT + 137, LEFT, WIDTHS, GAP), "the last pixel of the last tab");
    }

    /** The gaps are the rule showing through, and a rule is not a tab. */
    @Test
    void picksNothingInTheGapsOrOffTheEnds() {
        assertEquals(-1, TabRail.pick(LEFT + 40, LEFT, WIDTHS, GAP));
        assertEquals(-1, TabRail.pick(LEFT + 43, LEFT, WIDTHS, GAP));
        assertEquals(-1, TabRail.pick(LEFT + 138, LEFT, WIDTHS, GAP), "past the last tab");
        assertEquals(-1, TabRail.pick(LEFT - 1, LEFT, WIDTHS, GAP), "before the first");
    }

    /** Three page loads on the way to the fourth tab is what the mark exists to prevent. */
    @Test
    void arrowsWalkTheRailWithoutOpeningAnything() {
        TabRail rail = rail();
        assertTrue(rail.key(Chord.of(InputConstants.KEY_RIGHT)));
        rail.key(Chord.of(InputConstants.KEY_RIGHT));
        assertEquals(2, rail.marked());
        rail.key(Chord.of(InputConstants.KEY_RIGHT));
        assertEquals(2, rail.marked(), "off the end of the rail is the end of the rail");
        assertEquals(0, rail.index(), "the page has not changed");
        assertTrue(picked.isEmpty());

        rail.key(Chord.of(InputConstants.KEY_HOME));
        assertEquals(0, rail.marked());
        rail.key(Chord.of(InputConstants.KEY_LEFT));
        assertEquals(0, rail.marked(), "off the front is the front");
        rail.key(Chord.of(InputConstants.KEY_END));
        assertEquals(2, rail.marked());
    }

    @Test
    void enterOpensTheMarkedTabOnceAndOnly() {
        TabRail rail = rail();
        rail.key(Chord.of(InputConstants.KEY_END));
        rail.act();
        assertEquals(2, rail.index());
        assertEquals("목록", rail.picked());
        assertEquals(List.of(2), picked);

        rail.act();
        assertEquals(List.of(2), picked, "the page you are on does not reload");
    }

    /** Tab away mid-walk and the mark goes back to the page the screen is actually showing. */
    @Test
    void blurringPutsTheMarkBackOnThePageYouAreOn() {
        TabRail rail = rail();
        rail.key(Chord.of(InputConstants.KEY_END));
        rail.act();
        rail.key(Chord.of(InputConstants.KEY_HOME));
        assertEquals(0, rail.marked());

        rail.blurred();
        assertEquals(2, rail.marked());
        assertEquals(List.of(2), picked, "leaving the rail is not choosing from it");
    }

    /** A key the rail has no answer for belongs to the screen behind it, which closes on it. */
    @Test
    void leavesEscapeToTheScreen() {
        assertFalse(rail().key(Chord.of(InputConstants.KEY_ESCAPE)));
    }
}
