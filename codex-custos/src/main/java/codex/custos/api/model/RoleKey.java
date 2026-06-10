package codex.custos.api.model;

import java.util.Objects;

/**
 * Identifies a {@link Role} within Custos.
 * <p>
 * Role keys are case-sensitive and preserved exactly as given. Common values follow
 * SCREAMING_SNAKE_CASE convention (e.g. {@code EDITOR}, {@code SITE_ADMIN}), but the
 * format is not enforced — custom roles may use any non-blank string.
 */
public record RoleKey(String value) {

    public RoleKey {
        Objects.requireNonNull(value, "RoleKey value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RoleKey value must not be blank");
        }
    }

    /**
     * Creates a {@link RoleKey} from a raw string value.
     *
     * @param value the role name; must not be null or blank
     * @return a validated {@code RoleKey}
     */
    public static RoleKey of(final String value) {
        return new RoleKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
