package dev.fullmoon.client.ui;

import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * One line of text with a caret, a selection and a rule it has to satisfy.
 *
 * <p>The caret model measures nothing. Every index it moves through is an offset into the string,
 * so all of it runs without a font and therefore without a game; the two things that genuinely
 * need measuring — where a click lands, and how far the view has scrolled — are settled while the
 * text is being drawn and nowhere else.
 *
 * <p>Indices step by code point rather than by char, because a name field is exactly where a
 * player puts something outside the basic plane, and a left arrow that lands between the halves of
 * a surrogate pair takes half a character with it on the next backspace.
 *
 * <p>Loading puts {@link Dots} at the right end and keeps the text. What the player typed is still
 * what the field says; the request is about whether it is allowed.
 */
public final class TextField extends Widget {
    public static final int HEIGHT = Tokens.Space.SECTION;

    private final Clipboard clipboard;
    private final Predicate<String> rule;
    private final Consumer<String> onCommit;
    private final String placeholder;
    private final int limit;

    private String text;
    private String committed;
    private int caret;
    private int anchor;

    /** How far the view is shifted left, in pixels. Settled while drawing; see {@link #view}. */
    private int scrolled;

    public TextField(String label, String placeholder, String text, int limit,
            Predicate<String> rule, Consumer<String> onCommit) {
        this(label, placeholder, text, limit, rule, onCommit, Clipboard.game());
    }

    TextField(String label, String placeholder, String text, int limit, Predicate<String> rule,
            Consumer<String> onCommit, Clipboard clipboard) {
        super(Voice.QUIET, label);
        this.placeholder = placeholder;
        this.limit = limit;
        this.rule = rule;
        this.onCommit = onCommit;
        this.clipboard = clipboard;
        this.text = text;
        this.committed = text;
        this.caret = text.length();
        this.anchor = this.caret;
        invalid(!rule.test(text));
    }
    public String text() {
        return text;
    }

    /** Where the caret sits, as an offset into {@link #text}. Always on a code point boundary. */
    public int caret() {
        return caret;
    }

    public String selected() {
        return text.substring(Math.min(caret, anchor), Math.max(caret, anchor));
    }

    @Override
    public void draw(Painter painter, State state) {
        Box b = field();
        Chrome chrome = voice().chrome(state);
        painter.fill(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.SM, chrome.fill());
        painter.border(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.SM, Tokens.Stroke.HAIR,
            chrome.line());
        if (!label().isEmpty()) {
            Typeset.draw(painter, Tokens.Type.BODY, label(), bounds().x(),
                Typeset.centred(Tokens.Type.BODY, b.y(), b.h()), chrome.ink());
        }

        Box area = area(b);
        if (state == State.LOADING) {
            area = Box.between(area.x(), area.y(), area.right() - Dots.width() - Tokens.Space.SNUG,
                area.bottom());
        }
        // The caret answers to where the keyboard is and not to the loudest state: a field clicked
        // into keeps its caret while the pointer sits over it, and it wears no ring for either.
        boolean typing = holding() && state.live();
        int y = Typeset.centred(Tokens.Type.BODY, b.y(), b.h());
        // What view() keeps in sight is the caret, and a field nobody is typing in has none: it shows
        // the head of its text. Left to itself it would scroll that head off the moment LOADING
        // narrowed the area, and a field the player is only reading has no reason to move at all.
        scrolled = typing ? snappedView(area) : 0;

        if (typing && caret != anchor) {
            int from = area.x() + at(Math.min(caret, anchor)) - scrolled;
            int to = area.x() + at(Math.max(caret, anchor)) - scrolled;
            band(painter, area, from, to - from, y, Tokens.Color.ACCENT_WASH);
        }
        if (typing) {
            Typeset.draw(painter, Tokens.Type.BODY, "|",
                area.x() + at(caret) - scrolled, y, Tokens.Color.ACCENT);
        }
        if (text.isEmpty() && !typing) {
            Typeset.draw(painter, Tokens.Type.BODY,
                Typeset.fittingPrefix(Tokens.Type.BODY, placeholder, area.w()), area.x(), y,
                Tokens.Color.INK_TERTIARY);
        } else {
            Typeset.draw(painter, Tokens.Type.BODY, visibleText(area.w()), area.x(), y, chrome.ink());
        }

        if (state == State.LOADING) {
            Dots.draw(painter, b.right() - Tokens.Space.COZY - Dots.width() / 2.0f, b.midY(),
                chrome.ink());
        }
        ring(painter, state, Tokens.Radius.SM);
    }

