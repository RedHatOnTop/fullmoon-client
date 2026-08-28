package dev.fullmoon.client.ui;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;

/**
 * A control on a surface: a box, the signals it knows about itself, and what it does when the
 * pointer or the keyboard reaches it.
 *
 * <p>A widget holds no focus bit of its own. {@link Surface} passes the ring in when it asks for
 * a state, because a widget that remembered whether it was focused would drift out of step with
 * the one object that actually moves the keyboard.
 *
 * <p>{@link #draw} is handed the state rather than working it out, so the same drawing code that
 * runs in a live surface can be asked for all eight states side by side.
 */
public abstract class Widget implements Focus.Target {
    /** Clear of the control's own edge by a hairline, so the two do not read as one thick line. */
    private static final int RING_GAP = Tokens.Space.TIGHT + Tokens.Stroke.HAIR;

    private final Voice voice;
    private final String label;
    private Box bounds = Box.EMPTY;
    private State.Signals own = State.Signals.REST;
    private boolean ringing;

    protected Widget(Voice voice, String label) {
        this.voice = voice;
        this.label = label;
    }

    public final Voice voice() {
        return voice;
    }

    /**
     * What this control is called. Where the words go is the widget's business — a switch sets
     * them beside its track, a button inside itself — but the name lives up here because a
     * surface that has to say which control holds the keyboard cannot ask a subclass.
     */
    public final String label() {
        return label;
    }

    public final Box bounds() {
        return bounds;
    }

    /** Layout hands a widget its box. A widget never picks its own. */
    public final void place(Box box) {
        bounds = box;
    }

    public final void enabled(boolean value) {
        own = own.enabled(value);
    }

    /** A request is in flight: the control still holds the keyboard but answers nothing. */
    public final void busy(boolean value) {
        own = own.busy(value);
    }

    public final void invalid(boolean value) {
        own = own.invalid(value);
    }

    /** The state this widget is in, given who is holding the keyboard. */
    public final State state(Focus focus) {
        return State.of(own.focused(focus.holds(this), focus.visible()));
    }

    @Override
    public boolean takesFocus() {
        // Busy does not give up the ring: taking the keyboard away from the player because a
        // request is in flight loses their place on the surface for no reason they can see.
        return own.enabled();
    }

    public abstract void draw(Painter painter, State state);

    /** The ring, outside the control's bounds so it never covers the control's own edge. */
    protected final void ring(Painter painter, State state, float radius) {
        ring(painter, state, radius, voice.ring());
    }

    protected final void ring(Painter painter, State state, float radius, int color) {
        // A state is one value, so it can only name the loudest thing true about a control: focus
        // is lost the moment the pointer arrives or a request goes out. The ring cannot go with it
        // — losing the player's place because they moved the mouse is how a keyboard user ends up
        // hunting for it. FOCUS_VISIBLE is how a gallery asks for a ring with no surface behind
        // it; `ringing` is how a live one keeps it up underneath a louder state.
        if (state != State.FOCUS_VISIBLE && !ringing) {
            return;
        }
        Box around = bounds.inset(-RING_GAP);
        painter.border(around.x(), around.y(), around.w(), around.h(),
            radius + RING_GAP, Tokens.Stroke.FOCUS, color);
    }

    /** Mouse down inside the bounds. Returning true captures the pointer until it comes up. */
    protected boolean press(double mx, double my) {
        return true;
    }

    /** The pointer moved while this widget held the capture. */
    protected void drag(double mx, double my) {}

    /** The capture ended. {@code inside} is false when the pointer was dragged off first. */
    protected void release(double mx, double my, boolean inside) {
        if (inside) {
            act();
        }
    }

    /** A key press while this widget holds the keyboard, as a GLFW code. True if consumed. */
    protected boolean key(int code, boolean shift) {
        return false;
    }

    /** A typed codepoint — not a char, because a client that has to set Hangul cannot use one. */
    protected boolean type(int codepoint) {
        return false;
    }

    /** A scroll while the pointer is over this widget. */
    protected boolean scroll(double amount) {
        return false;
    }

    /** What Enter, Space and a click that comes up inside the bounds all do. */
    protected void act() {}

    /** Whether this control is wearing the ring. Surface-owned, like hover and press. */
    final boolean ringing() {
        return ringing;
    }

    /** Surface-owned: hover and press are facts about the pointer, not about the widget. */
    final void hovered(boolean value) {
        own = own.hovered(value);
    }

    final void ringing(boolean value) {
        ringing = value;
    }

    final void pressed(boolean value) {
        own = own.pressed(value);
    }
}
