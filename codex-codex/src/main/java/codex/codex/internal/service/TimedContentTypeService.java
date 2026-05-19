package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.observance.Observance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static codex.codex.internal.service.ContentTypeServiceMetricNames.*;

/**
 * A {@link ContentTypeService} decorator that measures caller-visible operation duration
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
 * <p>Metric names follow the {@code services.contentType.{operation}.duration /
 * services.contentType.{operation}.failed} convention defined in
 * {@link ContentTypeServiceMetricNames}.</p>
 */
public final class TimedContentTypeService implements ForwardingContentTypeService {

    private final ContentTypeService delegate;
    private final Observance observance;

    /**
     * @param delegate   the content type service to delegate to; must not be null
     * @param observance the observance for duration and failure metrics; must not be null
     */
    public TimedContentTypeService(final ContentTypeService delegate, final Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observance = Objects.requireNonNull(observance, "observance must not be null");
    }

    @Override
    public ContentTypeService getDelegate() {
        return delegate;
    }

    @Override
    public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        return timed(CREATE_DURATION, CREATE_FAILED,
                () -> delegate.create(command, actor));
    }

    @Override
    public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        return timed(ACTIVATE_DURATION, ACTIVATE_FAILED,
                () -> delegate.activate(command, actor));
    }

    @Override
    public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        return timed(ARCHIVE_DURATION, ARCHIVE_FAILED,
                () -> delegate.archive(command, actor));
    }

    @Override
    public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key,
                                           final Actor actor) {
        return timed(FIND_BY_KEY_DURATION, FIND_BY_KEY_FAILED,
                () -> delegate.findByKey(siteKey, key, actor));
    }

    @Override
    public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
        return timed(FIND_BY_SITE_KEY_DURATION, FIND_BY_SITE_KEY_FAILED,
                () -> delegate.findBySiteKey(siteKey, actor));
    }

    @Override
    public List<ContentType> findAll(final Actor actor) {
        return timed(FIND_ALL_DURATION, FIND_ALL_FAILED,
                () -> delegate.findAll(actor));
    }

    @Override
    public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
        return timed(ADD_FIELD_DURATION, ADD_FIELD_FAILED,
                () -> delegate.addField(command, actor));
    }

    @Override
    public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
        return timed(REMOVE_FIELD_DURATION, REMOVE_FIELD_FAILED,
                () -> delegate.removeField(command, actor));
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
}
