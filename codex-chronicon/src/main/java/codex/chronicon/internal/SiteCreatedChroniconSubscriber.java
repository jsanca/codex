package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link SiteCreatedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Chronicon does not own site lifecycle state; it records what happened,
 * derived from the domain event.</p>
 */
public final class SiteCreatedChroniconSubscriber implements CodexEventSubscriber<SiteCreatedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteCreatedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public SiteCreatedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<SiteCreatedEvent> eventType() {
        return SiteCreatedEvent.class;
    }

    @Override
    public void handle(final SiteCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording site created audit: siteKey={}", event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordId.of(
                        "audit:site-created:" + event.key().value() + ":" + event.occurredAt().toEpochMilli()))
                .action(AuditAction.CREATED)
                .subject(AuditSubject.of("site", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Created site " + event.key().value())
                .metadata(Map.of("siteKey", event.key().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} action={}", event.key(), AuditAction.CREATED);
    }
}
