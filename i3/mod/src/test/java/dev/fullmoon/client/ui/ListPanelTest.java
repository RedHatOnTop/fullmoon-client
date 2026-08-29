package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A list's arithmetic: where the thumb sits, which row a drag asks for, and the two doors the
 * wheel and the arrows come in through.
 *
 * <p>Rows are a fixed height, so all of this is integers over a row count — the panel itself can
 * be driven here, pointer and keyboard both, and only the names inside the rows would need a font.
 * The well below is five rows tall over a list of twelve, which is the only shape where a thumb
 * has anywhere to travel.
 */
class ListPanelTest {
    private static final int X = 40;
    private static final int Y = 60;
    private static final int W = 200;
    private static final int ROWS = 12;
    private static final int SEEN = 5;

    /** The viewport a well placed by {@link #well} ends up with, borders taken off. */
    private static final int TOP = Y + Tokens.Stroke.HAIR;
    private static final int TRACK = SEEN * ListRow.HEIGHT;

    /** Inside the scroll rail, which is the right edge of the viewport. */
    private static final double RAIL_X = X + W - Tokens.Stroke.HAIR - 1;

    private final List<Integer> picked = new ArrayList<>();
    private final AtomicInteger acted = new AtomicInteger();

    private ListPanel well(int count) {
        List<ListRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new ListRow("row " + i, "", acted::incrementAndGet));
        }
        ListPanel panel = new ListPanel("목록", rows, "비어 있다", picked::add);
        panel.place(new Box(X, Y, W, ListPanel.heightFor(SEEN)));
        return panel;
    }

    /** The y a click lands on row {@code i} at, taken from the middle of its band. */
    private static double rowY(int i) {
        return TOP + i * ListRow.HEIGHT + ListRow.HEIGHT / 2.0;
    }

    @Test
    void aWellSizedForFiveRowsShowsFive() {
        assertEquals(SEEN, well(ROWS).visible());
        assertTrue(well(ROWS).scrollable());
        assertFalse(well(SEEN).scrollable(), "a list that fits has nothing to scroll");
    }

    /** The mark leads and the view follows, which is what keeps the marked row on screen. */
    @Test
    void arrowsMoveTheMarkAndDragTheViewBehindThem() {
        ListPanel panel = well(ROWS);
        for (int i = 0; i < SEEN; i++) {
            panel.key(Chord.of(InputConstants.KEY_DOWN));
        }
        assertEquals(SEEN, panel.marked());
        assertEquals(1, panel.first(), "one row past the bottom is one row of view");

        panel.key(Chord.of(InputConstants.KEY_END));
        assertEquals(ROWS - 1, panel.marked());
        assertEquals(ROWS - SEEN, panel.first(), "the end of a list is the last row at the bottom");

        panel.key(Chord.of(InputConstants.KEY_PAGEUP));
        assertEquals(ROWS - 1 - SEEN, panel.marked());

        panel.key(Chord.of(InputConstants.KEY_HOME));
        assertEquals(0, panel.marked());
        assertEquals(0, panel.first());

        panel.key(Chord.of(InputConstants.KEY_UP));
        assertEquals(0, panel.marked(), "off the top of a list is the top of a list");
        assertTrue(picked.isEmpty(), "walking a list is not choosing from it");
    }

    /** The wheel is the other door: it moves the view and leaves the mark where it was. */
    @Test
    void theWheelMovesTheViewAndLeavesTheMark() {
        ListPanel panel = well(ROWS);
        assertTrue(panel.scroll(-1));
        assertEquals(3, panel.first(), "a notch is three rows");
        assertEquals(0, panel.marked(), "the wheel does not carry the keyboard with it");

        assertTrue(panel.scroll(1));
        assertEquals(0, panel.first());
        assertFalse(panel.scroll(1), "a list at its end has not answered for the wheel");
    }

    /** Scroll away from the mark and the next arrow brings the view back to it. */
    @Test
    void anArrowAfterTheWheelPullsTheViewBackToTheMark() {
        ListPanel panel = well(ROWS);
        panel.scroll(-1);
        panel.key(Chord.of(InputConstants.KEY_DOWN));
        assertEquals(1, panel.marked());
        assertEquals(1, panel.first());
    }

    @Test
    void theThumbIsAsTallAsTheShareOfTheListInView() {
        assertEquals(TRACK * SEEN / ROWS, ListPanel.thumbH(TRACK, ROWS, SEEN));
        assertEquals(TRACK, ListPanel.thumbH(TRACK, SEEN, SEEN), "all of it in view is all of it");
        assertEquals(Tokens.Space.LOOSE, ListPanel.thumbH(TRACK, 1000, SEEN),
            "a very long list still needs something to grab");
        assertEquals(TRACK, ListPanel.thumbH(TRACK, 0, SEEN), "an empty list is not a divide by zero");
    }

    @Test
    void theThumbSitsWhereTheViewIs() {
        int thumb = ListPanel.thumbH(TRACK, ROWS, SEEN);
        int room = TRACK - thumb;
        assertEquals(TOP, ListPanel.thumbY(TOP, TRACK, thumb, ROWS, SEEN, 0));
        assertEquals(TOP + room, ListPanel.thumbY(TOP, TRACK, thumb, ROWS, SEEN, ROWS - SEEN),
            "the last row in view puts the thumb on the floor");
        assertEquals(TOP, ListPanel.thumbY(TOP, TRACK, TRACK, SEEN, SEEN, 0),
            "a thumb with nowhere to go stays at the top");
    }

    /** A thumb is taken by its middle, so the row it asks for is the row under the middle. */
    @Test
    void aDraggedThumbIsTakenByItsMiddle() {
        int thumb = ListPanel.thumbH(TRACK, ROWS, SEEN);
        int over = ROWS - SEEN;
        assertEquals(0, ListPanel.firstAt(TOP + thumb / 2.0, TOP, TRACK, thumb, ROWS, SEEN));
        assertEquals(over, ListPanel.firstAt(TOP + TRACK, TOP, TRACK, thumb, ROWS, SEEN),
            "dragged off the bottom is the bottom");
        assertEquals(0, ListPanel.firstAt(TOP - 500, TOP, TRACK, thumb, ROWS, SEEN),
            "dragged off the top is the top");
        assertEquals(0, ListPanel.firstAt(TOP + 40, TOP, TRACK, TRACK, SEEN, SEEN),
            "a list that fits cannot be dragged anywhere");
    }

    /** The rail keeps the pointer for the drag; a row hands it straight back. */
    @Test
    void theRailTakesThePointerAndARowDoesNot() {
        ListPanel panel = well(ROWS);
        assertTrue(panel.press(RAIL_X, TOP + 40), "the thumb is the only capture a list takes");
        panel.drag(RAIL_X, TOP + TRACK);
        assertEquals(ROWS - SEEN, panel.first());
        panel.release(RAIL_X, TOP + TRACK, true);

        assertFalse(panel.press(X + 20, rowY(1)), "dragging off a row means nothing");
        assertEquals(ROWS - SEEN + 1, panel.marked(), "the second row in view, not the second row");
        assertEquals(List.of(ROWS - SEEN + 1), picked);
        assertEquals(1, acted.get(), "the row's own callback and the list's are two facts");
    }

    /** The mark walks through a dead row rather than around it; only the pick is refused. */
    @Test
    void aDeadRowTakesTheMarkAndRefusesThePick() {
        List<ListRow> rows = new ArrayList<>();
        for (int i = 0; i < SEEN; i++) {
            rows.add(new ListRow("row " + i, "", acted::incrementAndGet));
        }
        rows.get(2).enabled(false);
        ListPanel panel = new ListPanel("목록", rows, "비어 있다", picked::add);
        panel.place(new Box(X, Y, W, ListPanel.heightFor(SEEN)));

        panel.press(X + 20, rowY(2));
        assertEquals(2, panel.marked());
        assertEquals(-1, panel.selected(), "a row that cannot answer is not the chosen one");
        assertTrue(picked.isEmpty());

        panel.press(X + 20, rowY(3));
        assertEquals(List.of(3), picked, "the row past it still answers");
    }

    @Test
    void anEmptyWellAnswersNoKeyAndNoClick() {
        ListPanel panel = well(0);
        assertFalse(panel.key(Chord.of(InputConstants.KEY_DOWN)), "nothing to move the mark to");
        assertFalse(panel.press(X + 20, rowY(0)));
        assertTrue(picked.isEmpty());
        assertEquals(SEEN, panel.visible(), "an empty well is still five rows tall");
    }
}
