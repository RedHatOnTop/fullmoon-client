package dev.fullmoon.client.ui;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

/**
 * A labelled action.
 *
 * <p>Loading puts three dots where the label was instead of dimming it. Dimmed ink is what
 * disabled already looks like, and a still frame has to be able to tell a request in flight from
 * a control that is off. The dots do not move: motion arrives in P2, together with the
 * reduced-motion setting that has to be able to collapse it.
 */
public final class Button extends Widget {
    public static final int HEIGHT = Tokens.Space.SECTION;

    private static final int DOT_RADIUS = Tokens.Space.TIGHT;
    private static final int DOT_PITCH = Tokens.Space.BASE;

    private final Runnable action;

    public Button(Voice voice, String label, Runnable action) {
        super(voice, label);
        this.action = action;
    }

    /** The narrowest this button reads at. Layout may give it more, never less. */
    public int measure() {
        return Math.max(HEIGHT * 2,
            Typeset.width(Tokens.Type.BODY_STRONG, label()) + Tokens.Space.SECTION);
    }

    @Override
    public void draw(Painter painter, State state) {
        Box b = bounds();
        Chrome chrome = voice().chrome(state);
        painter.fill(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.MD, chrome.fill());
        painter.border(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.MD, Tokens.Stroke.HAIR, chrome.line());
        ring(painter, state, Tokens.Radius.MD);

        if (state == State.LOADING) {
            for (int i = -1; i <= 1; i++) {
                painter.dot(b.midX() + i * DOT_PITCH, b.midY(), DOT_RADIUS, chrome.ink());
            }
            return;
        }
        Typeset.drawCentered(painter, Tokens.Type.BODY_STRONG, label(), b.midX(),
            Typeset.centred(Tokens.Type.BODY_STRONG, b.y(), b.h()), chrome.ink());
    }

    @Override
    protected void act() {
        action.run();
    }
}
