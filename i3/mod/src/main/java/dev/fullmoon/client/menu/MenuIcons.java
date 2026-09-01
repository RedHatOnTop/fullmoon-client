package dev.fullmoon.client.menu;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.render.Painter;

/**
 * Bespoke vector marks for the casino's six games, drawn from the same SDF
 * primitives as the rest of the chrome. A raw item sprite says "block", not
 * "game"; a coin, a die and a wheel say what the tile plays. The server names
 * the mark through the menu item's icon id — anything it does not name falls
 * back to the item render in {@link ServerMenuEntry}.
 */
final class MenuIcons {
    private MenuIcons() {}

    /** Draws the mark centred on {@code cx},{@code cy} in a {@code size} box; false if unknown. */
    static boolean draw(Painter painter, String icon, float cx, float cy, float size) {
        switch (icon) {
            case "fullmoon.casino.coinflip" -> coin(painter, cx, cy, size);
            case "fullmoon.casino.dice" -> die(painter, cx, cy, size);
            case "fullmoon.casino.roulette" -> wheel(painter, cx, cy, size);
            case "fullmoon.casino.slots" -> reels(painter, cx, cy, size);
            case "fullmoon.casino.moonfall" -> moonfall(painter, cx, cy, size);
            case "fullmoon.casino.jackpot" -> burst(painter, cx, cy, size);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** A gold coin: the disc and its milled edge ring. */
    private static void coin(Painter painter, float cx, float cy, float size) {
        float r = size * 0.42f;
        painter.dot(cx, cy, r, Tokens.Color.ACCENT);
        painter.ring(cx, cy, r * 0.55f, Math.max(1f, size * 0.07f), Tokens.Color.INK_ON_ACCENT);
    }

    /** An ivory die showing three. */
    private static void die(Painter painter, float cx, float cy, float size) {
        float half = size * 0.36f;
        float pip = Math.max(1f, size * 0.09f);
        painter.fill(cx - half, cy - half, half * 2, half * 2, size * 0.14f,
            Tokens.Color.INK_PRIMARY);
        painter.dot(cx - half * 0.5f, cy - half * 0.5f, pip, Tokens.Color.INK_ON_ACCENT);
        painter.dot(cx, cy, pip, Tokens.Color.INK_ON_ACCENT);
        painter.dot(cx + half * 0.5f, cy + half * 0.5f, pip, Tokens.Color.INK_ON_ACCENT);
    }

    /** A roulette wheel: red rim, eight alternating pockets, gold spindle. */
    private static void wheel(Painter painter, float cx, float cy, float size) {
        float r = size * 0.42f;
        painter.ring(cx, cy, r, Math.max(1f, size * 0.1f), Tokens.Color.STATUS_DANGER);
        float pocket = Math.max(1f, size * 0.08f);
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI / 4 * k + Math.PI / 8;
            float px = cx + (float) Math.cos(angle) * r * 0.58f;
            float py = cy + (float) Math.sin(angle) * r * 0.58f;
            painter.dot(px, py, pocket, k % 2 == 0
                ? Tokens.Color.INK_PRIMARY : Tokens.Color.SURFACE_OVERLAY);
        }
        painter.dot(cx, cy, Math.max(1f, size * 0.11f), Tokens.Color.ACCENT);
    }

    /** Three reel windows on a payline; the middle one lands gold. */
    private static void reels(Painter painter, float cx, float cy, float size) {
        float w = size * 0.24f;
        float h = size * 0.58f;
        float step = size * 0.34f;
        for (int off = -1; off <= 1; off++) {
            float x = cx + off * step - w / 2;
            painter.fill(x, cy - h / 2, w, h, Math.max(1f, size * 0.06f),
                Tokens.Color.SURFACE_OVERLAY);
            painter.border(x, cy - h / 2, w, h, Math.max(1f, size * 0.06f),
                Tokens.Stroke.HAIR, Tokens.Color.LINE_STRONG);
            painter.dot(x + w / 2, cy, Math.max(1f, size * 0.08f),
                off == 0 ? Tokens.Color.ACCENT : Tokens.Color.INK_SECONDARY);
        }
    }

    /** The moon up top and three shards of light falling past it. */
    private static void moonfall(Painter painter, float cx, float cy, float size) {
        painter.dot(cx, cy - size * 0.2f, size * 0.26f, Tokens.Color.ACCENT);
        painter.dot(cx - size * 0.28f, cy + size * 0.14f, Math.max(1f, size * 0.07f),
            Tokens.Color.INK_SECONDARY);
        painter.dot(cx + size * 0.04f, cy + size * 0.34f, Math.max(1f, size * 0.09f),
            Tokens.Color.ACCENT_PRESSED);
        painter.dot(cx + size * 0.3f, cy + size * 0.1f, Math.max(1f, size * 0.06f),
            Tokens.Color.INK_TERTIARY);
    }

    /** A jackpot burst: gold core, four rays, four sparks. */
    private static void burst(Painter painter, float cx, float cy, float size) {
        float ray = size * 0.18f;
        painter.dot(cx, cy, size * 0.2f, Tokens.Color.ACCENT);
        painter.vRule(cx, cy - size * 0.44f, ray, Tokens.Color.ACCENT);
        painter.vRule(cx, cy + size * 0.44f - ray, ray, Tokens.Color.ACCENT);
        painter.hRule(cx - size * 0.44f, cy, ray, Tokens.Color.ACCENT);
        painter.hRule(cx + size * 0.44f - ray, cy, ray, Tokens.Color.ACCENT);
        float d = size * 0.3f;
        float spark = Math.max(1f, size * 0.06f);
        painter.dot(cx - d, cy - d, spark, Tokens.Color.ACCENT_PRESSED);
        painter.dot(cx + d, cy - d, spark, Tokens.Color.ACCENT_PRESSED);
        painter.dot(cx - d, cy + d, spark, Tokens.Color.ACCENT_PRESSED);
        painter.dot(cx + d, cy + d, spark, Tokens.Color.ACCENT_PRESSED);
    }
}
