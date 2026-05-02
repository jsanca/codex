package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.service.ContentItemService;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;

import java.time.Clock;
import java.util.Objects;

/**
 * A {@link ContentItemService} decorator that publishes domain events after each mutating operation.
 * Events are only dispatched when the delegate completes successfully — invalid operations
 * (duplicate key, missing content type, failed validation, etc.) produce no event.
 * Read-only operations are forwarded transparently via {@link ForwardingContentItemService}.
 *
 * <p>Events are dispatched through {@link CodexEventDispatcher}, which in production is a
 * {@link DeferredEventDispatcher} that buffers events inside a transaction and dispatches
 * them on commit. Rollbacks discard all buffered events.</p>
 *
 * <p>Content values are intentionally excluded from {@link ContentItemCreatedEvent}.
 * Values may be large or sensitive; downstream subscribers can load the item by id if needed.</p>
 */
public final class EventPublishingContentItemService implements ForwardingContentItemService {

    private final ContentItemService delegate;
    private final CodexEventDispatcher eventDispatcher;
    private final Clock clock;

    /**
     * @param delegate        the service to forward calls to; must not be null
     * @param eventDispatcher the dispatcher used to publish domain events; must not be null
     * @param clock           the clock used to timestamp events; must not be null
     */
    public EventPublishingContentItemService(final ContentItemService delegate,
                                              final CodexEventDispatcher eventDispatcher,
                                              final Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ContentItemService getDelegate() {
        return delegate;
    }

    @Override
    public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem result = delegate.create(command, actor);
        eventDispatcher.dispatch(new ContentItemCreatedEvent(
                result.id(),
                result.siteKey(),
                result.contentTypeKey(),
                result.contentTypeVersionId(),
                result.key(),
                actor,
                clock.instant()));
        return result;
    }
}
