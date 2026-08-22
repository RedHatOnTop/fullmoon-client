package dev.pinion.bridge;

import dev.pinion.hud.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The warp menu — the native screen that replaces the server's chest GUI.
 *
 *  The data is the server's ({@link FullmoonBridge#waypoints()}); the layout
 *  is ours. Where a chest menu shows a grid of blocks that happen to mean
 *  something, this shows what the lobby actually is: places, grouped, each
 *  with the live walk from here under its name. A click does not move anyone
 *  — it asks, and the footer reports the server's answer.
 *
 *  Mouse and keyboard share one selection: pointing at a row selects it, the
 *  arrow keys select around it, ENTER asks. One highlighted thing at all
 *  times, never a hover layer pretending to be a second cursor.
 *
 *  Drawn entirely out of {@link Ui} for the same reason the settings panel
 *  is: it has to read as the same product as the launcher, and a vanilla
 *  widget stack cannot do that. */
public final class WarpScreen extends Screen {
    private static final int PANEL_W = 344;
    private static final int HEAD_H = 30;
    private static final int FOOT_H = 24;
    private static final int ROW_H = 24;
    private static final int CAPTION_H = 18;
    private static final int BODY_PAD = 10;
    private static final int RADIUS = 4;
    /** Body height in row units — captions cost part of one, which is why
     *  scrolling steps whole items and the scrollbar absorbs the remainder. */
    private static final int BODY_ROWS = 8;

    private static final long OPEN_MS = 190;
    private static final long ROW_STAGGER_MS = 22;
    private static final float PITCH_MOVE = 0.9f;
    private static final float PITCH_ASK = 1.15f;

    private final Screen parent;

    private int panelX;
    private int panelY;
    private int panelH;
    private float fit;
    private long openedAt;
    private long lastFrame;

    private int scrollOffset;
    /** Index into {@link FullmoonBridge#waypoints()}, shared by pointer and keys. */
    private int selected = -1;

    public WarpScreen(Screen parent) {
        super(Component.literal("Fullmoon"));
        this.parent = parent;
    }

    // ── items ─────────────────────────────────────────────────────

    private interface Item {
    }

    private record Caption(String text) implements Item {
    }

    private record Row(Waypoint wp, int index) implements Item {
    }

    /** Server order preserved; a caption whenever the group changes. The
     *  grouping is presentation, not filtering — every waypoint stays in the
     *  list exactly as the server sent it. */
    private List<Item> items() {
        List<Item> out = new ArrayList<>();
        String group = null;
        List<Waypoint> wps = FullmoonBridge.waypoints();
        for (int i = 0; i < wps.size(); i++) {
            Waypoint wp = wps.get(i);
            if (!wp.group().equals(group)) {
                group = wp.group();
                if (!group.isEmpty()) {
                    out.add(new Caption(group));
                }
            }
            out.add(new Row(wp, i));
        }
        return out;
    }

    // ── lifecycle ─────────────────────────────────────────────────

    @Override
    protected void init() {
        panelH = HEAD_H + BODY_PAD * 2 + BODY_ROWS * ROW_H + FOOT_H;
        fit = Math.min(1f, Math.min((height - 12f) / (float) panelH, (width - 12f) / (float) PANEL_W));
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
        openedAt = System.currentTimeMillis();
        lastFrame = openedAt;
        clampScroll();
        if (selected < 0 && !FullmoonBridge.waypoints().isEmpty()) {
            selected = 0;
        }
    }

    // ── render ────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        float open = Ui.ease((now - openedAt) / (float) OPEN_MS);
        /* same arrival as the settings panel: rise the last few pixels into
           place, because appearing at full opacity reads as a texture pop */
        int lift = Math.round((1f - open) * 9f);
        int px = panelX;
        int py = panelY + lift;
        Font font = this.font;

        extractBlurredBackground(gfx);
        gfx.fillGradient(0, 0, width, height,
                Ui.alpha(0xFF050914, 0.34f * open), Ui.alpha(0xFF050914, 0.58f * open));

        int mx = (int) Math.round((mouseX - width / 2.0) / fit + width / 2.0);
        int my = (int) Math.round((mouseY - height / 2.0) / fit + height / 2.0);
        gfx.pose().pushMatrix();
        gfx.pose().translate(width / 2f, height / 2f);
        gfx.pose().scale(fit, fit);
        gfx.pose().translate(-width / 2f, -height / 2f);

