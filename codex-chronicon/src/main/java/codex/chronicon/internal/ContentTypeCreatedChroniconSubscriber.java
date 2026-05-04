package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link ContentTypeCreatedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Chronicon does not own content type lifecycle state; it records what happened,
 * derived from the domain event.</p>
 */
public final class ContentTypeCreatedChroniconSubscriber
        implements CodexEventSubscriber<ContentTypeCreatedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTypeCreatedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public ContentTypeCreatedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<ContentTypeCreatedEvent> eventType() {
        return ContentTypeCreatedEvent.class;
    }

    @Override
    public void handle(final ContentTypeCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording content type created audit: siteKey={} contentTypeKey={}",
                event.siteKey(), event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordId.of(
                        "audit:content-type-created:"
                                + event.siteKey().value() + ":"
                                + event.key().value() + ":"
                                + event.occurredAt().toEpochMilli()))
                .action(AuditAction.CREATED)
                .subject(AuditSubject.of("content-type", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Created content type " + event.key().value() + " in site " + event.siteKey().value())
                .metadata(Map.of(
                        "siteKey", event.siteKey().value(),
                        "contentTypeKey", event.key().value(),
                        "contentTypeId", event.id().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} contentTypeKey={} action={}",
                event.siteKey(), event.key(), AuditAction.CREATED);
    }
}
