package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * A forwarding interface that delegates all {@link ContentTypeService} method calls to a delegate
 * returned by {@link #getDelegate()}. Extend this interface to implement decorators that
 * only override methods where they add behavior, eliminating forwarding boilerplate.
 *
 * @author jsanca
 */
public interface ForwardingContentTypeService extends ContentTypeService {

    /**
     * Returns the delegate {@link ContentTypeService} to which all calls are forwarded.
     *
     * @return the underlying delegate; must not be null
     */
    ContentTypeService getDelegate();

    @Override
    default ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        return getDelegate().create(command, actor);
    }

    @Override
    default ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        return getDelegate().activate(command, actor);
    }

    @Override
    default ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        return getDelegate().archive(command, actor);
    }

    @Override
    default Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key, final Actor actor) {
        return getDelegate().findByKey(siteKey, key, actor);
    }

    @Override
    default List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
        return getDelegate().findBySiteKey(siteKey, actor);
    }

    @Override
    default List<ContentType> findAll(final Actor actor) {
        return getDelegate().findAll(actor);
    }
}
