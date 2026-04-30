package codex.codex.internal.service;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * A forwarding interface that delegates all {@link SiteService} method calls to a delegate
 * returned by {@link #getDelegate()}. Extend this interface to implement decorators that
 * only override methods with real behavior, eliminating forwarding boilerplate.
 *
 * @author jsanca
 */
public interface ForwardingSiteService extends SiteService {

    /**
     * Returns the delegate {@link SiteService} to which all calls are forwarded.
     *
     * @return the underlying delegate, must not be null
     */
    SiteService getDelegate();

    @Override
    default Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
        return getDelegate().create(createSiteCommand, actor);
    }

    @Override
    default Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
        return getDelegate().findByKey(siteKey, actor);
    }

    @Override
    default Site start(final StartSiteCommand startSiteCommand, final Actor actor) {
        return getDelegate().start(startSiteCommand, actor);
    }

    @Override
    default Site suspend(final SuspendSiteCommand suspendSiteCommand, final Actor actor) {
        return getDelegate().suspend(suspendSiteCommand, actor);
    }

    @Override
    default Site archive(final ArchiveSiteCommand archiveSiteCommand, final Actor actor) {
        return getDelegate().archive(archiveSiteCommand, actor);
    }

    @Override
    default Site unarchive(final UnarchiveSiteCommand unarchiveSiteCommand, final Actor actor) {
        return getDelegate().unarchive(unarchiveSiteCommand, actor);
    }

    @Override
    default Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
        return getDelegate().findByAlias(alias, actor);
    }

    @Override
    default List<Site> findAll(final Actor actor) {
        return getDelegate().findAll(actor);
    }
}
