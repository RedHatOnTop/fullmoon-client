package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The routing rules, driven the way a screen drives them. Nothing here draws — a probe that
 * needed a render target could not run on a build machine, and the rules worth pinning down are
 * about where an event goes, not what it looks like when it lands.
 */
class SurfaceTest {
    /** A control that writes down what it was told and does nothing else with it. */
    private static final class Probe extends Widget {
        private final boolean keepsSpace;
        private int acted;
        private int drags;
        private boolean landedInside;
        private int typed;
        private double scrolled;

        Probe(String label, int x, int y) {
            this(label, x, y, false);
        }

        Probe(String label, int x, int y, boolean keepsSpace) {
            super(Voice.QUIET, label);
            this.keepsSpace = keepsSpace;
            place(new Box(x, y, 40, 20));
        }

        @Override
        public void draw(Painter painter, State state) {
            throw new UnsupportedOperationException("a surface routes; it does not draw");
        }

        @Override
        protected void drag(double mx, double my) {
            drags++;
        }

        @Override
        protected void release(double mx, double my, boolean inside) {
            landedInside = inside;
            super.release(mx, my, inside);
        }

        @Override
        protected boolean key(Chord chord) {
            return keepsSpace && chord.is(InputConstants.KEY_SPACE);
        }

        @Override
        protected boolean type(int codepoint) {
            typed++;
            return true;
        }

        @Override
        protected boolean scroll(double amount) {
            scrolled += amount;
            return true;
        }

        @Override
        protected void act() {
            acted++;
        }
    }

    @Test
    void theTopmostWidgetTakesThePointer() {
        Surface surface = new Surface();
        Probe under = surface.add(new Probe("under", 0, 0));
        Probe over = surface.add(new Probe("over", 20, 0));

        assertSame(under, surface.at(5, 5));
        assertSame(over, surface.at(25, 5), "later arrivals sit on top");
        assertNull(surface.at(200, 200));
    }

    @Test
    void onlyOneWidgetIsEverHovered() {
        Surface surface = new Surface();
        Probe left = surface.add(new Probe("left", 0, 0));
        Probe right = surface.add(new Probe("right", 60, 0));

        surface.hover(10, 10);
        assertEquals(State.HOVER, surface.state(left));
        assertEquals(State.REST, surface.state(right));

        surface.hover(70, 10);
        assertEquals(State.REST, surface.state(left));
        assertEquals(State.HOVER, surface.state(right));

        surface.hover(50, 10);
        assertEquals(State.REST, surface.state(left));
        assertEquals(State.REST, surface.state(right));
    }

