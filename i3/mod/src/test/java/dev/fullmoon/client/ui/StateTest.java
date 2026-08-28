package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fullmoon.client.ui.State.Signals;
import org.junit.jupiter.api.Test;

class StateTest {
    @Test
    void nothingHappeningIsRest() {
        assertEquals(State.REST, State.of(Signals.REST));
    }

    @Test
    void everySignalIsOutrankedByTheOneAboveIt() {
        Signals loud = Signals.REST
            .hovered(true)
            .pressed(true)
            .focused(true, true)
            .invalid(true)
            .busy(true)
            .enabled(false);

        assertEquals(State.DISABLED, State.of(loud));
        assertEquals(State.LOADING, State.of(loud.enabled(true)));
        assertEquals(State.ERROR, State.of(loud.enabled(true).busy(false)));
        assertEquals(State.ACTIVE, State.of(loud.enabled(true).busy(false).invalid(false)));
        assertEquals(State.HOVER,
            State.of(loud.enabled(true).busy(false).invalid(false).pressed(false)));
        assertEquals(State.FOCUS_VISIBLE,
            State.of(loud.enabled(true).busy(false).invalid(false).pressed(false).hovered(false)));
    }

    @Test
    void theRouteToFocusDecidesWhetherItIsVisible() {
        assertEquals(State.FOCUS, State.of(Signals.REST.focused(true, false)));
        assertEquals(State.FOCUS_VISIBLE, State.of(Signals.REST.focused(true, true)));
    }

    @Test
    void unfocusingDropsTheKeyboardFlagWithIt() {
        Signals typed = Signals.REST.focused(true, true);
        assertTrue(typed.keyboard());

        Signals left = typed.focused(false, true);
        assertFalse(left.keyboard(), "focus-visible cannot outlive the focus it belongs to");
        assertEquals(State.REST, State.of(left));
    }

    @Test
    void onlyTheStatesThatCanActAreLive() {
        for (State state : State.values()) {
            assertEquals(state != State.DISABLED && state != State.LOADING, state.live(),
                state.name());
        }
    }
}
