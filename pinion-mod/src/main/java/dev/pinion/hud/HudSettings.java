package dev.pinion.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** The launcher's half of the contract: `<instance>/minecraft/pinion/hud.json`.
 *
 *  The launcher owns the file and rewrites it whenever the HUD editor moves a
 *  module, so this side re-reads on mtime change instead of caching once —
 *  a layout edit shows up on the next alt-tab, not the next restart. */
public final class HudSettings {
    /** Layout of one module, in percent of the scaled screen. */
    public record Module(boolean enabled, float x, float y, float scale) {
        static final Module OFF = new Module(false, 0, 0, 1);
    }

    private static final Path FILE = Path.of("pinion", "hud.json");
    private static final long RECHECK_MS = 1000;

    private static Map<String, Module> modules = defaults();
    private static long lastStat;
    private static long lastMtime = -1;

    private HudSettings() {
    }

    public static Module get(String id) {
        return modules.getOrDefault(id, Module.OFF);
    }

    /** Called once per frame; does real work at most once a second. */
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
            modules = parse(Files.readString(FILE, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            // a half-written file during a launcher save is expected; keep the last good layout
            PinionClient.LOG.warn("could not read {}: {}", FILE, e.toString());
        }
    }

    private static Map<String, Module> parse(String json) {
        Map<String, Module> out = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (JsonElement el : root.getAsJsonArray("modules")) {
            JsonObject m = el.getAsJsonObject();
            out.put(
                    m.get("id").getAsString(),
                    new Module(
                            m.get("enabled").getAsBoolean(),
                            m.get("x").getAsFloat(),
                            m.get("y").getAsFloat(),
                            m.has("scale") ? m.get("scale").getAsFloat() : 1f));
        }
        return out;
    }

    /** What the HUD looks like before the launcher has written anything. */
    private static Map<String, Module> defaults() {
        Map<String, Module> out = new HashMap<>();
        out.put("fps", new Module(true, 4, 5, 1));
        out.put("cps", new Module(true, 4, 16, 1));
        out.put("coords", new Module(true, 4, 27, 1));
        out.put("ping", new Module(true, 93, 6, 1));
        out.put("keystrokes", new Module(true, 86, 74, 1));
        return out;
    }
}
