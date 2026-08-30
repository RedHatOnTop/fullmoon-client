package dev.fullmoon.client.map;

import java.util.Map;

/** Matches Bukkit's default world folder names to Minecraft dimension identifiers. */
public final class WorldNames {
    private static final Map<String, String> BUKKIT_DIMENSIONS = Map.of(
        "world", "minecraft:overworld",
        "world_nether", "minecraft:the_nether",
        "world_the_end", "minecraft:the_end");

    private WorldNames() {}

    public static boolean matches(String publishedWorld, String dimensionId) {
        if (publishedWorld == null || dimensionId == null) {
            return false;
        }
        String published = publishedWorld.trim();
        String dimension = dimensionId.trim();
        if (published.isEmpty() || dimension.isEmpty()) {
            return false;
        }
        return published.equals(dimension)
            || BUKKIT_DIMENSIONS.getOrDefault(published, "").equals(dimension);
    }
}
