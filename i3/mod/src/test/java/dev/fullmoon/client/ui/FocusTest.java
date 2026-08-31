package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FocusTest {
    /** A stand-in for a widget: the ring only ever asks a target whether it will take focus. */
    private static final class Stop implements Focus.Target {
        private final String name;
        private boolean takes = true;

        private Stop(String name) {
            this.name = name;
        }

        @Override
        public boolean takesFocus() {
            return takes;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Stop first = new Stop("first");
    private final Stop second = new Stop("second");
    private final Stop third = new Stop("third");
    private final Focus focus = ring(first, second, third);

    private static Focus ring(Stop... stops) {
        Focus built = new Focus();
        for (Stop stop : stops) {
            built.add(stop);
        }
        return built;
    }

    @Test
    void anUntouchedRingHoldsNothing() {
        assertNull(focus.held());
        assertFalse(focus.visible());
    }

    @Test
    void forwardFromNowhereLandsOnTheFirstStop() {
        assertTrue(focus.advance(1));
        assertSame(first, focus.held());
        assertTrue(focus.visible());
    }

    @Test
    void backwardFromNowhereLandsOnTheLastStop() {
        assertTrue(focus.advance(-1));
        assertSame(third, focus.held());
    }

    @Test
    void traversalFollowsRegistrationOrderAndWrapsBothWays() {
        focus.advance(1);
        focus.advance(1);
        assertSame(second, focus.held());
        focus.advance(1);
        assertSame(third, focus.held());
        focus.advance(1);
        assertSame(first, focus.held(), "the ring wraps forward");
        focus.advance(-1);
        assertSame(third, focus.held(), "the ring wraps backward");
    }

    @Test
    void traversalStepsOverWhatWillNotTakeIt() {
        second.takes = false;
        focus.advance(1);
        focus.advance(1);
        assertSame(third, focus.held());
        focus.advance(-1);
        assertSame(first, focus.held());
    }

    @Test
    void aRingWithNothingLeftToFocusReleasesTheKeyboard() {
        focus.advance(1);
        first.takes = false;
        second.takes = false;
        third.takes = false;

        assertFalse(focus.advance(1));
        assertNull(focus.held());
        assertFalse(focus.visible());
    }

    @Test
    void oneLiveStopKeepsTheKeyboardOnEveryStep() {
        second.takes = false;
        third.takes = false;
        focus.advance(1);
        assertTrue(focus.advance(1));
        assertSame(first, focus.held());
    }

    @Test
    void pointingTakesFocusWithoutLightingTheRing() {
        assertTrue(focus.point(second));
        assertSame(second, focus.held());
        assertFalse(focus.visible());
        assertFalse(focus.rings(second));
    }

    @Test
    void theNextStepCarriesOnFromWhereThePointerLeftOff() {
        focus.point(second);
        focus.advance(1);
        assertSame(third, focus.held());
        assertTrue(focus.rings(third));
    }

    @Test
    void tabbingLeavesTheRingOnOneStopOnly() {
        focus.advance(1);
        assertTrue(focus.rings(first));
        assertFalse(focus.rings(second));
        assertFalse(focus.rings(third));
    }

    @Test
    void pointingAtADeadStopLeavesTheKeyboardWhereItWas() {
        focus.point(first);
        second.takes = false;

        assertFalse(focus.point(second));
        assertSame(first, focus.held());
    }

    @Test
    void pointingAtAStrangerChangesNothing() {
        focus.point(first);

        assertFalse(focus.point(new Stop("elsewhere")));
        assertSame(first, focus.held());
    }

    @Test
    void aStopThatGoesDeadDropsTheKeyboardAndGetsItBack() {
        focus.advance(1);
        first.takes = false;
        assertNull(focus.held());
        assertFalse(focus.rings(first));

        first.takes = true;
        assertSame(first, focus.held(), "the position outlives a control's disabled spell");
        assertTrue(focus.rings(first));
    }

    @Test
    void clearingReleasesTheKeyboardButKeepsTheOrder() {
        focus.advance(1);
        focus.clear();
        assertNull(focus.held());
        assertFalse(focus.visible());

        focus.advance(1);
        assertSame(first, focus.held());
    }

    @Test
    void anEmptyRingHasNowhereToGo() {
        Focus empty = new Focus();
        assertFalse(empty.advance(1));
        assertFalse(empty.advance(-1));
        assertNull(empty.held());
    }

    @Test
    void aStepOfZeroIsNotAStep() {
        focus.point(second);
        assertFalse(focus.advance(0));
        assertSame(second, focus.held());
        assertFalse(focus.visible());
    }

    @Test
    void aStepOfTwoSkipsAStopAndStillWraps() {
        focus.advance(2);
        assertSame(second, focus.held());
        focus.advance(2);
        assertSame(first, focus.held());
    }
}
