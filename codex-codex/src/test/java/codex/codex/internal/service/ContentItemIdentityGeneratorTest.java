package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemIdentityGenerator}.
 */
class ContentItemIdentityGeneratorTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");

    private final ContentItemIdentityGenerator generator = new ContentItemIdentityGenerator();

    @Test
    void nullCommandIsRejected() {
        assertThrows(NullPointerException.class, () -> generator.nextIdentity(null));
    }

    @Test
    void sameCommandProducesSameContentItemId() {
        final CreateContentItemCommand command = command(SITE_KEY, CT_KEY, ITEM_KEY);
        final ContentItemId first = generator.nextIdentity(command);
        final ContentItemId second = generator.nextIdentity(command);
        assertEquals(first, second);
    }

    @Test
    void differentSiteKeyProducesDifferentContentItemId() {
        final ContentItemId id1 = generator.nextIdentity(command(SiteKey.of("acme"), CT_KEY, ITEM_KEY));
        final ContentItemId id2 = generator.nextIdentity(command(SiteKey.of("beta"), CT_KEY, ITEM_KEY));
        assertNotEquals(id1, id2);
    }

    @Test
    void differentContentTypeKeyProducesDifferentContentItemId() {
        final ContentItemId id1 = generator.nextIdentity(command(SITE_KEY, ContentTypeKey.of("blog-post"), ITEM_KEY));
        final ContentItemId id2 = generator.nextIdentity(command(SITE_KEY, ContentTypeKey.of("product"), ITEM_KEY));
        assertNotEquals(id1, id2);
    }

    @Test
    void differentContentItemKeyProducesDifferentContentItemId() {
        final ContentItemId id1 = generator.nextIdentity(command(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")));
        final ContentItemId id2 = generator.nextIdentity(command(SITE_KEY, CT_KEY, ContentItemKey.of("other-post")));
        assertNotEquals(id1, id2);
    }

    private static CreateContentItemCommand command(final SiteKey siteKey,
                                                     final ContentTypeKey contentTypeKey,
                                                     final ContentItemKey itemKey) {
        return CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                Map.of(FieldKey.of("title"), "Test Title"));
    }
}
