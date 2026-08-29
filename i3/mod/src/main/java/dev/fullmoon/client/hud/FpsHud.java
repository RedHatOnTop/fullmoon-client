package dev.fullmoon.client.hud;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Client framerate and frame time chip. */
public final class FpsHud extends BaseHudElement {

    public FpsHud() {
        super("fps", "FPS", "성능", true, Anchor.TOP_LEFT, 12, 12 + CHIP_HEIGHT + Tokens.Space.SNUG);
    }

    @Override
    public int measureWidth(Minecraft client) {
        String text = formatText(client, false);
        return PADDING_H * 2 + Typeset.width(Tokens.Type.LABEL, "FPS") + Tokens.Space.SNUG
            + Typeset.width(Tokens.Type.BODY_STRONG, text);
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        String text = formatText(client, isEditor);
        drawChip(painter, bounds, "FPS", text, 0);
    }

    private String formatText(Minecraft client, boolean isEditor) {
        if (isEditor) {
            return "144 fps";
        }
        int fps = client.getFps();
        return fps + " fps";
    }
}
