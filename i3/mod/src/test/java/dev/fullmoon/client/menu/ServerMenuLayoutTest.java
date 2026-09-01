package dev.fullmoon.client.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fullmoon.client.layout.Box;

import org.junit.jupiter.api.Test;

final class ServerMenuLayoutTest {
    @Test
    void slotsKeepTheNineColumnServerGeometry() {
        ServerMenuLayout layout = ServerMenuLayout.fit(new Box(40, 70, 630, 252), 6);

        Box first = layout.slot(0);
        Box nextColumn = layout.slot(1);
        Box nextRow = layout.slot(9);

        assertEquals(first.y(), nextColumn.y());
        assertTrue(nextColumn.x() > first.x());
        assertEquals(first.x(), nextRow.x());
        assertTrue(nextRow.y() > first.y());
        assertTrue(layout.slot(53).bottom() <= 322);
    }

    @Test
    void smallerMenusUseTheAvailableAreaWithoutChangingSlotIdentity() {
        ServerMenuLayout layout = ServerMenuLayout.fit(new Box(10, 20, 450, 120), 3);

        assertEquals(9, layout.columns());
        assertEquals(3, layout.rows());
        assertEquals(layout.slot(0).w(), layout.slot(26).w());
        assertEquals(layout.slot(0).h(), layout.slot(26).h());
        assertTrue(layout.slot(26).right() <= 460);
        assertTrue(layout.slot(26).bottom() <= 140);
    }
}
