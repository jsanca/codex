package codex.codex.internal.service;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.event.SiteArchivedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.event.SiteStartedEvent;
import codex.codex.api.model.event.SiteSuspendedEvent;
import codex.codex.api.model.event.SiteUnarchivedEvent;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.model.value.SiteStatus;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;

import java.time.Clock;
import java.util.Objects;

/**
 * A {@link SiteService} decorator that publishes domain events after each mutating operation.
 * Events are only dispatched when a real state change occurs — idempotent operations
 * (e.g. suspending an already-suspended site) produce no event.
 * Read-only operations are forwarded transparently via {@link ForwardingSiteService}.
 *
 * @author jsanca
 */
public class EventPublishingSiteService implements ForwardingSiteService {

    private final SiteService delegate;
    private final CodexEventDispatcher eventDispatcher;
    private final Clock clock;

    /**
     * @param siteService     the delegate to forward calls to; must not be null
     * @param eventDispatcher the dispatcher used to publish domain events; must not be null
     * @param clock           the clock used to timestamp events; must not be null
     */
    public EventPublishingSiteService(final SiteService siteService,
                                      final CodexEventDispatcher eventDispatcher,
                                      final Clock clock) {
        this.delegate = Objects.requireNonNull(siteService, "siteService must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SiteService getDelegate() {
        return delegate;
    }

    @Override
    public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
        final Site site = delegate.create(createSiteCommand, actor);
        eventDispatcher.dispatch(new SiteCreatedEvent(site.id(), site.key(), actor, clock.instant()));
        return site;
    }

    @Override
    public Site start(final StartSiteCommand command, final Actor actor) {
        final SiteStatus before = statusOf(command.key(), actor);
        final Site site = delegate.start(command, actor);
        if (before != site.status()) {
            eventDispatcher.dispatch(new SiteStartedEvent(site.id(), site.key(), actor, clock.instant()));
        }
        return site;
    }

    @Override
    public Site suspend(final SuspendSiteCommand command, final Actor actor) {
        final SiteStatus before = statusOf(command.key(), actor);
        final Site site = delegate.suspend(command, actor);
        if (before != site.status()) {
            eventDispatcher.dispatch(new SiteSuspendedEvent(site.id(), site.key(), actor, clock.instant()));
        }
        return site;
    }

    @Override
    public Site archive(final ArchiveSiteCommand command, final Actor actor) {
        final SiteStatus before = statusOf(command.key(), actor);
        final Site site = delegate.archive(command, actor);
        if (before != site.status()) {
            eventDispatcher.dispatch(new SiteArchivedEvent(site.id(), site.key(), actor, clock.instant()));
        }
        return site;
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
        final SiteStatus before = statusOf(command.key(), actor);
        final Site site = delegate.unarchive(command, actor);
        if (before != site.status()) {
            eventDispatcher.dispatch(new SiteUnarchivedEvent(site.id(), site.key(), actor, clock.instant()));
        }
        return site;
    }

    private SiteStatus statusOf(final SiteKey key, final Actor actor) {
        return delegate.findByKey(key, actor).map(Site::status).orElseThrow();
    }
}
