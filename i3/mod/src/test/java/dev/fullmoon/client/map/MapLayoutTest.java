package dev.fullmoon.client.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;

import dev.fullmoon.client.layout.Box;

import org.junit.jupiter.api.Test;

class MapLayoutTest {
    private static final int ACTION_WIDTH = 96;
    private static final int ACTION_HEIGHT = 24;

    private static MapLayout wide() {
        return MapLayout.of(1280, 720, ACTION_WIDTH, ACTION_HEIGHT);
    }

    private static TerrainSnapshot snapshot(int width, int height) {
        return new TerrainSnapshot(width, height, Collections.nCopies(width * height,
            TerrainSnapshot.Cell.unmapped(7)));
    }

    @Test
    void planeIsAWholeNumberOfCellsInBothAxes() {
        MapLayout layout = wide();

        assertEquals(0, layout.map().w() % MapLayout.CELL_SIZE);
        assertEquals(0, layout.map().h() % MapLayout.CELL_SIZE);
    }

    @Test
    void planeSurvivesAWindowSmallerThanOneCell() {
        MapLayout layout = MapLayout.of(1, 1, ACTION_WIDTH, ACTION_HEIGHT);

        assertEquals(MapLayout.CELL_SIZE, layout.map().w());
        assertEquals(MapLayout.CELL_SIZE, layout.map().h());
        assertFalse(layout.content().w() < 0);
        assertFalse(layout.content().h() < 0);
    }

    @Test
    void aCompactWindowNarrowsTheRailAndKeepsTheMapWider() {
        MapLayout compact = MapLayout.of(MapLayout.COMPACT_WIDTH - 1, 480,
            ACTION_WIDTH, ACTION_HEIGHT);
        MapLayout roomy = MapLayout.of(MapLayout.COMPACT_WIDTH, 480, ACTION_WIDTH, ACTION_HEIGHT);

        assertTrue(compact.rail().w() < roomy.rail().w());
        assertTrue(compact.map().w() > compact.rail().w());
    }

    @Test
    void chromeStacksHeaderPlaneAndFooterWithoutOverlap() {
        MapLayout layout = wide();

        assertEquals(layout.headerBottom(), layout.map().y());
        assertTrue(layout.map().bottom() <= layout.footerTop());
        assertTrue(layout.footerTop() < layout.content().bottom());
        assertFalse(layout.map().overlaps(layout.rail()));
    }

    @Test
    void theActionSitsInsideTheBandAtItsFoot() {
        MapLayout layout = wide();

        assertEquals(layout.band().bottom() - ACTION_HEIGHT, layout.action().y());
        assertEquals(layout.band().right(), layout.action().right());
        assertTrue(layout.action().w() <= layout.band().w());
        assertEquals(layout.rail().bottom(), layout.band().bottom());
    }

    @Test
    void aNarrowBandStretchesTheActionRatherThanOverflowingIt() {
        MapLayout layout = MapLayout.of(320, 480, 4096, ACTION_HEIGHT);

        assertEquals(layout.band().w(), layout.action().w());
        assertEquals(layout.band().x(), layout.action().x());
    }

    @Test
    void railStepsRunDownwardsAndTheRowsBeginBelowTheHeading() {
        MapLayout layout = wide();

        assertTrue(layout.positionHeading() < layout.centreFact());
        assertTrue(layout.centreFact() < layout.scaleFact());
        assertTrue(layout.scaleFact() < layout.routesHeading());
        assertTrue(layout.routesHeading() < layout.routesTop());
        assertEquals(layout.routesTop(), layout.routeRow(0).y());
    }

    @Test
    void routeRowsStackAndStayInsideTheRail() {
        MapLayout layout = wide();

        Box first = layout.routeRow(0);
        Box second = layout.routeRow(1);

        assertEquals(MapLayout.ROUTE_ROW_HEIGHT, first.h());
        assertEquals(first.bottom(), second.y());
        assertEquals(layout.rail().x(), first.x());
        assertEquals(layout.rail().w(), first.w());
    }

