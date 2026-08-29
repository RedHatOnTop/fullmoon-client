package dev.fullmoon.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The caret model, driven with no font anywhere near it.
 *
 * <p>Every index here is an offset into the string, which is the whole point of keeping measurement
 * out of the model: a test can say where the caret went. The two measured parts — where a click
 * lands and how far the view has scrolled — belong to drawing and are not exercised here.
 *
 * <p>The moon is U+1F319, a code point outside the basic plane and two chars wide. It is in these
 * tests because a name field is where a player will actually put one.
 */
class TextFieldTest {
    private static final String MOON = "🌙";

    private final List<String> committed = new ArrayList<>();
    private final Clipboard clipboard = Clipboard.scratch();

    private TextField field(String text) {
        return field(text, 16);
    }

    private TextField field(String text, int limit) {
        return new TextField("", "이름", text, limit, s -> !s.isBlank(), committed::add, clipboard);
    }

    @Test
    void typingLandsAtTheCaret() {
        TextField field = field("가나");
        assertTrue(field.type('다'));
        assertEquals("가나다", field.text());
        assertTrue(field.key(Chord.of(InputConstants.KEY_HOME)));
        assertTrue(field.type('라'));
        assertEquals("라가나다", field.text());
        assertEquals(1, field.caret());
    }

    /** A control character is not text. It arrives here because the game sends every codepoint. */
    @Test
    void controlCharactersAreNotTyped() {
        TextField field = field("가");
        assertFalse(field.type('\n'));
        assertEquals("가", field.text());
    }

    @Test
    void theCaretStepsOverAPairInOneMove() {
        TextField field = field("a" + MOON);
        assertEquals(3, field.caret());
        field.key(Chord.of(InputConstants.KEY_LEFT));
        assertEquals(1, field.caret(), "one arrow, one code point");
        field.key(Chord.of(InputConstants.KEY_LEFT));
        assertEquals(0, field.caret());
        field.key(Chord.of(InputConstants.KEY_LEFT));
        assertEquals(0, field.caret(), "and it stops there");
        field.key(Chord.of(InputConstants.KEY_RIGHT));
        assertEquals(1, field.caret());
    }

    @Test
    void backspaceTakesTheWholePair() {
        TextField field = field("a" + MOON);
        assertTrue(field.key(Chord.of(InputConstants.KEY_BACKSPACE)));
        assertEquals("a", field.text());
        assertTrue(field.key(Chord.of(InputConstants.KEY_BACKSPACE)));
        assertEquals("", field.text());
        assertTrue(field.key(Chord.of(InputConstants.KEY_BACKSPACE)), "the key was still handled");
    }

    @Test
    void deleteWorksForwardAndStopsAtTheEnd() {
        TextField field = field(MOON + "a");
        field.key(Chord.of(InputConstants.KEY_HOME));
        assertTrue(field.key(Chord.of(InputConstants.KEY_DELETE)));
        assertEquals("a", field.text());
        field.key(Chord.of(InputConstants.KEY_END));
        assertTrue(field.key(Chord.of(InputConstants.KEY_DELETE)));
        assertEquals("a", field.text());
    }

    @Test
    void shiftExtendsAndAnUnshiftedArrowCollapses() {
        TextField field = field("abcd");
        field.key(Chord.shifted(InputConstants.KEY_LEFT));
        field.key(Chord.shifted(InputConstants.KEY_LEFT));
        assertEquals("cd", field.selected());
        field.key(Chord.of(InputConstants.KEY_LEFT));
        assertEquals("", field.selected());
        assertEquals(1, field.caret());
    }

    @Test
    void aSelectionIsWhatBackspaceTakes() {
        TextField field = field("abcd");
        field.key(Chord.shifted(InputConstants.KEY_HOME));
        assertEquals("abcd", field.selected());
        field.key(Chord.of(InputConstants.KEY_BACKSPACE));
        assertEquals("", field.text());
    }

