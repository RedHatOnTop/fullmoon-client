package dev.fullmoon.client.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StackTest {
    private static final Box AREA = new Box(10, 20, 100, 60);

    @Test
    void theFirstBandPaysNoGap() {
        Stack stack = new Stack(AREA, 8);
        assertEquals(new Box(10, 20, 100, 18), stack.next(18));
        assertEquals(18, stack.used());
    }

    @Test
    void laterBandsStackUnderTheLastOne() {
        Stack stack = new Stack(AREA, 8);
        stack.next(18);
        assertEquals(new Box(10, 46, 100, 12), stack.next(12));
        assertEquals(38, stack.used());
        assertEquals(58, stack.cursor());
    }

    @Test
    void skipMovesTheCursorWithoutHandingOutABand() {
        Stack stack = new Stack(AREA, 8);
        stack.next(10);
        stack.skip(6);
        assertEquals(new Box(10, 44, 100, 10), stack.next(10));
    }

    @Test
    void restIsWhatTheBandsHaveNotTaken() {
        Stack stack = new Stack(AREA, 8);
        stack.next(18);
        stack.next(12);
        assertEquals(new Box(10, 58, 100, 22), stack.rest());
    }

    @Test
    void anEmptyStackOwnsItsWholeArea() {
        Stack stack = new Stack(AREA, 8);
        assertEquals(AREA, stack.rest());
        assertEquals(AREA.y(), stack.cursor());
        assertFalse(stack.overflows());
    }

    @Test
    void overflowIsReportedAndNotClamped() {
        Stack stack = new Stack(AREA, 8);
        Box tall = stack.next(200);
        assertEquals(200, tall.h(), "a band that does not fit must keep its height");
        assertEquals(220, tall.bottom());
        assertTrue(stack.overflows());
        assertTrue(stack.rest().empty());
    }

    @Test
    void bandsThatExactlyFillTheAreaDoNotOverflow() {
        Stack stack = new Stack(AREA, 8);
        stack.next(26);
        stack.next(26);
        assertEquals(60, stack.used());
        assertFalse(stack.overflows());
        assertTrue(stack.rest().empty());
    }
}
