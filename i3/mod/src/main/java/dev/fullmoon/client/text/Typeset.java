package dev.fullmoon.client.text;

import java.util.HashMap;
import java.util.Map;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.render.Painter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The client's text layer: role in, glyphs out.
 *
 * <p>Two things the game will not do for us live here. It draws UI text with a hard drop
 * shadow by default, which is the one visual habit that marks a screen as vanilla chrome, so
 * every call in this class passes {@code shadow = false}. And its proportional digits jitter
 * a live counter by a pixel or two per frame, so {@link #tabular} lays digits out on a fixed
 * cell — the widest digit in the role — and leaves everything else on its natural advance.
 *
 * <p>{@code Font.lineHeight} is the constant 9 for every font in the game, including ours, so
 * vertical rhythm comes from {@link Tokens.Type.Role#leading()} and never from the font.
 */
public final class Typeset {
    private static final Map<String, Style> STYLES = new HashMap<>();
    private static final Map<String, Integer> DIGIT_CELLS = new HashMap<>();

    private Typeset() {}

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static Style style(Tokens.Type.Role role) {
        return STYLES.computeIfAbsent(role.font(),
            id -> Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(id))));
    }

    /** The role's text as a component, for the game's own text and tooltip APIs. */
    public static Component say(Tokens.Type.Role role, String text) {
        return Component.literal(text).withStyle(style(role));
    }

    public static int width(Tokens.Type.Role role, String text) {
        return font().width(say(role, text));
    }

    /** Draws left-aligned from the text's top-left corner. */
    public static int draw(Painter painter, Tokens.Type.Role role, String text, int x, int y, int color) {
        painter.gfx().text(font(), say(role, text), x, y, color, false);
        return width(role, text);
    }

    /** Draws right-aligned so that the text ends at {@code right}. */
    public static int drawRight(Painter painter, Tokens.Type.Role role, String text, int right, int y, int color) {
        int w = width(role, text);
        draw(painter, role, text, right - w, y, color);
        return w;
    }

    /** Draws centred on {@code cx}. */
    public static int drawCentered(Painter painter, Tokens.Type.Role role, String text, int cx, int y, int color) {
        int w = width(role, text);
        draw(painter, role, text, cx - w / 2, y, color);
        return w;
    }

    /** The advance of the widest digit in the role — one column of a tabular figure. */
    public static int digitCell(Tokens.Type.Role role) {
        return DIGIT_CELLS.computeIfAbsent(role.font(), id -> {
            int widest = 0;
            for (char digit = '0'; digit <= '9'; digit++) {
                widest = Math.max(widest, width(role, String.valueOf(digit)));
            }
            return widest;
        });
    }

    /** Advance of {@code text} once digits are forced onto the tabular cell. */
    public static int tabularWidth(Tokens.Type.Role role, String text) {
        int cell = digitCell(role);
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            total += isDigit(c) ? cell : width(role, String.valueOf(c));
        }
        return total;
    }

    /**
     * Draws with digits on a fixed cell, right-aligned inside it as figures are set. Use this
     * for anything that changes while the player is looking at it: counters, coordinates,
     * timers, ping.
     */
    public static int tabular(Painter painter, Tokens.Type.Role role, String text, int x, int y, int color) {
        int cell = digitCell(role);
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            if (isDigit(text.charAt(i))) {
                draw(painter, role, glyph, cursor + cell - width(role, glyph), y, color);
                cursor += cell;
            } else {
                cursor += draw(painter, role, glyph, cursor, y, color);
            }
        }
        return cursor - x;
    }

    /** As {@link #tabular}, ending at {@code right}. Keeps a changing value's last digit still. */
    public static int tabularRight(Painter painter, Tokens.Type.Role role, String text, int right, int y, int color) {
        int w = tabularWidth(role, text);
        tabular(painter, role, text, right - w, y, color);
        return w;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Drops the memoised metrics. Called on a resource reload, when the atlases change. */
    public static void invalidate() {
        DIGIT_CELLS.clear();
    }
}
