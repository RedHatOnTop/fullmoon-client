package dev.pinion.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The client's drawing kit.
 *
 *  Both halves of Pinion — the readouts over the game and the panel over those
 *  — are built out of these few primitives, which is the only reason the two
 *  read as one product. The palette is the launcher's `tokens.css` dark theme
 *  value for value; a colour that exists in only one of the two halves is a
 *  colour that will drift.
 *
 *  Nothing here paints over the backdrop to fake a rounded corner. A panel
 *  floats above a running game, so corners are <em>dropped</em> rather than
 *  covered: {@link #rect} steps its first and last rows inward. */
public final class Ui {
    // ── palette — fullmoon.ink tokens.css dark theme ──────────────
    public static final int INK = 0xFF0B101F;
    public static final int SURFACE = 0xFF151B2E;
    public static final int SUNKEN = 0xFF0F1526;
    public static final int OVERLAY = 0xFF1B2340;
    public static final int LINE = 0xFF232B47;
    public static final int LINE_STRONG = 0xFF30395C;
    public static final int TEXT = 0xFFF4F6FB;
    public static final int TEXT_2 = 0xFF9AA3B8;
    public static final int TEXT_3 = 0xFF6B7490;
    public static final int MOON = 0xFFF5D06E;
    public static final int MOON_LIT = 0xFFFFE9B0;
    public static final int MOON_PALE = 0xFFFAE7A8;
    public static final int MOON_DEEP = 0xFF6E5619;
    public static final int OCHRE = 0xFFC9A33F;
    public static final int POPPY = 0xFFD97D72;

    private Ui() {
    }

    // ── colour ────────────────────────────────────────────────────

    public static int alpha(int argb, float a) {
        int aa = (int) (((argb >>> 24) & 0xFF) * clamp(a, 0f, 1f));
        return (aa << 24) | (argb & 0x00FFFFFF);
    }

    public static int lerp(float t, int from, int to) {
        t = clamp(t, 0f, 1f);
        int a = mix(t, from >>> 24, to >>> 24);
        int r = mix(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = mix(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = mix(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int mix(float t, int from, int to) {
        return Math.round(from + (to - from) * t);
    }

    // ── motion ────────────────────────────────────────────────────

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    /** The launcher's `--ease-out`, near enough: fast out of the gate, long
     *  settle. Anything linear reads as a jump cut at 60 Hz. */
    public static float ease(float t) {
        t = clamp(t, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** Frame-rate independent approach — a hover that takes the same 120 ms at
     *  30 fps and at 240. */
    public static float approach(float now, float target, float dt, float rate) {
        float k = 1f - (float) Math.exp(-rate * dt);
        return now + (target - now) * k;
    }

    // ── shapes ────────────────────────────────────────────────────

    /** Filled rect whose corners step inward by `r` pixels. */
    public static void rect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int fill, int r) {
        if (w <= 0 || h <= 0 || (fill >>> 24) == 0) {
            return;
        }
        for (int i = 0; i < r; i++) {
            int in = r - i;
            gfx.fill(x + in, y + i, x + w - in, y + i + 1, fill);
            gfx.fill(x + in, y + h - i - 1, x + w - in, y + h - i, fill);
        }
        gfx.fill(x, y + r, x + w, y + h - r, fill);
    }

    /** The matching 1px outline. Drawn as edges plus the corner staircase, so
     *  it lands exactly on the silhouette {@link #rect} produced. */
    public static void border(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color, int r) {
        if ((color >>> 24) == 0) {
            return;
        }
        gfx.fill(x + r + 1, y, x + w - r - 1, y + 1, color);
        gfx.fill(x + r + 1, y + h - 1, x + w - r - 1, y + h, color);
        gfx.fill(x, y + r + 1, x + 1, y + h - r - 1, color);
        gfx.fill(x + w - 1, y + r + 1, x + w, y + h - r - 1, color);
        for (int i = 0; i <= r; i++) {
            int in = r - i;
            gfx.fill(x + in, y + i, x + in + 1, y + i + 1, color);
            gfx.fill(x + w - in - 1, y + i, x + w - in, y + i + 1, color);
            gfx.fill(x + in, y + h - i - 1, x + in + 1, y + h - i, color);
            gfx.fill(x + w - in - 1, y + h - i - 1, x + w - in, y + h - i, color);
        }
    }

    /** Three widening rings of near-black under a floating surface. The game
     *  behind is arbitrary — bright sky, dark cave — and a panel with no
     *  shadow loses its edge against both. */
    public static void shadow(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r, float a) {
        for (int k = 3; k >= 1; k--) {
            rect(gfx, x - k, y - k + 1, w + k * 2, h + k * 2, alpha(0xFF000000, 0.13f * a), r + k);
        }
    }

    /** Left-to-right ramp. `fillGradient` only runs top to bottom, and a
     *  header rule that fades sideways is worth the handful of strips. */
    public static void hGradient(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int from, int to) {
        int steps = Math.max(1, Math.min(w, 28));
        for (int i = 0; i < steps; i++) {
            int x0 = x + w * i / steps;
            int x1 = x + w * (i + 1) / steps;
            if (x1 > x0) {
                gfx.fill(x0, y, x1, y + h, lerp((i + 0.5f) / steps, from, to));
            }
        }
    }

    /** Sliding switch. `t` is the animated position, not the state, so a row
     *  that was just clicked shows the knob travelling. */
    public static void pill(GuiGraphicsExtractor gfx, int x, int y, int w, int h, float t, float a) {
        int track = lerp(t, alpha(LINE, a), alpha(MOON_DEEP, a));
        rect(gfx, x, y, w, h, track, 1);
        border(gfx, x, y, w, h, lerp(t, alpha(LINE_STRONG, a * 0.8f), alpha(MOON, a)), 1);
        int knobW = h - 2;
        int knobX = Math.round(x + 1 + (w - knobW - 2) * t);
        rect(gfx, knobX, y + 1, knobW, h - 2, lerp(t, alpha(TEXT_3, a), alpha(MOON_PALE, a)), 1);
    }

    // ── text ──────────────────────────────────────────────────────

    /** Letter-spaced caps. The launcher's overlines carry 0.14em of tracking
     *  and the game font has none, so the spacing is dealt out by hand — it is
     *  what keeps a header from reading as chat. */
    public static int tracked(GuiGraphicsExtractor gfx, Font font, String s, int x, int y, int color, int gap) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            gfx.text(font, ch, cx, y, color, false);
            cx += font.width(ch) + gap;
        }
        return cx - x - gap;
    }

    public static int trackedWidth(Font font, String s, int gap) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += font.width(s.substring(i, i + 1)) + gap;
        }
        return Math.max(0, w - gap);
    }

    public static void rightText(GuiGraphicsExtractor gfx, Font font, String s, int right, int y, int color, boolean shadow) {
        gfx.text(font, s, right - font.width(s), y, color, shadow);
    }
}
