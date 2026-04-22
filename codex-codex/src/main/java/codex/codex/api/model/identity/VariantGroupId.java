package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier for grouping different language variants of the same logical content.
 */
public record VariantGroupId(String value) {
    /**
     * Canonical constructor for VariantGroupId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public VariantGroupId {
        Objects.requireNonNull(value, "VariantGroupId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("VariantGroupId value cannot be blank");
        }
    }

    /**
     * Creates a VariantGroupId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new VariantGroupId instance
     */
    public static VariantGroupId of(String value) {
        return new VariantGroupId(value);
    }

    /**
     * Generates a new VariantGroupId using a random UUID.
     * 
     * @return a new VariantGroupId instance with a random UUID value
     */
    public static VariantGroupId generate() {
        return new VariantGroupId(UUID.randomUUID().toString());
    }
}
