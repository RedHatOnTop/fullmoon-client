package dev.pinion.hud;

import dev.pinion.hud.HudSettings.Module;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Everything the client draws over the game.
 *
 *  Every module sits on the same plate — chamfered ink, an ember rule down the
 *  left that fades as it falls, label in tracked caps and value in bone — so
 *  the HUD reads as one product rather than as seven debug lines that happen to
 *  share a screen. It is {@link Ui}'s palette, which is the launcher's, on
 *  purpose: the two halves are one client.
 *
 *  Numbers that mean something bad look bad. Frame rate and ping cross into
 *  ochre and then poppy, so a stutter is visible in the corner of the eye
 *  without reading the digits. */
public final class PinionHud {
    private static final int PLATE = 0xB80F100F;
    private static final int PAD_X = 5;
    private static final int PAD_Y = 4;
    private static final int KEY = 17;
    private static final int KEY_GAP = 2;

    /** W A S D, left, right, jump — parallel to {@link #KEY_ORDER} */
    private static final float[] KEY_GLOW = new float[7];
    private static final String[] KEY_ORDER = {"W", "A", "S", "D", "LMB", "RMB", "—"};

    private static long lastFrame = System.currentTimeMillis();

    private PinionHud() {
    }

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        HudSettings.poll();

        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        Font font = mc.font;
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();