    @Test
    void aPressCapturesUntilItComesUp() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));

        assertTrue(surface.press(10, 10));
        assertSame(probe, surface.captured());
        assertEquals(State.ACTIVE, surface.state(probe));

        assertTrue(surface.release(10, 10));
        assertNull(surface.captured());
        assertTrue(probe.landedInside);
        assertEquals(1, probe.acted);
        assertEquals(State.HOVER, surface.state(probe), "the pointer is still on it afterwards");
    }

    @Test
    void draggingOffAHeldControlLetsGoOfIt() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));

        surface.press(10, 10);
        surface.pointer(200, 200);
        assertSame(probe, surface.captured(), "the capture outlives the pointer leaving");
        assertEquals(State.FOCUS, surface.state(probe),
            "the press is dropped; the keyboard the click put there is not");
        assertEquals(1, probe.drags);

        assertTrue(surface.release(200, 200));
        assertFalse(probe.landedInside);
        assertEquals(0, probe.acted, "a release off the control is the way out of a press");
    }

    @Test
    void draggingBackOnRearmsIt() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));

        surface.press(10, 10);
        surface.pointer(200, 200);
        surface.pointer(12, 12);
        assertEquals(State.ACTIVE, surface.state(probe));

        surface.release(12, 12);
        assertEquals(1, probe.acted);
    }

    @Test
    void aDeadControlSwallowsTheClickInsteadOfLeakingIt() {
        Surface surface = new Surface();
        Probe field = surface.add(new Probe("field", 0, 0));
        Probe off = surface.add(new Probe("off", 60, 0));
        off.enabled(false);
        surface.focus().point(field);

        assertTrue(surface.press(70, 10), "taken and dropped, not passed to whatever sits behind");
        assertNull(surface.captured());
        assertEquals(0, off.acted);
        assertSame(field, surface.held(), "a dead control does not steal the caret either");
    }

    @Test
    void aClickOnTheGroundGivesUpTheKeyboard() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));
        surface.press(10, 10);
        surface.release(10, 10);
        assertSame(probe, surface.held());

        assertFalse(surface.press(200, 200), "nothing took it, so the screen still gets the click");
        assertNull(surface.held());
        surface.state(probe);
        assertFalse(probe.holding(), "and the control it left knows it is gone");
    }

    @Test
    void aPointedControlTakesTypingWithoutSproutingARing() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("field", 0, 0));

        surface.press(10, 10);
        assertSame(probe, surface.held());
        surface.state(probe);
        assertFalse(probe.ringing(), "a click moves the keyboard and leaves the ring down");
        assertTrue(probe.holding(), "the keyboard is here even so, which is what a caret answers to");
        assertTrue(surface.type('a'));
        assertEquals(1, probe.typed);
    }

    @Test
    void tabRunsInRegistrationOrderAndWrapsBothWays() {
        Surface surface = new Surface();
        Probe first = surface.add(new Probe("first", 0, 0));
        Probe second = surface.add(new Probe("second", 0, 30));

        assertTrue(surface.key(Chord.of(InputConstants.KEY_TAB)));
        assertSame(first, surface.held());
        assertEquals(State.FOCUS_VISIBLE, surface.state(first), "Tab lights the ring");

        surface.key(Chord.of(InputConstants.KEY_TAB));
        assertSame(second, surface.held());
        surface.key(Chord.of(InputConstants.KEY_TAB));
        assertSame(first, surface.held(), "forward off the end wraps to the top");
        surface.key(Chord.shifted(InputConstants.KEY_TAB));
        assertSame(second, surface.held(), "and Shift-Tab off the top wraps back to the end");
    }

    @Test
    void tabSkipsWhatWillNotTakeTheKeyboard() {
        Surface surface = new Surface();
        Probe first = surface.add(new Probe("first", 0, 0));
        Probe off = surface.add(new Probe("off", 0, 30));
        Probe last = surface.add(new Probe("last", 0, 60));
        off.enabled(false);

        surface.key(Chord.of(InputConstants.KEY_TAB));
        assertSame(first, surface.held());
        surface.key(Chord.of(InputConstants.KEY_TAB));
        assertSame(last, surface.held());
    }

    @Test
    void theRingSurvivesThePointerArriving() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));
        surface.key(Chord.of(InputConstants.KEY_TAB));

        surface.hover(10, 10);
        assertEquals(State.HOVER, surface.state(probe), "the hover is the loudest thing about it");
        assertTrue(probe.ringing(), "and the keyboard has not gone anywhere");
    }

    @Test
    void enterAndSpaceActivateTheKeyboardHolder() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0));
        surface.key(Chord.of(InputConstants.KEY_TAB));

        assertTrue(surface.key(Chord.of(InputConstants.KEY_RETURN)));
        assertTrue(surface.key(Chord.of(InputConstants.KEY_NUMPADENTER)));
        assertTrue(surface.key(Chord.of(InputConstants.KEY_SPACE)));
        assertEquals(3, probe.acted);
    }

    @Test
    void theKeyboardHolderGetsFirstRefusal() {
        Surface surface = new Surface();
        Probe field = surface.add(new Probe("field", 0, 0, true));
        surface.key(Chord.of(InputConstants.KEY_TAB));

        assertTrue(surface.key(Chord.of(InputConstants.KEY_SPACE)));
        assertEquals(0, field.acted, "a control that keeps Space is not also fired by it");
    }

    @Test
    void anUnclaimedKeyIsLeftForTheScreen() {
        Surface surface = new Surface();
        surface.add(new Probe("apply", 0, 0));
        surface.key(Chord.of(InputConstants.KEY_TAB));

        assertFalse(surface.key(Chord.of(InputConstants.KEY_ESCAPE)), "Esc is the screen's to answer");
    }

    @Test
    void aControlInFlightKeepsTheRingAndAnswersNothing() {
        Surface surface = new Surface();
        Probe probe = surface.add(new Probe("apply", 0, 0, true));
        surface.key(Chord.of(InputConstants.KEY_TAB));
        probe.busy(true);

        assertEquals(State.LOADING, surface.state(probe));
        assertTrue(probe.ringing(), "the ring stays or the player loses their place on the surface");
        assertSame(probe, surface.held());

        assertFalse(surface.key(Chord.of(InputConstants.KEY_RETURN)));
        assertFalse(surface.key(Chord.of(InputConstants.KEY_SPACE)), "not even the key it claims");
        assertEquals(0, probe.acted);
        assertFalse(surface.type('a'));
        assertEquals(0, probe.typed);

        assertTrue(surface.press(10, 10));
        assertNull(surface.captured());
        assertEquals(0, probe.acted);
    }

    @Test
    void tabStillLeavesAControlInFlight() {
        Surface surface = new Surface();
        Probe busy = surface.add(new Probe("apply", 0, 0));
        Probe next = surface.add(new Probe("cancel", 0, 30));
        surface.key(Chord.of(InputConstants.KEY_TAB));
        busy.busy(true);

        assertTrue(surface.key(Chord.of(InputConstants.KEY_TAB)));
        assertSame(next, surface.held(), "a request in flight cannot trap the keyboard");
    }

    @Test
    void aScrollFollowsThePointerAndNotTheCapture() {
        Surface surface = new Surface();
        Probe held = surface.add(new Probe("held", 0, 0));
        Probe elsewhere = surface.add(new Probe("elsewhere", 60, 0));

        surface.press(10, 10);
        assertTrue(surface.scroll(70, 10, 3));
        assertEquals(3.0, elsewhere.scrolled);
        assertEquals(0.0, held.scrolled);
        assertFalse(surface.scroll(200, 200, 3), "empty ground scrolls nothing");
    }

    @Test
    void aDeadControlDoesNotScroll() {
        Surface surface = new Surface();
        Probe off = surface.add(new Probe("off", 0, 0));
        off.enabled(false);

        assertFalse(surface.scroll(10, 10, 3));
        assertEquals(0.0, off.scrolled);
    }
}
