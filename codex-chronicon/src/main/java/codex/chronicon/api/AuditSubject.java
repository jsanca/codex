package codex.chronicon.api;

import java.util.Objects;

/**
 * Identifies the resource that was the subject of an audited action.
 * <p>
 * A subject has a {@link #type()} (e.g. {@code "site"}, {@code "content-item"}),
 * a stable {@link #id()} (the entity's identity), and an optional human-readable
 * {@link #key()} (e.g. a slug or a content type key).
 * <p>
 * Suggested subject types:
 * <ul>
 *   <li>{@code site}</li>
 *   <li>{@code content-type}</li>
 *   <li>{@code content-type-version}</li>
 *   <li>{@code content-item}</li>
 *   <li>{@code content-revision}</li>
 *   <li>{@code workflow}</li>
 *   <li>{@code user}</li>
 * </ul>
 * The type is kept as a plain string to support future external/sync subjects that are
 * not known at design time.
 */
public record AuditSubject(String type, String id, String key) {

    /**
     * Canonical constructor for {@link AuditSubject}.
     *
     * @param type the resource type; must not be null or blank
     * @param id   the resource identifier; must not be null or blank
     * @param key  a human-readable key; may be null; trimmed if present
     * @throws NullPointerException     if {@code type} or {@code id} is null
     * @throws IllegalArgumentException if {@code type} or {@code id} is blank after trimming
     */
    public AuditSubject {
        Objects.requireNonNull(type, "AuditSubject type must not be null");
        type = type.trim();
        if (type.isBlank()) {
            throw new IllegalArgumentException("AuditSubject type must not be blank");
        }
        Objects.requireNonNull(id, "AuditSubject id must not be null");
        id = id.trim();
        if (id.isBlank()) {
            throw new IllegalArgumentException("AuditSubject id must not be blank");
        }
        if (key != null) {
            key = key.trim();
        }
    }

    /**
     * Creates an {@link AuditSubject} with a human-readable key.
     *
     * @param type the resource type; must not be null or blank
     * @param id   the resource identifier; must not be null or blank
     * @param key  the human-readable key; may be null
     * @return a new {@code AuditSubject}
     */
    public static AuditSubject of(final String type, final String id, final String key) {
        return new AuditSubject(type, id, key);
    }

    /**
     * Creates an {@link AuditSubject} without a human-readable key.
     *
     * @param type the resource type; must not be null or blank
     * @param id   the resource identifier; must not be null or blank
     * @return a new {@code AuditSubject} with a null key
     */
    public static AuditSubject of(final String type, final String id) {
        return new AuditSubject(type, id, null);
    }
}
