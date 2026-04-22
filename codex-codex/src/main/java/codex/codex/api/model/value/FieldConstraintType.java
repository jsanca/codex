package codex.codex.api.model.entity;

/**
 * First draft of known constraint kinds.
 * This list can grow as the domain becomes clearer.
 */
public enum FieldConstraintType {
    MIN_LENGTH,
    MAX_LENGTH,
    PATTERN,
    MIN_VALUE,
    MAX_VALUE,
    ALLOWED_VALUES,
    MIME_TYPES,
    MAX_ITEMS,
    CUSTOM
}