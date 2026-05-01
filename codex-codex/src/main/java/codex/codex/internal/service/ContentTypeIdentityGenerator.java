package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.identity.ContentTypeId;
import codex.fundamentum.api.model.IdentityGenerator;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Generates a deterministic {@link ContentTypeId} from a {@link CreateContentTypeCommand}.
 * <p>
 * Identity is based on {@code siteKey + contentTypeKey}, so the same key in different
 * site scopes produces different identifiers and the same key in the same site always
 * produces the same identifier.
 */
class ContentTypeIdentityGenerator implements IdentityGenerator<CreateContentTypeCommand, ContentTypeId> {

    @Override
    public ContentTypeId nextIdentity(final CreateContentTypeCommand source) {
        Objects.requireNonNull(source, "source cannot be null");
        final String identitySource = "content-type:" + source.siteKey().value() + ":" + source.key().value();
        final UUID uuid = UUID.nameUUIDFromBytes(identitySource.getBytes(StandardCharsets.UTF_8));
        return ContentTypeId.of(uuid.toString());
    }
}
