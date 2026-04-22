package codex.codex.api.model.entity;

/**
 * First draft of supported field kinds.
 * This can later be extracted to a dedicated type if needed.
 */
public enum FieldType {
    TEXT,
    LONG_TEXT,
    NUMBER,
    BOOLEAN,
    DATE_TIME,
    REFERENCE,
    JSON,
    BINARY
}
