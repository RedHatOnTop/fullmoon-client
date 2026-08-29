package dev.fullmoon.client.settings;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Search over the exact setting copy a player sees, in catalog order. */
public final class SettingSearch {
    private SettingSearch() {}

    public record Entry(String id, String section, String label, String description,
            List<String> aliases) {
        public Entry {
            id = required(id, "id");
            section = required(section, "section");
            label = required(label, "label");
            description = Objects.requireNonNull(description, "description");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            if (aliases.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("aliases must not contain blank values");
            }
        }

        private String corpus() {
            return normalize(String.join(" ", id, section, label, description,
                String.join(" ", aliases)));
        }
    }

    public static List<Entry> filter(List<Entry> catalog, String query) {
        List<Entry> source = List.copyOf(Objects.requireNonNull(catalog, "catalog"));
        String normalized = normalize(Objects.requireNonNull(query, "query"));
        if (normalized.isBlank()) {
            return source;
        }

        String[] words = normalized.split("\\s+");
        List<Entry> found = new ArrayList<>();
        for (Entry entry : source) {
            String corpus = entry.corpus();
            boolean matches = true;
            for (String word : words) {
                if (!corpus.contains(word)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                found.add(entry);
            }
        }
        return List.copyOf(found);
    }

    private static String required(String value, String name) {
        String present = Objects.requireNonNull(value, name).strip();
        if (present.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return present;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .strip();
    }
}
