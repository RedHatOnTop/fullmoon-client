package dev.fullmoon.client.menu;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.render.Painter;

/**
 * Hand-drawn 14×14 pixel marks for the casino's six games — the same idiom as
 * the vanilla HUD's own sprites, so they read as the game's craft rather than
 * chrome drawn on top of it. A raw item sprite says "block", not "game". The
 * server names the mark through the menu item's icon id; anything it does not
 * name falls back to the item render in {@link ServerMenuEntry}.
 */
final class MenuIcons {
    private static final float CELL_INSET = 0.86f;

    /** A gold coin with a milled rim and a crescent stamped in the middle. */
    private static final String[] COIN = {
        "..............",
        "....GGGGGG....",
        "..GGWWGGGGGg..",
        ".GWWWGGGGGGgg.",
        ".GWGGGGddddgg.",
        ".GWGGddGGGGgg.",
        ".GWGGdGGGGGgg.",
        ".GWGGdGGGGGgg.",
        ".GWGGddGGGGgg.",
        ".GWGGGGddddgg.",
        "..GWGGGGGGGg..",
        "..GWGGGGGGGg..",
        "...GWGGGGgg...",
        "....Gggggg....",
    };

    /** An ivory die showing five, shaded at its lower right. */
    private static final String[] DICE = {
        "..............",
        "..............",
        "...WWWWWWWW...",
        "..WWWWWWWWWWw.",
        "..WddWWWWddWw.",
        "..WddWWWWddWw.",
        "..WWWWWddWWWw.",
        "..WWWWWddWWWw.",
        "..WddWWWWddWw.",
        "..WddWWWWddWw.",
        "..WWWWWWWWWWw.",
        "...wwwwwwww...",
        "..............",
        "..............",
    };

    /** A roulette wheel: gold rim, red cardinal and dark diagonal pockets, ivory hub. */
    private static final String[] ROULETTE = {
        "..............",
        "....GGGGGG....",
        "..GGdRRRRdGG..",
        ".GGdRRRRRRdGG.",
        ".GGdRRGGGGdGG.",
        ".GGRGWWWWWGRG.",
        ".GRGGWWGWWGRG.",
        ".GRGGWWGWWGRG.",
        ".GRGGWWGWWGRG.",
        ".GGRGWWWWWGRG.",
        ".GGdRRGGGGdGG.",
        ".GGdRRRRRRdGG.",
        "..GGdRRRRdGG..",
        "....GGGGGG....",
    };

    /** Triple seven across the payline. */
    private static final String[] SLOTS = {
        "..............",
        "..............",
        "..............",
        "..............",
        "GGGG.GGGG.GGGG",
        "...G....G....G",
        "..G....G....G.",
        ".G....G....G..",
        "G....G....G...",
        "..............",
        "..............",
        "..............",
        "..............",
        "..............",
    };

    /** The ivory moon with gold light streaking down past it. */
    private static final String[] MOONFALL = {
        "..............",
        "...WWWW.......",
        ".WWWWWW...G...",
        "WWWWWW....G...",
        "WWWWW.........",
        "WWWW....G.....",
        "WWWW.....G....",
        "WWWWW.........",
        "WWWWWW....G...",
        ".WWWWWW....G..",
        "...WWWW.......",
        "..............",
        "..............",
        "..............",
    };

    /** A four-point jackpot sparkle with a lit core and four sparks. */
    private static final String[] JACKPOT = {
        "..............",
        ".......G......",
        "..w....G....w.",
        ".......G......",
        "......GGG.....",
        ".....GGGGG....",
        "....GGGGGGG...",
        "GGGGGGGWGGGGGG",
        "....GGGGGGG...",
        ".....GGGGG....",
        "......GGG.....",
        "..w....G....w.",
        ".......G......",
        ".......G......",
    };

    private MenuIcons() {}

    /** Draws the mark centred on {@code cx},{@code cy} in a {@code size} box; false if unknown. */
    static boolean draw(Painter painter, String icon, float cx, float cy, float size) {
        String[] art = switch (icon) {
            case "fullmoon.casino.coinflip" -> COIN;
            case "fullmoon.casino.dice" -> DICE;
            case "fullmoon.casino.roulette" -> ROULETTE;
            case "fullmoon.casino.slots" -> SLOTS;
            case "fullmoon.casino.moonfall" -> MOONFALL;
            case "fullmoon.casino.jackpot" -> JACKPOT;
            default -> null;
        };
        if (art == null) {
            return false;
        }
        float cell = size * CELL_INSET / art.length;
        float left = cx - art.length * cell / 2;
        float top = cy - art[0].length() * cell / 2;
        float pad = cell + Math.max(0.02f, cell * 0.02f);
        for (int row = 0; row < art.length; row++) {
            for (int col = 0; col < art[row].length(); col++) {
                int color = color(art[row].charAt(col));
                if (color != 0) {
                    painter.fill(left + col * cell, top + row * cell, pad, pad, color);
                }
            }
        }
        return true;
    }

    private static int color(char pixel) {
        return switch (pixel) {
            case 'G' -> Tokens.Color.ACCENT;
            case 'g' -> Tokens.Color.ACCENT_PRESSED;
            case 'W' -> Tokens.Color.INK_PRIMARY;
            case 'w' -> Tokens.Color.INK_SECONDARY;
            case 'd' -> Tokens.Color.INK_ON_ACCENT;
            case 'R' -> Tokens.Color.STATUS_DANGER;
            default -> 0;
        };
    }
}
