package dev.fullmoon.client.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ModSearchTest {

    private static final ModEntry FULLMOON = new ModEntry(
        "fullmoon", "Fullmoon Client", "3.0.0", "Custom network client for Fullmoon",
        List.of("Fullmoon Team"), "CLIENT");

    private static final ModEntry FABRIC = new ModEntry(
        "fabricloader", "Fabric Loader", "0.16.10", "Modular mod loader",
        List.of("FabricMC"), "UNIVERSAL");

    private static final ModEntry SODIUM = new ModEntry(
        "sodium", "Sodium", "0.6.0", "Modern rendering engine and optimization",
        List.of("jellysquid3", "IMS"), "CLIENT");

    @Test
    void matchesByNameIdAndAuthor() {
        List<ModEntry> mods = List.of(FULLMOON, FABRIC, SODIUM);

        assertEquals(List.of(FULLMOON), ModSearch.filter(mods, "fullmoon"));
        assertEquals(List.of(SODIUM), ModSearch.filter(mods, "rendering optimization"));
        assertEquals(List.of(SODIUM), ModSearch.filter(mods, "jellysquid3"));
        assertEquals(List.of(FABRIC), ModSearch.filter(mods, "fabricloader"));
        assertEquals(mods, ModSearch.filter(mods, ""));
    }
}
