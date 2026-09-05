package dev.fullmoon.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudGridTest {

    @Test
    void aCoordinateGoesToTheNearestGridLine() {
        assertEquals(0, HudGrid.snap(1, 4));
        assertEquals(4, HudGrid.snap(3, 4));
        assertEquals(56, HudGrid.snap(57, 4));
        assertEquals(56, HudGrid.snap(56, 4));
    }

    @Test
    void aCoordinateExactlyBetweenTwoLinesGoesUp() {
        // Math.round's rule, and JavaScript's: the launcher's editor has to break the tie the same way
        assertEquals(4, HudGrid.snap(2, 4));
        assertEquals(-4, HudGrid.snap(-6, 4));
    }

    @Test
    void aNegativeCoordinateStillLandsOnTheGrid() {
        // a drag can pass the left edge before the offset is clamped
        assertEquals(-8, HudGrid.snap(-7, 4));
        assertEquals(0, HudGrid.snap(-1, 4));
    }

    @Test
    void aStepTheFileCannotMeanFallsBackToTheDefault() {
        assertEquals(4, HudGrid.sanitize(0));
        assertEquals(4, HudGrid.sanitize(-16));
        assertEquals(16, HudGrid.sanitize(16));
        assertEquals(1, HudGrid.sanitize(1));
        assertEquals(56, HudGrid.snap(57, 0));
    }

    @Test
    void aStepOfOneSnapsToEveryPixel() {
        assertEquals(57, HudGrid.snap(57, 1));
    }
}
