package dev.fullmoon.client.map;

import java.util.ArrayList;
import java.util.List;

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