    @Test
    void controlJumpsAWordAtATime() {
        TextField field = field("달빛 서버 이름");
        field.key(Chord.controlled(InputConstants.KEY_LEFT));
        assertEquals(6, field.caret());
        field.key(Chord.controlled(InputConstants.KEY_LEFT));
        assertEquals(3, field.caret(), "the space goes with the word behind it");
        field.key(Chord.controlled(InputConstants.KEY_RIGHT));
        assertEquals(5, field.caret());
        field.key(Chord.controlled(InputConstants.KEY_LEFT));
        field.key(Chord.controlled(InputConstants.KEY_LEFT));
        assertEquals(0, field.caret());
    }

    @Test
    void theLimitCountsCodePointsAndCutsOnABoundary() {
        TextField field = field("", 3);
        clipboard.put(MOON.repeat(5));
        assertTrue(field.key(Chord.editing(Chord.Edit.PASTE)));
        assertEquals(MOON.repeat(3), field.text());
        assertTrue(field.type('a'), "the key belongs to the field either way");
        assertEquals(MOON.repeat(3), field.text(), "and a full field has nowhere to put it");
    }

    /** A paste brings whatever the system had in it, including the newlines a line cannot hold. */
    @Test
    void aPasteArrivesOnOneLine() {
        TextField field = field("");
        clipboard.put("달빛\n서버\t");
        field.key(Chord.editing(Chord.Edit.PASTE));
        assertEquals("달빛서버", field.text());
    }

    @Test
    void selectAllThenTypingReplacesEverything() {
        TextField field = field("달빛 서버");
        assertTrue(field.key(Chord.editing(Chord.Edit.SELECT_ALL)));
        assertEquals("달빛 서버", field.selected());
        field.type('가');
        assertEquals("가", field.text());
    }

    @Test
    void copyLeavesTheTextAndCutTakesIt() {
        TextField field = field("달빛");
        field.key(Chord.editing(Chord.Edit.SELECT_ALL));
        field.key(Chord.editing(Chord.Edit.COPY));
        assertEquals("달빛", clipboard.get());
        assertEquals("달빛", field.text());

        field.key(Chord.editing(Chord.Edit.CUT));
        assertEquals("달빛", clipboard.get());
        assertEquals("", field.text());
    }

    @Test
    void anEmptySelectionIsNothingToCopy() {
        TextField field = field("달빛");
        clipboard.put("held");
        field.key(Chord.editing(Chord.Edit.COPY));
        assertEquals("held", clipboard.get(), "an empty copy does not empty the clipboard");
        field.key(Chord.editing(Chord.Edit.CUT));
        assertEquals("달빛", field.text());
    }

    /** Space reaches the surface as an activation. A field being typed into has to claim it. */
    @Test
    void spaceIsClaimedSoTheSurfaceCannotSubmitOnIt() {
        TextField field = field("달빛");
        assertTrue(field.key(Chord.of(InputConstants.KEY_SPACE)));
        assertEquals("달빛", field.text(), "the character itself arrives through type()");
        assertTrue(field.type(' '));
        assertEquals("달빛 ", field.text());
    }

    @Test
    void anEditCommitsOnceAndOnlyWhenItChangedSomething() {
        TextField field = field("달빛");
        field.act();
        assertEquals(List.of(), committed, "Enter on an untouched field is not an edit");

        field.type('님');
        field.act();
        assertEquals(List.of("달빛님"), committed);
        field.act();
        field.blurred();
        assertEquals(List.of("달빛님"), committed, "one edit, one commit");

        field.key(Chord.of(InputConstants.KEY_BACKSPACE));
        field.blurred();
        assertEquals(List.of("달빛님", "달빛"), committed, "walking away from an edit still commits");
    }

    /** A click puts the caret somewhere. It is not a submission, and the way up is not either. */
    @Test
    void aReleaseInsideDoesNotCommit() {
        TextField field = field("달빛");
        field.type('님');
        field.release(10, 10, true);
        assertEquals(List.of(), committed);
    }

    @Test
    void theRuleDecidesWhetherTheFieldIsWrong() {
        Focus focus = new Focus();
        TextField field = field("달빛");
        assertEquals(State.REST, field.state(focus));

        field.key(Chord.editing(Chord.Edit.SELECT_ALL));
        field.key(Chord.of(InputConstants.KEY_BACKSPACE));
        assertEquals(State.ERROR, field.state(focus), "blank breaks the rule this field was given");

        field.type('가');
        assertEquals(State.REST, field.state(focus));
    }
}
