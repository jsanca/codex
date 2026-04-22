package codex.codex.api.model.entity;

import codex.codex.api.model.identity.FieldSettingKey;
import java.util.Map;
import java.util.Objects;

/**
 * A typed field configuration entry.
 * Settings are different from constraints: they describe configuration,
 * rendering hints, adapter hints, or field-specific variables.
 */
public record FieldSetting(
        FieldSettingKey key,
        Object value,
        Map<String, Object> metadata
) {
    /**
     * Canonical constructor for FieldSetting.
     * 
     * @param key the setting key, cannot be null
     * @param value the value associated with the setting
     * @param metadata additional configuration metadata, defaults to empty map if null
     */
    public FieldSetting {
        Objects.requireNonNull(key, "Field setting key cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Creates a FieldSetting with empty metadata.
     * 
     * @param key the setting key
     * @param value the setting value
     * @return a new FieldSetting instance
     */
    public static FieldSetting of(FieldSettingKey key, Object value) {
        return new FieldSetting(key, value, Map.of());
    }
}
