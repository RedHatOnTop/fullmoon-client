package dev.fullmoon.client.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import dev.fullmoon.client.layout.Box;

import org.junit.jupiter.api.Test;

class MapMarkersTest {
    @Test
    void recognisesBukkitAndMinecraftDimensionNamesWithoutGuessingOtherWorlds() {
        assertTrue(WorldNames.matches("world", "minecraft:overworld"));
        assertTrue(WorldNames.matches("world_nether", "minecraft:the_nether"));
        assertTrue(WorldNames.matches("world_the_end", "minecraft:the_end"));
        assertTrue(WorldNames.matches("minecraft:overworld", "minecraft:overworld"));
        assertTrue(WorldNames.matches("  world  ", "minecraft:overworld"));
        assertFalse(WorldNames.matches("creative", "minecraft:overworld"));
        assertFalse(WorldNames.matches("", "minecraft:overworld"));
        assertFalse(WorldNames.matches("world", " "));
        assertFalse(WorldNames.matches(null, "minecraft:overworld"));
        assertFalse(WorldNames.matches("world", null));
    }

    @Test
    void projectsOnlyMarkersInsideTheCurrentMap() {
        MapViewport viewport = new MapViewport(0.0, 0.0, 2);
        List<MapMarkers.Marker> markers = List.of(
            new MapMarkers.Marker("spawn", "Spawn", 0, 0),
            new MapMarkers.Marker("edge", "Edge", 9, -7),
            new MapMarkers.Marker("outside", "Outside", 12, 0));

        assertEquals(List.of(
            new MapMarkers.Placed("spawn", "Spawn", 4.5, 3.5),
            new MapMarkers.Placed("edge", "Edge", 9.0, 0.0)),
            MapMarkers.place(markers, viewport, 10, 8));
    }

    @Test
    void clipsMarkersOffEverySideOfTheRaster() {
        MapViewport viewport = new MapViewport(0.0, 0.0, 1);
        List<MapMarkers.Marker> markers = List.of(
            new MapMarkers.Marker("west", "West", -6, 0),
            new MapMarkers.Marker("east", "East", 6, 0),
            new MapMarkers.Marker("north", "North", 0, -5),
            new MapMarkers.Marker("south", "South", 0, 5),
            new MapMarkers.Marker("inside", "Inside", 0, 0));

        assertEquals(List.of("inside"), MapMarkers.place(markers, viewport, 11, 9).stream()
            .map(MapMarkers.Placed::id)
            .toList());
    }

    @Test
    void rejectsBlankMarkerIdsAndCopiesTheInput() {
        assertThrows(IllegalArgumentException.class,
            () -> new MapMarkers.Marker(" ", "Nowhere", 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new MapMarkers.Marker(null, "Nowhere", 0, 0));
        assertEquals("", new MapMarkers.Marker("spawn", null, 0, 0).label());

        List<MapMarkers.Marker> source = new java.util.ArrayList<>(List.of(
            new MapMarkers.Marker("spawn", "Spawn", 0, 0)));
        List<MapMarkers.Placed> placed = MapMarkers.place(
            source, new MapViewport(0.0, 0.0, 1), 3, 3);
        source.clear();

        assertEquals(1, placed.size());
    }

    @Test
    void blanksTheLabelsThatWouldLandOnAKeptOneAndKeepsEveryMarker() {
        List<MapMarkers.Placed> markers = List.of(
            new MapMarkers.Placed("west", "West Gate", 10.0, 4.0),
            new MapMarkers.Placed("east", "East Gate", 12.0, 4.0),
            new MapMarkers.Placed("south", "South Gate", 10.0, 8.0),
            new MapMarkers.Placed("anon", "", 10.0, 4.0));

        List<MapMarkers.Placed> named = MapMarkers.declutter(markers, MapMarkersTest::labelBox);

        assertEquals(List.of("West Gate", "", "South Gate", ""),
            named.stream().map(MapMarkers.Placed::label).toList());
        assertEquals(List.of("west", "east", "south", "anon"),
            named.stream().map(MapMarkers.Placed::id).toList());
    }

    @Test
    void refusesToDeclutterWithoutMarkersOrALabelBox() {
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.declutter(null, MapMarkersTest::labelBox));
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.declutter(List.of(), null));
    }

    /** Four px a character and eight px a line, so the overlap is arithmetic a reader can check. */
    private static Box labelBox(MapMarkers.Placed marker) {
        return new Box((int) (marker.column() * 3), (int) (marker.row() * 3),
            marker.label().length() * 4, 8);
    }

    @Test
    void findsTheNearestMarkerInReachAndBreaksTiesByLedgerOrder() {
        MapMarkers.Placed west = new MapMarkers.Placed("west", "West Gate", 10.0, 4.0);
        MapMarkers.Placed east = new MapMarkers.Placed("east", "East Gate", 12.0, 4.0);
        MapMarkers.Placed twin = new MapMarkers.Placed("twin", "Twin Gate", 10.0, 4.0);
        List<MapMarkers.Placed> markers = List.of(west, east, twin);

        assertEquals(Optional.of(west), MapMarkers.at(markers, 10.4, 4.0, 2.5));
        assertEquals(Optional.of(east), MapMarkers.at(markers, 11.6, 4.0, 2.5));
        assertEquals(Optional.of(west), MapMarkers.at(markers, 10.0, 4.0, 2.5));
        assertEquals(Optional.of(west), MapMarkers.at(List.of(west), 12.5, 4.0, 2.5));
        assertEquals(Optional.empty(), MapMarkers.at(List.of(west), 12.6, 4.0, 2.5));
        assertEquals(Optional.empty(), MapMarkers.at(List.of(), 10.0, 4.0, 2.5));
    }

    @Test
    void refusesToHitTestWithoutMarkersOrAPositiveRadius() {
        assertThrows(IllegalArgumentException.class, () -> MapMarkers.at(null, 0.0, 0.0, 2.5));
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.at(List.of(), 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.at(List.of(), 0.0, 0.0, Double.NaN));
    }

    @Test
    void refusesToPlaceWithoutMarkersOrAViewport() {
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.place(null, new MapViewport(0.0, 0.0, 1), 3, 3));
        assertThrows(IllegalArgumentException.class,
            () -> MapMarkers.place(List.of(), null, 3, 3));
    }
}
