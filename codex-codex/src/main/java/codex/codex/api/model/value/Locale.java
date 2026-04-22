package codex.codex.api.model.value;

import java.util.Objects;

/**
 * Represents a locale variant of content.
 * Follows IETF BCP 47 language tag standards (e.g., en-US, es-CR).
 */
public record Locale(String languageTag) {
    /**
     * Canonical constructor for Locale.
     * 
     * @param languageTag the language tag value, cannot be null or blank
     */
    public Locale {
        Objects.requireNonNull(languageTag, "Locale languageTag cannot be null");
        languageTag = languageTag.trim();
        if (languageTag.isBlank()) {
            throw new IllegalArgumentException("Locale languageTag cannot be blank");
        }
    }

    /**
     * Creates a Locale from a language tag string.
     * 
     * @param languageTag the language tag string
     * @return a new Locale instance
     */
    public static Locale of(String languageTag) {
        return new Locale(languageTag);
    }
}