    /**
     * The caret and the selection are the same shape: the band the capitals occupy, plus a hairline
     * above and below so that neither of them ends flush with a letter.
     */
    private static void band(Painter painter, Box clip, int x, int w, int y, int color) {
        int left = Math.max(clip.x(), x);
        int right = Math.min(clip.right(), x + w);
        painter.fill(left, Typeset.capTop(Tokens.Type.BODY, y) - Tokens.Space.HAIR,
            Math.max(0, right - left), Typeset.capHeight(Tokens.Type.BODY) + Tokens.Space.TIGHT,
            color);
    }

    @Override
    protected boolean key(Chord chord) {
        switch (chord.edit()) {
            case SELECT_ALL -> {
                anchor = 0;
                caret = text.length();
                return true;
            }
            case COPY -> {
                copy();
                return true;
            }
            case CUT -> {
                copy();
                replace("");
                return true;
            }
            case PASTE -> {
                replace(oneLine(clipboard.get()));
                return true;
            }
            case NONE -> {
                // A key with no editing intent on it. Read as movement below.
            }
        }
        if (chord.is(InputConstants.KEY_LEFT)) {
            return moveTo(chord.control() ? wordBack() : back(), chord.shift());
        }
        if (chord.is(InputConstants.KEY_RIGHT)) {
            return moveTo(chord.control() ? wordForward() : forward(), chord.shift());
        }
        if (chord.is(InputConstants.KEY_HOME)) {
            return moveTo(0, chord.shift());
        }
        if (chord.is(InputConstants.KEY_END)) {
            return moveTo(text.length(), chord.shift());
        }
        if (chord.is(InputConstants.KEY_BACKSPACE)) {
            return erase(back());
        }
        if (chord.is(InputConstants.KEY_DELETE)) {
            return erase(forward());
        }
        // Space arrives again through type() as a character. Claiming the key here is what stops
        // the surface from also reading it as an activation and submitting a half-typed field.
        return chord.is(InputConstants.KEY_SPACE);
    }

    @Override
    protected boolean type(int codepoint) {
        if (Character.isISOControl(codepoint)) {
            return false;
        }
        replace(Character.toString(codepoint));
        return true;
    }

    /** Enter. */
    @Override
    protected void act() {
        commit();
    }

    /** Tab, or a click elsewhere. An edit the player walked away from is still an edit. */
    @Override
    protected void blurred() {
        commit();
    }

    /**
     * Deliberately not {@link #act}: a click inside a field puts the caret where the pointer is.
     * A field that submitted on the way up would submit every time it was clicked into.
     */
    @Override
    protected void release(double mx, double my, boolean inside) {
    }

    @Override
    protected boolean press(double mx, double my) {
        moveTo(indexAt(mx), false);
        return true;
    }

    @Override
    protected void drag(double mx, double my) {
        moveTo(indexAt(mx), true);
    }

    /** Replaces the selection, or inserts at the caret when there is none. */
    private void replace(String with) {
        int from = Math.min(caret, anchor);
        int to = Math.max(caret, anchor);
        String head = text.substring(0, from);
        String tail = text.substring(to);
        String fits = clip(with, limit - count(head) - count(tail));
        text = head + fits + tail;
        caret = head.length() + fits.length();
        anchor = caret;
        invalid(!rule.test(text));
    }

    /** Backspace and Delete: the selection if there is one, and otherwise the neighbour at {@code to}. */
    private boolean erase(int to) {
        if (caret == anchor) {
            anchor = to;
        }
        replace("");
        return true;
    }

    /** True either way: a caret already at the end it was sent to has still answered for the key. */
    private boolean moveTo(int index, boolean extend) {
        caret = Math.clamp(index, 0, text.length());
        if (!extend) {
            anchor = caret;
        }
        return true;
    }

    private int back() {
        return caret == 0 ? 0 : caret - Character.charCount(text.codePointBefore(caret));
    }

    private int forward() {
        return caret == text.length()
            ? caret
            : caret + Character.charCount(text.codePointAt(caret));
    }

    /** Control-Left: off the spaces behind the caret, then off the word behind those. */
    private int wordBack() {
        return skip(skip(caret, true, true), true, false);
    }

