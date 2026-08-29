package dev.fullmoon.client.mods;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Normalized multi-word search over mods. */
public final class ModSearch {
    private ModSearch() {}

    public static List<ModEntry> filter(List<ModEntry> mods, String query) {
        List<ModEntry> source = List.copyOf(Objects.requireNonNull(mods, "mods"));
        String normalized = normalize(Objects.requireNonNull(query, "query"));
        if (normalized.isBlank()) {
            return source;
        }

        String[] words = normalized.split("\\s+");
        List<ModEntry> found = new ArrayList<>();
        for (ModEntry mod : source) {
            String corpus = corpusOf(mod);
            boolean matches = true;
            for (String word : words) {
                if (!corpus.contains(word)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                found.add(mod);
            }
        }
        return List.copyOf(found);
    }

    private static String corpusOf(ModEntry mod) {
        return normalize(String.join(" ", mod.id(), mod.name(), mod.description(),
            String.join(" ", mod.authors())));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .strip();
    }
}
