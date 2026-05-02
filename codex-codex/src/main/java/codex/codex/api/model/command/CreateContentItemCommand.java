package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Map;
import java.util.Objects;

/**
 * Command expressing intent to create a new content item.
 * <p>
 * The actor is passed separately to the service. The content type version is
 * resolved by the service against the latest published version of the referenced
 * content type — it is not part of the command.
 */
public record CreateContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key,
        Map<FieldKey, Object> values
) {

    /**
     * Canonical constructor for {@link CreateContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @param values         field values; defaults to empty map if null; null keys and values rejected
     */
    public CreateContentItemCommand {
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
     * Creates a {@link CreateContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @param values         field values; may be null (defaults to empty map)
     * @return a new command instance
     */
    public static CreateContentItemCommand of(final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key,
                                               final Map<FieldKey, Object> values) {
        return new CreateContentItemCommand(siteKey, contentTypeKey, key, values);
    }

    /**
     * Creates a {@link CreateContentItemCommand} using raw string values.
     *
     * @param siteKey        the raw site key
     * @param contentTypeKey the raw content type key
     * @param itemKey        the raw content item key
     * @param values         field values; may be null (defaults to empty map)
     * @return a new command instance
     */
    public static CreateContentItemCommand of(final String siteKey,
                                               final String contentTypeKey,
                                               final String itemKey,
                                               final Map<FieldKey, Object> values) {
        return new CreateContentItemCommand(
                SiteKey.of(siteKey),
                ContentTypeKey.of(contentTypeKey),
                ContentItemKey.of(itemKey),
                values);
    }
}