    @Test
    void everyRowItCountsClearsTheActionBand() {
        MapLayout layout = wide();
        int capacity = layout.routeCapacity();

        assertTrue(capacity > 0);
        assertTrue(layout.routeRow(capacity - 1).bottom() <= layout.band().y());
        assertTrue(layout.routeRow(capacity).bottom() > layout.band().y());
    }

    @Test
    void aRailWithNoRoomForRowsCountsNone() {
        MapLayout layout = MapLayout.of(1280, 200, ACTION_WIDTH, ACTION_HEIGHT);

        assertEquals(0, layout.routeCapacity());
        assertEquals(0, layout.visibleRoutes(4));
        assertEquals(4, layout.beyond(4));
    }

    @Test
    void whatTheRailShowsPlusWhatItAdmitsToIsEveryPublishedRoute() {
        MapLayout layout = wide();
        int capacity = layout.routeCapacity();

        assertEquals(capacity, layout.visibleRoutes(capacity + 7));
        assertEquals(7, layout.beyond(capacity + 7));
        assertEquals(0, layout.beyond(capacity));
        assertEquals(1, layout.visibleRoutes(1));
        assertEquals(0, layout.beyond(1));
    }

    @Test
    void aNegativeRouteCountIsNoRoutes() {
        MapLayout layout = wide();

        assertEquals(0, layout.visibleRoutes(-3));
        assertEquals(0, layout.beyond(-3));
    }

    @Test
    void planeIsCentredInWhateverTheWindowLeftForIt() {
        Box bounds = new Box(10, 20, 60, 30);

        Box raster = MapLayout.raster(bounds, 3, snapshot(10, 6));

        assertEquals(new Box(25, 26, 30, 18), raster);
    }

    @Test
    void aCellCoordinateRoundsToTheNearestPixelFromTheOrigin() {
        assertEquals(100, MapLayout.plot(100, 0.0, 3));
        assertEquals(107, MapLayout.plot(100, 2.4, 3));
        assertEquals(108, MapLayout.plot(100, 2.5, 3));
    }

    @Test
    void pointerCellsAreMeasuredFromThePlaneNotTheSlot() {
        MapLayout layout = wide();
        TerrainSnapshot snapshot = snapshot(4, 4);
        Box raster = MapLayout.raster(layout.map(), MapLayout.CELL_SIZE, snapshot);

        Optional<MapViewport.GridPoint> cell = layout.cellAt(
            raster.x() + MapLayout.CELL_SIZE * 2, raster.y() + MapLayout.CELL_SIZE, snapshot);

        assertEquals(Optional.of(new MapViewport.GridPoint(2.0, 1.0)), cell);
    }

    @Test
    void aPointerOffTheMapIsOnNoCell() {
        MapLayout layout = wide();
        TerrainSnapshot snapshot = snapshot(4, 4);

        assertEquals(Optional.empty(),
            layout.cellAt(layout.rail().x(), layout.rail().y(), snapshot));
        assertEquals(Optional.empty(),
            layout.cellAt(layout.map().right(), layout.map().y(), snapshot));
    }

    @Test
    void geometryRefusesArgumentsItCannotMeasure() {
        MapLayout layout = wide();
        TerrainSnapshot snapshot = snapshot(2, 2);

        assertThrows(IllegalArgumentException.class, () -> layout.cellAt(0.0, 0.0, null));
        assertThrows(IllegalArgumentException.class,
            () -> MapLayout.raster(null, 3, snapshot));
        assertThrows(IllegalArgumentException.class,
            () -> MapLayout.raster(Box.EMPTY, 3, null));
        assertThrows(IllegalArgumentException.class,
            () -> MapLayout.raster(Box.EMPTY, 0, snapshot));
    }
}
