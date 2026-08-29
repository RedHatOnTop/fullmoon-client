package dev.fullmoon.client.hud;

import java.util.List;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.Button;
import dev.fullmoon.client.ui.Chord;
import dev.fullmoon.client.ui.Surface;
import dev.fullmoon.client.ui.Toggle;
import dev.fullmoon.client.ui.Voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.InputConstants;

/** In-game HUD placement and layout editor with 4px grid snapping and corner ticks. */
public final class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private static final int CORNER_LEN = 6;
    private static final int CORNER_THICK = Tokens.Stroke.FOCUS;

    private final Screen parent;
    private final Surface surface = new Surface();
    private final List<HudElement> elements;
    private String selectedId = null;

    private boolean dragging = false;
    private int dragStartX;
    private int dragStartY;
    private int dragInitialOffsetX;
    private int dragInitialOffsetY;

    private final Button closeBtn;
    private final Button resetBtn;
    private Toggle enableToggle;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("fullmoon.hud.editor.title"));
        this.parent = parent;
        this.elements = HudElementRegistry.getInstance().elements();
        if (!elements.isEmpty()) {
            this.selectedId = elements.get(0).id();
        }

        closeBtn = surface.add(new Button(Voice.QUIET, tr("action.close"), this::onClose));
        resetBtn = surface.add(new Button(Voice.QUIET, tr("action.reset"), this::resetDefaults));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        HudElementRegistry.getInstance().save();
        Minecraft.getInstance().setScreen(parent);
    }

    private void resetDefaults() {
        for (HudElement elem : elements) {
            elem.setEnabled(true);
            elem.setOffsetX(12);
            elem.setOffsetY(12);
        }
        HudElementRegistry.getInstance().save();
    }

    @Override
    protected void init() {
        int barY = Tokens.Space.COZY;
        int barW = Math.min(640, width - Tokens.Space.SECTION * 2);
        int barX = (width - barW) / 2;

        int closeW = closeBtn.measure();
        int resetW = resetBtn.measure();

        closeBtn.place(new Box(barX + barW - closeW, barY, closeW, Button.HEIGHT));
        resetBtn.place(new Box(barX + barW - closeW - Tokens.Space.COZY - resetW, barY, resetW, Button.HEIGHT));

        HudElement current = selectedElement();
        if (current != null) {
            if (enableToggle != null) {
                // remove existing if needed
            }
            enableToggle = surface.add(new Toggle(current.label() + " " + tr("action.enabled"),
                current.enabled(), current::setEnabled));
            int toggleW = enableToggle.measure();
            enableToggle.place(new Box(barX, barY + Tokens.Type.TITLE.leading() + Tokens.Space.COZY,
                toggleW, Toggle.HEIGHT));
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        surface.pointer(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (surface.press(event.x(), event.y())) {
                return true;
            }

            int mx = (int) event.x();
            int my = (int) event.y();

            Minecraft client = Minecraft.getInstance();
            for (HudElement elem : elements) {
                Box b = elem.computeBounds(width, height, client);
                if (b.holds(mx, my)) {
                    selectedId = elem.id();
                    dragging = true;
                    dragStartX = mx;
                    dragStartY = my;
                    dragInitialOffsetX = elem.offsetX();
                    dragInitialOffsetY = elem.offsetY();
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            HudElementRegistry.getInstance().save();
            return true;
        }
        return surface.release(event.x(), event.y()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            HudElement elem = selectedElement();
            if (elem != null) {
                int dx = (int) (event.x() - dragStartX);
                int dy = (int) (event.y() - dragStartY);

                int rawX = elem.anchor().computeX(width, elem.measureWidth(Minecraft.getInstance()), dragInitialOffsetX) + dx;
                int rawY = elem.anchor().computeY(height, elem.measureHeight(Minecraft.getInstance()), dragInitialOffsetY) + dy;

                // 4px grid snap
                int snappedX = Math.round(rawX / (float) SNAP) * SNAP;
                int snappedY = Math.round(rawY / (float) SNAP) * SNAP;

                Anchor nearest = Anchor.nearest(width, height, elem.measureWidth(Minecraft.getInstance()),
                    elem.measureHeight(Minecraft.getInstance()), snappedX, snappedY);
                elem.setAnchor(nearest);

                int offX = nearest.computeOffsetX(width, elem.measureWidth(Minecraft.getInstance()), snappedX);
                int offY = nearest.computeOffsetY(height, elem.measureHeight(Minecraft.getInstance()), snappedY);

                elem.setOffsetX(Math.max(0, offX));
                elem.setOffsetY(Math.max(0, offY));
                return true;
            }
        }
        if (surface.captured() != null) {
            surface.pointer(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return surface.scroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (surface.key(Chord.from(event))) {
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return surface.type(event.codepoint()) || super.charTyped(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        Painter painter = new Painter(gfx);
        painter.blurredStratum();
        painter.fill(0, 0, painter.width(), painter.height(),
            Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.72f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        surface.hover(mouseX, mouseY);
        Painter painter = new Painter(gfx);

        drawSnapGrid(painter);
        drawElements(painter);
        drawSelectedCornerTicks(painter);
        drawToolbar(painter);
        surface.draw(painter);
    }

    private void drawSnapGrid(Painter painter) {
        if (dragging) {
            // Draw subtle guide lines along center and borders
            int midX = width / 2;
            int midY = height / 2;
            painter.vRule(midX, 0, height, Tokens.Color.LINE_HAIRLINE);
            painter.hRule(0, midY, width, Tokens.Color.LINE_HAIRLINE);
        }
    }

    private void drawElements(Painter painter) {
        Minecraft client = Minecraft.getInstance();
        for (HudElement elem : elements) {
            Box b = elem.computeBounds(width, height, client);
            elem.draw(painter, b, client, true);

            // Subtle bounding border in editor
            boolean selected = elem.id().equals(selectedId);
            if (!selected) {
                painter.border(b.x(), b.y(), b.w(), b.h(), Tokens.Radius.SM, Tokens.Stroke.HAIR,
                    Tokens.Color.LINE_HAIRLINE);
            }
        }
    }

    private void drawSelectedCornerTicks(Painter painter) {
        HudElement elem = selectedElement();
        if (elem == null) return;

        Minecraft client = Minecraft.getInstance();
        Box b = elem.computeBounds(width, height, client);
        int x = b.x();
        int y = b.y();
        int w = b.w();
        int h = b.h();
        int color = Tokens.Color.ACCENT;

        // Top-Left L-tick
        painter.fill(x - CORNER_THICK, y - CORNER_THICK, CORNER_LEN, CORNER_THICK, color);
        painter.fill(x - CORNER_THICK, y - CORNER_THICK, CORNER_THICK, CORNER_LEN, color);

        // Top-Right L-tick
        painter.fill(x + w + CORNER_THICK - CORNER_LEN, y - CORNER_THICK, CORNER_LEN, CORNER_THICK, color);
        painter.fill(x + w, y - CORNER_THICK, CORNER_THICK, CORNER_LEN, color);

        // Bottom-Left L-tick
        painter.fill(x - CORNER_THICK, y + h, CORNER_LEN, CORNER_THICK, color);
        painter.fill(x - CORNER_THICK, y + h + CORNER_THICK - CORNER_LEN, CORNER_THICK, CORNER_LEN, color);

        // Bottom-Right L-tick
        painter.fill(x + w + CORNER_THICK - CORNER_LEN, y + h, CORNER_LEN, CORNER_THICK, color);
        painter.fill(x + w, y + h + CORNER_THICK - CORNER_LEN, CORNER_THICK, CORNER_LEN, color);
    }

    private void drawToolbar(Painter painter) {
        int barY = Tokens.Space.COZY;
        int barW = Math.min(640, width - Tokens.Space.SECTION * 2);
        int barX = (width - barW) / 2;

        // Title and Accent Tick
        painter.fill(barX, Typeset.capTop(Tokens.Type.TITLE, barY), Tokens.Stroke.FOCUS,
            Typeset.capHeight(Tokens.Type.TITLE), Tokens.Color.ACCENT);
        int textX = barX + Tokens.Stroke.FOCUS + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.TITLE, tr("title"), textX, barY, Tokens.Color.INK_PRIMARY);

        // Snap indicator badge
        int badgeX = textX + Typeset.width(Tokens.Type.TITLE, tr("title")) + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.LABEL, "·  " + tr("grid.snap"), badgeX,
            barY + (Tokens.Type.TITLE.leading() - Tokens.Type.LABEL.leading()) / 2, Tokens.Color.INK_TERTIARY);
    }

    private HudElement selectedElement() {
        if (selectedId == null) return null;
        return HudElementRegistry.getInstance().get(selectedId);
    }

    private static String tr(String key, Object... args) {
        return I18n.get("fullmoon.hud.editor." + key, args);
    }
}
