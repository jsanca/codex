package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.identity.ContentItemId;
import codex.fundamentum.api.model.IdentityGenerator;

import java.util.Objects;

/**
 * Generates a deterministic {@link ContentItemId} from a {@link CreateContentItemCommand}.
 * <p>
 * Identity is based on {@code siteKey + contentTypeKey + contentItemKey}, so the same
 * key triple always produces the same identifier and different triples produce different ones.
 * <p>
 * Produces the same value as {@link ContentItemId#forItem(
 * codex.codex.api.model.identity.SiteKey,
 * codex.codex.api.model.identity.ContentTypeKey,
 * codex.codex.api.model.identity.ContentItemKey)}.
 */
class ContentItemIdentityGenerator implements IdentityGenerator<CreateContentItemCommand, ContentItemId> {

    @Override
    public ContentItemId nextIdentity(final CreateContentItemCommand source) {
        Objects.requireNonNull(source, "source cannot be null");
        return ContentItemId.forItem(source.siteKey(), source.contentTypeKey(), source.key());
    }
}
