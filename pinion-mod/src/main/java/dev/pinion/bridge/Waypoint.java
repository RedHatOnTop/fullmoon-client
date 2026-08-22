package dev.pinion.bridge;

/** One destination the server has told us about.
 *
 *  Everything here is server data, echoed verbatim into the warp screen —
 *  the client never invents a place to go, and never moves anyone anywhere:
 *  a click only asks, and the server's answer is the truth. */
public record Waypoint(String id, String name, String icon, int x, int y, int z,
                       String world, String group) {
}
