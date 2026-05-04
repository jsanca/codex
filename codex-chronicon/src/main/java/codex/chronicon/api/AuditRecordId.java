package codex.chronicon.api;

import java.util.Objects;

/**
 * Unique identifier for an {@link AuditRecord}.
 * <p>
 * Use {@link #of(String)} to create an id from any stable string.
 * The value is trimmed and must not be blank.
 */
public record AuditRecordId(String value) {

    /**
     * Canonical constructor for {@link AuditRecordId}.
     *
     * @param value the identifier value; must not be null or blank
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is blank after trimming
     */
    public AuditRecordId {
        Objects.requireNonNull(value, "AuditRecordId value must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("AuditRecordId value must not be blank");
        }
    }

    /**
     * Creates an {@link AuditRecordId} from a raw string value.
     *
     * @param value the identifier value; must not be null or blank
     * @return a new {@code AuditRecordId}
     */
    public static AuditRecordId of(final String value) {
        return new AuditRecordId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
