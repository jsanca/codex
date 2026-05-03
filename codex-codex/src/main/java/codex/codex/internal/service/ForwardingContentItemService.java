package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * A forwarding interface that delegates all {@link ContentItemService} method calls to a delegate
 * returned by {@link #getDelegate()}. Extend this interface to implement decorators that
 * only override methods where they add behavior, eliminating forwarding boilerplate.
 *
 * <p>Mirrors the pattern established by {@link ForwardingContentTypeService}.</p>
 */
public interface ForwardingContentItemService extends ContentItemService {

    /**
     * Returns the delegate {@link ContentItemService} to which all calls are forwarded.
     *
     * @return the underlying delegate; must not be null
     */
    ContentItemService getDelegate();

    @Override
    default ContentItem create(final CreateContentItemCommand command, final Actor actor) {
        return getDelegate().create(command, actor);
    }

    @Override
    default Optional<ContentItem> findByKey(final SiteKey siteKey,
                                             final ContentTypeKey contentTypeKey,
                                             final ContentItemKey key,
                                             final Actor actor) {
        return getDelegate().findByKey(siteKey, contentTypeKey, key, actor);
    }

    @Override
    default List<ContentItem> findByContentType(final SiteKey siteKey,
                                                 final ContentTypeKey contentTypeKey,
                                                 final Actor actor) {
        return getDelegate().findByContentType(siteKey, contentTypeKey, actor);
    }

    @Override
    default List<ContentItem> findAll(final Actor actor) {
        return getDelegate().findAll(actor);
    }

    @Override
    default ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
        return getDelegate().publish(command, actor);
    }
}
