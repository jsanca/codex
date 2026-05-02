package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command to remove a field by key from the schema of an existing {@link codex.codex.api.model.entity.ContentType}.
 * <p>
 * Field modification is only permitted while the content type is in {@code DRAFT} status.
 * The {@code Actor} is passed separately to the service method.
 *
 * @param siteKey         the site scope of the target content type; must not be null
 * @param contentTypeKey  the key of the target content type; must not be null
 * @param fieldKey        the key of the field to remove; must not be null
 */
public record RemoveContentTypeFieldCommand(SiteKey siteKey, ContentTypeKey contentTypeKey, FieldKey fieldKey) {

    public RemoveContentTypeFieldCommand {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(fieldKey, "fieldKey must not be null");
    }

    /**
     * Creates a {@link RemoveContentTypeFieldCommand} using typed identity values.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param fieldKey       the field key to remove; must not be null
     * @return a new command instance
     */
    public static RemoveContentTypeFieldCommand of(final SiteKey siteKey,
                                                   final ContentTypeKey contentTypeKey,
                                                   final FieldKey fieldKey) {
        return new RemoveContentTypeFieldCommand(siteKey, contentTypeKey, fieldKey);
    }

    /**
     * Creates a {@link RemoveContentTypeFieldCommand} using raw string identity values.
     *
     * @param siteKey        the site scope as a string; must not be null
     * @param contentTypeKey the content type key as a string; must not be null
     * @param fieldKey       the field key as a string; must not be null
     * @return a new command instance
     */
    public static RemoveContentTypeFieldCommand of(final String siteKey,
                                                   final String contentTypeKey,
                                                   final String fieldKey) {
        return new RemoveContentTypeFieldCommand(
                SiteKey.of(siteKey), ContentTypeKey.of(contentTypeKey), FieldKey.of(fieldKey));
    }
}
