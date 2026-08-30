package dev.fullmoon.client.warp;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import dev.fullmoon.client.network.BridgeProtocol;

public final class WarpRoutes {
    private static final Comparator<BridgeProtocol.Waypoint> ORDER =
        Comparator.comparing(BridgeProtocol.Waypoint::group, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(BridgeProtocol.Waypoint::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(BridgeProtocol.Waypoint::id);

    private WarpRoutes() {}

    public static List<BridgeProtocol.Waypoint> ordered(
            List<BridgeProtocol.Waypoint> waypoints) {
        Objects.requireNonNull(waypoints, "waypoints");
        return waypoints.stream().sorted(ORDER).toList();
    }

    public static int distanceMeters(
            BridgeProtocol.Waypoint waypoint, double x, double y, double z) {
        Objects.requireNonNull(waypoint, "waypoint");
        double dx = x - (waypoint.x() + 0.5);
        double dy = y - waypoint.y();
        double dz = z - (waypoint.z() + 0.5);
        return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
