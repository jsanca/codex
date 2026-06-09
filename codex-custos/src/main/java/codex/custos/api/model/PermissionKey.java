package codex.custos.api.model;

import java.util.Objects;

/**
 * Identifies a domain operation that an {@link codex.fundamentum.api.model.Actor} may or may not
 * be allowed to perform.
 * <p>
 * Permission keys use dot-separated namespacing by convention: {@code contentItem.publish},
 * {@code site.archive}, {@code permission.grant}. They are not endpoint paths.
 */
public record PermissionKey(String value) {

    public PermissionKey {
        Objects.requireNonNull(value, "PermissionKey value must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("PermissionKey value must not be blank");
        }
    }

    /**
     * Creates a {@link PermissionKey} from a raw string value.
     *
     * @param value the permission name; trimmed on construction
     * @return a validated {@code PermissionKey}
     */
    public static PermissionKey of(final String value) {
        return new PermissionKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
