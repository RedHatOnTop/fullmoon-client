package dev.pinion.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /** Display order for the in-game editor. The file is a map as far as
     *  lookups are concerned; the screen needs a stable list. */
    private static final String[] ORDER = {"fps", "cps", "coords", "ping", "keystrokes", "gear", "potion"};

    private static Map<String, Module> modules = defaults();
    private static long lastStat;
    private static long lastMtime = -1;

    private HudSettings() {
    }

    public static Module get(String id) {
        return modules.getOrDefault(id, Module.OFF);
    }

    /** Only the modules this build can actually draw. */
    public static List<String> moduleIds() {
        List<String> out = new ArrayList<>();
        for (String id : ORDER) {
            if (modules.containsKey(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Flip a module from inside the game and write it back to the file the
     *  launcher reads, so the two editors never disagree. Placement stays
     *  whatever it was — this screen switches modules, it does not move them. */
    public static void setEnabled(String id, boolean enabled) {
        Module m = get(id);
        modules.put(id, new Module(enabled, m.x(), m.y(), m.scale()));
        save();
    }

    /** Resize from inside the game. Placement stays the launcher's — this
     *  screen switches modules and sizes them, it does not move them. */
    public static void setScale(String id, float scale) {
        Module m = get(id);
        modules.put(id, new Module(m.enabled(), m.x(), m.y(), Math.max(0.5f, Math.min(2f, scale))));
        save();
    }

    static int enabledCount() {
        int n = 0;
        for (String id : moduleIds()) {
            if (get(id).enabled()) {
                n++;
            }
        }
        return n;
    }

    private static void save() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String id : ORDER) {
            Module m = modules.get(id);
            if (m == null) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("enabled", m.enabled());
            o.addProperty("x", m.x());
            o.addProperty("y", m.y());
            o.addProperty("scale", m.scale());
            arr.add(o);
        }
        root.add("modules", arr);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, root.toString(), StandardCharsets.UTF_8);
            // our own write must not read back as someone else's edit
            lastMtime = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            PinionClient.LOG.warn("could not write {}: {}", FILE, e.toString());
        }
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
        out.put("gear", new Module(true, 4, 60, 1));
        out.put("potion", new Module(true, 93, 16, 1));
        return out;
    }
}
