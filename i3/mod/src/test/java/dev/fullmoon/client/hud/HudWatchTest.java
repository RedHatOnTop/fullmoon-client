package dev.fullmoon.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudWatchTest {
    @Test
    void theFirstFrameStatsAndTheNextOnesInThatWindowDoNot() {
        HudWatch watch = new HudWatch();

        assertTrue(watch.due(1_000L));
        assertFalse(watch.due(1_001L));
        assertFalse(watch.due(1_000L + HudWatch.POLL_MS - 1L));
        assertTrue(watch.due(1_000L + HudWatch.POLL_MS));
    }

    @Test
    void aWindowIsMeasuredFromTheStatNotFromTheFrameThatWantedOne() {
        HudWatch watch = new HudWatch();

        assertTrue(watch.due(0L));
        assertTrue(watch.due(10_000L));
        assertFalse(watch.due(10_000L + HudWatch.POLL_MS - 1L));
    }

    @Test
    void aNewTimestampIsAdoptedOnceAndThenIsTheOneWeHave() {
        HudWatch watch = new HudWatch();

        assertTrue(watch.changed(1_700L));
        assertFalse(watch.changed(1_700L));
        assertTrue(watch.changed(1_800L));
    }

    @Test
    void ourOwnWriteIsNotAnEditToAdopt() {
        HudWatch watch = new HudWatch();

        watch.authored(2_400L);

        assertFalse(watch.changed(2_400L));
        assertTrue(watch.changed(2_401L));
    }

    @Test
    void aMissingFileIsNothingToAdoptAndTheFileComingBackIsReadAgain() {
        HudWatch watch = new HudWatch();
        assertTrue(watch.changed(3_000L));

        assertFalse(watch.changed(0L));
        assertFalse(watch.changed(-1L));

        assertTrue(watch.changed(3_000L));
    }
}
