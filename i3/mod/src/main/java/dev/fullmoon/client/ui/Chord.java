package dev.fullmoon.client.ui;

import net.minecraft.client.input.KeyEvent;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A key press as a control sees it: the GLFW code, the modifiers that change what the code means,
 * and the editing intent the game has already worked out for the platform it is running on.
 *
 * <p>The intent is read off the event rather than derived from {@link #control}, because the game
 * reads Command for copy on a mac. A field that checked a control bit of its own would be wrong
 * there and correct everywhere else, which is the worst shape a bug can have.
 *
 * <p>{@link #from} is the only thing here that touches the game, and no control calls it — a
 * screen does, once, on the way into its surface.
 */
public record Chord(int code, boolean shift, boolean control, Edit edit) {
    /** What a modifier combination means to a control that holds text. */
    public enum Edit { NONE, SELECT_ALL, COPY, CUT, PASTE }

    /** A bare key. */
    public static Chord of(int code) {
        return new Chord(code, false, false, Edit.NONE);
    }

    /** A key with Shift held: extends a selection, and walks the Tab order backwards. */
    public static Chord shifted(int code) {
        return new Chord(code, true, false, Edit.NONE);
    }

    /** A key with the platform's word modifier held. */
    public static Chord controlled(int code) {
        return new Chord(code, false, true, Edit.NONE);
    }

    public static Chord editing(Edit edit) {
        return new Chord(InputConstants.UNKNOWN.getValue(), false, true, edit);
    }

    public static Chord from(KeyEvent event) {
        return new Chord(event.key(), event.hasShiftDown(), event.hasControlDownWithQuirk(),
            intent(event));
    }

    /** Enter, numpad Enter and Space: the keys that fire whatever holds the keyboard. */
    public boolean activates() {
        return code == InputConstants.KEY_RETURN
            || code == InputConstants.KEY_NUMPADENTER
            || code == InputConstants.KEY_SPACE;
    }

    public boolean is(int other) {
        return code == other;
    }

    private static Edit intent(KeyEvent event) {
        if (event.isSelectAll()) {
            return Edit.SELECT_ALL;
        }
        if (event.isCopy()) {
            return Edit.COPY;
        }
        if (event.isCut()) {
            return Edit.CUT;
        }
        return event.isPaste() ? Edit.PASTE : Edit.NONE;
    }
}
