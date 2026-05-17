package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentTypeArchivedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link ContentTypeArchivedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Builds the audit record from event fields only — does not reload the ContentType entity.</p>
 */
public final class ContentTypeArchivedChroniconSubscriber
        implements CodexEventSubscriber<ContentTypeArchivedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTypeArchivedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public ContentTypeArchivedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<ContentTypeArchivedEvent> eventType() {
        return ContentTypeArchivedEvent.class;
    }

    @Override
    public void handle(final ContentTypeArchivedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording content type archived audit: siteKey={} contentTypeKey={}",
                event.siteKey(), event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordIdGenerator.contentTypeLifecycle(
                        "archived", event.siteKey(), event.key(), event.occurredAt()))
                .action(AuditAction.ARCHIVED)
                .subject(AuditSubject.of("content-type", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Archived content type " + event.key().value() + " in site " + event.siteKey().value())
                .metadata(Map.of(
                        "siteKey", event.siteKey().value(),
                        "contentTypeKey", event.key().value(),
                        "contentTypeId", event.id().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} contentTypeKey={} action={}",
                event.siteKey(), event.key(), AuditAction.ARCHIVED);
    }
}
