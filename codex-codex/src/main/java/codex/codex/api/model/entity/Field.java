package codex.codex.api.model.entity;

import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.FieldType;
import java.util.List;
import java.util.Objects;

/**
 * Defines a field inside a content type version.
 * <p>
 * This is a domain model type and intentionally stays free from
 * persistence or framework concerns.
 */
public record Field(
        FieldKey key,
        String displayName,
        FieldType type,
        boolean required,
        boolean repeatable,
        Object defaultValue,
        List<FieldConstraint> constraints,
        List<FieldSetting> settings,
        boolean searchable,
        boolean filterable,
        boolean sortable,
        boolean localized
) {
    /**
     * Canonical constructor for Field.
     * 
     * @param key the stable semantic key, cannot be null
     * @param displayName the human-friendly display name
     * @param type the data type of the field
     * @param required whether the field is mandatory
     * @param repeatable whether the field can contain multiple values
     * @param defaultValue optional default value
     * @param constraints validation constraints
     * @param settings configuration settings
     * @param searchable whether the field is searchable
     * @param filterable whether the field is filterable
     * @param sortable whether the field is sortable
     * @param localized whether the field value is locale-dependent
     */
    public Field {
        Objects.requireNonNull(key, "Field key cannot be null");

        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isBlank()) {
                displayName = null;
            }
        }

        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        settings = settings == null ? List.of() : List.copyOf(settings);
    }

    /**
     * Creates a new builder for Field.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Field}.
     */
    public static final class Builder {
        private FieldKey key;
        private String displayName;
        private FieldType type;
        private boolean required;
        private boolean repeatable;
        private Object defaultValue;
        private List<FieldConstraint> constraints;
        private List<FieldSetting> settings;
        private boolean searchable;
        private boolean filterable;
        private boolean sortable;
        private boolean localized;

        public Builder key(FieldKey key) {
            this.key = key;
            return this;
        }

        public Builder key(String key) {
            this.key = FieldKey.of(key);
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder type(FieldType type) {
            this.type = type;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder constraints(List<FieldConstraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder settings(List<FieldSetting> settings) {
            this.settings = settings;
            return this;
        }

        public Builder searchable(boolean searchable) {
            this.searchable = searchable;
            return this;
        }

        public Builder filterable(boolean filterable) {
            this.filterable = filterable;
            return this;
        }

        public Builder sortable(boolean sortable) {
            this.sortable = sortable;
            return this;
        }

        public Builder localized(boolean localized) {
            this.localized = localized;
            return this;
        }

        /**
         * Builds a new Field instance.
         * 
         * @return a new Field instance
         */
        public Field build() {
            return new Field(
                    key,
                    displayName,
                    type,
                    required,
                    repeatable,
                    defaultValue,
                    constraints,
                    settings,
                    searchable,
                    filterable,
                    sortable,
                    localized
            );
        }
    }
}
