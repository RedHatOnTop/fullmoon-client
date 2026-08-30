package dev.fullmoon.client.map;

import java.util.List;

/** Terrain snapshot and its precomputed horizontal paint runs. */
public record TerrainSample(TerrainSnapshot snapshot, List<TerrainSnapshot.Run> runs) {
    public TerrainSample {
        if (snapshot == null || runs == null) {
            throw new IllegalArgumentException("Terrain sample data is required");
        }
        runs = List.copyOf(runs);
    }

    public static TerrainSample of(TerrainSnapshot snapshot) {
        return new TerrainSample(snapshot, snapshot.runs());
    }
}
