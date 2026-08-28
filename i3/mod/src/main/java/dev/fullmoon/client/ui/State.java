package dev.fullmoon.client.ui;

/**
 * The eight states every interactive thing in this client answers for.
 *
 * <p>{@link #FOCUS} and {@link #FOCUS_VISIBLE} are two states because they arrive by different
 * routes: a click leaves a field holding focus with no ring on it, while Tab has to light one,
 * and a client that collapses the two either rings everything the mouse touches or strands the
 * keyboard with no idea where it is.
 */
public enum State {
    REST,
    HOVER,
    ACTIVE,
    FOCUS,
    FOCUS_VISIBLE,
    DISABLED,
    LOADING,
    ERROR;

    /** What a widget knows about itself before it knows which state that makes it. */
    public record Signals(
        boolean enabled,
        boolean hovered,
        boolean pressed,
        boolean focused,
        boolean keyboard,
        boolean busy,
        boolean invalid) {

        public static final Signals REST = new Signals(true, false, false, false, false, false, false);

        public Signals enabled(boolean value) {
            return new Signals(value, hovered, pressed, focused, keyboard, busy, invalid);
        }

        public Signals hovered(boolean value) {
            return new Signals(enabled, value, pressed, focused, keyboard, busy, invalid);
        }

        public Signals pressed(boolean value) {
            return new Signals(enabled, hovered, value, focused, keyboard, busy, invalid);
        }

        public Signals focused(boolean value, boolean byKeyboard) {
            return new Signals(enabled, hovered, pressed, value, value && byKeyboard, busy, invalid);
        }

        public Signals busy(boolean value) {
            return new Signals(enabled, hovered, pressed, focused, keyboard, value, invalid);
        }

        public Signals invalid(boolean value) {
            return new Signals(enabled, hovered, pressed, focused, keyboard, busy, value);
        }
    }

    /**
     * One precedence order, shared by every widget, so no two widgets in a surface can disagree
     * about what they are. Ground and ink come from the state this returns; the focus ring is
     * drawn from {@link Signals#keyboard} instead, because a hovered control that also holds
     * focus has to show both and one enum cannot say two things.
     */
    public static State of(Signals s) {
        if (!s.enabled()) {
            return DISABLED;
        }
        if (s.busy()) {
            return LOADING;
        }
        if (s.invalid()) {
            return ERROR;
        }
        if (s.pressed()) {
            return ACTIVE;
        }
        if (s.hovered()) {
            return HOVER;
        }
        if (s.keyboard()) {
            return FOCUS_VISIBLE;
        }
        return s.focused() ? FOCUS : REST;
    }

    /** Whether a state still answers a click. The gallery draws all eight; only these act. */
    public boolean live() {
        return this != DISABLED && this != LOADING;
    }
}
