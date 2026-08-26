package dev.pinion.bridge;

import dev.pinion.hud.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Locale;

/**
 * The casino's result card — the client half of `casino_result`.
 *
 * The server settles a bet and pushes one event; this draws it as a card that
 * slides up from the bottom-centre, plays its game's little reveal (the reels
 * spinning to a stop, the die tumbling, the wheel decelerating on the pocket),
 * then fades. A few seconds later there is nothing — the money already moved,
 * the chat line already said so, and a result banner has no business outstaying
 * its welcome.
 *
 * This is presentation only: the payload arrives after settlement, carries no
 * balance, and clicking does nothing. The chest GUI stays authoritative for
 * placing bets; this card is why a Fullmoon client sees the wheel.
 */
public final class CasinoResultCard {

    private static final long SPIN_MS = 1400;
    private static final long HOLD_MS = 2600;
    private static final long FADE_MS = 500;
    private static final long TOTAL_MS = SPIN_MS + HOLD_MS + FADE_MS;
    private static final int CARD_W = 190;
    private static final int CARD_H = 74;

    /** One live card at a time — bets are serialized server-side anyway. */
    private static CasinoResultCard active;

    private long startedAt = System.currentTimeMillis();
    private final String game;
    private final boolean won;
    private final double multiplier;
    private final List<String> reels;
    private final int roll;
    private final int target;
    private final int pocket;
    private final String betType;

    private CasinoResultCard(String game, boolean won, double multiplier,
                             List<String> reels, int roll, int target, int pocket, String betType) {
        this.game = game;
        this.won = won;
        this.multiplier = multiplier;
        this.reels = reels;
        this.roll = roll;
        this.target = target;
        this.pocket = pocket;
        this.betType = betType;
    }

