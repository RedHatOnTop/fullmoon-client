package dev.pinion.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** The in-game half of the launcher's HUD editor.
 *
 *  Drawn rather than assembled out of vanilla widgets: this is the client's own
 *  surface and it wears the launcher's ink-and-ember palette, so moving between
 *  the two feels like one product. Rows are hit-tested by hand, which is the
 *  price of not looking like every other options screen.
 *
 *  Every edit writes `pinion/hud.json` — the same file the launcher writes, so
 *  a change made here shows up there and the HUD picks it up on its next poll
 *  without a reload. */
public final class PinionSettingsScreen extends Screen {
    private static final int SCRIM = 0xC8_07080A;
    private static final int PANEL = 0xF2_121311;
    private static final int PANEL_EDGE = 0xFF_2A2724;
    private static final int EMBER = 0xFF_B0481A;
    private static final int EMBER_DIM = 0xFF_6E2C10;
    private static final int BONE = 0xFF_F2EFEA;
    private static final int MUTED = 0xFF_8B8781;
    private static final int ROW_HOVER = 0x24_B0481A;

    private static final int PANEL_W = 246;
    private static final int ROW_H = 22;
    private static final int HEAD_H = 34;
    private static final int FOOT_H = 26;

    /** the screen this replaced, so Escape goes back where the player was */
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelH;

    public PinionSettingsScreen(Screen parent) {
        super(Component.literal("Pinion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        for (String id : HudSettings.moduleIds()) {
            rows.add(new Row(id));
        }
        panelH = HEAD_H + rows.size() * ROW_H + FOOT_H;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, width, height, SCRIM);

        gfx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL);
        gfx.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, EMBER);
        gfx.fill(panelX, panelY + panelH - 1, panelX + PANEL_W, panelY + panelH, PANEL_EDGE);
        gfx.fill(panelX, panelY, panelX + 1, panelY + panelH, PANEL_EDGE);
        gfx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + panelH, PANEL_EDGE);

        Font font = this.font;
        gfx.text(font, "PINION", panelX + 12, panelY + 12, EMBER, false);
        gfx.text(font, "HUD", panelX + 12 + font.width("PINION") + 6, panelY + 12, BONE, false);
        String hint = PinionKeys.isFullbright() ? "FULLBRIGHT" : "";
        if (!hint.isEmpty()) {
            gfx.text(font, hint, panelX + PANEL_W - 12 - font.width(hint), panelY + 12, EMBER_DIM, false);
        }
        gfx.fill(panelX + 12, panelY + HEAD_H - 8, panelX + PANEL_W - 12, panelY + HEAD_H - 7, PANEL_EDGE);

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).draw(gfx, font, i, mouseX, mouseY);
        }

        String foot = "Esc  close      B  fullbright      C  zoom";
        gfx.text(font, foot, panelX + 12, panelY + panelH - 17, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).hit(event.x(), event.y(), i)) {
                    rows.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // the open key closes it too, the way a client HUD toggle should
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class Row {
        private final String id;

        Row(String id) {
            this.id = id;
        }

        private int top(int index) {
            return panelY + HEAD_H + index * ROW_H;
        }

        boolean hit(double mx, double my, int index) {
            int y = top(index);
            return mx >= panelX + 1 && mx <= panelX + PANEL_W - 1 && my >= y && my < y + ROW_H;
        }

        void toggle() {
            HudSettings.setEnabled(id, !HudSettings.get(id).enabled());
        }

        void draw(GuiGraphicsExtractor gfx, Font font, int index, int mouseX, int mouseY) {
            int y = top(index);
            boolean on = HudSettings.get(id).enabled();
            boolean hover = hit(mouseX, mouseY, index);

            if (hover) {
                gfx.fill(panelX + 1, y, panelX + PANEL_W - 1, y + ROW_H, ROW_HOVER);
            }
            gfx.fill(panelX + 12, y + 7, panelX + 14, y + ROW_H - 7, on ? EMBER : PANEL_EDGE);
            gfx.text(font, label(id), panelX + 22, y + (ROW_H - font.lineHeight) / 2 + 1, on ? BONE : MUTED, false);

            /* the switch is the state: filled and forward when on, hollow and
               back when off — readable without colour */
            int sx = panelX + PANEL_W - 12 - 22;
            int sy = y + (ROW_H - 10) / 2;
            gfx.fill(sx, sy, sx + 22, sy + 10, on ? EMBER_DIM : PANEL_EDGE);
            int knob = on ? sx + 12 : sx + 1;
            gfx.fill(knob, sy + 1, knob + 9, sy + 9, on ? EMBER : MUTED);
        }
    }

    private static String label(String id) {
        return switch (id) {
            case "fps" -> "FPS";
            case "cps" -> "CPS";
            case "coords" -> "Coordinates";
            case "ping" -> "Ping";
            case "keystrokes" -> "Keystrokes";
            case "gear" -> "Gear";
            case "potion" -> "Potions";
            default -> id;
        };
    }
}
