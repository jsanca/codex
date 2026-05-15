package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link ContentItemCreatedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Relies only on event fields; does not reload the entity from a repository.</p>
 */
public final class ContentItemCreatedChroniconSubscriber
        implements CodexEventSubscriber<ContentItemCreatedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemCreatedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public ContentItemCreatedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<ContentItemCreatedEvent> eventType() {
        return ContentItemCreatedEvent.class;
    }

    @Override
    public void handle(final ContentItemCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording content item created audit: siteKey={} contentTypeKey={} key={}",
                event.siteKey(), event.contentTypeKey(), event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordId.of(
                        "audit:content-item-created:"
                                + event.siteKey().value() + ":"
                                + event.contentTypeKey().value() + ":"
                                + event.key().value() + ":"
                                + event.occurredAt().toEpochMilli()))
                .action(AuditAction.CREATED)
                .subject(AuditSubject.of("content-item", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Created content item " + event.key().value()
                        + " in content type " + event.contentTypeKey().value())
                .metadata(Map.of(
                        "siteKey", event.siteKey().value(),
                        "contentTypeKey", event.contentTypeKey().value(),
                        "contentTypeVersionId", event.contentTypeVersionId().value(),
                        "contentItemKey", event.key().value(),
                        "contentItemId", event.id().value()))
                .build();

        repository.save(record);

        LOGGER.info("Audit record saved: siteKey={} contentTypeKey={} key={} action={}",
                event.siteKey(), event.contentTypeKey(), event.key(), AuditAction.CREATED);
    }
}
