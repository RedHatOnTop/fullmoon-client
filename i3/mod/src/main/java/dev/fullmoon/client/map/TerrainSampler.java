package dev.fullmoon.client.map;

import java.util.List;
import java.util.stream.IntStream;

import dev.fullmoon.client.design.Tokens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import net.minecraft.client.multiplayer.ClientLevel;

/** Samples only chunks already present in the client cache. */
public final class TerrainSampler {
    private TerrainSampler() {}

    public static TerrainSample sample(
            ClientLevel level, MapViewport viewport, int width, int height) {
        if (level == null || viewport == null) {
            throw new IllegalArgumentException("Level and map viewport are required");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Terrain sample dimensions must be positive");
        }

        List<RawCell> raw = IntStream.range(0, Math.multiplyExact(width, height))
            .mapToObj(index -> sampleCell(level, viewport, width, height,
                index % width, index / width))
            .toList();
        List<TerrainSnapshot.Cell> cells = IntStream.range(0, raw.size())
            .mapToObj(index -> paint(raw, index, width))
            .toList();
        return TerrainSample.of(new TerrainSnapshot(width, height, cells));
    }

    private static RawCell sampleCell(ClientLevel level, MapViewport viewport,
            int width, int height, int column, int row) {
        MapViewport.WorldPoint point = viewport.worldAt(column, row, width, height);
        int chunkX = Math.floorDiv(point.x(), 16);
        int chunkZ = Math.floorDiv(point.z(), 16);
        if (!level.hasChunk(chunkX, chunkZ)) {
            return RawCell.unmapped();
        }
        LevelChunk chunk = level.getChunkSource().getChunk(
            chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk.isEmpty()) {
            return RawCell.unmapped();
        }

        int elevation = chunk.getHeight(
            Heightmap.Types.WORLD_SURFACE, point.x(), point.z()) - 1;
        if (!level.isInsideBuildHeight(elevation)) {
            return RawCell.unmapped();
        }
        BlockPos position = new BlockPos(point.x(), elevation, point.z());
        BlockState state = chunk.getBlockState(position);
        MapColor mapColor = state.getMapColor(chunk, position);
        return new RawCell(true, elevation, mapColor);
    }

    private static TerrainSnapshot.Cell paint(List<RawCell> raw, int index, int width) {
        RawCell cell = raw.get(index);
        if (!cell.mapped()) {
            return TerrainSnapshot.Cell.unmapped(Tokens.Color.SURFACE_SUNKEN);
        }
        MapColor.Brightness brightness = brightness(raw, index, width, cell.elevation());
        int color = cell.mapColor() == MapColor.NONE
            ? Tokens.Color.SURFACE_RAISED
            : cell.mapColor().calculateARGBColor(brightness);
        return TerrainSnapshot.Cell.mapped(color, cell.elevation());
    }

    private static MapColor.Brightness brightness(
            List<RawCell> raw, int index, int width, int elevation) {
        if (index < width) {
            return MapColor.Brightness.NORMAL;
        }
        RawCell north = raw.get(index - width);
        if (!north.mapped()) {
            return MapColor.Brightness.NORMAL;
        }
        int slope = elevation - north.elevation();
        if (slope > 1) {
            return MapColor.Brightness.HIGH;
        }
        if (slope < -1) {
            return MapColor.Brightness.LOW;
        }
        return MapColor.Brightness.NORMAL;
    }

    private record RawCell(boolean mapped, int elevation, MapColor mapColor) {
        private static RawCell unmapped() {
            return new RawCell(false, 0, MapColor.NONE);
        }
    }
}
