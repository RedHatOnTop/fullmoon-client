package dev.fullmoon.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Premier PvP Keystrokes with tactile keycaps, dual-line LMB/RMB CPS meters, and spacebar. */
public final class KeystrokesHud extends BaseHudElement {
    private static final int KEY_W = 24;
    private static final int KEY_H = 24;
    private static final int GAP = 2;
    private static final int TOTAL_W = KEY_W * 3 + GAP * 2; // 76px
    private static final int MOUSE_W = (TOTAL_W - GAP) / 2; // 37px
    private static final int MOUSE_H = 26;
    private static final int SPACE_H = 12;
    private static final int TOTAL_H = KEY_H * 2 + MOUSE_H + SPACE_H + GAP * 3; // 24+24+26+12+6 = 92px

    private static final Deque<Long> LMB_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RMB_CLICKS = new ArrayDeque<>();
    private static boolean lastLmbState = false;
    private static boolean lastRmbState = false;

    public KeystrokesHud() {
        super("keystrokes", "키스트로크", "플레이어", true, Anchor.BOTTOM_RIGHT, 16, 56);
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
        boolean spaceDown = !isEditor && client.options != null && client.options.keyJump.isDown();

        if (!isEditor) {
            recordClick(lmbDown, rmbDown);
        }

        int lmbCps = isEditor ? 10 : getCps(LMB_CLICKS, now);
        int rmbCps = isEditor ? 0 : getCps(RMB_CLICKS, now);

        int x = bounds.x();
        int y = bounds.y();

        // 1. Row 1: [ W ] (centered above S)
        drawKeycap(painter, x + KEY_W + GAP, y, KEY_W, KEY_H, "W", wDown);

        // 2. Row 2: [ A ] [ S ] [ D ]
        int row2Y = y + KEY_H + GAP;
        drawKeycap(painter, x, row2Y, KEY_W, KEY_H, "A", aDown);
        drawKeycap(painter, x + KEY_W + GAP, row2Y, KEY_W, KEY_H, "S", sDown);
        drawKeycap(painter, x + (KEY_W + GAP) * 2, row2Y, KEY_W, KEY_H, "D", dDown);

        // 3. Row 3: [ LMB ] [ RMB ] with dual-line CPS readouts
        int row3Y = row2Y + KEY_H + GAP;
        drawMouseKeycap(painter, x, row3Y, MOUSE_W, MOUSE_H, "LMB", lmbCps + " CPS", lmbDown);
        drawMouseKeycap(painter, x + MOUSE_W + GAP, row3Y, MOUSE_W, MOUSE_H, "RMB", rmbCps + " CPS", rmbDown);

        // 4. Row 4: [ ──────── ] Spacebar
        int row4Y = row3Y + MOUSE_H + GAP;
        drawSpacebar(painter, x, row4Y, TOTAL_W, SPACE_H, spaceDown);
    }

    private void drawKeycap(Painter painter, int kx, int ky, int kw, int kh, String text, boolean down) {
        int bg = down ? Tokens.Color.ACCENT : Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.78f);
        int border = down ? Tokens.Color.ACCENT_PRESSED : Tokens.Color.LINE_HAIRLINE;
        int ink = down ? Tokens.Color.INK_ON_ACCENT : Tokens.Color.INK_PRIMARY;

        painter.fill(kx, ky, kw, kh, Tokens.Radius.SM, bg);
        painter.border(kx, ky, kw, kh, Tokens.Radius.SM, Tokens.Stroke.HAIR, border);

        int textW = Typeset.width(Tokens.Type.BODY_STRONG, text);
        int textX = kx + (kw - textW) / 2;
        int textY = Typeset.centred(Tokens.Type.BODY_STRONG, ky, kh);
        Typeset.draw(painter, Tokens.Type.BODY_STRONG, text, textX, textY, ink);
    }

    private void drawMouseKeycap(Painter painter, int kx, int ky, int kw, int kh, String label, String sub, boolean down) {
        int bg = down ? Tokens.Color.ACCENT : Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.78f);
        int border = down ? Tokens.Color.ACCENT_PRESSED : Tokens.Color.LINE_HAIRLINE;
        int labelInk = down ? Tokens.Color.INK_ON_ACCENT : Tokens.Color.INK_TERTIARY;
        int subInk = down ? Tokens.Color.INK_ON_ACCENT : Tokens.Color.INK_PRIMARY;

        painter.fill(kx, ky, kw, kh, Tokens.Radius.SM, bg);
        painter.border(kx, ky, kw, kh, Tokens.Radius.SM, Tokens.Stroke.HAIR, border);

        // Top line: LMB / RMB
        int labelW = Typeset.width(Tokens.Type.LABEL, label);
        int labelX = kx + (kw - labelW) / 2;
        int labelY = ky + 3;
        Typeset.draw(painter, Tokens.Type.LABEL, label, labelX, labelY, labelInk);

        // Bottom line: XX CPS
        int subW = Typeset.width(Tokens.Type.LABEL, sub);
        int subX = kx + (kw - subW) / 2;
        int subY = ky + 13;
        Typeset.draw(painter, Tokens.Type.LABEL, sub, subX, subY, subInk);
    }

    private void drawSpacebar(Painter painter, int kx, int ky, int kw, int kh, boolean down) {
        int bg = down ? Tokens.Color.ACCENT : Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.78f);
        int border = down ? Tokens.Color.ACCENT_PRESSED : Tokens.Color.LINE_HAIRLINE;
        int barColor = down ? Tokens.Color.INK_ON_ACCENT : Tokens.Color.LINE_STRONG;

        painter.fill(kx, ky, kw, kh, Tokens.Radius.SM, bg);
        painter.border(kx, ky, kw, kh, Tokens.Radius.SM, Tokens.Stroke.HAIR, border);

        // Centered space indicator bar
        int barW = kw - 24;
        int barX = kx + (kw - barW) / 2;
        int barY = ky + (kh - 2) / 2;
        painter.fill(barX, barY, barW, 2, 1, barColor);
    }
}
