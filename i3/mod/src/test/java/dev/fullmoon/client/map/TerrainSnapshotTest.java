package dev.fullmoon.client.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TerrainSnapshotTest {
    private static final TerrainSnapshot.Cell UNKNOWN = TerrainSnapshot.Cell.unmapped(7);
    private static final TerrainSnapshot.Cell GRASS = TerrainSnapshot.Cell.mapped(11, 64);
    private static final TerrainSnapshot.Cell WATER = TerrainSnapshot.Cell.mapped(13, 62);

    @Test
    void copiesCellsAndReportsHonestCoverage() {
        List<TerrainSnapshot.Cell> source = new java.util.ArrayList<>(
            List.of(GRASS, GRASS, UNKNOWN, WATER));
        TerrainSnapshot snapshot = new TerrainSnapshot(2, 2, source);
        source.set(0, UNKNOWN);

        assertEquals(GRASS, snapshot.cell(0, 0));
        assertEquals(75, snapshot.mappedPercent());
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.cells().set(0, UNKNOWN));
    }

    @Test
    void compressesOnlyAdjacentCellsWithTheSameTruthStateAndColour() {
        TerrainSnapshot snapshot = new TerrainSnapshot(5, 2, List.of(
            GRASS, GRASS, UNKNOWN, UNKNOWN, WATER,
            WATER, GRASS, GRASS, WATER, WATER));

        assertEquals(List.of(
            new TerrainSnapshot.Run(0, 0, 2, 11, true),
            new TerrainSnapshot.Run(0, 2, 2, 7, false),
            new TerrainSnapshot.Run(0, 4, 1, 13, true),
            new TerrainSnapshot.Run(1, 0, 1, 13, true),
            new TerrainSnapshot.Run(1, 1, 2, 11, true),
            new TerrainSnapshot.Run(1, 3, 2, 13, true)), snapshot.runs());
    }

    @Test
    void rejectsMalformedRastersAndCoordinates() {
        assertThrows(IllegalArgumentException.class,
            () -> new TerrainSnapshot(0, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new TerrainSnapshot(2, 2, List.of(GRASS)));

        TerrainSnapshot snapshot = new TerrainSnapshot(1, 1, List.of(GRASS));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.cell(1, 0));
        assertTrue(snapshot.runs().getFirst().mapped());
    }
}
