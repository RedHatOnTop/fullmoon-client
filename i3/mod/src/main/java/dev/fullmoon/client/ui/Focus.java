package dev.fullmoon.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the keyboard is on a surface, and whether it should be showing.
 *
 * <p>Position and visibility are separate on purpose. Tab moves the keyboard and lights the
 * ring; a click moves it and leaves it dark. Keeping the two apart is what lets a clicked field
 * take typing without the surface sprouting a ring no mouse user asked for, and what lets the
 * next Tab pick up from the field the mouse left off at.
 */
public final class Focus {
    /** A thing the keyboard can land on. */
    public interface Target {
        /** Whether this target will take the keyboard right now. A disabled control will not. */
        boolean takesFocus();
    }

    private final List<Target> order = new ArrayList<>();
    private int at = -1;
    private boolean visible;

    /** Appends a target to the traversal order. Registration order is Tab order. */
    public void add(Target target) {
        order.add(target);
    }

    /**
     * The target holding the keyboard, or {@code null} — including when the target that has it
     * has since gone disabled. The position is left alone rather than cleared, so a control that
     * comes back enabled comes back focused instead of dumping the keyboard at the top of the
     * surface.
     */
    public Target held() {
        Target target = at < 0 ? null : order.get(at);
        return target != null && target.takesFocus() ? target : null;
    }

    public boolean holds(Target target) {
        return held() == target;
    }

    /** Whether the ring is up: the keyboard put it there, not the mouse. */
    public boolean visible() {
        return visible;
    }

    /** Whether {@code target} should be wearing the ring this frame. */
    public boolean rings(Target target) {
        return visible && holds(target);
    }

    /**
     * Moves the keyboard {@code step} places and lights the ring, skipping what will not take it
     * and wrapping at both ends. From nowhere, a forward step lands on the first target and a
     * backward step on the last.
     *
     * @return whether any target took it; if none would, the keyboard is released
     */
    public boolean advance(int step) {
        if (order.isEmpty() || step == 0) {
            return false;
        }
        int n = order.size();
        int from = at >= 0 ? at : (step > 0 ? -1 : 0);
        for (int i = 1; i <= n; i++) {
            int probe = Math.floorMod(from + step * i, n);
            if (order.get(probe).takesFocus()) {
                at = probe;
                visible = true;
                return true;
            }
        }
        clear();
        return false;
    }

    /**
     * Puts the keyboard on {@code target} with the ring down — what a click does. A target that
     * will not take it leaves the keyboard where it was, so clicking a disabled button does not
     * silently steal the caret out of a field.
     */
    public boolean point(Target target) {
        if (!target.takesFocus()) {
            return false;
        }
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) == target) {
                at = i;
                visible = false;
                return true;
            }
        }
        return false;
    }

    /** Releases the keyboard. The traversal order stands. */
    public void clear() {
        at = -1;
        visible = false;
    }
}
