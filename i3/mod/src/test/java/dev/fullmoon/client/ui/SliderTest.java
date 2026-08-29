package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import dev.fullmoon.client.layout.Box;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A slider's arithmetic: the grain its value sits on, and the mapping from a pointer to a step.
 *
 * <p>A bare rail carries no label and no readout, so both ends of its travel are plain geometry
 * and the control itself can be driven here rather than a stand-in for it. The labelled shape has
 * measured text at both ends, which is why {@link Slider#valueAt} takes the travel as two numbers.
 */
class SliderTest {
    private static final int X = 100;
    private static final int W = 200;

    /** The travel a bare rail placed by {@link #rail} ends up with. */
    private static final int LEFT = X + Slider.KNOB / 2;
    private static final int RIGHT = X + W - Slider.KNOB / 2;

    private static Slider rail(int min, int max, int step, int value, AtomicInteger seen) {
        Slider slider = new Slider(min, max, step, value, seen::set);
        slider.place(new Box(X, 50, W, Slider.HEIGHT));
        return slider;
    }

    @Test
    void snapsToTheGrainAndClampsIntoTheRange() {
        AtomicInteger seen = new AtomicInteger(-1);
        assertEquals(45, rail(0, 100, 5, 43, seen).value());
        assertEquals(0, rail(0, 100, 5, -20, seen).value());
        assertEquals(100, rail(0, 100, 5, 999, seen).value());
        assertEquals(-1, seen.get(), "a constructor is not a change");
    }

    /** A step that does not divide the span still has to leave the top of the range reachable. */
    @Test
    void bothEndsAreOnTheGrain() {
        AtomicInteger seen = new AtomicInteger();
        assertEquals(90, rail(0, 90, 7, 90, seen).value());
        assertEquals(0, rail(0, 90, 7, 0, seen).value());
    }

    @Test
    void mapsAPointerOntoTheTravel() {
        Slider slider = rail(0, 100, 5, 0, new AtomicInteger());
        assertEquals(50, slider.valueAt((LEFT + RIGHT) / 2.0, LEFT, RIGHT));
        assertEquals(0, slider.valueAt(LEFT, LEFT, RIGHT));
        assertEquals(100, slider.valueAt(RIGHT, LEFT, RIGHT));
        assertEquals(0, slider.valueAt(LEFT - 500, LEFT, RIGHT), "off the left end is the left end");
        assertEquals(100, slider.valueAt(RIGHT + 500, LEFT, RIGHT));
    }

    /** A travel of no width would divide by zero, and a rail that narrow has nothing to say. */
    @Test
    void aCollapsedTravelKeepsTheValue() {
        Slider slider = rail(0, 100, 5, 40, new AtomicInteger());
        assertEquals(40, slider.valueAt(0, LEFT, LEFT));
    }

    @Test
    void theKnobSitsWhereTheValueIs() {
        assertEquals(LEFT, rail(0, 100, 5, 0, new AtomicInteger()).knobAt(LEFT, RIGHT));
        assertEquals(RIGHT, rail(0, 100, 5, 100, new AtomicInteger()).knobAt(LEFT, RIGHT));
        assertEquals((LEFT + RIGHT) / 2.0f, rail(0, 100, 5, 50, new AtomicInteger())
            .knobAt(LEFT, RIGHT));
        assertEquals(LEFT, rail(7, 7, 1, 7, new AtomicInteger()).knobAt(LEFT, RIGHT),
            "a range of one value has nowhere to travel");
    }

    @Test
    void keysStepAndAnswerEvenAtTheEnd() {
        AtomicInteger seen = new AtomicInteger(-1);
        Slider slider = rail(0, 100, 5, 40, seen);

        assertTrue(slider.key(Chord.of(InputConstants.KEY_RIGHT)));
        assertEquals(45, slider.value());
        assertEquals(45, seen.get());
        assertTrue(slider.key(Chord.of(InputConstants.KEY_DOWN)));
        assertEquals(40, slider.value());
        assertTrue(slider.key(Chord.of(InputConstants.KEY_HOME)));
        assertEquals(0, slider.value());
        assertTrue(slider.key(Chord.of(InputConstants.KEY_END)));
        assertEquals(100, slider.value());

        seen.set(-1);
        assertTrue(slider.key(Chord.of(InputConstants.KEY_END)), "the key was still handled");
        assertEquals(-1, seen.get(), "and nothing changed, so nobody was told");
    }

    /** Layout moves the knob when a row resizes. That is not the player, so nobody hears about it. */
    @Test
    void settingTheValueDirectlyIsSilent() {
        AtomicInteger seen = new AtomicInteger(-1);
        Slider slider = rail(0, 100, 5, 40, seen);
        slider.value(72);
        assertEquals(70, slider.value());
        assertEquals(-1, seen.get());
    }

    @Test
    void aPressGrabsTheRailAndTheDragFollows() {
        AtomicInteger seen = new AtomicInteger();
        Slider slider = rail(0, 100, 5, 0, seen);
        Surface surface = new Surface();
        surface.add(slider);

        assertTrue(surface.press((LEFT + RIGHT) / 2.0, 60));
        assertSame(slider, surface.captured());
        assertEquals(50, slider.value());

        surface.pointer(RIGHT + 400, 60);
        assertEquals(100, slider.value(), "a drag off the end stays at the end");
        surface.pointer(LEFT - 400, 60);
        assertEquals(0, slider.value());
        assertTrue(surface.release(LEFT, 60));
    }
}
