package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dev.fullmoon.client.layout.Box;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Where an open select can be clicked, and what the keyboard does while its list is up.
 *
 * <p>An unlabelled select has no measured text in its geometry, so the rows land at arithmetic this
 * test can state outright: placed at y 20 with a height of 24, the list starts at 50 and each row
 * is 16 tall.
 */
class SelectTest {
    private static final List<String> OPTIONS = List.of("가", "나", "다");

    private static final int FIRST_ROW = 58;
    private static final int SECOND_ROW = 74;
    private static final int THIRD_ROW = 90;

    private static Select select(AtomicInteger picked) {
        Select select = new Select("", OPTIONS, 0, picked::set);
        select.place(new Box(40, 20, 160, Select.HEIGHT));
        return select;
    }

    @Test
    void closedItReachesNoFurtherThanItDraws() {
        Select select = select(new AtomicInteger());
        assertEquals(select.bounds(), select.reach());
        assertFalse(select.open());
    }

    @Test
    void openItReachesDownOverItsList() {
        Select select = select(new AtomicInteger());
        select.act();
        assertTrue(select.open());
        Box reach = select.reach();
        assertEquals(select.bounds().x(), reach.x());
        assertEquals(select.bounds().right(), reach.right());
        assertTrue(reach.holds(120, THIRD_ROW), "the last row is inside the control");
        assertFalse(reach.holds(120, THIRD_ROW + 40), "and the floor below it is not");
    }

    @Test
    void aClickOnARowPicksIt() {
        AtomicInteger picked = new AtomicInteger(-1);
        Select select = select(picked);
        select.act();

        assertFalse(select.press(120, SECOND_ROW), "picking a row does not grab the pointer");
        assertEquals(1, select.index());
        assertEquals("나", select.picked());
        assertEquals(1, picked.get());
        assertFalse(select.open(), "and it takes the list down with it");
    }

    @Test
    void pickingWhatWasAlreadyPickedTellsNobody() {
        AtomicInteger picked = new AtomicInteger(-1);
        Select select = select(picked);
        select.act();
        assertFalse(select.press(120, FIRST_ROW));
        assertEquals(0, select.index());
        assertEquals(-1, picked.get());
    }

    /** The list's own padding is not a row. The control has been pressed, and the release says what for. */
    @Test
    void aClickOnThePaddingHoldsTheControl() {
        Select select = select(new AtomicInteger());
        select.act();
        assertTrue(select.press(120, 47));
        assertTrue(select.open());
    }

    @Test
    void hoverMarksTheRowUnderThePointer() {
        Select select = select(new AtomicInteger());
        select.act();
        select.hovering(120, THIRD_ROW);
        assertEquals(2, select.marked());
        select.hovering(120, 47);
        assertEquals(2, select.marked(), "the padding does not unmark the row the pointer left");
    }

    @Test
    void theKeyboardMarksARowAndThenTakesIt() {
        AtomicInteger picked = new AtomicInteger(-1);
        Select select = select(picked);
        select.act();

        assertTrue(select.key(Chord.of(InputConstants.KEY_DOWN)));
        assertTrue(select.key(Chord.of(InputConstants.KEY_DOWN)));
        assertTrue(select.key(Chord.of(InputConstants.KEY_DOWN)));
        assertEquals(2, select.marked(), "the mark stops at the last row");
        assertEquals(0, select.index(), "and marking is not picking");

        assertTrue(select.key(Chord.of(InputConstants.KEY_RETURN)));
        assertEquals(2, select.index());
        assertEquals(2, picked.get());
        assertFalse(select.open());
    }

    @Test
    void escapeClosesTheListAndKeepsThePick() {
        AtomicInteger picked = new AtomicInteger(-1);
        Select select = select(picked);
        select.act();
        select.key(Chord.of(InputConstants.KEY_UP));
        assertTrue(select.key(Chord.of(InputConstants.KEY_ESCAPE)));
        assertFalse(select.open());
        assertEquals(0, select.index());
        assertEquals(-1, picked.get());
    }

    /** A closed select answers no keys at all, so Space still reaches the surface as an activation. */
    @Test
    void closedItLeavesTheKeysAlone() {
        Select select = select(new AtomicInteger());
        assertFalse(select.key(Chord.of(InputConstants.KEY_DOWN)));
        assertFalse(select.key(Chord.of(InputConstants.KEY_ESCAPE)));
    }

    /**
     * Registration order is Tab order, so a select that opens sits above controls registered after
     * it. Drawing over them and being clickable over them have to be the same fact.
     */
    @Test
    void anOpenListIsHitBeforeWhateverItCovers() {
        Surface surface = new Surface();
        Select select = surface.add(select(new AtomicInteger()));
        Slider under = surface.add(new Slider(0, 100, 5, 0, i -> { }));
        under.place(new Box(40, SECOND_ROW - Slider.HEIGHT / 2, 160, Slider.HEIGHT));

        assertSame(under, surface.at(120, SECOND_ROW), "closed, there is no list to hit");
        select.act();
        assertSame(select, surface.at(120, SECOND_ROW), "open, the row is in front of the rail");
        select.press(120, SECOND_ROW);
        assertEquals(1, select.index());
        assertSame(under, surface.at(120, SECOND_ROW), "and the rail is back the moment it closes");
    }

    @Test
    void tabbingAwayTakesTheListWithIt() {
        Surface surface = new Surface();
        Select first = surface.add(select(new AtomicInteger()));
        Select second = surface.add(select(new AtomicInteger()));

        assertTrue(surface.key(Chord.of(InputConstants.KEY_TAB)));
        assertSame(first, surface.held());
        assertTrue(surface.key(Chord.of(InputConstants.KEY_SPACE)));
        assertTrue(first.open());

        assertTrue(surface.key(Chord.of(InputConstants.KEY_TAB)));
        assertSame(second, surface.held());
        assertFalse(first.open(), "a list nobody is holding is litter");
    }
}
