package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentTypeActivatedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link ContentTypeActivatedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Builds the audit record from event fields only — does not reload the ContentType entity.</p>
 */
public final class ContentTypeActivatedChroniconSubscriber
        implements CodexEventSubscriber<ContentTypeActivatedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTypeActivatedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public ContentTypeActivatedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<ContentTypeActivatedEvent> eventType() {
        return ContentTypeActivatedEvent.class;
    }

    @Override
    public void handle(final ContentTypeActivatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording content type activated audit: siteKey={} contentTypeKey={}",
                event.siteKey(), event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordIdGenerator.contentTypeLifecycle(
                        "activated", event.siteKey(), event.key(), event.occurredAt()))
                .action(AuditAction.ACTIVATED)
                .subject(AuditSubject.of("content-type", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Activated content type " + event.key().value() + " in site " + event.siteKey().value())
                .metadata(Map.of(
                        "siteKey", event.siteKey().value(),
                        "contentTypeKey", event.key().value(),
                        "contentTypeId", event.id().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} contentTypeKey={} action={}",
                event.siteKey(), event.key(), AuditAction.ACTIVATED);
    }
}