    /** Registered once from the mod init: while any screen is open the HUD
     *  layer is hidden underneath it, and a bet result landing while the
     *  player is in the chest GUI is exactly the case that matters — so the
     *  card also draws after every screen render. */
    public static void registerScreenLayer() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register(
                (mc, scr, sx, sy) -> net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
                        .afterExtract(scr).register((s2, gfx, mx, my, delta) ->
                                render(gfx, s2.width, s2.height)));
    }

    /** Parse a `casino_result` payload; unknown games are ignored quietly. */
    public static void handle(com.google.gson.JsonObject json) {
        String game = str(json, "game");
        if (!game.equals("slots") && !game.equals("dice") && !game.equals("roulette")
                && !game.equals("coinflip")) {
            return;
        }
        boolean won = json.has("won") && json.get("won").getAsBoolean();
        double mult = json.has("payout_multiplier") ? json.get("payout_multiplier").getAsDouble() : 0.0;
        List<String> reels = null;
        int roll = -1, target = -1, pocket = -1;
        String betType = "";
        if (json.has("detail") && json.get("detail").isJsonObject()) {
            var d = json.getAsJsonObject("detail");
            if (d.has("reels") && d.get("reels").isJsonArray()) {
                var collected = new java.util.ArrayList<String>();
                d.getAsJsonArray("reels").forEach(el -> collected.add(el.getAsString()));
                reels = collected;
            }
            if (d.has("roll")) roll = d.get("roll").getAsInt();
            if (d.has("target")) target = d.get("target").getAsInt();
            if (d.has("pocket")) pocket = d.get("pocket").getAsInt();
            if (d.has("bet_type")) betType = d.get("bet_type").getAsString();
        }
        active = new CasinoResultCard(game, won, mult, reels, roll, target, pocket, betType);
    }

    /** Called from the HUD layer every frame; draws while a card is alive. */
    public static void render(GuiGraphicsExtractor gfx, int screenW, int screenH) {
        CasinoResultCard c = active;
        if (c == null) {
            if (Boolean.getBoolean("fullmoon.uiRig")) {
                // rig: one demo card so the reveal is reviewable without betting
                active = new CasinoResultCard("slots", true, 12.0,
                        List.of("seven", "seven", "star"), -1, -1, -1, "");
                active.startedAt = System.currentTimeMillis() + 1200; // brief pause first
                return;
            }
            return;
        }
        long now = System.currentTimeMillis() - c.startedAt;
        if (now > TOTAL_MS) {
            // rig: loop the demo so the reveal stays reviewable
            if (Boolean.getBoolean("fullmoon.uiRig") && c.game.equals("slots") && c.multiplier == 12.0) {
                active = new CasinoResultCard(c.game, c.won, c.multiplier,
                        c.reels, c.roll, c.target, c.pocket, c.betType);
                active.startedAt = System.currentTimeMillis() + 2500;
            } else {
                active = null;
            }
            return;
        }
        c.draw(gfx, Minecraft.getInstance().font, screenW, screenH, now);
    }

    // ── drawing ───────────────────────────────────────────────────

    private void draw(GuiGraphicsExtractor gfx, Font font, int screenW, int screenH, long now) {
        /* rise in over the spin, hold still, fade at the end */
        float arrive = Ui.ease(Math.min(1f, now / 450f));
        float fade = now > SPIN_MS + HOLD_MS
                ? 1f - (now - SPIN_MS - HOLD_MS) / (float) FADE_MS : 1f;
        float a = Math.min(arrive, Math.max(0f, fade));
        if (a <= 0.01f) {
            return;
        }
        int x = (screenW - CARD_W) / 2;
        int y = screenH - CARD_H - 58 + Math.round((1f - arrive) * 14f);

        Ui.shadow(gfx, x, y, CARD_W, CARD_H, 4, a);
        Ui.rect(gfx, x, y, CARD_W, CARD_H, Ui.alpha(Ui.INK, 0.97f * a), 4);
        Ui.border(gfx, x, y, CARD_W, CARD_H, Ui.alpha(won ? Ui.MOON_DEEP : Ui.LINE_STRONG, a), 4);
        gfx.fill(x + 4, y + 1, x + CARD_W - 4, y + 2, Ui.alpha(Ui.MOON_DEEP, 0.5f * a));

        header(gfx, font, x, y, now, a);
        body(gfx, font, x, y, now, a);
        footer(gfx, font, x, y, a);
    }

    private void header(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        int ty = y + 9;
        int tx = x + 12;
        tx += Ui.tracked(gfx, font, title(), tx, ty, Ui.alpha(Ui.TEXT, a), 1) + 8;
        gfx.fill(tx, ty - 1, tx + 1, ty + 9, Ui.alpha(Ui.LINE_STRONG, a));
        Ui.tracked(gfx, font, won ? "당첨" : "다음 기회에", tx + 8, ty,
                Ui.alpha(won ? Ui.MOON : Ui.TEXT_3, a), 1);

        String m = won ? "x" + strip(multiplier) : "-";
        int mw = font.width(m) + 10;
        Ui.rect(gfx, x + CARD_W - 12 - mw, y + 7, mw, 13, Ui.alpha(Ui.SUNKEN, a), 1);
        Ui.border(gfx, x + CARD_W - 12 - mw, y + 7, mw, 13, Ui.alpha(Ui.LINE, a), 1);
        gfx.text(font, m, x + CARD_W - 12 - mw + 5, y + 10,
                Ui.alpha(won ? Ui.MOON_PALE : Ui.TEXT_3, a), false);
    }

    private void body(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        switch (game) {
            case "slots" -> reels(gfx, font, x, y, now, a);
            case "dice" -> dice(gfx, font, x, y, now, a);
            case "roulette" -> roulette(gfx, font, x, y, now, a);
            case "coinflip" -> coinflip(gfx, font, x, y, now, a);
            default -> { }
        }
    }

    /** Three reels: characters scramble while spinning, then settle left to
     *  right with staggered stops — the classic rhythm, cheaply. */
    private void reels(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        if (reels == null || reels.isEmpty()) {
            return;
        }
        String[] glyphs = {"◆", "★", "●", "▲", "✦", "◇"};
        int cellW = 34;
        int cellH = 30;
        int bx = x + (CARD_W - reels.size() * cellW) / 2;
        int by = y + 24;
        for (int i = 0; i < reels.size(); i++) {
            int cx = bx + i * cellW;
            Ui.rect(gfx, cx, by, cellW - 6, cellH, Ui.alpha(Ui.SUNKEN, a), 3);
            long stopAt = 500L + i * 350L;
            String shown;
            int color;
            if (now < stopAt) {
                shown = glyphs[(int) ((now / 70 + i) % glyphs.length)];
                color = Ui.TEXT_2;
            } else {
                shown = symbolGlyph(reels.get(i));
                boolean isWinner = matched(reels, i);
                color = now < stopAt + 300 ? Ui.MOON_LIT : (isWinner ? Ui.MOON : Ui.TEXT_3);
            }
            Ui.centeredText(gfx, font, shown, cx + (cellW - 6) / 2, by + (cellH - 8) / 2,
                    Ui.alpha(color, a), false);
        }
        String sub = matchedCount(reels) > 0
                ? matchedCount(reels) + "개 일치" : "아깝습니다";
        gfx.text(font, sub, x + 12, y + CARD_H - 26, Ui.alpha(Ui.TEXT_3, a), false);
    }

    /** The die: a square whose face scrambles, then lands on the roll against
     *  the target with a pass/fail rule underneath. */
    private void dice(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        int s = 30;
        int bx = x + 14;
        int by = y + 22;
        Ui.rect(gfx, bx, by, s, s, Ui.alpha(Ui.SUNKEN, a), 3);
        String face = now < 900 ? pips((int) ((now / 90) % 6) + 1)
                : pips(Math.min(6, Math.max(1, (roll % 100) / 17 + 1)));
        Ui.centeredText(gfx, font, face, bx + s / 2, by + 8, Ui.alpha(Ui.MOON_PALE, a), false);

        String line = now < 900 ? "···" : roll + " / 목표 " + target;
        gfx.text(font, line, bx + s + 10, by + 2,
                Ui.alpha(now < 900 ? Ui.TEXT_2 : (won ? Ui.MOON : Ui.POPPY), a), false);
        String verdict = now < 900 ? "" : (won ? "통과" : "미달");
        if (!verdict.isEmpty()) {
            gfx.text(font, verdict, bx + s + 10, by + 14, Ui.alpha(Ui.TEXT_3, a), false);
        }
    }

    /** Roulette: the European WHEEL sweeping past in wheel order, braking on
     *  the landed pocket. Win: gold rays behind the result — the same
     *  celebration language as the Discord bot's win cards (drawRays). */
    private void roulette(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        int bx = x + 14;
        int by = y + 24;
        int cw = 18;
        int visible = 7;
        boolean landed = now >= 1300;
        if (won && landed) {
            rays(gfx, bx + (visible / 2) * cw + (cw - 3) / 2, by + 11, now, a);
        }
        float t = Ui.clamp(now / 1300f, 0f, 1f);
        float eased = 1f - (1f - t) * (1f - t); // ease-out: the wheel brakes
        int landedIdx = wheelIndexOf(pocket);
        int center = Math.round(landedIdx + (visible / 2f) * (eased - 1f) * 2.4f);
        for (int i = 0; i < visible; i++) {
            int n = WHEEL[Math.floorMod(center - visible / 2 + i, WHEEL.length)];
            int px = bx + i * cw;
            boolean here = landed && n == pocket && i == visible / 2;
            Ui.rect(gfx, px, by, cw - 3, 22,
                    Ui.alpha(here ? Ui.MOON_DEEP : Ui.OVERLAY, a), 2);
            Ui.centeredText(gfx, font, Integer.toString(n), px + (cw - 3) / 2, by + 7,
                    Ui.alpha(here ? Ui.MOON_LIT : pocketColor(n), a), false);
        }
        String label = landed ? ("포켓 " + pocket + " · " + betLabel(betType)) : "";
        if (!label.isEmpty()) {
            gfx.text(font, label, x + 14, y + CARD_H - 26,
                    Ui.alpha(won ? Ui.MOON : Ui.TEXT_3, a), false);
        }
    }

    /** The bot's drawRays, cheaply: gold sparks around the landed pocket,
     *  pulsing. A full radial-wedge gradient is not worth the fill count at
     *  card scale — twelve bright points read as rays at this size. */
    private void rays(GuiGraphicsExtractor gfx, int cx, int cy, long now, float a) {
        float pulse = 0.55f + 0.45f * (float) Math.sin(now / 180.0);
        for (int i = 0; i < 12; i++) {
            float ang = (float) (Math.PI * 2 * i / 12);
            int r = 9 + (i % 2) * 3;
            int px = (int) (cx + Math.cos(ang) * r);
            int py = (int) (cy + Math.sin(ang) * r);
            gfx.fill(px, py, px + 2, py + 2, Ui.alpha(Ui.MOON, 0.65f * pulse * a));
        }
    }

    /** Coinflip: two faces, flickering between them, landing on the call. */
    private void coinflip(GuiGraphicsExtractor gfx, Font font, int x, int y, long now, float a) {
        int bx = x + 14;
        int by = y + 24;
        boolean settled = now >= 800;
        boolean heads = settled ? won : (now / 120) % 2 == 0;
        int disc = 28;
        Ui.rect(gfx, bx, by, disc, disc,
                Ui.alpha(settled && !heads ? Ui.SUNKEN : Ui.MOON_DEEP, a), 14);
        String face = heads ? "월" : "신";
        Ui.centeredText(gfx, font, face, bx + disc / 2, by + 8,
                Ui.alpha(heads ? Ui.MOON_LIT : Ui.TEXT_3, a), false);
        String label = now < 800 ? "동전이 공중에서…"
                : (won ? "앞면 — 맞았습니다" : "뒷면 — 아쉽네요");
        gfx.text(font, label, bx + disc + 10, by + 8,
                Ui.alpha(now < 800 ? Ui.TEXT_2 : (won ? Ui.MOON : Ui.POPPY), a), false);
    }

    private void footer(GuiGraphicsExtractor gfx, Font font, int x, int y, float a) {
        gfx.fill(x + 1, y + CARD_H - 15, x + CARD_W - 1, y + CARD_H - 14, Ui.alpha(Ui.LINE, a));
        gfx.text(font, "/카지노 로 다시", x + 12, y + CARD_H - 11,
                Ui.alpha(Ui.TEXT_3, a), false);
        Ui.rightText(gfx, font, "풀문 카지노", x + CARD_W - 12, y + CARD_H - 11,
                Ui.alpha(Ui.TEXT_3, a), false);
    }

    // ── small helpers ─────────────────────────────────────────────

    private static String title() {
        return switch (active.game) {
            case "slots" -> "슬롯";
            case "dice" -> "주사위";
            case "roulette" -> "룰렛";
            case "coinflip" -> "코인플립";
            default -> "카지노";
        };
    }

    private static String symbolGlyph(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "moon" -> "●";
            case "star" -> "★";
            case "gem" -> "◆";
            case "bell" -> "♪";
            case "clover" -> "♣";
            case "seven" -> "7";
            default -> id.substring(0, Math.min(1, id.length())).toUpperCase(Locale.ROOT);
        };
    }

    private static boolean matched(List<String> reels, int i) {
        return reels.stream().filter(r -> r.equals(reels.get(i))).count() >= 2;
    }

    private static int matchedCount(List<String> reels) {
        return (int) reels.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r, java.util.stream.Collectors.counting()))
                .values().stream().max(Long::compare).orElse(0L).intValue();
    }

    private static String pips(int n) {
        return switch (n) {
            case 1 -> "⚀"; case 2 -> "⚁"; case 3 -> "⚂";
            case 4 -> "⚃"; case 5 -> "⚄"; default -> "⚅";
        };
    }

    private static int pocketColor(int n) {
        if (n == 0) {
            return 0xFF5EE6D0; // teal zero — the bot palette's green
        }
        return redTable(n) ? 0xFFFF8FA3 : Ui.TEXT; // rose red — cardTheme.rose
    }

    /** European wheel order — the strip sweeps in WHEEL order, not numeric,
     *  so it reads as the wheel instead of a table. */
    private static final int[] WHEEL = {
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
        5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26,
    };

    private static int wheelIndexOf(int n) {
        for (int i = 0; i < WHEEL.length; i++) {
            if (WHEEL[i] == n) {
                return i;
            }
        }
        return 0;
    }

    /** Vanilla roulette reds — the standard European layout. */
    private static boolean redTable(int n) {
        return switch (n) {
            case 1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36 -> true;
            default -> false;
        };
    }

    private static String betLabel(String betType) {
        return switch (betType) {
            case "red" -> "빨강";
            case "black" -> "검정";
            case "even" -> "짝수";
            case "odd" -> "홀수";
            case "low" -> "1-18";
            case "high" -> "19-36";
            default -> betType.startsWith("straight:") ? "단일 " + betType.substring(9)
                    : betType.startsWith("column:") ? "컬럼 " + betType.substring(7)
                    : betType;
        };
    }

    /** A JSON string field, or "" when absent or not a primitive. */
    private static String str(com.google.gson.JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive()
                ? o.get(key).getAsString() : "";
    }

    private static String strip(double d) {
        String s = String.format(Locale.ROOT, "%.2f", d);
        return s.endsWith(".00") ? s.substring(0, s.length() - 3) : s;
    }
}
