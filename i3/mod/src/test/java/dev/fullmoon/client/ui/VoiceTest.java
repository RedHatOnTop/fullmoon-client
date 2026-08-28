package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fullmoon.client.design.Tokens;
import org.junit.jupiter.api.Test;

class VoiceTest {
    @Test
    void inkNeverLandsOnItsOwnGround() {
        for (Voice voice : Voice.values()) {
            for (State state : State.values()) {
                Chrome chrome = voice.chrome(state);
                assertNotEquals(chrome.fill(), chrome.ink(), voice + " " + state);
            }
        }
    }

    @Test
    void everyStateIsOpaque() {
        for (Voice voice : Voice.values()) {
            for (State state : State.values()) {
                Chrome chrome = voice.chrome(state);
                assertEquals(0xFF, chrome.fill() >>> 24, voice + " " + state + " fill alpha");
                assertEquals(0xFF, chrome.ink() >>> 24, voice + " " + state + " ink alpha");
                assertEquals(0xFF, chrome.line() >>> 24, voice + " " + state + " line alpha");
            }
        }
    }

    @Test
    void hoverIsVisibleWithoutTheMouseMoving() {
        assertNotEquals(Voice.QUIET.chrome(State.REST).fill(), Voice.QUIET.chrome(State.HOVER).fill());
        assertNotEquals(Voice.QUIET.chrome(State.REST).line(), Voice.QUIET.chrome(State.HOVER).line());
        assertNotEquals(Voice.LOUD.chrome(State.REST).line(), Voice.LOUD.chrome(State.HOVER).line(),
            "a loud control cannot repaint its fill on hover, so its edge has to carry it");
    }

    @Test
    void aPressDarkensRatherThanLifts() {
        assertTrue(luminance(Voice.LOUD.chrome(State.ACTIVE).fill())
            < luminance(Voice.LOUD.chrome(State.REST).fill()));
        assertTrue(luminance(Voice.QUIET.chrome(State.ACTIVE).fill())
                > luminance(Voice.QUIET.chrome(State.REST).fill()),
            "a quiet control has no fill to darken; it takes the accent wash instead");
    }

    @Test
    void focusVisibleLeavesTheGroundAloneBecauseTheRingCarriesIt() {
        for (Voice voice : Voice.values()) {
            assertEquals(voice.chrome(State.REST), voice.chrome(State.FOCUS_VISIBLE), voice.name());
        }
    }

    @Test
    void aRingIsNeverTheFillItSurrounds() {
        for (Voice voice : Voice.values()) {
            assertNotEquals(voice.ring(), voice.chrome(State.FOCUS_VISIBLE).fill(), voice.name());
        }
    }

    @Test
    void bothVoicesGoGreyTogether() {
        for (Voice voice : Voice.values()) {
            assertEquals(Tokens.Color.INK_DISABLED, voice.chrome(State.DISABLED).ink());
            assertEquals(Tokens.Color.SURFACE_SUNKEN, voice.chrome(State.DISABLED).fill());
        }
    }

    @Test
    void bothVoicesRaiseTheSameAlarm() {
        for (Voice voice : Voice.values()) {
            assertEquals(Tokens.Color.STATUS_DANGER, voice.chrome(State.ERROR).line());
        }
    }

    @Test
    void loadingDropsTheInkWithoutGoingAsQuietAsDisabled() {
        for (Voice voice : Voice.values()) {
            assertNotEquals(voice.chrome(State.REST).ink(), voice.chrome(State.LOADING).ink());
            assertNotEquals(Tokens.Color.INK_DISABLED, voice.chrome(State.LOADING).ink());
        }
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
