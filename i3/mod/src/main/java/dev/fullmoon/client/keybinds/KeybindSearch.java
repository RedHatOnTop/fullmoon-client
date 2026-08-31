package dev.fullmoon.client.keybinds;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Normalized multi-word search over keybindings. */
public final class KeybindSearch {
    private KeybindSearch() {}

    public static List<KeybindEntry> filter(List<KeybindEntry> entries, String query) {
        List<KeybindEntry> source = List.copyOf(Objects.requireNonNull(entries, "entries"));
        String normalized = normalize(Objects.requireNonNull(query, "query"));
        if (normalized.isBlank()) {
            return source;
        }

        String[] words = normalized.split("\\s+");
        List<KeybindEntry> found = new ArrayList<>();
        for (KeybindEntry entry : source) {
            String corpus = corpusOf(entry);
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

    private static String corpusOf(KeybindEntry entry) {
        return normalize(String.join(" ", entry.id(), entry.category(), entry.label(), entry.boundKey()));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .strip();
    }
}
