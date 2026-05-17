package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.SiteUnarchivedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link SiteUnarchivedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Unarchiving returns a site to {@code SUSPENDED} status. This subscriber records
 * the transition as {@link AuditAction#RESTORED}.</p>
 *
 * <p>Builds the audit record from event fields only — does not reload the Site entity.</p>
 */
public final class SiteUnarchivedChroniconSubscriber implements CodexEventSubscriber<SiteUnarchivedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteUnarchivedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public SiteUnarchivedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<SiteUnarchivedEvent> eventType() {
        return SiteUnarchivedEvent.class;
    }

    @Override
    public void handle(final SiteUnarchivedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording site unarchived audit: siteKey={}", event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordIdGenerator.siteLifecycle("unarchived", event.key(), event.occurredAt()))
                .action(AuditAction.RESTORED)
                .subject(AuditSubject.of("site", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Unarchived site " + event.key().value())
                .metadata(Map.of("siteKey", event.key().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} action={}", event.key(), AuditAction.RESTORED);
    }
}
