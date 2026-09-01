package dev.fullmoon.client.menu;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.network.MenuProtocol;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.State;
import dev.fullmoon.client.ui.Voice;
import dev.fullmoon.client.ui.Widget;

final class ServerMenuTile extends Widget {
    private static final int PAD = Tokens.Space.SNUG;

    private final MenuProtocol.Item item;
    private final Runnable action;

    ServerMenuTile(MenuProtocol.Item item, Runnable action) {
        super(Voice.QUIET, item.label());
        this.item = item;
        this.action = action;
        enabled(!item.actions().isEmpty());
    }

    MenuProtocol.Item item() {
        return item;
    }

    @Override
    public void draw(Painter painter, State state) {
        Box box = bounds();
        boolean actionable = !item.actions().isEmpty();
        int ground = actionable ? voice().chrome(state).fill() : Tokens.Color.SURFACE_SUNKEN;
        int line = actionable ? voice().chrome(state).line() : Tokens.Color.LINE_HAIRLINE;
        int ink = actionable ? voice().chrome(state).ink() : Tokens.Color.INK_SECONDARY;

        painter.fill(box.x(), box.y(), box.w(), box.h(), Tokens.Radius.SM, ground);
        painter.border(box.x(), box.y(), box.w(), box.h(), Tokens.Radius.SM,
            Tokens.Stroke.HAIR, line);
        if (actionable) {
            painter.fill(box.x(), box.y(), Tokens.Stroke.FOCUS, box.h(), Tokens.Color.ACCENT);
            ring(painter, state, Tokens.Radius.SM);
        }

        int left = box.x() + PAD + (actionable ? Tokens.Stroke.FOCUS : 0);
        int countSpace = item.count() > 1 && box.w() >= 36 ? 24 : 0;
        int textWidth = Math.max(0, box.right() - PAD - countSpace - left);
        painter.pushClip(left, box.y(), textWidth, box.h());
        Typeset.draw(painter, Tokens.Type.BODY_STRONG, item.label(), left,
            Typeset.centred(Tokens.Type.BODY_STRONG, box.y(), box.h()), ink);
        painter.popClip();

        if (item.count() > 1 && box.w() >= 36) {
            Typeset.tabularRight(painter, Tokens.Type.LABEL, Integer.toString(item.count()),
                box.right() - PAD, box.bottom() - Tokens.Type.LABEL.leading() - PAD,
                Tokens.Color.INK_TERTIARY);
        }
    }

    @Override
    protected void act() {
        action.run();
    }
}