        Ui.shadow(gfx, px, py, PANEL_W, panelH, RADIUS, open);
        Ui.rect(gfx, px, py, PANEL_W, panelH, Ui.alpha(Ui.INK, 0.98f * open), RADIUS);
        Ui.border(gfx, px, py, PANEL_W, panelH, Ui.alpha(Ui.LINE_STRONG, open), RADIUS);
        gfx.fill(px + RADIUS + 1, py + 1, px + PANEL_W - RADIUS - 1, py + 2, Ui.alpha(Ui.TEXT, 0.06f * open));

        header(gfx, font, px, py, open);

        Minecraft mc = Minecraft.getInstance();
        List<Item> list = items();
        if (list.isEmpty()) {
            emptyState(gfx, font, px, py, open);
        } else {
            body(gfx, font, px, py, mx, my, dt, open, mc, now);
        }
        footer(gfx, font, px, py, open);

        gfx.pose().popMatrix();
    }

    private void header(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        /* the same moonlit wash as the settings header — the brand corner is
           the brightest thing on the panel, so the eye starts there */
        Ui.hGradient(gfx, px + 1, py + 1, PANEL_W - 2, HEAD_H - 1,
                Ui.alpha(Ui.MOON_DEEP, 0.38f * a), Ui.alpha(Ui.MOON_DEEP, 0f));
        gfx.fill(px + 1, py + 1, px + 4, py + HEAD_H - 1, Ui.alpha(Ui.MOON, a));

        int ty = py + 11;
        int tx = px + 12;
        tx += Ui.tracked(gfx, font, "FULLMOON", tx, ty, Ui.alpha(Ui.TEXT, a), 1) + 8;
        gfx.fill(tx, ty - 1, tx + 1, ty + 9, Ui.alpha(Ui.LINE_STRONG, a));
        Ui.tracked(gfx, font, "워프", tx + 8, ty, Ui.alpha(Ui.MOON_PALE, a), 1);

        chip(gfx, font, px, py, a);
        gfx.fill(px + 1, py + HEAD_H - 1, px + PANEL_W - 1, py + HEAD_H, Ui.alpha(Ui.LINE, a));
    }

    /** Connection state lives where a title-bar dot would: small, always on,
     *  never asking to be clicked. DEV labels fixture data honestly. */
    private void chip(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        String label;
        int dot;
        if (FullmoonBridge.devData()) {
            label = "BRIDGE · DEV";
            dot = Ui.OCHRE;
        } else if (FullmoonBridge.supported()) {
            label = "BRIDGE";
            dot = Ui.MOON_PALE;
        } else {
            label = "미지원";
            dot = Ui.TEXT_3;
        }
        int cw = Ui.trackedWidth(font, label, 1) + 18;
        int cx = px + PANEL_W - 12 - cw;
        Ui.rect(gfx, cx, py + 8, cw, 14, Ui.alpha(Ui.SUNKEN, a), 1);
        Ui.border(gfx, cx, py + 8, cw, 14, Ui.alpha(Ui.LINE, a), 1);
        gfx.fill(cx + 5, py + 13, cx + 7, py + 15, Ui.alpha(dot, a));
        Ui.tracked(gfx, font, label, cx + 11, py + 12, Ui.alpha(Ui.TEXT_2, a), 1);
    }

    private void body(GuiGraphicsExtractor gfx, Font font, int px, int py,
                      int mouseX, int mouseY, float dt, float a, Minecraft mc, long now) {
        List<Item> list = items();
        int x0 = px + BODY_PAD;
        int x1 = px + PANEL_W - BODY_PAD;
        int yTop = py + HEAD_H + BODY_PAD;
        int yBottom = yTop + BODY_ROWS * ROW_H;

        int pX = mc.player != null ? mc.player.getBlockX() : 0;
        int pZ = mc.player != null ? mc.player.getBlockZ() : 0;

        int y = yTop;
        int visibleIndex = 0;
        for (int i = scrollOffset; i < list.size() && y + ROW_H <= yBottom + CAPTION_H; i++) {
            Item item = list.get(i);
            int h = item instanceof Caption ? CAPTION_H : ROW_H;
            if (y + h > yBottom) {
                break;
            }
            float in = Ui.ease((now - openedAt - visibleIndex * ROW_STAGGER_MS) / (float) OPEN_MS);
            float alpha = a * in;
            if (item instanceof Caption caption) {
                caption(gfx, font, caption.text(), x0, x1, y, alpha);
            } else {
                Row row = (Row) item;
                boolean over = mouseX >= x0 - 4 && mouseX <= x1 + 4 && mouseY >= y && mouseY < y + ROW_H;
                if (over && !isPending(row)) {
                    selected = row.index();
                    ensureVisible(list);
                }
                row(gfx, font, row, x0, x1, y, alpha, pX, pZ, now);
            }
            y += h;
            visibleIndex++;
        }
        scrollbar(gfx, x1, yTop, yBottom - yTop, list);
    }

    private void caption(GuiGraphicsExtractor gfx, Font font, String text,
                         int x0, int x1, int y, float a) {
        String up = text.toUpperCase(Locale.ROOT);
        int w = Ui.tracked(gfx, font, up, x0, y + CAPTION_H - 11, Ui.alpha(Ui.TEXT_3, a), 1);
        gfx.fill(x0 + w + 8, y + CAPTION_H - 3, x1, y + CAPTION_H - 2, Ui.alpha(Ui.LINE, a));
    }

    private void row(GuiGraphicsExtractor gfx, Font font, Row row, int x0, int x1, int y,
                     float a, int playerX, int playerZ, long now) {
        Waypoint wp = row.wp();
        boolean isSelected = selected == row.index();
        boolean pending = isPending(row);

        if (isSelected) {
            Ui.rect(gfx, x0 - 4, y, x1 - x0 + 8, ROW_H, Ui.alpha(Ui.SURFACE, 0.85f * a), RADIUS - 2);
            /* two pixels of moonlight where a chest GUI would have put a grey
               outline — the selection rule of this screen */
            gfx.fill(x0 - 4, y, x0 - 2, y + ROW_H, Ui.alpha(Ui.MOON, a));
        }

        int ty = y + (ROW_H - 9) / 2;
        int nameColor = pending
                ? Ui.lerp(0.5f + 0.5f * (float) Math.sin(now / 180.0), Ui.TEXT, Ui.MOON_PALE)
                : Ui.TEXT;
        gfx.text(font, wp.name(), x0 + 10, ty, Ui.alpha(nameColor, a), false);

        /* right side: the honest number. Distance is what makes this a place
           rather than a button; coords would make it feel like a database */
        Minecraft mc = Minecraft.getInstance();
        String dist = "";
        if (mc.player != null) {
            dist = distance(mc.player.getBlockX(), mc.player.getBlockZ(), wp) + "m";
        }
        int right = x1 - (isSelected ? 14 : 0);
        Ui.rightText(gfx, font, dist, right, ty, Ui.alpha(isSelected ? Ui.MOON_PALE : Ui.TEXT_3, a), false);
        if (isSelected && !pending) {
            gfx.text(font, "›", x1 - 7, ty, Ui.alpha(Ui.MOON_PALE, a), false);
        }
    }

    private String distance(int playerX, int playerZ, Waypoint wp) {
        int dx = wp.x() - playerX;
        int dz = wp.z() - playerZ;
        return String.valueOf(Math.round(Math.sqrt((double) dx * dx + (double) dz * dz)));
    }

    /** A hairline track with a shorter hairline thumb — how much there is,
     *  said quietly. Never wide enough to look draggable, because it isn't. */
    private void scrollbar(GuiGraphicsExtractor gfx, int x1, int yTop, int viewH, List<Item> list) {
        int totalSlots = 0;
        for (Item item : list) {
            totalSlots += item instanceof Caption ? 1 : ROW_H / CAPTION_H + 1;
        }
        int viewSlots = Math.max(1, viewH / ROW_H);
        if (totalSlots <= viewSlots) {
            return;
        }
        int thumbH = Math.max(8, viewH * viewSlots / totalSlots);
        int maxScroll = Math.max(1, list.size() - viewSlots);
        int thumbY = yTop + (viewH - thumbH) * scrollOffset / maxScroll;
        gfx.fill(x1 + 3, yTop + 1, x1 + 4, yTop + viewH - 1, Ui.alpha(Ui.LINE, 0.6f));
        gfx.fill(x1 + 3, thumbY, x1 + 4, thumbY + thumbH, Ui.alpha(Ui.LINE_STRONG, 1f));
    }

    private void emptyState(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        int cy = py + HEAD_H + (panelH - HEAD_H - FOOT_H) / 2;
        if (!FullmoonBridge.supported()) {
            String over = "WARP";
            Ui.tracked(gfx, font, over, px + PANEL_W / 2 - Ui.trackedWidth(font, over, 2) / 2,
                    cy - 24, Ui.alpha(Ui.TEXT_3, 0.7f * a), 2);
            centerText(gfx, font, "이 서버는 네이티브 워프를 지원하지 않습니다", cy - 2, Ui.alpha(Ui.TEXT_2, a));
            centerText(gfx, font, "서버 명령어로 이용해 주세요", cy + 12, Ui.alpha(Ui.TEXT_3, a));
        } else {
            centerText(gfx, font, "아직 열린 경로가 없습니다", cy - 8, Ui.alpha(Ui.TEXT_2, a));
        }
    }

    private void centerText(GuiGraphicsExtractor gfx, Font font, String s, int y, int color) {
        gfx.text(font, s, panelX + (PANEL_W - font.width(s)) / 2, y, color, false);
    }

    private void footer(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        int fy = py + panelH - FOOT_H + (FOOT_H - 9) / 2 + 1;
        gfx.fill(px + 1, py + panelH - FOOT_H, px + PANEL_W - 1, py + panelH - FOOT_H + 1, Ui.alpha(Ui.LINE, a));

        gfx.text(font, "↑↓ 선택 · ENTER 실행 · ESC 닫기", px + 12, fy, Ui.alpha(Ui.TEXT_3, a), false);

        String status;
        int color;
        Boolean ok = FullmoonBridge.statusOk();
        if (FullmoonBridge.pendingId() != null) {
            status = "서버 응답 대기…";
            color = Ui.MOON_PALE;
        } else if (ok != null) {
            status = ok ? "이동했습니다" : "거부됨 — 잠시 후 다시 시도하세요";
            color = ok ? Ui.MOON_PALE : Ui.POPPY;
        } else {
            return;
        }
        Ui.rightText(gfx, font, status, px + PANEL_W - 12, fy, Ui.alpha(color, a), false);
    }

    // ── input ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            int wx = (int) Math.round((event.x() - width / 2.0) / fit + width / 2.0);
            int wy = (int) Math.round((event.y() - height / 2.0) / fit + height / 2.0);
            Row hit = rowAt(wx, wy);
            if (hit != null && !isPending(hit)) {
                selected = hit.index();
                ask(hit);
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int before = scrollOffset;
        scrollOffset -= Integer.signum((int) dy);
        clampScroll();
        if (scrollOffset != before) {
            click(PITCH_MOVE);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case GLFW.GLFW_KEY_UP -> {
                move(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                move(1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER -> {
                Row r = bySelected();
                if (r != null && !isPending(r)) {
                    ask(r);
                }
                return true;
            }
            default -> {
            }
        }
        return super.keyPressed(event);
    }

    /** The ask. Nothing moves here — the request goes out, the row pulses,
     *  and the footer says what the server decided. */
    private void ask(Row row) {
        click(PITCH_ASK);
        FullmoonBridge.requestTp(row.wp());
    }

    private void move(int dir) {
        int count = FullmoonBridge.waypoints().size();
        if (count == 0) {
            return;
        }
        selected = Math.floorMod(selected + dir, count);
        ensureVisible(items());
        click(PITCH_MOVE);
    }

    // ── geometry ──────────────────────────────────────────────────

    private void ensureVisible(List<Item> list) {
        int itemIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Row row && row.index() == selected) {
                itemIndex = i;
                break;
            }
        }
        if (itemIndex < 0) {
            return;
        }
        int viewItems = viewItems(list);
        if (itemIndex < scrollOffset) {
            scrollOffset = itemIndex;
        } else if (itemIndex >= scrollOffset + viewItems) {
            scrollOffset = itemIndex - viewItems + 1;
        }
        clampScrollTo(list);
    }

    /** How many list entries fit the body — rows and captions both spend an
     *  entry, so the count is conservative and the scrollbar absorbs the gap. */
    private int viewItems(List<Item> list) {
        int budget = BODY_ROWS * ROW_H;
        int used = 0;
        int count = 0;
        for (Item item : list) {
            int h = item instanceof Caption ? CAPTION_H : ROW_H;
            if (used + h > budget) {
                break;
            }
            used += h;
            count++;
        }
        return Math.max(1, count);
    }

    private void clampScroll() {
        clampScrollTo(items());
    }

    private void clampScrollTo(List<Item> list) {
        scrollOffset = Math.max(0, Math.min(scrollOffset, list.size() - viewItems(list)));
    }

    private Row rowAt(int mx, int my) {
        List<Item> list = items();
        int y = panelY + HEAD_H + BODY_PAD;
        for (int i = scrollOffset; i < list.size(); i++) {
            Item item = list.get(i);
            int h = item instanceof Caption ? CAPTION_H : ROW_H;
            if (my >= y && my < y + h && item instanceof Row row) {
                return row;
            }
            y += h;
        }
        return null;
    }

    private Row bySelected() {
        for (Item item : items()) {
            if (item instanceof Row row && row.index() == selected) {
                return row;
            }
        }
        return null;
    }

    private boolean isPending(Row row) {
        String id = FullmoonBridge.pendingId();
        return id != null && id.equals(row.wp().id());
    }

    private void click(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
