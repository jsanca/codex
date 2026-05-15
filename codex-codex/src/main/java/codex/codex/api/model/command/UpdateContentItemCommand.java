package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Map;
import java.util.Objects;

/**
 * Command expressing intent to update the field values of an existing content item's
 * current working revision.
 * <p>
 * The actor is passed separately to the service. The content type version is resolved
 * by the service against the item's current version — it is not part of the command.
 */
public record UpdateContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key,
        Map<FieldKey, Object> values
) {

    /**
     * Canonical constructor for {@link UpdateContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @param values         field values; defaults to empty map if null; null keys and values rejected
     */
    public UpdateContentItemCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        if (values == null) {
            values = Map.of();
        } else {
            for (final var entry : values.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "values map key cannot be null");
                Objects.requireNonNull(entry.getValue(), "values map value cannot be null");
            }
            values = Map.copyOf(values);
        }
    }

    /**
     * Creates an {@link UpdateContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @param values         field values; may be null (defaults to empty map)
     * @return a new command instance
     */
    public static UpdateContentItemCommand of(final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key,
                                               final Map<FieldKey, Object> values) {
        return new UpdateContentItemCommand(siteKey, contentTypeKey, key, values);
    }
}
