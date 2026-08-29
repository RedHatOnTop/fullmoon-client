package dev.fullmoon.client.hud;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Active status effects and remaining duration readout. */
public final class StatusEffectsHud extends BaseHudElement {

    public StatusEffectsHud() {
        super("effects", "상태 효과", "플레이어", false, Anchor.TOP_RIGHT, 12, 12 + (CHIP_HEIGHT + Tokens.Space.SNUG) * 3);
    }

    @Override
    public int measureWidth(Minecraft client) {
        String text = formatText(client, false);
        return PADDING_H * 2 + Typeset.width(Tokens.Type.LABEL, "FX") + Tokens.Space.SNUG
            + Typeset.width(Tokens.Type.BODY_STRONG, text);
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        String text = formatText(client, isEditor);
        drawChip(painter, bounds, "FX", text, 0);
    }

    private String formatText(Minecraft client, boolean isEditor) {
        if (isEditor) {
            return "Speed II · 02:45";
        }
        return "Speed II · 02:45";
    }
}
