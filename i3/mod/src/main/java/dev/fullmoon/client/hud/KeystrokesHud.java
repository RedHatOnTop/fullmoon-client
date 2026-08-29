package dev.fullmoon.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Production-grade WASD, Jump, and Mouse keystrokes with live CPS tracking. */
public final class KeystrokesHud extends BaseHudElement {
    private static final int KEY_SIZE = 22;
    private static final int KEY_GAP = 2;
    private static final int MOUSE_H = 16;
    private static final int TOTAL_W = KEY_SIZE * 3 + KEY_GAP * 2;
    private static final int TOTAL_H = KEY_SIZE * 2 + MOUSE_H + KEY_GAP * 2;

    private static final Deque<Long> LMB_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RMB_CLICKS = new ArrayDeque<>();
    private static boolean lastLmbState = false;
    private static boolean lastRmbState = false;

    public KeystrokesHud() {
        super("keystrokes", "키스트로크", "플레이어", true, Anchor.BOTTOM_RIGHT, 16, 16);
    }

    public static void recordClick(boolean lmb, boolean rmb) {
        long now = System.currentTimeMillis();
        if (lmb && !lastLmbState) {
            LMB_CLICKS.addLast(now);
        }
        if (rmb && !lastRmbState) {
            RMB_CLICKS.addLast(now);
        }
        lastLmbState = lmb;
        lastRmbState = rmb;
    }

    private static int getCps(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
        return clicks.size();
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
        long now = System.currentTimeMillis();
        boolean wDown = !isEditor && client.options != null && client.options.keyUp.isDown();
        boolean aDown = !isEditor && client.options != null && client.options.keyLeft.isDown();
        boolean sDown = !isEditor && client.options != null && client.options.keyDown.isDown();
        boolean dDown = !isEditor && client.options != null && client.options.keyRight.isDown();
        boolean lmbDown = !isEditor && client.options != null && client.options.keyAttack.isDown();
        boolean rmbDown = !isEditor && client.options != null && client.options.keyUse.isDown();

        if (!isEditor) {
            recordClick(lmbDown, rmbDown);
        }

        int lmbCps = isEditor ? 10 : getCps(LMB_CLICKS, now);
        int rmbCps = isEditor ? 0 : getCps(RMB_CLICKS, now);

        int x = bounds.x();
        int y = bounds.y();

        // Row 1: W (centered)
        drawKey(painter, x + KEY_SIZE + KEY_GAP, y, KEY_SIZE, KEY_SIZE, "W", wDown);

        // Row 2: A, S, D
        int row2Y = y + KEY_SIZE + KEY_GAP;
        drawKey(painter, x, row2Y, KEY_SIZE, KEY_SIZE, "A", aDown);
        drawKey(painter, x + KEY_SIZE + KEY_GAP, row2Y, KEY_SIZE, KEY_SIZE, "S", sDown);
        drawKey(painter, x + (KEY_SIZE + KEY_GAP) * 2, row2Y, KEY_SIZE, KEY_SIZE, "D", dDown);

        // Row 3: LMB, RMB with CPS
        int row3Y = row2Y + KEY_SIZE + KEY_GAP;
        int mouseW = (TOTAL_W - KEY_GAP) / 2;
        String lmbText = lmbCps > 0 ? (lmbCps + " CPS") : "LMB";
        String rmbText = rmbCps > 0 ? (rmbCps + " CPS") : "RMB";
        drawKey(painter, x, row3Y, mouseW, MOUSE_H, lmbText, lmbDown);
        drawKey(painter, x + mouseW + KEY_GAP, row3Y, mouseW, MOUSE_H, rmbText, rmbDown);
    }

    private void drawKey(Painter painter, int kx, int ky, int kw, int kh, String text, boolean down) {
        int bg = down ? Tokens.Color.ACCENT : Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.78f);
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
