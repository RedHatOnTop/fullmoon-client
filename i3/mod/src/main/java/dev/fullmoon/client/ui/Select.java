package dev.fullmoon.client.ui;

import java.util.List;
import java.util.function.IntConsumer;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * One of a short list of options, with the list itself hanging below the control while it is open.
 *
 * <p>Open is not a state. The eight say how a control is being handled, and this one can be
 * hovered, focused, in flight or off with its list either up or down — the same reason
 * {@link Toggle} does not have an {@code ON} state. What open does change is where the control can
 * be clicked, which is what {@link #reach} is for, and what it draws over, which is what the second
 * pass is for.
 *
 * <p>The arrow is three rows of pixels laid out by hand. The shape pipeline draws rectangles,
 * rounded rectangles and circles, and a triangle is none of those; a glyph borrowed from a font
 * would be the vanilla chrome this client exists to not look like.
 */
public final class Select extends Widget {
    public static final int HEIGHT = Tokens.Space.SECTION;

    /** Dense on purpose: five rows have to read as a list and not as five buttons in a stack. */
    private static final int ROW_H = Tokens.Space.GUTTER;

    /** The arrow's row widths, widest first. Pointing up, the same rows run the other way. */
    private static final int[] ARROW = {5, 3, 1};

    private final List<String> options;
    private final IntConsumer onPick;
    private int index;
    private int marked;
    private boolean open;

    public Select(String label, List<String> options, int index, IntConsumer onPick) {
        super(Voice.QUIET, label);
        this.options = List.copyOf(options);
        this.index = Math.clamp(index, 0, this.options.size() - 1);
        this.marked = this.index;
        this.onPick = onPick;
    }

    public int index() {
        return index;
    }

    public String picked() {
        return options.get(index);
    }

    public boolean open() {
        return open;
    }

    /** The row the keyboard is on while the list is up. Equal to {@link #index} while it is down. */
    public int marked() {
        return marked;
    }

    /**
     * The control plus its list, so a click on a row is a click on this widget. The ring keeps to
     * {@link #bounds}: it belongs to the control, and a ring around an open list would be a ring
     * around a hole in the surface.
     */
    @Override
    protected Box reach() {
        Box b = bounds();
        return open ? Box.between(b.x(), b.y(), b.right(), popover().bottom()) : b;
    }

    /** Open, the list is over the surface: it paints on the second pass and is hit on the first. */
    @Override
    protected boolean overlaying() {
        return open;
    }

    @Override
    public void draw(Painter painter, State state) {
        Box b = field();
        Chrome chrome = voice().chrome(state);
        painter.fill(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.SM, chrome.fill());
        painter.border(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.SM, Tokens.Stroke.HAIR,
            chrome.line());

        int arrowX = b.right() - Tokens.Space.COZY - ARROW[0];
        if (state == State.LOADING) {
            Dots.draw(painter, b.x() + Tokens.Space.COZY + Dots.width() / 2.0f, b.midY(),
                chrome.ink());
        } else {
            int textX = b.x() + Tokens.Space.COZY;
            painter.pushClip(textX, b.y(), arrowX - Tokens.Space.SNUG - textX, b.h());
            Typeset.draw(painter, Tokens.Type.BODY, picked(), textX,
                Typeset.centred(Tokens.Type.BODY, b.y(), b.h()), chrome.ink());
            painter.popClip();
        }
        arrow(painter, arrowX, b.midY(), open, chrome.ink());

        if (!label().isEmpty()) {
            Typeset.draw(painter, Tokens.Type.BODY, label(), bounds().x(),
                Typeset.centred(Tokens.Type.BODY, b.y(), b.h()), chrome.ink());
        }
        ring(painter, state, Tokens.Radius.SM);
    }

    @Override
    protected void drawOverlay(Painter painter, State state) {
        if (!open) {
            return;
        }
        Box p = popover();
        painter.fill(p.x(), p.y(), p.w(), p.h(), Tokens.Radius.SM, Tokens.Color.SURFACE_OVERLAY);
        painter.border(p.x(), p.y(), p.w(), p.h(), Tokens.Radius.SM, Tokens.Stroke.HAIR,
            Tokens.Color.LINE_STRONG);

        for (int i = 0; i < options.size(); i++) {
            Box row = row(i);
            int textY = Typeset.centred(Tokens.Type.BODY, row.y(), row.h());
            if (i == marked) {
                painter.fill(row.x(), row.y(), row.w(), row.h(), Tokens.Color.ACCENT_WASH);
            }
            if (i == index) {
                // The same tick a section head wears, for the same reason: a mark beside the words
                // says "this one" without spending a second colour on saying it.
                painter.fill(row.x() + Tokens.Space.SNUG, Typeset.capTop(Tokens.Type.BODY, textY),
                    Tokens.Stroke.HAIR, Typeset.capHeight(Tokens.Type.BODY), Tokens.Color.ACCENT);
            }
            Typeset.draw(painter, Tokens.Type.BODY, options.get(i), row.x() + Tokens.Space.COZY,
                textY, i == index ? Tokens.Color.INK_PRIMARY : Tokens.Color.INK_SECONDARY);
        }
    }

    @Override
    protected void hovering(double mx, double my) {
        if (!open) {
            return;
        }
        int row = rowAt(my);
        if (row >= 0) {
            marked = row;
        }
    }

    @Override
    protected boolean press(double mx, double my) {
        int row = open ? rowAt(my) : -1;
        if (row < 0) {
            // Anywhere else, including the list's own border: the control is being pressed, and
            // what that does is settled on the way back up.
            return true;
        }
        pick(row);
        return false;
    }

    @Override
    protected boolean key(Chord chord) {
        if (!open) {
            return false;
        }
        if (chord.is(InputConstants.KEY_ESCAPE)) {
            open = false;
            return true;
        }
        if (chord.is(InputConstants.KEY_UP)) {
            marked = Math.max(0, marked - 1);
            return true;
        }
        if (chord.is(InputConstants.KEY_DOWN)) {
            marked = Math.min(options.size() - 1, marked + 1);
            return true;
        }
        if (chord.activates()) {
            pick(marked);
            return true;
        }
        return false;
    }

    @Override
    protected void act() {
        open = !open;
        marked = index;
    }

    /** Tab out of an open select and the list goes with it. A list nobody is holding is litter. */
    @Override
    protected void blurred() {
        open = false;
    }

    private void pick(int row) {
        open = false;
        marked = row;
        if (row != index) {
            index = row;
            onPick.accept(index);
        }
    }

    /** The row a pointer at {@code my} falls on, or -1 for the padding, the border and elsewhere. */
    private int rowAt(double my) {
        if (!open) {
            return -1;
        }
        Box list = popover().inset(0, Tokens.Space.SNUG);
        if (my < list.y() || my >= list.bottom()) {
            return -1;
        }
        return Math.min(options.size() - 1, (int) ((my - list.y()) / ROW_H));
    }

    private Box row(int i) {
        Box list = popover().inset(0, Tokens.Space.SNUG);
        return new Box(list.x(), list.y() + i * ROW_H, list.w(), ROW_H);
    }

    private Box popover() {
        Box b = field();
        return new Box(b.x(), b.bottom() + Tokens.Space.TIGHT, b.w(),
            options.size() * ROW_H + Tokens.Space.COZY);
    }

    /** The control itself, which is the bounds less whatever the label took on the left. */
    private Box field() {
        Box b = bounds();
        int left = b.x() + (label().isEmpty()
            ? 0
            : Typeset.width(Tokens.Type.BODY, label()) + Tokens.Space.LOOSE);
        return Box.between(left, b.y(), b.right(), b.bottom());
    }

    private static void arrow(Painter painter, int left, int cy, boolean up, int color) {
        int top = cy - ARROW.length / 2;
        for (int i = 0; i < ARROW.length; i++) {
            int w = ARROW[up ? ARROW.length - 1 - i : i];
            painter.fill(left + (ARROW[0] - w) / 2, top + i, w, 1, color);
        }
    }
}
