package dev.fullmoon.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The event half of a screen: which control the pointer is over, which one holds the keyboard,
 * and what becomes of a key nobody asked for.
 *
 * <p>It does not draw. Draw order belongs to whatever laid the controls out — a popover has to
 * paint over the row that opened it, and only the layout knows about both. Leaving drawing out
 * is also what makes these rules testable rather than eyeballed: the only game code on this
 * path is {@link InputConstants}' key numbers, and those are constant variables that javac folds
 * into the bytecode, so no game class is loaded when the rules run headless.
 */
public final class Surface {
    private final List<Widget> widgets = new ArrayList<>();
    private final Focus focus = new Focus();

    /** The widget the pointer went down on, until it comes back up. */
    private Widget captured;

    /** Registration order is Tab order, and later arrivals sit on top when the pointer lands. */
    public <W extends Widget> W add(W widget) {
        widgets.add(widget);
        focus.add(widget);
        return widget;
    }

    public List<Widget> widgets() {
        return Collections.unmodifiableList(widgets);
    }

    public Focus focus() {
        return focus;
    }

    /** The widget holding the keyboard, or null. */
    public Widget held() {
        return focus.held() instanceof Widget widget ? widget : null;
    }

    /** The widget holding the pointer, or null. */
    public Widget captured() {
        return captured;
    }

    /**
     * The state a widget is in on this surface, and the frame's answer to whether it wears the
     * ring. The ring is settled here rather than folded into the state because only one of the two
     * is a single value: a focused control that is also hovered has to report the hover and keep
     * the ring, and a state enum cannot say both.
     */
    public State state(Widget widget) {
        widget.ringing(focus.rings(widget));
        return widget.state(focus);
    }

    /** The topmost widget under the pointer, or null. Later-added widgets win. */
    public Widget at(double mx, double my) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.bounds().holds(mx, my)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * Re-reads what the pointer is over. Cheap and idempotent, so a screen can call it every
     * frame — which it has to, because a layout can change under a pointer that never moved.
     *
     * <p>While a widget holds the capture it is the only one that can be hovered, and its press
     * follows the pointer in and out of its bounds: dragging off a held button has to let go of
     * it, or the only way out of a press is to commit to it.
     */
    public void hover(double mx, double my) {
        Widget over = captured != null
            ? (captured.bounds().holds(mx, my) ? captured : null)
            : at(mx, my);
        for (Widget widget : widgets) {
            widget.hovered(widget == over);
        }
        if (captured != null) {
            captured.pressed(over == captured);
        }
    }

    /** The pointer moved: same as {@link #hover}, and the captured widget hears about it. */
    public void pointer(double mx, double my) {
        hover(mx, my);
        if (captured != null) {
            captured.drag(mx, my);
        }
    }

    /** Mouse down. True when the surface took the click. */
    public boolean press(double mx, double my) {
        Widget hit = at(mx, my);
        if (hit == null) {
            focus.clear();
            return false;
        }
        // A dead control is not a hole in the surface. Letting the click through to whatever sits
        // behind it punishes the player's aim for the control being off.
        if (!hit.state(focus).live()) {
            return true;
        }
        focus.point(hit);
        if (hit.press(mx, my)) {
            captured = hit;
            hit.pressed(true);
        }
        return true;
    }

    /** Mouse up. True when a widget was holding the pointer. */
    public boolean release(double mx, double my) {
        Widget holder = captured;
        if (holder == null) {
            return false;
        }
        captured = null;
        holder.pressed(false);
        holder.release(mx, my, holder.bounds().holds(mx, my));
        // The pointer has not moved but what is under it may have: a control that acted on the
        // release can have just gone disabled, and hover has to be read again from scratch.
        hover(mx, my);
        return true;
    }

    /**
     * A key press, as a GLFW code. The widget holding the keyboard gets first refusal — a text
     * field has to keep Space for itself — and only then is the key read as traversal or as
     * activation. An unclaimed key returns false, so the screen's own Esc still closes it.
     *
     * <p>A control in flight keeps the ring and answers nothing, so the keyboard reaches it for
     * traversal only. Tab is read before the holder is asked to activate, and outside the gate:
     * a request that leaves the player unable to leave the control they fired it from has taken
     * the surface away, not just the control.
     */
    public boolean key(int code, boolean shift) {
        Widget holder = held();
        boolean answers = holder != null && holder.state(focus).live();
        if (answers && holder.key(code, shift)) {
            return true;
        }
        if (code == InputConstants.KEY_TAB) {
            return focus.advance(shift ? -1 : 1);
        }
        if (answers && activates(code)) {
            holder.act();
            return true;
        }
        return false;
    }

    /** A typed codepoint goes to the keyboard holder and nowhere else. */
    public boolean type(int codepoint) {
        Widget holder = held();
        return holder != null && holder.state(focus).live() && holder.type(codepoint);
    }

    /** A scroll goes to whatever is under the pointer, capture or no capture. */
    public boolean scroll(double mx, double my, double amount) {
        Widget over = at(mx, my);
        return over != null && over.state(focus).live() && over.scroll(amount);
    }

    private static boolean activates(int code) {
        return code == InputConstants.KEY_RETURN
            || code == InputConstants.KEY_NUMPADENTER
            || code == InputConstants.KEY_SPACE;
    }
}
