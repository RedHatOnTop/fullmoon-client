package dev.pinion.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** What the in-game panel owns: `<instance>/minecraft/pinion/client.json`.
 *
 *  Deliberately a second file rather than more keys in `hud.json`. The launcher
 *  rewrites `hud.json` whole every time its editor moves a module, so anything
 *  this side added there would come back deleted; this file is the mod's, and
 *  nothing else writes it. Fullbright surviving a restart is the whole point —
 *  a visual toggle that forgets is a toggle the player re-presses every session. */
public final class ClientSettings {
    private static final Path FILE = Path.of("pinion", "client.json");

    /** vanilla's own floor — under it the option logs "Illegal option value"
     *  and snaps back to whatever the player had */
    static final int FOV_MIN = 20;
    static final int FOV_MAX = 70;

    private static boolean fullbright;
    private static int zoomFov = 30;
    private static float zoomSensitivity = 0.35f;
    private static boolean loaded;

    private ClientSettings() {
    }

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (!Files.isReadable(FILE)) {
                return;
            }
            JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (o.has("fullbright")) {
                fullbright = o.get("fullbright").getAsBoolean();
            }
            if (o.has("zoomFov")) {
                zoomFov = clampFov(o.get("zoomFov").getAsInt());
            }
            if (o.has("zoomSensitivity")) {
                zoomSensitivity = clampSens(o.get("zoomSensitivity").getAsFloat());
            }
        } catch (IOException | RuntimeException e) {
            PinionClient.LOG.warn("could not read {}: {}", FILE, e.toString());
        }
    }

    private static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("fullbright", fullbright);
        o.addProperty("zoomFov", zoomFov);
        o.addProperty("zoomSensitivity", zoomSensitivity);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, o.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PinionClient.LOG.warn("could not write {}: {}", FILE, e.toString());
        }
    }

    public static boolean fullbright() {
        load();
        return fullbright;
    }

    static void setFullbright(boolean on) {
        load();
        fullbright = on;
        save();
    }

    static int zoomFov() {
        load();
        return zoomFov;
    }

    static void setZoomFov(int fov) {
        load();
        zoomFov = clampFov(fov);
        save();
    }

    static float zoomSensitivity() {
        load();
        return zoomSensitivity;
    }

    static void setZoomSensitivity(float s) {
        load();
        zoomSensitivity = clampSens(s);
        save();
    }

    private static int clampFov(int v) {
        return Math.max(FOV_MIN, Math.min(FOV_MAX, v));
    }

    private static float clampSens(float v) {
        return Math.max(0.05f, Math.min(1f, v));
    }
}
