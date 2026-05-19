package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.observance.Observance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static codex.codex.internal.service.ContentItemServiceMetricNames.*;

/**
 * A {@link ContentItemService} decorator that measures caller-visible operation duration
 * and failure counts via {@link Observance}.
 *
 * <p>For every method:</p>
 * <ul>
 *   <li>a timer records the operation duration (always — on success and on failure)</li>
 *   <li>a failure counter is incremented only when the delegate throws</li>
 * </ul>
 *
 * <p>Exceptions are never swallowed — they propagate after the failure counter is
 * incremented. Dispatch semantics, ordering, and return values are preserved
 * exactly as the delegate provides them.</p>
 *
 * <p>Metric names follow the {@code services.contentItem.{operation}.duration /
 * services.contentItem.{operation}.failed} convention defined in
 * {@link ContentItemServiceMetricNames}.</p>
 */
public final class TimedContentItemService implements ForwardingContentItemService {

    private final ContentItemService delegate;
    private final Observance observance;

    /**
     * @param delegate   the content item service to delegate to; must not be null
     * @param observance the observance for duration and failure metrics; must not be null
     */
    public TimedContentItemService(final ContentItemService delegate, final Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observance = Objects.requireNonNull(observance, "observance must not be null");
    }

    @Override
    public ContentItemService getDelegate() {
        return delegate;
    }

    @Override
    public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
        return timed(CREATE_DURATION, CREATE_FAILED,
                () -> delegate.create(command, actor));
    }

    @Override
    public Optional<ContentItem> findByKey(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                           final ContentItemKey key, final Actor actor) {
        return timed(FIND_BY_KEY_DURATION, FIND_BY_KEY_FAILED,
                () -> delegate.findByKey(siteKey, contentTypeKey, key, actor));
    }

    @Override
    public List<ContentItem> findByContentType(final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final Actor actor) {
        return timed(FIND_BY_CONTENT_TYPE_DURATION, FIND_BY_CONTENT_TYPE_FAILED,
                () -> delegate.findByContentType(siteKey, contentTypeKey, actor));
    }

    @Override
    public List<ContentItem> findAll(final Actor actor) {
        return timed(FIND_ALL_DURATION, FIND_ALL_FAILED,
                () -> delegate.findAll(actor));
    }

    @Override
    public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
        return timed(UPDATE_DURATION, UPDATE_FAILED,
                () -> delegate.update(command, actor));
    }

    @Override
    public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
        return timed(ARCHIVE_DURATION, ARCHIVE_FAILED,
                () -> delegate.archive(command, actor));
    }

    @Override
    public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
        return timed(UNPUBLISH_DURATION, UNPUBLISH_FAILED,
                () -> delegate.unpublish(command, actor));
    }

    @Override
    public void delete(final DeleteContentItemCommand command, final Actor actor) {
        timedVoid(DELETE_DURATION, DELETE_FAILED,
                () -> delegate.delete(command, actor));
    }

    @Override
    public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
        return timed(RESTORE_DURATION, RESTORE_FAILED,
                () -> delegate.restore(command, actor));
    }

    @Override
    public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
        return timed(PUBLISH_DURATION, PUBLISH_FAILED,
                () -> delegate.publish(command, actor));
    }

    private <T> T timed(final String durationKey, final String failedKey,
                        final Supplier<T> action) {
        try {
            return observance.timer(durationKey).record(action);
        } catch (final RuntimeException ex) {
            observance.counter(failedKey).increment();
            throw ex;
        }
    }

    private void timedVoid(final String durationKey, final String failedKey,
                           final Runnable action) {
        try {
            observance.timer(durationKey).record(action);
        } catch (final RuntimeException ex) {
            observance.counter(failedKey).increment();
            throw ex;
        }
    }
}
