package dev.fullmoon.client.map;

import java.util.List;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.gui.navigation.ScreenRectangle;

import org.joml.Matrix3x2f;

/** Shared north-up terrain plane for the full map and HUD minimap. */
public final class MapCanvas {
    private static final int REGION_GRID_BLOCKS = 64;

    private MapCanvas() {}

    public static void draw(Painter painter, Box bounds, int cellSize,
            TerrainSample terrain, MapViewport viewport,
            List<MapMarkers.Placed> markers, MapViewport.GridPoint player, boolean labels) {
        int mapWidth = terrain.snapshot().width() * cellSize;
        int mapHeight = terrain.snapshot().height() * cellSize;
        Box raster = bounds.centred(mapWidth, mapHeight);

        painter.fill(raster.x(), raster.y(), raster.w(), raster.h(),
            Tokens.Color.SURFACE_SUNKEN);
        painter.gfx().nextStratum();
        ScreenRectangle clip = new ScreenRectangle(
            raster.x(), raster.y(), raster.w(), raster.h());
        painter.gfx().guiRenderState.addGuiElement(new TerrainRenderState(
            new Matrix3x2f(painter.gfx().pose()), raster.x(), raster.y(), cellSize,
            terrain.runs(), terrain.snapshot().width(), terrain.snapshot().height(), clip));
        painter.gfx().nextStratum();

        painter.pushClip(raster.x(), raster.y(), raster.w(), raster.h());
        drawRegionGrid(painter, raster, cellSize, terrain.snapshot(), viewport);
        drawMarkers(painter, raster, cellSize, markers, labels);
        drawPlayer(painter, raster, cellSize, terrain.snapshot(), player);
        painter.popClip();
        painter.border(raster.x(), raster.y(), raster.w(), raster.h(), Tokens.Radius.NONE,
            Tokens.Stroke.HAIR, Tokens.Color.LINE_STRONG);
        Typeset.draw(painter, Tokens.Type.LABEL, "N", raster.midX() - Tokens.Space.TIGHT,
            raster.y() + Tokens.Space.SNUG, Tokens.Color.INK_PRIMARY);
    }

    private static void drawRegionGrid(Painter painter, Box raster, int cellSize,
            TerrainSnapshot snapshot, MapViewport viewport) {
        int gridColor = Rgb.alpha(Tokens.Color.LINE_HAIRLINE, 0.52f);
        for (int column = 0; column < snapshot.width(); column++) {
            MapViewport.WorldPoint point = viewport.worldAt(
                column, 0, snapshot.width(), snapshot.height());
            if (Math.floorMod(point.x(), REGION_GRID_BLOCKS) < viewport.blocksPerCell()) {
                painter.vRule(raster.x() + column * cellSize, raster.y(), raster.h(), gridColor);
            }
        }
        for (int row = 0; row < snapshot.height(); row++) {
            MapViewport.WorldPoint point = viewport.worldAt(
                0, row, snapshot.width(), snapshot.height());
            if (Math.floorMod(point.z(), REGION_GRID_BLOCKS) < viewport.blocksPerCell()) {
                painter.hRule(raster.x(), raster.y() + row * cellSize, raster.w(), gridColor);
            }
        }
    }

    private static void drawMarkers(Painter painter, Box raster, int cellSize,
            List<MapMarkers.Placed> markers, boolean labels) {
        for (MapMarkers.Placed marker : markers) {
            int x = raster.x() + (int) Math.round(marker.column() * cellSize);
            int y = raster.y() + (int) Math.round(marker.row() * cellSize);
            painter.ring(x, y, Tokens.Space.SNUG, Tokens.Stroke.FOCUS,
                Tokens.Color.INK_PRIMARY);
            painter.dot(x, y, Tokens.Space.HAIR, Tokens.Color.INK_PRIMARY);
            if (labels && !marker.label().isBlank()) {
                Typeset.draw(painter, Tokens.Type.LABEL, marker.label(),
                    x + Tokens.Space.COZY, y - Tokens.Space.SNUG,
                    Tokens.Color.INK_PRIMARY);
            }
        }
    }

    private static void drawPlayer(Painter painter, Box raster, int cellSize,
            TerrainSnapshot snapshot, MapViewport.GridPoint player) {
        if (player.column() < 0.0 || player.column() > snapshot.width() - 1
                || player.row() < 0.0 || player.row() > snapshot.height() - 1) {
            return;
        }
        int x = raster.x() + (int) Math.round(player.column() * cellSize);
        int y = raster.y() + (int) Math.round(player.row() * cellSize);
        painter.ring(x, y, Tokens.Space.SNUG,
            Tokens.Stroke.FOCUS, Tokens.Color.ACCENT);
        painter.hRule(x - Tokens.Space.COZY, y,
            Tokens.Space.GUTTER, Tokens.Color.ACCENT);
        painter.vRule(x, y - Tokens.Space.COZY,
            Tokens.Space.GUTTER, Tokens.Color.ACCENT);
    }
}
