package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.SiteSuspendedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link SiteSuspendedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Builds the audit record from event fields only — does not reload the Site entity.</p>
 */
public final class SiteSuspendedChroniconSubscriber implements CodexEventSubscriber<SiteSuspendedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteSuspendedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public SiteSuspendedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<SiteSuspendedEvent> eventType() {
        return SiteSuspendedEvent.class;
    }

    @Override
    public void handle(final SiteSuspendedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording site suspended audit: siteKey={}", event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordIdGenerator.siteLifecycle("suspended", event.key(), event.occurredAt()))
                .action(AuditAction.SUSPENDED)
                .subject(AuditSubject.of("site", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Suspended site " + event.key().value())
                .metadata(Map.of("siteKey", event.key().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} action={}", event.key(), AuditAction.SUSPENDED);
    }
}