        fps(gfx, font, mc, w, h);
        cps(gfx, font, w, h);
        coords(gfx, font, mc, w, h);
        ping(gfx, font, mc, w, h);
        keystrokes(gfx, font, mc, w, h, dt);
        gear(gfx, font, mc, w, h);
        potion(gfx, font, mc, w, h);
    }

    // ── modules ───────────────────────────────────────────────────

    private static void fps(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("fps");
        if (!m.enabled()) {
            return;
        }
        int fps = mc.getFps();
        readout(gfx, font, m, w, h, "FPS", Integer.toString(fps), grade(fps, 55, 28), null);
    }

    private static void cps(GuiGraphicsExtractor gfx, Font font, int w, int h) {
        Module m = HudSettings.get("cps");
        if (!m.enabled()) {
            return;
        }
        int left = PinionClient.LEFT_CLICKS.perSecond();
        int right = PinionClient.RIGHT_CLICKS.perSecond();
        readout(gfx, font, m, w, h, "CPS", left + " / " + right, Ui.TEXT, null);
    }

    private static void coords(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("coords");
        if (!m.enabled()) {
            return;
        }
        Vec3 p = mc.player.position();
        String value = String.format(Locale.ROOT, "%.0f %.0f %.0f", p.x, p.y, p.z);
        readout(gfx, font, m, w, h, "XYZ", value, Ui.TEXT, facing(mc.player.getYRot()));
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
        int ms = self.getLatency();
        readout(gfx, font, m, w, h, "PING", ms + "ms", grade(-ms, -110, -240), null);
    }

    /** The one module that is a diagram rather than a line of text. A released
     *  key fades rather than snapping back, so a fast rotation still reads as
     *  movement instead of as a flicker. */
    private static void keystrokes(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h, float dt) {
        Module m = HudSettings.get("keystrokes");
        if (!m.enabled()) {
            return;
        }
        var o = mc.options;
        boolean[] down = {
            o.keyUp.isDown(), o.keyLeft.isDown(), o.keyDown.isDown(), o.keyRight.isDown(),
            mc.mouseHandler.isLeftPressed(), mc.mouseHandler.isRightPressed(), o.keyJump.isDown(),
        };
        for (int i = 0; i < KEY_GLOW.length; i++) {
            KEY_GLOW[i] = down[i] ? 1f : Ui.approach(KEY_GLOW[i], 0f, dt, 9f);
        }

        int padW = KEY * 3 + KEY_GAP * 2;
        int padH = KEY * 3 + KEY_GAP * 2 + 6 + KEY;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, padW, padH);

        cap(gfx, font, KEY + KEY_GAP, 0, KEY, 0);
        cap(gfx, font, 0, KEY + KEY_GAP, KEY, 1);
        cap(gfx, font, KEY + KEY_GAP, KEY + KEY_GAP, KEY, 2);
        cap(gfx, font, (KEY + KEY_GAP) * 2, KEY + KEY_GAP, KEY, 3);

        int row = (KEY + KEY_GAP) * 2;
        int half = (padW - KEY_GAP) / 2;
        cap(gfx, font, 0, row, half, 4);
        cap(gfx, font, half + KEY_GAP, row, padW - half - KEY_GAP, 5);
        cap(gfx, font, 0, row + KEY + KEY_GAP, padW, 6);
        gfx.pose().popMatrix();
    }

    /** Armour, offhand and what is in the hand, each with the wear vanilla
     *  draws on an inventory slot. Empty slots are left out rather than drawn
     *  as holes — a naked player gets a two-item plate, not six blanks. */
    private static void gear(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("gear");
        if (!m.enabled()) {
            return;
        }
        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND,
        };
        List<ItemStack> held = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                held.add(stack);
            }
        }
        if (held.isEmpty()) {
            return;
        }

        int cell = 18;
        int plateW = held.size() * cell + PAD_X * 2 - 2;
        int plateH = cell + PAD_Y * 2 - 2;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, plateW, plateH);
        plate(gfx, plateW, plateH);
        for (int i = 0; i < held.size(); i++) {
            int x = PAD_X + i * cell - 1;
            int y = PAD_Y - 1;
            gfx.item(held.get(i), x, y);
            gfx.itemDecorations(font, held.get(i), x, y);
        }
        gfx.pose().popMatrix();
    }

    /** One line per effect: the effect's own colour as the rule, its name, and
     *  what is left of it right-aligned so the timers form a column. */
    private static void potion(GuiGraphicsExtractor gfx, Font font, Minecraft mc, int w, int h) {
        Module m = HudSettings.get("potion");
        if (!m.enabled()) {
            return;
        }
        List<MobEffectInstance> live = new ArrayList<>();
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            if (e.isVisible()) {
                live.add(e);
            }
        }
        if (live.isEmpty()) {
            return;
        }
        live.sort((a, b) -> Integer.compare(a.getDuration(), b.getDuration()));

        int lineH = 10;
        int gap = 8;
        int nameW = 0;
        int timeW = 0;
        for (MobEffectInstance e : live) {
            nameW = Math.max(nameW, font.width(effectName(e)));
            timeW = Math.max(timeW, font.width(remaining(e)));
        }
        int plateW = PAD_X * 2 + 5 + nameW + gap + timeW;
        int plateH = PAD_Y * 2 + live.size() * lineH - 2;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, plateW, plateH);
        plate(gfx, plateW, plateH);
        for (int i = 0; i < live.size(); i++) {
            MobEffectInstance e = live.get(i);
            int y = PAD_Y + i * lineH;
            int tint = 0xFF000000 | e.getEffect().value().getColor();
            gfx.fill(PAD_X, y + 1, PAD_X + 2, y + 7, tint);
            gfx.text(font, effectName(e), PAD_X + 5, y, Ui.TEXT, false);
            Ui.rightText(gfx, font, remaining(e), plateW - PAD_X, y,
                    e.getDuration() < 200 && !e.isInfiniteDuration() ? Ui.POPPY : Ui.TEXT_2, false);
        }
        gfx.pose().popMatrix();
    }

    // ── drawing ───────────────────────────────────────────────────

    /** Label, value, and an optional tail the module can use for context that
     *  is not the number itself — the compass letters, so far. */
    private static void readout(GuiGraphicsExtractor gfx, Font font, Module m, int w, int h,
                                String label, String value, int valueColor, String tail) {
        int labelW = Ui.trackedWidth(font, label, 1);
        int textW = labelW + 6 + font.width(value) + (tail == null ? 0 : 5 + font.width(tail));
        int plateW = textW + PAD_X * 2;
        int plateH = font.lineHeight + PAD_Y * 2 - 1;

        gfx.pose().pushMatrix();
        anchor(gfx, m, w, h, plateW, plateH);
        plate(gfx, plateW, plateH);

        int ty = PAD_Y - 1;
        Ui.tracked(gfx, font, label, PAD_X, ty, Ui.EMBER_PALE, 1);
        int vx = PAD_X + labelW + 6;
        gfx.text(font, value, vx, ty, valueColor, false);
        if (tail != null) {
            gfx.text(font, tail, vx + font.width(value) + 5, ty, Ui.TEXT_3, false);
        }
        gfx.pose().popMatrix();
    }

    /** The shared surface: chamfered ink, a hairline edge, and an ember rule
     *  down the left that fades out as it falls. */
    private static void plate(GuiGraphicsExtractor gfx, int w, int h) {
        Ui.rect(gfx, 0, 0, w, h, PLATE, 2);
        Ui.border(gfx, 0, 0, w, h, Ui.alpha(Ui.LINE_STRONG, 0.55f), 2);
        // inside the chamfer, or the rule juts past the plate's own silhouette
        gfx.fillGradient(0, 2, 1, h - 2, Ui.EMBER, Ui.alpha(Ui.EMBER_DEEP, 0.35f));
    }

    private static void cap(GuiGraphicsExtractor gfx, Font font, int x, int y, int width, int index) {
        float g = KEY_GLOW[index];
        String label = KEY_ORDER[index];
        Ui.rect(gfx, x, y, width, KEY, Ui.lerp(g, PLATE, Ui.EMBER), 2);
        Ui.border(gfx, x, y, width, KEY, Ui.lerp(g, Ui.alpha(Ui.LINE_STRONG, 0.6f), Ui.EMBER_PALE), 2);
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (KEY - font.lineHeight) / 2 + 1;
        gfx.text(font, label, textX, textY, Ui.lerp(g, Ui.TEXT_2, 0xFF1A0E07), false);
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

    // ── values ────────────────────────────────────────────────────

    /** Bone while the number is fine, ochre once it slips, poppy once it is a
     *  problem. Ping passes its value negated so one comparison serves both. */
    private static int grade(int value, int warn, int bad) {
        if (value <= bad) {
            return Ui.POPPY;
        }
        return value <= warn ? Ui.OCHRE : Ui.TEXT;
    }

    private static String facing(float yaw) {
        int i = Math.floorMod(Math.round(yaw / 45f), 8);
        return switch (i) {
            case 0 -> "S";
            case 1 -> "SW";
            case 2 -> "W";
            case 3 -> "NW";
            case 4 -> "N";
            case 5 -> "NE";
            case 6 -> "E";
            default -> "SE";
        };
    }

    private static String effectName(MobEffectInstance e) {
        String name = Component.translatable(e.getDescriptionId()).getString();
        int level = e.getAmplifier();
        if (level > 0 && level < 4) {
            name += " " + "II III IV".split(" ")[level - 1];
        } else if (level >= 4) {
            name += " " + (level + 1);
        }
        return name;
    }

    private static String remaining(MobEffectInstance e) {
        if (e.isInfiniteDuration()) {
            return "--:--";
        }
        int seconds = e.getDuration() / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
