package dev.pinion.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The launcher's other contract file: `<instance>/minecraft/pinion/cosmetics.json`.
 *
 *  Only the slots that render are read. The art is the launcher's own cape
 *  catalogue, copied into this jar at build time, so equipping in the launcher
 *  and seeing it in game are the same picture rather than two that have to be
 *  kept in step by hand. */
public final class Cosmetics {
    private static final Path FILE = Path.of("pinion", "cosmetics.json");
    private static final long RECHECK_MS = 1000;

    private static ClientAsset.ResourceTexture cape;
    private static long lastStat;
    private static long lastMtime = -1;

    private Cosmetics() {
    }

    /** null when nothing is equipped — the vanilla cape (usually none) stands. */
    public static ClientAsset.ResourceTexture cape() {
        return cape;
    }

    public static void poll() {
        long now = System.currentTimeMillis();
        if (now - lastStat < RECHECK_MS) {
            return;
        }
        lastStat = now;

        try {
            if (!Files.isReadable(FILE)) {
                return;
            }
            long mtime = Files.getLastModifiedTime(FILE).toMillis();
            if (mtime == lastMtime) {
                return;
            }
            lastMtime = mtime;
            cape = read(Files.readString(FILE, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            PinionClient.LOG.warn("could not read {}: {}", FILE, e.toString());
        }
    }

    private static ClientAsset.ResourceTexture read(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("cape") || root.get("cape").isJsonNull()) {
            return null;
        }
        String id = root.get("cape").getAsString();
        return new ClientAsset.ResourceTexture(
                Identifier.fromNamespaceAndPath(PinionClient.MOD_ID, "cosmetics/" + id),
                Identifier.fromNamespaceAndPath(PinionClient.MOD_ID, "textures/cosmetics/" + id + ".png"));
    }
}
