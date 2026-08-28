package dev.fullmoon.client.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoxTest {
    private static final Box AREA = new Box(10, 20, 100, 60);

    @Test
    void edgesReadBack() {
        assertEquals(110, AREA.right());
        assertEquals(80, AREA.bottom());
        assertEquals(60, AREA.midX());
        assertEquals(50, AREA.midY());
    }

    @Test
    void reversedEdgesCollapseInsteadOfGoingNegative() {
        Box collapsed = Box.between(40, 40, 10, 10);
        assertEquals(new Box(40, 40, 0, 0), collapsed);
        assertTrue(collapsed.empty());
    }

    @Test
    void oversizedInsetCollapses() {
        assertTrue(AREA.inset(80).empty());
        assertEquals(new Box(20, 30, 80, 40), AREA.inset(10));
        assertEquals(new Box(11, 22, 97, 56), AREA.insets(1, 2, 2, 2));
    }

    @Test
    void trailingEdgesBelongToTheNextBox() {
        assertTrue(AREA.holds(10, 20));
        assertTrue(AREA.holds(109.9, 79.9));
        assertFalse(AREA.holds(110, 79));
        assertFalse(AREA.holds(109, 80));
        assertFalse(AREA.holds(9.9, 20));
    }

    @Test
    void splitLeftTakesTheHeadAndSkipsTheGap() {
        Box.Split split = AREA.splitLeft(30, 6);
        assertEquals(new Box(10, 20, 30, 60), split.head());
        assertEquals(new Box(46, 20, 64, 60), split.rest());
    }

    @Test
    void splitBeyondTheBoxLeavesNothingBehind() {
        Box.Split split = AREA.splitLeft(500, 6);
        assertEquals(AREA, split.head());
        assertTrue(split.rest().empty());
    }

    @Test
    void splitTopTakesTheHeadAndSkipsTheGap() {
        Box.Split split = AREA.splitTop(18, 4);
        assertEquals(new Box(10, 20, 100, 18), split.head());
        assertEquals(new Box(10, 42, 100, 38), split.rest());
    }

    @Test
    void columnsTileTheBoxExactly() {
        for (int count = 1; count <= 7; count++) {
            for (int gap = 0; gap <= 5; gap++) {
                Box first = AREA.col(0, count, gap);
                assertEquals(AREA.x(), first.x(), "count=" + count + " gap=" + gap);

                int narrowest = first.w();
                int widest = first.w();
                for (int i = 1; i < count; i++) {
                    Box previous = AREA.col(i - 1, count, gap);
                    Box column = AREA.col(i, count, gap);
                    assertEquals(previous.right() + gap, column.x(),
                        "count=" + count + " gap=" + gap + " index=" + i);
                    assertEquals(AREA.y(), column.y());
                    assertEquals(AREA.h(), column.h());
                    narrowest = Math.min(narrowest, column.w());
                    widest = Math.max(widest, column.w());
                }

                assertEquals(AREA.right(), AREA.col(count - 1, count, gap).right(),
                    "count=" + count + " gap=" + gap);
                assertTrue(widest - narrowest <= 1,
                    "remainder left a ragged column: count=" + count + " gap=" + gap);
            }
        }
    }

    @Test
    void rowsAreColumnsTransposed() {
        for (int count = 1; count <= 5; count++) {
            for (int i = 0; i < count; i++) {
                Box column = new Box(AREA.y(), AREA.x(), AREA.h(), AREA.w()).col(i, count, 4);
                Box row = AREA.row(i, count, 4);
                assertEquals(new Box(column.y(), column.x(), column.h(), column.w()), row);
            }
        }
    }

    @Test
    void centredLeavesTheOddPixelOnTheTrailingEdge() {
        assertEquals(new Box(34, 39, 51, 21), AREA.centred(51, 21));
        assertEquals(AREA, AREA.centred(AREA.w(), AREA.h()));
    }
}
