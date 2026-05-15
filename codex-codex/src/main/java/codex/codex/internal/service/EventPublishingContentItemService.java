package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemArchivedEvent;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.event.ContentItemDeletedEvent;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.event.ContentItemRestoredEvent;
import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.codex.api.model.event.ContentItemUpdatedEvent;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.ContentItemStatus;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

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
 * <p>Content values are intentionally excluded from {@link ContentItemCreatedEvent} and
 * {@link ContentItemPublishedEvent}. Values may be large or sensitive; downstream subscribers
 * can load the revision by id if needed.</p>
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

    @Override
    public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem result = delegate.update(command, actor);
        eventDispatcher.dispatch(new ContentItemUpdatedEvent(
                result.id(),
                result.siteKey(),
                result.contentTypeKey(),
                result.contentTypeVersionId(),
                result.key(),
                actor,
                clock.instant()));
        return result;
    }

    @Override
    public void delete(final DeleteContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem item = delegate.findByKey(
                command.siteKey(), command.contentTypeKey(), command.key(), actor)
                .orElseThrow(() -> new NotFoundException("ContentItem not found: "
                        + command.siteKey() + "/" + command.contentTypeKey() + "/" + command.key()));

        delegate.delete(command, actor);

        eventDispatcher.dispatch(new ContentItemDeletedEvent(
                item.id(),
                item.siteKey(),
                item.contentTypeKey(),
                item.contentTypeVersionId(),
                item.key(),
                actor,
                clock.instant()));
    }

    @Override
    public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem result = delegate.restore(command, actor);
        eventDispatcher.dispatch(new ContentItemRestoredEvent(
                result.id(),
                result.siteKey(),
                result.contentTypeKey(),
                result.contentTypeVersionId(),
                result.key(),
                actor,
                clock.instant()));
        return result;
    }

    @Override
    public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem result = delegate.archive(command, actor);
        eventDispatcher.dispatch(new ContentItemArchivedEvent(
                result.id(),
                result.siteKey(),
                result.contentTypeKey(),
                result.contentTypeVersionId(),
                result.key(),
                actor,
                clock.instant()));
        return result;
    }

    @Override
    public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItem result = delegate.unpublish(command, actor);
        eventDispatcher.dispatch(new ContentItemUnpublishedEvent(
                result.id(),
                result.siteKey(),
                result.contentTypeKey(),
                result.contentTypeVersionId(),
                result.key(),
                actor,
                clock.instant()));
        return result;
    }

    @Override
    public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final Optional<ContentItem> previous = delegate.findByKey(
                command.siteKey(), command.contentTypeKey(), command.key(), actor);

        final ContentItem result = delegate.publish(command, actor);

        if (result.status() == ContentItemStatus.PUBLISHED && result.currentPublishedRevisionId() == null) {
            throw new IllegalStateException(
                    "Delegate returned published item with null currentPublishedRevisionId for "
                            + result.key().value());
        }

        if (shouldPublishEvent(previous, result)) {

            eventDispatcher.dispatch(new ContentItemPublishedEvent(
                    result.id(),
                    result.siteKey(),
                    result.contentTypeKey(),
                    result.contentTypeVersionId(),
                    result.key(),
                    result.currentPublishedRevisionId(),
                    actor,
                    clock.instant()));
        }
        return result;
    }

    private boolean shouldPublishEvent(final Optional<ContentItem> previous, final ContentItem result) {
        if (previous.isEmpty()) {
            return false;
        }
        if (result.status() != ContentItemStatus.PUBLISHED) {
            return false;
        }
        if (result.currentPublishedRevisionId() == null) {
            return false;
        }

        final ContentItem prev = previous.get();
        return !(prev.status() == ContentItemStatus.PUBLISHED
                && Objects.equals(prev.currentPublishedRevisionId(), result.currentPublishedRevisionId()));
    }


}
