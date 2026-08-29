package dev.fullmoon.client.hud;

import java.util.Locale;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/** In-game coordinates, facing direction and biome chip. */
public final class CoordinatesHud extends BaseHudElement {

    public CoordinatesHud() {
        super("coords", "좌표 및 방향", "플레이어", true, Anchor.TOP_LEFT, 12, 12);
    }

    @Override
    public int measureWidth(Minecraft client) {
        String text = formatText(client, false);
        return PADDING_H * 2 + Typeset.width(Tokens.Type.LABEL, "XYZ") + Tokens.Space.SNUG
            + Typeset.width(Tokens.Type.BODY_STRONG, text);
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        String text = formatText(client, isEditor);
        drawChip(painter, bounds, "XYZ", text, 0);
    }

    private String formatText(Minecraft client, boolean isEditor) {
        if (isEditor || client.player == null) {
            return "124.5  64.0  -320.8  (N)";
        }
        Entity player = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        Direction dir = player.getDirection();
        String dirName = switch (dir) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
            default -> "?";
        };
        return String.format(Locale.ROOT, "%.1f  %.1f  %.1f  (%s)", x, y, z, dirName);
    }
}
