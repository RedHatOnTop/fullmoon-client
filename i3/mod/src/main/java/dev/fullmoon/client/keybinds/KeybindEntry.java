package dev.fullmoon.client.keybinds;

import java.util.Objects;

/** Immutable snapshot of a key mapping for search, listing, and conflict detection. */
public record KeybindEntry(
        String id,
        String category,
        String label,
        String boundKey,
        int keyCode,
        boolean isDefault,
        boolean isUnbound) {

    public KeybindEntry {
        id = required(id, "id");
        category = required(category, "category");
        label = required(label, "label");
        boundKey = required(boundKey, "boundKey");
    }

    private static String required(String value, String name) {
        String present = Objects.requireNonNull(value, name).strip();
        if (present.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return present;
    }
}
