package dev.pinion.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** The client's own panel — rail on the left, one page of rows on the right.
 *
 *  Drawn out of {@link Ui} rather than assembled from vanilla widgets, because
 *  a stack of grey 200x20 buttons is what every other mod's options screen
 *  looks like and this one has to look like the launcher instead: chamfered
 *  surfaces, an ember rule, tracked caps. The price is hand hit-testing, which
 *  is what {@link Row#click} is.
 *
 *  The world keeps rendering behind it and so does the HUD, so a module
 *  switched here changes on screen underneath the panel while it is still
 *  open — that is the reason the scrim is thin and the panel is narrow. Module
 *  edits land in `pinion/hud.json`, the same file the launcher's editor writes;
 *  everything on the Visuals page is the mod's own `pinion/client.json`. */
public final class PinionSettingsScreen extends Screen {
    private enum Page {
        MODULES("Modules"),
        VISUALS("Visuals"),
        KEYS("Keys");

        final String label;

        Page(String label) {
            this.label = label;
        }
    }

    private static final int PANEL_W = 302;
    private static final int RAIL_W = 84;
    private static final int HEAD_H = 28;
    private static final int FOOT_H = 22;
    private static final int ROW_H = 22;
    private static final int BODY_PAD = 8;
    /** the panel holds the tallest page so switching tabs does not resize it */
    private static final int BODY_ROWS = 7;
    private static final int RADIUS = 3;

    private static final long OPEN_MS = 190;
    private static final long ROW_STAGGER_MS = 22;

    private static final int PILL_W = 20;
    private static final int PILL_H = 10;
    private static final int STEP_W = 9;

    private static final float PITCH_ON = 1f;
    private static final float PITCH_OFF = 0.8f;
    private static final float PITCH_STEP = 1.15f;
    private static final float PITCH_PAGE = 0.9f;

    /** the screen this replaced, so Escape goes back where the player was */
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    private Page page = Page.MODULES;
    private int panelX;
    private int panelY;
    private int panelH;

    /** 1 unless the panel is taller than the surface it has to fit. Vanilla
     *  clamps GUI scale so the logical screen is never smaller than 320x240,
     *  which leaves the 302x220 panel 20 pixels of air — this only bites if
     *  something else ever hands the client a smaller surface. */
    private float fit = 1f;
    private long openedAt;
    private long pageAt;
    private long lastFrame;
    private final float[] railHover = new float[Page.values().length];
    private String footNote = "";

    public PinionSettingsScreen(Screen parent) {
        super(Component.literal("Pinion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelH = HEAD_H + BODY_PAD * 2 + BODY_ROWS * ROW_H + FOOT_H;
        fit = Math.min(1f, Math.min((height - 12f) / panelH, (width - 12f) / PANEL_W));
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
        openedAt = System.currentTimeMillis();
        lastFrame = openedAt;
        buildRows();
    }

    // ── pages ─────────────────────────────────────────────────────

    private void buildRows() {
        rows.clear();
        pageAt = System.currentTimeMillis();
        switch (page) {
            case MODULES -> {
                for (String id : HudSettings.moduleIds()) {
                    rows.add(new ModuleRow(id));
                }
            }
            case VISUALS -> {
                rows.add(new ToggleRow(
                        "Fullbright",
                        "Floods the lightmap. Caves read as daylight; shadows do not.",
                        ClientSettings::fullbright,
                        ClientSettings::setFullbright));
                rows.add(new StepRow(
                        "Zoom field of view",
                        "How far the zoom key pulls in. Lower is closer.",
                        () -> ClientSettings.zoomFov() + "°",
                        d -> ClientSettings.setZoomFov(ClientSettings.zoomFov() + d * 5)));
                rows.add(new StepRow(
                        "Zoom sensitivity",
                        "Mouse speed while zoomed, against your normal setting.",
                        () -> Math.round(ClientSettings.zoomSensitivity() * 100) + "%",
                        d -> ClientSettings.setZoomSensitivity(ClientSettings.zoomSensitivity() + d * 0.05f)));
            }
            case KEYS -> {
                rows.add(new KeyRow("This panel", PinionKeys.settingsKey()));
                rows.add(new KeyRow("Zoom", PinionKeys.zoomKey()));
                rows.add(new KeyRow("Fullbright", PinionKeys.fullbrightKey()));
            }
        }
    }

    // ── geometry ──────────────────────────────────────────────────

    private int contentX() {
        return panelX + RAIL_W;
    }

    private int rowX0() {
        return contentX() + 10;
    }

    private int rowX1() {
        return panelX + PANEL_W - 12;
    }

    private int rowY(int index) {
        return panelY + HEAD_H + BODY_PAD + index * ROW_H;
    }

    private int railItemY(int index) {
        return panelY + HEAD_H + BODY_PAD + index * 18;
    }

    private boolean inRow(double mx, double my, int index) {
        int y = rowY(index);
        return mx >= contentX() && mx <= panelX + PANEL_W && my >= y && my < y + ROW_H;
    }

    /* The panel is drawn about the screen's centre through {@link #fit}, so
       every pointer coordinate has to come back the other way before it can be
       compared with the layout, which is all written unscaled. */
    private double localX(double mx) {
        return (mx - width / 2.0) / fit + width / 2.0;
    }

    private double localY(double my) {
        return (my - height / 2.0) / fit + height / 2.0;
    }

    // ── render ────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        float open = Ui.ease((now - openedAt) / (float) OPEN_MS);
        /* the panel rises the last few pixels into place — a window that simply
           appears at full opacity reads as a texture pop, not as a surface */
        int lift = Math.round((1f - open) * 9f);
        int px = panelX;
        int py = panelY + lift;
        Font font = this.font;

        /* Vanilla's own menu blur, then a thinner scrim than an unblurred
           backdrop would need. The HUD keeps drawing underneath either way, so
           a module switched here still visibly comes and goes — softened, but
           the panel has to be the thing in focus. */
        extractBlurredBackground(gfx);
        gfx.fillGradient(0, 0, width, height,
                Ui.alpha(0xFF06070A, 0.34f * open), Ui.alpha(0xFF06070A, 0.58f * open));

        int localMouseX = (int) Math.round(localX(mouseX));
        int localMouseY = (int) Math.round(localY(mouseY));
        gfx.pose().pushMatrix();
        gfx.pose().translate(width / 2f, height / 2f);
        gfx.pose().scale(fit, fit);
        gfx.pose().translate(-width / 2f, -height / 2f);

        Ui.shadow(gfx, px, py, PANEL_W, panelH, RADIUS, open);
        Ui.rect(gfx, px, py, PANEL_W, panelH, Ui.alpha(Ui.INK, 0.98f * open), RADIUS);
        Ui.border(gfx, px, py, PANEL_W, panelH, Ui.alpha(Ui.LINE_STRONG, open), RADIUS);
        // the light catch along the top edge, which is what stops a flat fill
        // from reading as a hole cut in the screen
        gfx.fill(px + RADIUS + 1, py + 1, px + PANEL_W - RADIUS - 1, py + 2, Ui.alpha(Ui.TEXT, 0.06f * open));

        rail(gfx, font, px, py, localMouseX, localMouseY, dt, open);
        header(gfx, font, px, py, open);

        footNote = "";
        for (int i = 0; i < rows.size(); i++) {
            float in = Ui.ease((now - pageAt - i * ROW_STAGGER_MS) / (float) OPEN_MS);
            rows.get(i).draw(gfx, font, i, localMouseX, localMouseY, dt, open * in, lift);
        }

        footer(gfx, font, px, py, open);
        gfx.pose().popMatrix();
    }

    private void header(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        /* an ember wash that dies out across the header, so the brand corner is
           the brightest thing on the panel and the eye starts there */
        Ui.hGradient(gfx, px + 1, py + 1, PANEL_W - 2, HEAD_H - 1,
                Ui.alpha(Ui.EMBER_DEEP, 0.38f * a), Ui.alpha(Ui.EMBER_DEEP, 0f));
        gfx.fill(px + 1, py + 1, px + 4, py + HEAD_H - 1, Ui.alpha(Ui.EMBER, a));

        int ty = py + 10;
        int tx = px + 12;
        tx += Ui.tracked(gfx, font, "PINION", tx, ty, Ui.alpha(Ui.TEXT, a), 1) + 8;
        gfx.fill(tx, ty - 1, tx + 1, ty + 9, Ui.alpha(Ui.LINE_STRONG, a));
        Ui.tracked(gfx, font, page.label.toUpperCase(Locale.ROOT), tx + 8, ty, Ui.alpha(Ui.EMBER_PALE, a), 1);

        String v = version();
        int vw = font.width(v) + 10;
        int vx = px + PANEL_W - 12 - vw;
        Ui.rect(gfx, vx, py + 7, vw, 13, Ui.alpha(Ui.SUNKEN, a), 1);
        Ui.border(gfx, vx, py + 7, vw, 13, Ui.alpha(Ui.LINE, a), 1);
        gfx.text(font, v, vx + 5, py + 10, Ui.alpha(Ui.TEXT_2, a), false);

        gfx.fill(px + 1, py + HEAD_H - 1, px + PANEL_W - 1, py + HEAD_H, Ui.alpha(Ui.LINE, a));
    }

    private void rail(GuiGraphicsExtractor gfx, Font font, int px, int py,
                      int mouseX, int mouseY, float dt, float a) {
        Ui.rect(gfx, px + 1, py + 1, RAIL_W, panelH - 2, Ui.alpha(Ui.SUNKEN, 0.96f * a), RADIUS);
        gfx.fill(px + 1 + RADIUS, py + 1, px + 1 + RAIL_W, py + panelH - 1, Ui.alpha(Ui.SUNKEN, 0.96f * a));
        gfx.fill(px + RAIL_W, py + HEAD_H, px + RAIL_W + 1, py + panelH - FOOT_H, Ui.alpha(Ui.LINE, a));

        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            Page p = pages[i];
            int y = railItemY(i) + (py - panelY);
            int hit = railItemY(i);
            boolean hover = mouseX >= px + 6 && mouseX <= px + RAIL_W - 6 && mouseY >= hit && mouseY < hit + 16;
            railHover[i] = Ui.approach(railHover[i], hover ? 1f : 0f, dt, 16f);
            boolean active = p == page;

            if (active) {
                Ui.rect(gfx, px + 6, y, RAIL_W - 12, 16, Ui.alpha(Ui.EMBER_DEEP, 0.45f * a), 1);
                gfx.fill(px + 6, y, px + 8, y + 16, Ui.alpha(Ui.EMBER, a));
            } else if (railHover[i] > 0.01f) {
                Ui.rect(gfx, px + 6, y, RAIL_W - 12, 16, Ui.alpha(Ui.SURFACE, 0.9f * railHover[i] * a), 1);
                gfx.fill(px + 6, y, px + 7, y + 16, Ui.alpha(Ui.LINE_STRONG, railHover[i] * a));
            }
            int color = active ? Ui.TEXT : Ui.lerp(railHover[i], Ui.TEXT_3, Ui.TEXT_2);
            Ui.tracked(gfx, font, p.label.toUpperCase(Locale.ROOT), px + 14, y + 4, Ui.alpha(color, a), 1);
        }

        /* the rail's foot is the live corner: a count that moves as rows are
           switched and the frame rate the panel itself is running at, so the
           thing never looks like a static mock-up. Stacked rather than
           label-left value-right — the rail is 84 wide and the two collided. */
        int fy = py + panelH - FOOT_H - 46;
        gfx.fill(px + 12, fy - 9, px + RAIL_W - 12, fy - 8, Ui.alpha(Ui.LINE, a));
        int total = HudSettings.moduleIds().size();
        stat(gfx, font, px + 12, fy, "ACTIVE", HudSettings.enabledCount() + " / " + total, Ui.TEXT, a);
        stat(gfx, font, px + 12, fy + 22, "FRAMES", Integer.toString(Minecraft.getInstance().getFps()),
                Ui.EMBER_PALE, a);
    }

    private void stat(GuiGraphicsExtractor gfx, Font font, int x, int y,
                      String label, String value, int color, float a) {
        Ui.tracked(gfx, font, label, x, y, Ui.alpha(Ui.TEXT_3, a), 1);
        gfx.text(font, value, x, y + 10, Ui.alpha(color, a), false);
    }

    private void footer(GuiGraphicsExtractor gfx, Font font, int px, int py, float a) {
        int y = py + panelH - FOOT_H;
        gfx.fill(px + 1, y, px + PANEL_W - 1, y + 1, Ui.alpha(Ui.LINE, a));
        Ui.rect(gfx, px + 1, y + 1, PANEL_W - 2, FOOT_H - 2, Ui.alpha(Ui.SUNKEN, 0.8f * a), RADIUS);
        gfx.fill(px + 1, y + 1, px + PANEL_W - 1, y + RADIUS + 1, Ui.alpha(Ui.SUNKEN, 0.8f * a));

        if (!footNote.isEmpty()) {
            gfx.fill(px + 12, y + 8, px + 14, y + 15, Ui.alpha(Ui.EMBER, a));
            gfx.text(font, trim(font, footNote, PANEL_W - 34), px + 19, y + 7, Ui.alpha(Ui.TEXT_2, a), false);
            return;
        }
        int x = px + 12;
        x = hint(gfx, font, "ESC", "close", x, y + 7, a);
        x = hint(gfx, font, "WHEEL", "resize", x, y + 7, a);
        hint(gfx, font, "LIVE", "changes apply as you click", x, y + 7, a);
    }

    private int hint(GuiGraphicsExtractor gfx, Font font, String key, String what, int x, int y, float a) {
        int kw = Ui.trackedWidth(font, key, 1) + 8;
        Ui.rect(gfx, x, y - 2, kw, 12, Ui.alpha(Ui.OVERLAY, a), 1);
        Ui.border(gfx, x, y - 2, kw, 12, Ui.alpha(Ui.LINE, a), 1);
        Ui.tracked(gfx, font, key, x + 4, y + 1, Ui.alpha(Ui.TEXT_2, a), 1);
        int tx = x + kw + 4;
        gfx.text(font, what, tx, y, Ui.alpha(Ui.TEXT_3, a), false);
        return tx + font.width(what) + 10;
    }

    private static String trim(Font font, String s, int max) {
        if (font.width(s) <= max) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && font.width(cut + "…") > max) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private static String version() {
        return FabricLoader.getInstance()
                .getModContainer(PinionClient.MOD_ID)
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    // ── input ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = localX(event.x());
        double my = localY(event.y());
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Page[] pages = Page.values();
            for (int i = 0; i < pages.length; i++) {
                int y = railItemY(i);
                if (mx >= panelX + 6 && mx <= panelX + RAIL_W - 6
                        && my >= y && my < y + 16) {
                    if (page != pages[i]) {
                        page = pages[i];
                        buildRows();
                        click(PITCH_PAGE);
                    }
                    return true;
                }
            }
            for (int i = 0; i < rows.size(); i++) {
                if (inRow(mx, my, i) && rows.get(i).click(mx, rowY(i))) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    /** The wheel resizes whatever module the pointer is over. Reaching for the
     *  stepper works too, but a HUD is sized by looking at it, not by clicking
     *  a 9-pixel box seven times. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (dy != 0) {
            for (int i = 0; i < rows.size(); i++) {
                if (inRow(localX(mouseX), localY(mouseY), i) && rows.get(i).scroll(dy > 0 ? 1 : -1)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // the open key closes it too, the way a client HUD toggle should
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        /* a panel that opens on a keypress should also be walkable on the
           keyboard — reaching for the mouse to change tab is a mode switch */
        if (event.key() == GLFW.GLFW_KEY_TAB || event.key() == GLFW.GLFW_KEY_RIGHT) {
            cyclePage(1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            cyclePage(-1);
            return true;
        }
        return super.keyPressed(event);
    }

    private void cyclePage(int dir) {
        Page[] pages = Page.values();
        page = pages[Math.floorMod(page.ordinal() + dir, pages.length)];
        buildRows();
        click(PITCH_PAGE);
    }

    /** Vanilla's button click, pitched by what happened: a switch going on
     *  sounds different from one going off, and a size step different from
     *  both. Silence is the one thing a client control must not be. */
    private void click(float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── rows ──────────────────────────────────────────────────────

    private abstract class Row {
        final String title;
        final String hint;
        float hover;

        Row(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }

        /** True when the row's own name should read as live rather than dormant. */
        boolean lit() {
            return true;
        }

        boolean click(double mx, int y) {
            return false;
        }

        boolean scroll(int dir) {
            return false;
        }

        abstract void control(GuiGraphicsExtractor gfx, Font font, int y, float dt, float a);

        final void draw(GuiGraphicsExtractor gfx, Font font, int index,
                        int mouseX, int mouseY, float dt, float a, int lift) {
            /* the row is drawn lifted while the panel is still arriving, but it
               is hit-tested where it lands — a click during a 190 ms slide must
               not land on the row above */
            int y = rowY(index) + lift;
            boolean over = inRow(mouseX, mouseY, index);
            hover = Ui.approach(hover, over ? 1f : 0f, dt, 18f);
            if (over && !hint.isEmpty()) {
                footNote = hint;
            }

            if (hover > 0.01f) {
                Ui.rect(gfx, contentX() + 4, y + 1, PANEL_W - RAIL_W - 12, ROW_H - 2,
                        Ui.alpha(Ui.SURFACE, 0.85f * hover * a), 2);
                int bar = Math.round((ROW_H - 6) * hover);
                gfx.fill(contentX() + 4, y + 1 + (ROW_H - 2 - bar) / 2,
                        contentX() + 6, y + 1 + (ROW_H - 2 + bar) / 2, Ui.alpha(Ui.EMBER, a));
            }

            boolean lit = lit();
            gfx.fill(rowX0(), y + ROW_H / 2 - 2, rowX0() + 3, y + ROW_H / 2 + 1,
                    Ui.alpha(lit ? Ui.EMBER : Ui.LINE_STRONG, a));
            gfx.text(font, title, rowX0() + 9, y + (ROW_H - 8) / 2,
                    Ui.alpha(lit ? Ui.TEXT : Ui.TEXT_3, a), false);

            control(gfx, font, y, dt, a);
        }
    }

    /** A module: the switch that turns it on and the size it draws at. */
    private final class ModuleRow extends Row {
        private final String id;
        private float knob;

        ModuleRow(String id) {
            super(label(id), describe(id));
            this.id = id;
            this.knob = HudSettings.get(id).enabled() ? 1f : 0f;
        }

        @Override
        boolean lit() {
            return HudSettings.get(id).enabled();
        }

        private int pillX() {
            return rowX1() - PILL_W;
        }

        private int minusX() {
            return pillX() - 12 - STEP_W * 2 - 26;
        }

        private int plusX() {
            return pillX() - 12 - STEP_W;
        }

        @Override
        boolean click(double mx, int y) {
            if (lit() && mx >= minusX() && mx < minusX() + STEP_W) {
                return step(-1);
            }
            if (lit() && mx >= plusX() && mx < plusX() + STEP_W) {
                return step(1);
            }
            boolean on = !lit();
            HudSettings.setEnabled(id, on);
            PinionSettingsScreen.this.click(on ? PITCH_ON : PITCH_OFF);
            return true;
        }

        @Override
        boolean scroll(int dir) {
            return lit() && step(dir);
        }

        private boolean step(int dir) {
            HudSettings.setScale(id, Math.round((HudSettings.get(id).scale() + dir * 0.1f) * 10f) / 10f);
            PinionSettingsScreen.this.click(PITCH_STEP);
            return true;
        }

        @Override
        void control(GuiGraphicsExtractor gfx, Font font, int y, float dt, float a) {
            boolean on = lit();
            knob = Ui.approach(knob, on ? 1f : 0f, dt, 16f);

            /* the size control only exists while the module does — showing a
               stepper for something that is not drawn invites a click that
               changes nothing on screen. It also only appears under the
               pointer: seven rows of minus-value-plus is a wall of chrome, and
               the size is worth reading even when it is not being changed */
            float shown = Ui.clamp(knob * 1.4f - 0.4f, 0f, 1f) * a;
            if (shown > 0.01f) {
                String value = String.format(Locale.ROOT, "%.1f", HudSettings.get(id).scale()) + "×";
                gfx.text(font, value, minusX() + STEP_W + 5, y + (ROW_H - 8) / 2,
                        Ui.alpha(Ui.TEXT_2, shown * (0.55f + 0.45f * hover)), false);
                float ctl = shown * hover;
                if (ctl > 0.01f) {
                    stepBox(gfx, font, minusX(), y + (ROW_H - 11) / 2, "-", ctl);
                    stepBox(gfx, font, plusX(), y + (ROW_H - 11) / 2, "+", ctl);
                }
            }
            Ui.pill(gfx, pillX(), y + (ROW_H - PILL_H) / 2, PILL_W, PILL_H, knob, a);
        }
    }

    private final class ToggleRow extends Row {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float knob;

        ToggleRow(String title, String hint, BooleanSupplier get, Consumer<Boolean> set) {
            super(title, hint);
            this.get = get;
            this.set = set;
            this.knob = get.getAsBoolean() ? 1f : 0f;
        }

        @Override
        boolean lit() {
            return get.getAsBoolean();
        }

        @Override
        boolean click(double mx, int y) {
            boolean on = !get.getAsBoolean();
            set.accept(on);
            PinionSettingsScreen.this.click(on ? PITCH_ON : PITCH_OFF);
            return true;
        }

        @Override
        void control(GuiGraphicsExtractor gfx, Font font, int y, float dt, float a) {
            knob = Ui.approach(knob, get.getAsBoolean() ? 1f : 0f, dt, 16f);
            Ui.pill(gfx, rowX1() - PILL_W, y + (ROW_H - PILL_H) / 2, PILL_W, PILL_H, knob, a);
        }
    }

    private final class StepRow extends Row {
        private final Supplier<String> value;
        private final IntConsumer step;

        StepRow(String title, String hint, Supplier<String> value, IntConsumer step) {
            super(title, hint);
            this.value = value;
            this.step = step;
        }

        private int plusX() {
            return rowX1() - STEP_W;
        }

        private int minusX() {
            return plusX() - STEP_W - 30;
        }

        @Override
        boolean click(double mx, int y) {
            if (mx >= minusX() && mx < minusX() + STEP_W) {
                return step(-1);
            }
            if (mx >= plusX() && mx < plusX() + STEP_W) {
                return step(1);
            }
            return false;
        }

        @Override
        boolean scroll(int dir) {
            return step(dir);
        }

        private boolean step(int dir) {
            step.accept(dir);
            PinionSettingsScreen.this.click(PITCH_STEP);
            return true;
        }

        @Override
        void control(GuiGraphicsExtractor gfx, Font font, int y, float dt, float a) {
            int top = y + (ROW_H - 11) / 2;
            String v = value.get();
            gfx.text(font, v, (minusX() + STEP_W + plusX() - font.width(v)) / 2, y + (ROW_H - 8) / 2,
                    Ui.alpha(Ui.TEXT, a), false);
            float ctl = a * hover;
            if (ctl > 0.01f) {
                stepBox(gfx, font, minusX(), top, "-", ctl);
                stepBox(gfx, font, plusX(), top, "+", ctl);
            }
        }
    }

    /** Read-only: the binding itself lives in vanilla Controls, and a second
     *  place to rebind a key is a second place for it to be wrong. */
    private final class KeyRow extends Row {
        private final KeyMapping mapping;

        KeyRow(String title, KeyMapping mapping) {
            super(title, "Rebind in Options / Controls — Pinion has its own category there.");
            this.mapping = mapping;
        }

        @Override
        void control(GuiGraphicsExtractor gfx, Font font, int y, float dt, float a) {
            String cap = mapping == null ? "—" : mapping.getTranslatedKeyMessage().getString();
            int w = Ui.trackedWidth(font, cap, 1) + 10;
            int x = rowX1() - w;
            int top = y + (ROW_H - 13) / 2;
            Ui.rect(gfx, x, top, w, 13, Ui.alpha(Ui.OVERLAY, a), 1);
            Ui.border(gfx, x, top, w, 13, Ui.alpha(Ui.LINE_STRONG, a), 1);
            Ui.tracked(gfx, font, cap, x + 5, top + 3, Ui.alpha(Ui.EMBER_PALE, a), 1);
        }
    }

    private static void stepBox(GuiGraphicsExtractor gfx, Font font, int x, int y, String glyph, float a) {
        Ui.rect(gfx, x, y, STEP_W, 11, Ui.alpha(Ui.OVERLAY, a), 1);
        Ui.border(gfx, x, y, STEP_W, 11, Ui.alpha(Ui.LINE, a), 1);
        gfx.text(font, glyph, x + (STEP_W - font.width(glyph)) / 2 + 1, y + 2, Ui.alpha(Ui.TEXT_2, a), false);
    }

    private static String label(String id) {
        return switch (id) {
            case "fps" -> "Frame rate";
            case "cps" -> "Clicks per second";
            case "coords" -> "Coordinates";
            case "ping" -> "Ping";
            case "keystrokes" -> "Keystrokes";
            case "gear" -> "Gear";
            case "potion" -> "Potions";
            default -> id;
        };
    }

    private static String describe(String id) {
        return switch (id) {
            case "fps" -> "Frames per second, coloured once it drops.";
            case "cps" -> "Left and right clicks over the last second.";
            case "coords" -> "Block position and the direction you face.";
            case "ping" -> "Round trip to the server you are on.";
            case "keystrokes" -> "WASD, both mouse buttons and jump, with an afterglow.";
            case "gear" -> "Armour, held and offhand items with their wear.";
            case "potion" -> "Active effects and what is left of them.";
            default -> "";
        };
    }
}
