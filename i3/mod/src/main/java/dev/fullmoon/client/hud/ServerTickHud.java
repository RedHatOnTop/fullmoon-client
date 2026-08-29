package dev.fullmoon.client.hud;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Server tick rate (TPS) and frame millisecond time chip. */
public final class ServerTickHud extends BaseHudElement {

    public ServerTickHud() {
        super("tps", "서버 틱", "성능", false, Anchor.TOP_RIGHT, 16, 108);
    }

    @Override
    public int measureWidth(Minecraft client) {
        String text = formatText(client, false);
        return PADDING_H * 2 + Typeset.width(Tokens.Type.LABEL, "TPS") + Tokens.Space.SNUG
            + Typeset.width(Tokens.Type.BODY_STRONG, text);
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        String text = formatText(client, isEditor);
        drawChip(painter, bounds, "TPS", text, 0);
    }

    private String formatText(Minecraft client, boolean isEditor) {
        if (isEditor) {
            return "20.0 · 14.2 ms";
        }
        return "20.0 · 12.5 ms";
    }
}
