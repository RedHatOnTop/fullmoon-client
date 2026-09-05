package dev.fullmoon.client.map;

import java.util.ArrayList;
import java.util.List;

/** Immutable sampled terrain raster with explicit unmapped cells. */
public record TerrainSnapshot(int width, int height, List<Cell> cells) {
    public TerrainSnapshot {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Terrain raster dimensions must be positive");
        }
        if (cells == null || cells.size() != Math.multiplyExact(width, height)) {
            throw new IllegalArgumentException("Terrain cell count does not match the raster");
        }
        cells = List.copyOf(cells);
    }

    public Cell cell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Terrain cell outside raster: " + x + "," + y);
        }
        return cells.get(y * width + x);
    }

    public int mappedPercent() {
        long mapped = cells.stream().filter(Cell::mapped).count();
        return (int) Math.round(mapped * 100.0 / cells.size());
    }

    public List<Run> runs() {
        List<Run> result = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            appendRow(result, row);
        }
        return List.copyOf(result);
    }

    private void appendRow(List<Run> result, int row) {
        int start = 0;
        while (start < width) {
            Cell first = cell(start, row);
            int end = start + 1;
            while (end < width && first.samePaint(cell(end, row))) {
                end++;
            }
            result.add(new Run(row, start, end - start, first.color(), first.mapped()));
            start = end;
        }
    }

    public record Cell(boolean mapped, int color, int elevation) {
        public static Cell mapped(int color, int elevation) {
            return new Cell(true, color, elevation);
        }

        public static Cell unmapped(int color) {
            return new Cell(false, color, 0);
        }

        private boolean samePaint(Cell other) {
            return color == other.color && mapped == other.mapped;
        }
    }

    public record Run(int row, int column, int length, int color, boolean mapped) {
        public Run {
            if (row < 0 || column < 0 || length <= 0) {
                throw new IllegalArgumentException("Invalid terrain run");
            }
        }
    }
}
