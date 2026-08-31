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
import net.minecraft.util.FormattedCharSequence;

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
    /**
     * Distance from a draw origin to the baseline the glyphs actually sit on: the ascent of the
     * game's one 9 px line box. It does not scale with the provider, so a 22 px face draws well
     * above its origin and a band that wants a face centred in it has to work from here.
     */
    private static final int ASCENT = 7;

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

    /** The longest complete-code-point prefix that fits inside {@code width}. */
    public static String fittingPrefix(Tokens.Type.Role role, String text, int width) {
        int end = 0;
        while (end < text.length()) {
            int next = end + Character.charCount(text.codePointAt(end));
            if (width(role, text.substring(0, next)) > width) {
                break;
            }
            end = next;
        }
        return text.substring(0, end);
    }

    /** Draws left-aligned from the text's top-left corner. */
    public static int draw(Painter painter, Tokens.Type.Role role, String text, int x, int y, int color) {
        int measured = width(role, text);
        painter.gfx().nextStratum();
        drawRaw(painter, role, text, x, y, color);
        painter.gfx().nextStratum();
        return measured;
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

    /** Draws at most {@code maxLines} on the role's leading and returns the height it used. */
    public static int drawWrapped(Painter painter, Tokens.Type.Role role, String text, int x, int y,
            int width, int maxLines, int color) {
        java.util.List<FormattedCharSequence> lines = font().split(say(role, text), width);
        int shown = Math.min(maxLines, lines.size());
        painter.gfx().nextStratum();
        for (int i = 0; i < shown; i++) {
            painter.gfx().text(font(), lines.get(i), x, y + i * role.leading(), color, false);
        }
        painter.gfx().nextStratum();
        return shown * role.leading();
    }

    /**
     * The draw origin that puts a role's ink in the optical middle of a band {@code h} tall,
     * starting at {@code top}. A text face carries about a quarter of its size below the
     * baseline, so its middle sits that far above it.
     */
    public static int centred(Tokens.Type.Role role, int top, int h) {
        return top + h / 2 + role.px() / 4 - ASCENT;
    }

    /**
     * The height of a role's capitals: the cap line down to the baseline. Same model as
     * {@link #centred} — a face carries about a quarter of its size below the baseline and the
     * rest of it above.
     */
    public static int capHeight(Tokens.Type.Role role) {
        return role.px() - role.px() / 4;
    }

    /**
     * The y a role's capitals begin at when it is drawn at origin {@code y}. A rule that stands
     * beside a wordmark has to start here and not at the origin: the line box is 9 px whatever
     * the face is, so a large role draws most of its body above the origin it was handed.
     */
    public static int capTop(Tokens.Type.Role role, int y) {
        return y + ASCENT - capHeight(role);
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
     * Draws with digits on a fixed cell, centred in it the way a tabular face cuts its own
     * figures — a narrow 1 pushed to one side of the cell reads as a word space. Use this
     * for anything that changes while the player is looking at it: counters, coordinates,
     * timers, ping.
     */
    public static int tabular(Painter painter, Tokens.Type.Role role, String text, int x, int y, int color) {
        int cell = digitCell(role);
        int cursor = x;
        painter.gfx().nextStratum();
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            if (isDigit(text.charAt(i))) {
                drawRaw(painter, role, glyph, cursor + (cell - width(role, glyph)) / 2, y, color);
                cursor += cell;
            } else {
                drawRaw(painter, role, glyph, cursor, y, color);
                cursor += width(role, glyph);
            }
        }
        painter.gfx().nextStratum();
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

    /** Text gets its own strata so pipeline batching cannot move a later solid in front of it. */
    private static void drawRaw(Painter painter, Tokens.Type.Role role, String text,
            int x, int y, int color) {
        painter.gfx().text(font(), say(role, text), x, y, color, false);
    }

    /** Drops the memoised metrics. Called on a resource reload, when the atlases change. */
    public static void invalidate() {
        DIGIT_CELLS.clear();
    }
}
