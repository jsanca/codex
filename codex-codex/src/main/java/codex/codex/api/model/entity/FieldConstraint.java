package codex.codex.api.model.entity;

import codex.codex.api.model.value.FieldConstraintType;
import java.util.Map;
import java.util.Objects;

/**
 * A typed validation or behavioral restriction applied to a field.
 */
public record FieldConstraint(
        FieldConstraintType type,
        Object value,
        Map<String, Object> metadata
) {
    /**
     * Canonical constructor for FieldConstraint.
     * 
     * @param type the type of constraint, cannot be null
     * @param value the value associated with the constraint
     * @param metadata additional configuration metadata, defaults to empty map if null
     */
    public FieldConstraint {
        Objects.requireNonNull(type, "Field constraint type cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Creates a FieldConstraint with empty metadata.
     * 
     * @param type the type of constraint
     * @param value the constraint value
     * @return a new FieldConstraint instance
     */
    public static FieldConstraint of(FieldConstraintType type, Object value) {
        return new FieldConstraint(type, value, Map.of());
    }
}
