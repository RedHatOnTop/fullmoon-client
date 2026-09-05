package dev.fullmoon.client.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.fullmoon.client.layout.Box;

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

    /**
     * The same markers, with every label that would land on a kept one blanked.
     *
     * <p>Ledger order decides who keeps the name, so a coarse scale drops the same labels every
     * frame instead of flickering between them. The ring stays on every marker either way: a
     * dropped label costs the name, never the destination.
     */
    public static List<Placed> declutter(List<Placed> markers, LabelBox labels) {
        if (markers == null || labels == null) {
            throw new IllegalArgumentException("Map markers and a label box are required");
        }
        List<Box> kept = new ArrayList<>();
        List<Placed> named = new ArrayList<>();
        for (Placed marker : List.copyOf(markers)) {
            Box box = marker.label().isBlank() ? Box.EMPTY : labels.of(marker);
            if (!box.empty() && kept.stream().noneMatch(box::overlaps)) {
                kept.add(box);
                named.add(marker);
            } else {
                named.add(new Placed(marker.id(), "", marker.column(), marker.row()));
            }
        }
        return List.copyOf(named);
    }

    /**
     * The marker under a point in cell space, or empty.
     *
     * <p>Nearest wins and ledger order breaks a tie, because two markers a player cannot tell
     * apart at this scale have to resolve the same way every frame: a hint that names one route
     * and a click that chooses another is a surface lying about where the pointer is.
     */
    public static Optional<Placed> at(List<Placed> markers, double column, double row,
            double radiusCells) {
        if (markers == null || !(radiusCells > 0.0)) {
            throw new IllegalArgumentException(
                "Map markers and a positive hit radius are required");
        }
        Placed nearest = null;
        double reach = radiusCells * radiusCells;
        double best = Double.POSITIVE_INFINITY;
        for (Placed marker : List.copyOf(markers)) {
            double dx = marker.column() - column;
            double dz = marker.row() - row;
            double distance = dx * dx + dz * dz;
            if (distance <= reach && distance < best) {
                nearest = marker;
                best = distance;
            }
        }
        return Optional.ofNullable(nearest);
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

    /** The box a label would fill, which only a running client can measure. */
    public interface LabelBox {
        Box of(Placed marker);
    }

    public record Placed(String id, String label, double column, double row) {}
}