    private int wordForward() {
        return skip(skip(caret, false, true), false, false);
    }

    /**
     * Walks off from {@code i} while the code point on the {@code left} side is whitespace, or while
     * it is not, whichever {@code spaces} asks for. Two calls are a word jump.
     */
    private int skip(int i, boolean left, boolean spaces) {
        int at = i;
        while (left ? at > 0 : at < text.length()) {
            int cp = left ? text.codePointBefore(at) : text.codePointAt(at);
            if (Character.isWhitespace(cp) != spaces) {
                return at;
            }
            at += left ? -Character.charCount(cp) : Character.charCount(cp);
        }
        return at;
    }

    private void copy() {
        String held = selected();
        if (!held.isEmpty()) {
            clipboard.put(held);
        }
    }

    /** Only on a change: Enter and then Tab out is one edit, and one edit fires once. */
    private void commit() {
        if (!text.equals(committed)) {
            committed = text;
            onCommit.accept(text);
        }
    }

    /** As much of {@code text} as {@code room} code points allow, cut on a boundary. */
    private static String clip(String text, int room) {
        if (room <= 0) {
            return "";
        }
        return count(text) <= room
            ? text
            : text.substring(0, text.offsetByCodePoints(0, room));
    }

    /** The limit is in code points, because that is what a player counts when they look at it. */
    private static int count(String text) {
        return text.codePointCount(0, text.length());
    }

    /** A paste brings whatever the system had in it, and one line of text has no lines in it. */
    private static String oneLine(String pasted) {
        StringBuilder kept = new StringBuilder(pasted.length());
        pasted.codePoints().filter(cp -> !Character.isISOControl(cp)).forEach(kept::appendCodePoint);
        return kept.toString();
    }

    /** How far into the text {@code index} is. The one place the caret model meets a font. */
    private int at(int index) {
        return Typeset.width(Tokens.Type.BODY, text.substring(0, index));
    }

    /**
     * The index a pointer at {@code mx} lands on, which is the boundary it is nearest rather than
     * the letter it is over: clicking the right half of a letter puts the caret after it.
     */
    private int indexAt(double mx) {
        double want = mx - area(field()).x() + scrolled;
        int best = 0;
        double gap = Math.abs(want);
        int i = 0;
        while (i < text.length()) {
            i += Character.charCount(text.codePointAt(i));
            double d = Math.abs(want - at(i));
            if (d < gap) {
                gap = d;
                best = i;
            }
        }
        return best;
    }

    /**
     * How far the view shifts left to keep the caret inside {@code area}, and no further. Settled
     * while drawing because that is the only moment the text has been measured — which is the same
     * reason nothing above here has to measure anything.
     */
    private int view(Box area) {
        int caretX = at(caret);
        int room = Math.max(0, at(text.length()) - area.w() + Tokens.Stroke.FOCUS);
        int kept = Math.max(Math.min(scrolled, caretX), caretX - area.w() + Tokens.Stroke.FOCUS);
        return Math.clamp(kept, 0, room);
    }

    /** Aligns the pixel view to a code point boundary so text never needs a render-time scissor. */
    private int snappedView(Box area) {
        int wanted = view(area);
        int index = 0;
        while (index < text.length() && at(index) < wanted) {
            index += Character.charCount(text.codePointAt(index));
        }
        return at(index);
    }

    /** The complete glyphs that fit from the snapped view through the field's right edge. */
    private String visibleText(int width) {
        int from = 0;
        while (from < text.length() && at(from) < scrolled) {
            from += Character.charCount(text.codePointAt(from));
        }
        int to = from;
        while (to < text.length()) {
            int next = to + Character.charCount(text.codePointAt(to));
            if (at(next) - scrolled > width) {
                break;
            }
            to = next;
        }
        return text.substring(from, to);
    }

    /** Where the text goes: the field, less a gutter at each end. */
    private static Box area(Box field) {
        return Box.between(field.x() + Tokens.Space.COZY, field.y(),
            field.right() - Tokens.Space.COZY, field.bottom());
    }

    /** The control itself, which is the bounds less whatever the label took on the left. */
    private Box field() {
        Box b = bounds();
        int left = b.x() + (label().isEmpty()
            ? 0
            : Typeset.width(Tokens.Type.BODY, label()) + Tokens.Space.LOOSE);
        return Box.between(left, b.y(), b.right(), b.bottom());
    }
}
