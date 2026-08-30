package dev.fullmoon.client.map;

import java.util.List;

/** Pure projection and clipping for server-published map markers. */
public final class MapMarkers {
    private MapMarkers() {}

    public static List<Placed> place(
            List<Marker> markers, MapViewport viewport, int columns, int rows) {
        if (markers == null || viewport == null) {
            throw new IllegalArgumentException("Map markers and viewport are required");
        }
        return List.copyOf(markers).stream()
            .map(marker -> place(marker, viewport, columns, rows))
            .filter(placed -> visible(placed, columns, rows))
            .toList();
    }

    private static Placed place(Marker marker, MapViewport viewport, int columns, int rows) {
        MapViewport.GridPoint point = viewport.project(marker.x(), marker.z(), columns, rows);
        return new Placed(marker.id(), marker.label(), point.column(), point.row());
    }

    private static boolean visible(Placed marker, int columns, int rows) {
        return marker.column() >= 0.0 && marker.column() <= columns - 1
            && marker.row() >= 0.0 && marker.row() <= rows - 1;
    }

    public record Marker(String id, String label, int x, int z) {
        public Marker {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Map marker id must not be blank");
            }
            label = label == null ? "" : label;
        }
    }

    public record Placed(String id, String label, double column, double row) {}
}
