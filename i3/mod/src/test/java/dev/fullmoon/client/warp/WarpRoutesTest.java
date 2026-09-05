package dev.fullmoon.client.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import dev.fullmoon.client.network.BridgeProtocol;

import org.junit.jupiter.api.Test;

final class WarpRoutesTest {
    @Test
    void routesAreOrderedByGroupNameAndIdWithoutMutatingTheSnapshot() {
        BridgeProtocol.Waypoint west = waypoint("west", "West Gate", "gate", 3, 4, 12);
        BridgeProtocol.Waypoint keep = waypoint("keep", "Main Keep", "palace", 0, 64, 0);
        BridgeProtocol.Waypoint gate = waypoint("gate", "Palace Gate", "palace", 0, 64, 0);
        List<BridgeProtocol.Waypoint> snapshot = new ArrayList<>(List.of(gate, west, keep));

        List<BridgeProtocol.Waypoint> ordered = WarpRoutes.ordered(snapshot);

        assertEquals(List.of(gate, west, keep), snapshot);
        assertEquals(List.of(west, keep, gate), ordered);
        assertThrows(UnsupportedOperationException.class, () -> ordered.clear());
    }

    @Test
    void distanceUsesTheSameThreeDimensionalFloorAsTheServerFallback() {
        BridgeProtocol.Waypoint route = waypoint("gate", "Gate", "palace", 3, 4, 12);

        assertEquals(13, WarpRoutes.distanceMeters(route, 0, 0, 0));
        assertEquals(0, WarpRoutes.distanceMeters(route, 3.5, 4, 12.5));
    }

    @Test
    void namesOnlyTheRefusalsTheProtocolDefinesAndCallsTheRestTheServersOwn() {
        assertEquals("cooldown", WarpRoutes.reasonKey("cooldown"));
        assertEquals("permission", WarpRoutes.reasonKey("permission"));
        assertEquals("world", WarpRoutes.reasonKey("world"));
        assertEquals("unknown", WarpRoutes.reasonKey("unknown"));
        assertEquals("unloaded", WarpRoutes.reasonKey("unloaded"));
        assertEquals("timeout", WarpRoutes.reasonKey("timeout"));
        assertEquals("client_send", WarpRoutes.reasonKey("client_send"));
        assertEquals("server", WarpRoutes.reasonKey("Cooldown"));
        assertEquals("server", WarpRoutes.reasonKey(""));
        assertEquals("server", WarpRoutes.reasonKey(null));
    }

    private static BridgeProtocol.Waypoint waypoint(
            String id, String name, String group, int x, int y, int z) {
        return new BridgeProtocol.Waypoint(id, name, "moon", x, y, z, "world", group, "");
    }
}
