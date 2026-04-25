package codex.codex.internal.service.internal;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.event.SiteArchivedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.event.SiteStartedEvent;
import codex.codex.api.model.event.SiteSuspendedEvent;
import codex.codex.api.model.event.SiteUnarchivedEvent;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EventPublishingSiteService implements SiteService {

    private final SiteService delegate;
    private final CodexEventDispatcher eventDispatcher;
    private final Clock clock;

    public EventPublishingSiteService(final SiteService siteService,
                                      final CodexEventDispatcher eventDispatcher,
                                      final Clock clock) {
        this.delegate = Objects.requireNonNull(siteService, "siteService must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
        final Site site = delegate.create(createSiteCommand, actor);

        eventDispatcher.dispatch(new SiteCreatedEvent(
                site.id(),
                site.key(),
                actor,
                clock.instant()
        ));

        return site;
    }

    @Override
    public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
        return delegate.findByKey(siteKey, actor);
    }

    @Override
    public Site start(final StartSiteCommand command, final Actor actor) {
        final Site site = delegate.start(command, actor);

        eventDispatcher.dispatch(new SiteStartedEvent(
                site.id(),
                site.key(),
                actor,
                clock.instant()
        ));

        return site;
    }

    @Override
    public Site suspend(final SuspendSiteCommand command, final Actor actor) {
        final Site site = delegate.suspend(command, actor);

        eventDispatcher.dispatch(new SiteSuspendedEvent(
                site.id(),
                site.key(),
                actor,
                clock.instant()
        ));

        return site;
    }

    @Override
    public Site archive(final ArchiveSiteCommand command, final Actor actor) {
        final Site site = delegate.archive(command, actor);

        eventDispatcher.dispatch(new SiteArchivedEvent(
                site.id(),
                site.key(),
                actor,
                clock.instant()
        ));

        return site;
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
        final Site site = delegate.unarchive(command, actor);

        eventDispatcher.dispatch(new SiteUnarchivedEvent(
                site.id(),
                site.key(),
                actor,
                clock.instant()
        ));

        return site;
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
        return delegate.findByAlias(alias, actor);
    }

    @Override
    public List<Site> findAll(final Actor actor) {
        return delegate.findAll(actor);
    }
}
