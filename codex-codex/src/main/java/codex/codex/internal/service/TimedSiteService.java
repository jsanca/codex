package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.StartSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.command.UnarchiveSiteCommand;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.observance.Observance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static codex.codex.internal.service.SiteServiceMetricNames.*;

/**
 * A {@link SiteService} decorator that measures caller-visible operation duration
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
 * <p>Metric names follow the {@code services.site.{operation}.duration /
 * services.site.{operation}.failed} convention defined in {@link SiteServiceMetricNames}.</p>
 */
public final class TimedSiteService implements ForwardingSiteService {

    private final SiteService delegate;
    private final Observance observance;

    /**
     * @param delegate   the site service to delegate to; must not be null
     * @param observance the observance for duration and failure metrics; must not be null
     */
    public TimedSiteService(final SiteService delegate, final Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observance = Objects.requireNonNull(observance, "observance must not be null");
    }

    @Override
    public SiteService getDelegate() {
        return delegate;
    }

    @Override
    public Site create(final CreateSiteCommand command, final Actor actor) {
        return timed(CREATE_DURATION, CREATE_FAILED,
                () -> delegate.create(command, actor));
    }

    @Override
    public Site start(final StartSiteCommand command, final Actor actor) {
        return timed(START_DURATION, START_FAILED,
                () -> delegate.start(command, actor));
    }

    @Override
    public Site suspend(final SuspendSiteCommand command, final Actor actor) {
        return timed(SUSPEND_DURATION, SUSPEND_FAILED,
                () -> delegate.suspend(command, actor));
    }

    @Override
    public Site archive(final ArchiveSiteCommand command, final Actor actor) {
        return timed(ARCHIVE_DURATION, ARCHIVE_FAILED,
                () -> delegate.archive(command, actor));
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
        return timed(UNARCHIVE_DURATION, UNARCHIVE_FAILED,
                () -> delegate.unarchive(command, actor));
    }

    @Override
    public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
        return timed(FIND_BY_KEY_DURATION, FIND_BY_KEY_FAILED,
                () -> delegate.findByKey(siteKey, actor));
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
        return timed(FIND_BY_ALIAS_DURATION, FIND_BY_ALIAS_FAILED,
                () -> delegate.findByAlias(alias, actor));
    }

    @Override
    public List<Site> findAll(final Actor actor) {
        return timed(FIND_ALL_DURATION, FIND_ALL_FAILED,
                () -> delegate.findAll(actor));
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
