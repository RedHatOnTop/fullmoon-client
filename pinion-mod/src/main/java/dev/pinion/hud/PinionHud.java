package dev.pinion.hud;

import dev.pinion.hud.HudSettings.Module;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Everything the client draws over the game.
 *
 *  The readouts share one plate treatment — ink backdrop, ember rule down the
 *  left, label in ember and value in bone — so the HUD reads as one product
 *  rather than as five debug lines that happen to be on screen together. It is
 *  the launcher's ember palette, on purpose: the two halves are one client. */
public final class PinionHud {
    private static final int INK = 0xB4_0F100F;
    private static final int EMBER = 0xFF_B0481A;
    private static final int EMBER_DIM = 0xFF_8E3813;
    private static final int BONE = 0xFF_F2EFEA;
    private static final int MUTED = 0xFF_9A968F;

    private static final int PAD_X = 4;
    private static final int PAD_Y = 3;
    private static final int KEY = 17;
    private static final int KEY_GAP = 2;

    private PinionHud() {
    }

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        HudSettings.poll();

        Font font = mc.font;
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();

        fps(gfx, font, mc, w, h);
        cps(gfx, font, w, h);
        coords(gfx, font, mc, w, h);
        ping(gfx, font, mc, w, h);
        keystrokes(gfx, font, mc, w, h);
    }

    // ── modules ───────────────────────────────────────────────────

    private static void fps(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("fps");
        if (!m.enabled()) {
            return;
        }
        readout(gfx, font, m, w, h, "FPS", Integer.toString(mc.getFps()));
    }

    private static void cps(GuiGraphicsExtractor gfx, Font font, int w, int h) {
        Module m = HudSettings.get("cps");
        if (!m.enabled()) {
            return;
        }
        int left = PinionClient.LEFT_CLICKS.perSecond();
        int right = PinionClient.RIGHT_CLICKS.perSecond();
        readout(gfx, font, m, w, h, "CPS", left + " / " + right);
    }

    private static void coords(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("coords");
        if (!m.enabled()) {
            return;
        }
        Vec3 p = mc.player.position();
        String value = String.format(Locale.ROOT, "%.0f %.0f %.0f", p.x, p.y, p.z);
        readout(gfx, font, m, w, h, "XYZ", value);
    }

    private static void ping(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("ping");
        if (!m.enabled()) {
            return;
        }
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) {
            return;
        }
        PlayerInfo self = conn.getPlayerInfo(mc.player.getUUID());
        if (self == null) {
            return;
        }
        readout(gfx, font, m, w, h, "PING", self.getLatency() + "ms");
    }

    /** The one module that is a diagram rather than a line of text. */
    private static void keystrokes(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("keystrokes");
        if (!m.enabled()) {
            return;
        }
        int padW = KEY * 3 + KEY_GAP * 2;
        int padH = KEY * 3 + KEY_GAP * 2 + 6 + KEY;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, padW, padH);

        var o = mc.options;
        key(gfx, font, KEY + KEY_GAP, 0, "W", o.keyUp.isDown());
        key(gfx, font, 0, KEY + KEY_GAP, "A", o.keyLeft.isDown());
        key(gfx, font, KEY + KEY_GAP, KEY + KEY_GAP, "S", o.keyDown.isDown());
        key(gfx, font, (KEY + KEY_GAP) * 2, KEY + KEY_GAP, "D", o.keyRight.isDown());

        int row = (KEY + KEY_GAP) * 2;
        wideKey(gfx, font, 0, row, (padW - KEY_GAP) / 2, "LMB", mc.mouseHandler.isLeftPressed());
        wideKey(gfx, font, (padW - KEY_GAP) / 2 + KEY_GAP, row, padW - (padW - KEY_GAP) / 2 - KEY_GAP,
                "RMB", mc.mouseHandler.isRightPressed());

        wideKey(gfx, font, 0, row + KEY + KEY_GAP, padW, "—", o.keyJump.isDown());
        gfx.pose().popMatrix();
    }

    // ── drawing ───────────────────────────────────────────────────

    /** Label + value on one plate. Modules past the middle of the screen grow
     *  leftwards so a right-hand layout cannot run off the edge. */
    private static void readout(
            GuiGraphicsExtractor gfx, Font font, Module m, int w, int h, String label, String value) {
        int textW = font.width(label) + 5 + font.width(value);
        int plateW = textW + PAD_X * 2;
        int plateH = font.lineHeight + PAD_Y * 2;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, plateW, plateH);

        gfx.fill(0, 0, plateW, plateH, INK);
        gfx.fill(0, 0, 1, plateH, EMBER);

        gfx.text(font, label, PAD_X, PAD_Y, EMBER, false);
        gfx.text(font, value, PAD_X + font.width(label) + 5, PAD_Y, BONE, false);
        gfx.pose().popMatrix();
    }

    private static void key(GuiGraphicsExtractor gfx, Font font, int x, int y, String cap, boolean down) {
        wideKey(gfx, font, x, y, KEY, cap, down);
    }

    private static void wideKey(
            GuiGraphicsExtractor gfx, Font font, int x, int y, int width, String cap, boolean down) {
        gfx.fill(x, y, x + width, y + KEY, down ? EMBER : INK);
        gfx.fill(x, y, x + width, y + 1, down ? EMBER_DIM : EMBER_DIM);
        int textX = x + (width - font.width(cap)) / 2;
        int textY = y + (KEY - font.lineHeight) / 2 + 1;
        gfx.text(font, cap, textX, textY, down ? 0xFF_1A0E07 : MUTED, false);
    }

    /** Percent-of-screen placement, kept fully on screen, then scaled about
     *  its own corner so a scaled module stays where the editor put it. */
    private static void anchor(
            GuiGraphicsExtractor gfx, Module m, int w, int h, int contentW, int contentH) {
        float scale = m.scale() <= 0 ? 1f : m.scale();
        float sw = contentW * scale;
        float sh = contentH * scale;

        float x = w * m.x() / 100f;
        float y = h * m.y() / 100f;
        if (m.x() > 50f) {
            x -= sw;
        }
        x = Math.max(2f, Math.min(x, w - sw - 2f));
        y = Math.max(2f, Math.min(y, h - sh - 2f));

        gfx.pose().translate(x, y);
        gfx.pose().scale(scale, scale);
    }
}
