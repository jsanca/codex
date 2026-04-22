package codex.codex.api.model.identity;

import java.util.Objects;

/**
 * Stable semantic key for a field definition inside a content type version.
 * <p>
 * A field key is part of the schema language of Codex and is intended to be
 * more expressive and safer than using raw strings everywhere.
 */
public record FieldKey(String value) {

    /**
     * Predefined key for the title field.
     */
    public static final FieldKey TITLE = new FieldKey("title");

    /**
     * Canonical constructor for FieldKey.
     * 
     * @param value the raw key value, cannot be null or blank
     */
    public FieldKey {
        Objects.requireNonNull(value, "FieldKey value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("FieldKey value cannot be blank");
        }
    }

    /**
     * Creates a FieldKey from a raw string value.
     * 
     * @param value the raw string value
     * @return a new FieldKey instance
     */
    public static FieldKey of(String value) {
        return new FieldKey(value);
    }
}
