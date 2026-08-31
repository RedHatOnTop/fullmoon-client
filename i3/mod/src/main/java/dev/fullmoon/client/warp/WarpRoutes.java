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

    /** The refusals the protocol names. Everything else is the server's own and reads as such. */
    private static final List<String> REASONS = List.of("cooldown", "permission", "world",
        "unknown", "unloaded", "timeout", "client_send");

    private WarpRoutes() {}

    public static List<BridgeProtocol.Waypoint> ordered(
            List<BridgeProtocol.Waypoint> waypoints) {
        Objects.requireNonNull(waypoints, "waypoints");
        return waypoints.stream().sorted(ORDER).toList();
    }

    /**
     * The {@code fullmoon.warp.reason.*} key a server reason maps to.
     *
     * <p>Here rather than on a screen because two surfaces now request the same warp, and a
     * vocabulary copied into both is a vocabulary that drifts in one of them. Anything the server
     * sends that this does not know is the server's own refusal and says so.
     */
    public static String reasonKey(String reason) {
        return reason != null && REASONS.contains(reason) ? reason : "server";
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
