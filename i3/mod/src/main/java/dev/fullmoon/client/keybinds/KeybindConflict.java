package dev.fullmoon.client.keybinds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Conflict analysis over key bindings. */
public final class KeybindConflict {
    private KeybindConflict() {}

    /**
     * Maps each key mapping ID that has one or more conflicts to the list of entries it conflicts with.
     */
    public static Map<String, List<KeybindEntry>> findConflicts(List<KeybindEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<Integer, List<KeybindEntry>> byKey = new HashMap<>();

        for (KeybindEntry entry : entries) {
            if (entry.isUnbound() || entry.keyCode() == -1) {
                continue;
            }
            byKey.computeIfAbsent(entry.keyCode(), k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<KeybindEntry>> conflicts = new HashMap<>();
        for (List<KeybindEntry> group : byKey.values()) {
            if (group.size() > 1) {
                for (KeybindEntry entry : group) {
                    List<KeybindEntry> others = new ArrayList<>(group);
                    others.remove(entry);
                    conflicts.put(entry.id(), Collections.unmodifiableList(others));
                }
            }
        }
        return Collections.unmodifiableMap(conflicts);
    }

    /**
     * Returns the conflicting entries for a specific key mapping.
     */
    public static List<KeybindEntry> conflictsFor(KeybindEntry target, List<KeybindEntry> all) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(all, "all");
        if (target.isUnbound() || target.keyCode() == -1) {
            return List.of();
        }

        List<KeybindEntry> list = new ArrayList<>();
        for (KeybindEntry entry : all) {
            if (!entry.id().equals(target.id()) && !entry.isUnbound() && entry.keyCode() == target.keyCode()) {
                list.add(entry);
            }
        }
        return List.copyOf(list);
    }
}
