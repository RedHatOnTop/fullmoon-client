package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fullmoon.client.design.Tokens;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ChromeTest {
    @Test
    void inkNeverLandsOnItsOwnGround() {
        for (State state : State.values()) {
            assertNotEquals(Chrome.quiet(state).fill(), Chrome.quiet(state).ink(), "quiet " + state);
            assertNotEquals(Chrome.loud(state).fill(), Chrome.loud(state).ink(), "loud " + state);
        }
    }

    @Test
    void everyStateIsOpaque() {
        for (State state : State.values()) {
            for (Function<State, Chrome> voice : voices()) {
                Chrome chrome = voice.apply(state);
                assertEquals(0xFF, chrome.fill() >>> 24, "fill alpha, " + state);
                assertEquals(0xFF, chrome.ink() >>> 24, "ink alpha, " + state);
                assertEquals(0xFF, chrome.line() >>> 24, "line alpha, " + state);
            }
        }
    }

    @Test
    void hoverIsVisibleWithoutTheMouseMoving() {
        assertNotEquals(Chrome.quiet(State.REST).fill(), Chrome.quiet(State.HOVER).fill());
        assertNotEquals(Chrome.quiet(State.REST).line(), Chrome.quiet(State.HOVER).line());
        assertNotEquals(Chrome.loud(State.REST).line(), Chrome.loud(State.HOVER).line(),
            "a loud control cannot repaint its fill on hover, so its edge has to carry it");
    }

    @Test
    void aPressDarkensRatherThanLifts() {
        assertTrue(luminance(Chrome.loud(State.ACTIVE).fill()) < luminance(Chrome.loud(State.REST).fill()));
        assertTrue(luminance(Chrome.quiet(State.ACTIVE).fill()) > luminance(Chrome.quiet(State.REST).fill()),
            "a quiet control has no fill to darken; it takes the accent wash instead");
    }

    @Test
    void focusVisibleLeavesTheGroundAloneBecauseTheRingCarriesIt() {
        assertEquals(Chrome.quiet(State.REST), Chrome.quiet(State.FOCUS_VISIBLE));
        assertEquals(Chrome.loud(State.REST), Chrome.loud(State.FOCUS_VISIBLE));
    }

    @Test
    void bothVoicesGoGreyTogether() {
        for (Function<State, Chrome> voice : voices()) {
            assertEquals(Tokens.Color.INK_DISABLED, voice.apply(State.DISABLED).ink());
            assertEquals(Tokens.Color.SURFACE_SUNKEN, voice.apply(State.DISABLED).fill());
        }
    }

    @Test
    void bothVoicesRaiseTheSameAlarm() {
        for (Function<State, Chrome> voice : voices()) {
            assertEquals(Tokens.Color.STATUS_DANGER, voice.apply(State.ERROR).line());
        }
    }

    @Test
    void loadingDropsTheInkWithoutGoingAsQuietAsDisabled() {
        for (Function<State, Chrome> voice : voices()) {
            assertNotEquals(voice.apply(State.REST).ink(), voice.apply(State.LOADING).ink());
            assertNotEquals(Tokens.Color.INK_DISABLED, voice.apply(State.LOADING).ink());
        }
    }

    private static List<Function<State, Chrome>> voices() {
        return List.of(Chrome::quiet, Chrome::loud);
    }

    /** Relative luminance, so "darker" is a measurement and not an opinion about a hex code. */
    private static double luminance(int argb) {
        return 0.2126 * channel(argb >>> 16) + 0.7152 * channel(argb >>> 8) + 0.0722 * channel(argb);
    }

    private static double channel(int shifted) {
        double v = (shifted & 0xFF) / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
