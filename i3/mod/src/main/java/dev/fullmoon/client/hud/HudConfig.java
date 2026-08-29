package dev.fullmoon.client.hud;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Serializable HUD configuration state. */
public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class ElementState {
        public boolean enabled;
        public String anchor;
        public int offsetX;
        public int offsetY;
        public float scale = 1.0f;

        public ElementState() {}

        public ElementState(boolean enabled, String anchor, int offsetX, int offsetY, float scale) {
            this.enabled = enabled;
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
        }
    }

    public Map<String, ElementState> elements = new LinkedHashMap<>();
    public int gridSnap = 4;

    public static HudConfig load(Path path) {
        if (!Files.exists(path)) {
            return new HudConfig();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            HudConfig config = GSON.fromJson(reader, HudConfig.class);
            return config != null ? config : new HudConfig();
        } catch (Exception e) {
            return new HudConfig();
        }
    }

    public void save(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static HudConfig fromJson(String json) {
        HudConfig config = GSON.fromJson(json, HudConfig.class);
        return config != null ? config : new HudConfig();
    }
}
