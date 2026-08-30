package dev.fullmoon.client.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class TerrainSampleTest {
    private static final TerrainSnapshot SNAPSHOT = new TerrainSnapshot(2, 1, List.of(
        TerrainSnapshot.Cell.mapped(11, 64), TerrainSnapshot.Cell.unmapped(7)));

    @Test
    void carriesTheRunsOfItsOwnSnapshot() {
        TerrainSample sample = TerrainSample.of(SNAPSHOT);

        assertEquals(SNAPSHOT, sample.snapshot());
        assertEquals(SNAPSHOT.runs(), sample.runs());
    }

    @Test
    void copiesTheRunsItIsGivenAndRefusesMissingData() {
        List<TerrainSnapshot.Run> source = new java.util.ArrayList<>(SNAPSHOT.runs());
        TerrainSample sample = new TerrainSample(SNAPSHOT, source);
        source.clear();

        assertEquals(2, sample.runs().size());
        assertThrows(IllegalArgumentException.class, () -> new TerrainSample(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TerrainSample(SNAPSHOT, null));
    }
}
