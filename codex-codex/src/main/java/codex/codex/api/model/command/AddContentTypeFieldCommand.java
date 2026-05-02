package codex.codex.api.model.command;

import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command to add a {@link Field} to the schema of an existing {@link codex.codex.api.model.entity.ContentType}.
 * <p>
 * Field modification is only permitted while the content type is in {@code DRAFT} status.
 * The {@code Actor} is passed separately to the service method.
 *
 * @param siteKey         the site scope of the target content type; must not be null
 * @param contentTypeKey  the key of the target content type; must not be null
 * @param field           the field to add; must not be null
 */
public record AddContentTypeFieldCommand(SiteKey siteKey, ContentTypeKey contentTypeKey, Field field) {

    public AddContentTypeFieldCommand {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(field, "field must not be null");
    }

    /**
     * Creates an {@link AddContentTypeFieldCommand} using typed identity values.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param field          the field to add; must not be null
     * @return a new command instance
     */
    public static AddContentTypeFieldCommand of(final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final Field field) {
        return new AddContentTypeFieldCommand(siteKey, contentTypeKey, field);
    }

    /**
     * Creates an {@link AddContentTypeFieldCommand} using raw string identity values.
     *
     * @param siteKey        the site scope as a string; must not be null
     * @param contentTypeKey the content type key as a string; must not be null
     * @param field          the field to add; must not be null
     * @return a new command instance
     */
    public static AddContentTypeFieldCommand of(final String siteKey,
                                                final String contentTypeKey,
                                                final Field field) {
        return new AddContentTypeFieldCommand(SiteKey.of(siteKey), ContentTypeKey.of(contentTypeKey), field);
    }
}
