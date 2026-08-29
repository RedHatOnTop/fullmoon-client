package dev.fullmoon.client.hud;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Minimalist WASD & Mouse keystrokes module with active press highlights. */
public final class KeystrokesHud extends BaseHudElement {
    private static final int KEY_SIZE = 20;
    private static final int KEY_GAP = 2;
    private static final int MOUSE_H = 14;
    private static final int TOTAL_W = KEY_SIZE * 3 + KEY_GAP * 2;
    private static final int TOTAL_H = KEY_SIZE * 2 + MOUSE_H + KEY_GAP * 2;

    public KeystrokesHud() {
        super("keystrokes", "키스트로크", "플레이어", true, Anchor.BOTTOM_RIGHT, 16, 16);
    }

    @Override
    public int measureWidth(Minecraft client) {
        return TOTAL_W;
    }

    @Override
    public int measureHeight(Minecraft client) {
        return TOTAL_H;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        boolean wDown = !isEditor && client.options.keyUp.isDown();
        boolean aDown = !isEditor && client.options.keyLeft.isDown();
        boolean sDown = !isEditor && client.options.keyDown.isDown();
        boolean dDown = !isEditor && client.options.keyRight.isDown();
        boolean lmbDown = !isEditor && client.options.keyAttack.isDown();
        boolean rmbDown = !isEditor && client.options.keyUse.isDown();

        int x = bounds.x();
        int y = bounds.y();

        // Row 1: W (centered)
        drawKey(painter, x + KEY_SIZE + KEY_GAP, y, KEY_SIZE, KEY_SIZE, "W", wDown);

        // Row 2: A, S, D
        int row2Y = y + KEY_SIZE + KEY_GAP;
        drawKey(painter, x, row2Y, KEY_SIZE, KEY_SIZE, "A", aDown);
        drawKey(painter, x + KEY_SIZE + KEY_GAP, row2Y, KEY_SIZE, KEY_SIZE, "S", sDown);
        drawKey(painter, x + (KEY_SIZE + KEY_GAP) * 2, row2Y, KEY_SIZE, KEY_SIZE, "D", dDown);

        // Row 3: LMB, RMB
        int row3Y = row2Y + KEY_SIZE + KEY_GAP;
        int mouseW = (TOTAL_W - KEY_GAP) / 2;
        drawKey(painter, x, row3Y, mouseW, MOUSE_H, "LMB", lmbDown);
        drawKey(painter, x + mouseW + KEY_GAP, row3Y, mouseW, MOUSE_H, "RMB", rmbDown);
    }

    private void drawKey(Painter painter, int kx, int ky, int kw, int kh, String text, boolean down) {
        int bg = down ? Tokens.Color.ACCENT : Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.76f);
        int border = down ? Tokens.Color.ACCENT_PRESSED : Tokens.Color.LINE_HAIRLINE;
        int ink = down ? Tokens.Color.INK_ON_ACCENT : Tokens.Color.INK_PRIMARY;

        painter.fill(kx, ky, kw, kh, Tokens.Radius.SM, bg);
        painter.border(kx, ky, kw, kh, Tokens.Radius.SM, Tokens.Stroke.HAIR, border);

        int textW = Typeset.width(Tokens.Type.LABEL, text);
        int textX = kx + (kw - textW) / 2;
        int textY = ky + (kh - Tokens.Type.LABEL.leading()) / 2;
        Typeset.draw(painter, Tokens.Type.LABEL, text, textX, textY, ink);
    }
}
