package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.event.ContentTypeActivatedEvent;
import codex.codex.api.model.event.ContentTypeArchivedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ContentTypeService} decorator that publishes domain events after each mutating operation.
 * Events are only dispatched when a real state change occurs — idempotent operations
 * (e.g. activating an already-{@code ACTIVE} content type) produce no event.
 * Read-only operations are forwarded transparently via {@link ForwardingContentTypeService}.
 *
 * <p>Events are dispatched through {@link CodexEventDispatcher}, which in production is a
 * {@link DeferredEventDispatcher} that buffers events inside a transaction and dispatches
 * them on commit. Rollbacks discard all buffered events.</p>
 *
 * @author jsanca
 */
public final class EventPublishingContentTypeService implements ForwardingContentTypeService {

    private final ContentTypeService delegate;
    private final CodexEventDispatcher eventDispatcher;
    private final Clock clock;

    /**
     * @param delegate        the service to forward calls to; must not be null
     * @param eventDispatcher the dispatcher used to publish domain events; must not be null
     * @param clock           the clock used to timestamp events; must not be null
     */
    public EventPublishingContentTypeService(final ContentTypeService delegate,
                                             final CodexEventDispatcher eventDispatcher,
                                             final Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ContentTypeService getDelegate() {
        return delegate;
    }

    @Override
    public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentType result = delegate.create(command, actor);
        eventDispatcher.dispatch(new ContentTypeCreatedEvent(
                result.id(), result.siteKey(), result.key(), actor, clock.instant()));
        return result;
    }

    @Override
    public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final Optional<ContentTypeStatus> previousStatus = statusOf(command.siteKey(), command.key(), actor);
        final ContentType result = delegate.activate(command, actor);
        previousStatus
                .filter(status -> status != result.status())
                .ifPresent(status -> eventDispatcher.dispatch(new ContentTypeActivatedEvent(
                        result.id(),
                        result.siteKey(),
                        result.key(),
                        status,
                        result.status(),
                        actor,
                        clock.instant()
                )));

        return result;
    }

    @Override
    public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final Optional<ContentTypeStatus> previousStatus = statusOf(command.siteKey(), command.key(), actor);
        final ContentType result = delegate.archive(command, actor);
        previousStatus
                .filter(status -> status != result.status())
                .ifPresent(status -> eventDispatcher.dispatch(new ContentTypeArchivedEvent(
                        result.id(),
                        result.siteKey(),
                        result.key(),
                        previousStatus.get(),
                        result.status(),
                        actor,
                        clock.instant())));

        return result;
    }

    private Optional<ContentTypeStatus> statusOf(final codex.codex.api.model.identity.SiteKey siteKey,
                                       final codex.codex.api.model.identity.ContentTypeKey key,
                                       final Actor actor) {
        return delegate.findByKey(siteKey, key, actor)
                .map(ContentType::status);
    }
}
