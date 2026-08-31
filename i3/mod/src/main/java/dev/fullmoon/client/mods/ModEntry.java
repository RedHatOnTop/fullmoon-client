package dev.fullmoon.client.mods;

import java.util.List;
import java.util.Objects;

/** Immutable descriptor of a loaded Fabric mod. */
public record ModEntry(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        String environment) {

    public ModEntry {
        id = required(id, "id");
        name = required(name, "name");
        version = required(version, "version");
        description = Objects.requireNonNullElse(description, "").strip();
        authors = List.copyOf(Objects.requireNonNull(authors, "authors"));
        environment = Objects.requireNonNullElse(environment, "CLIENT");
    }

    private static String required(String value, String name) {
        String present = Objects.requireNonNull(value, name).strip();
        if (present.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return present;
    }
}
