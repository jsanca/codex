package codex.codex.api.model.identity;

import java.util.Objects;

/**
 * A lightweight strongly typed key for field settings.
 * Known constants provide guidance without preventing future custom keys.
 */
public record FieldSettingKey(String value) {
    public static final FieldSettingKey ACCEPT = new FieldSettingKey("accept");
    public static final FieldSettingKey DATASOURCE = new FieldSettingKey("datasource");
    public static final FieldSettingKey PLACEHOLDER = new FieldSettingKey("placeholder");
    public static final FieldSettingKey UI_HINT = new FieldSettingKey("uiHint");
    public static final FieldSettingKey CUSTOM = new FieldSettingKey("custom");

    public FieldSettingKey {
        Objects.requireNonNull(value, "Field setting key value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Field setting key value cannot be blank");
        }
    }

    public static FieldSettingKey of(String value) {
        return new FieldSettingKey(value);
    }
}