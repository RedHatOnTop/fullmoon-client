package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.fullmoon.client.design.Tokens;
import org.junit.jupiter.api.Test;

/**
 * What a row draws as. A row is the one control here whose selection outlives all eight states,
 * so the thing worth checking is not any single look but that the sixteen a row can take are
 * sixteen and not twelve — the 목록 page draws every one of them side by side, and two cells that
 * come out identical make the sweep a claim rather than a proof.
 *
 * <p>{@link ListRow.Look} is the whole of that decision and carries no text, so the enumeration
 * runs without a font: the row's name is the only part of it that needs measuring.
 */
class ListRowTest {
    private static ListRow row(boolean selected) {
        ListRow row = new ListRow("accent", "1F1B2E", () -> {});
        row.selected(selected);
        return row;
    }

    /** Chosen has to read as chosen in the state the mouse is in, the keyboard is in, and off. */
    @Test
    void nothingLooksTheSameChosenAsUnchosen() {
        ListRow plain = row(false);
        ListRow chosen = row(true);
        for (State state : State.values()) {
            assertNotEquals(plain.look(state), chosen.look(state), state.name());
        }
    }

    /**
     * A row has no border, so the focus that draws no ring — the keyboard arriving by mouse click
     * — has nothing of its own left to change. Every other pair differs, and that includes the two
     * the voice hands the same ground to: hover and the keyboard's own focus part on the tick.
     */
    @Test
    void restAndTheRinglessFocusAreTheOnlyPairThatCoincide() {
        assertEquals(List.of("REST=FOCUS"), collisions(row(false)), "unchosen");
        assertEquals(List.of("REST=FOCUS"), collisions(row(true)), "chosen");
    }

    /** A tick on a row that answers nothing would be claiming that it does. */
    @Test
    void aRowThatCannotAnswerWearsNoAccent() {
        for (State state : List.of(State.DISABLED, State.LOADING)) {
            ListRow.Look look = row(true).look(state);
            assertNotEquals(Tokens.Color.ACCENT, look.tick(), state.name());
            assertTrue(look.tickWidth() > 0, "a list that is off still knows which row was chosen");
        }
    }

    /** Chosen is the wide tick because it is the only one of the three that outlives the state. */
    @Test
    void theChosenTickIsWiderThanTheKeyboardsOwn() {
        assertEquals(Tokens.Stroke.FOCUS, row(true).look(State.REST).tickWidth());
        assertEquals(Tokens.Stroke.HAIR, row(false).look(State.FOCUS_VISIBLE).tickWidth());
        assertEquals(0, row(false).look(State.REST).tickWidth(), "a row at rest is not a mark");
    }

    /** The row's own callback, which the panel fires on top of choosing it. */
    @Test
    void actingPicksOnce() {
        AtomicInteger picks = new AtomicInteger();
        new ListRow("accent", "", picks::incrementAndGet).act();
        assertEquals(1, picks.get());
    }

    @Test
    void aLiveMetaReadingFollowsItsSourceWithoutReplacingTheRow() {
        AtomicReference<String> value = new AtomicReference<>("Off");
        ListRow row = new ListRow("Subtitles", value::get, () -> {});

        assertEquals("Off", row.meta());
        value.set("On");
        assertEquals("On", row.meta());
    }

    /** Every pair of states this row cannot tell apart, named, so a failure says which. */
    private static List<String> collisions(ListRow row) {
        List<String> same = new ArrayList<>();
        State[] states = State.values();
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                if (row.look(states[i]).equals(row.look(states[j]))) {
                    same.add(states[i] + "=" + states[j]);
                }
            }
        }
        return same;
    }
}
