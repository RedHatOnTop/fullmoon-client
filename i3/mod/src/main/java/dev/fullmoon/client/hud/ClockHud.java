package dev.fullmoon.client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Real-world clock chip. */
public final class ClockHud extends BaseHudElement {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ClockHud() {
        super("clock", "시계", "일반", true, Anchor.TOP_RIGHT, 16, 82);
    }

    @Override
    public int measureWidth(Minecraft client) {
        String text = formatText(client, false);
        return PADDING_H * 2 + Typeset.width(Tokens.Type.LABEL, "TIME") + Tokens.Space.SNUG
            + Typeset.width(Tokens.Type.BODY_STRONG, text);
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        String text = formatText(client, isEditor);
        drawChip(painter, bounds, "TIME", text, 0);
    }

    private String formatText(Minecraft client, boolean isEditor) {
        return LocalTime.now().format(TIME_FMT);
    }
}
