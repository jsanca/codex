package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Listens to {@link ContentItemUnpublishedEvent} and writes an {@link AuditRecord} to
 * {@link ChroniconRepository}.
 *
 * <p>Relies only on event fields; does not reload the entity from a repository.</p>
 */
public final class ContentItemUnpublishedChroniconSubscriber
        implements CodexEventSubscriber<ContentItemUnpublishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemUnpublishedChroniconSubscriber.class);

    private final ChroniconRepository repository;

    /**
     * @param repository the audit repository; must not be null
     */
    public ContentItemUnpublishedChroniconSubscriber(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Class<ContentItemUnpublishedEvent> eventType() {
        return ContentItemUnpublishedEvent.class;
    }

    @Override
    public void handle(final ContentItemUnpublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Recording content item unpublished audit: siteKey={} contentTypeKey={} key={}",
                event.siteKey(), event.contentTypeKey(), event.key());

        final AuditRecord record = AuditRecord.builder()
                .id(AuditRecordId.of(
                        "audit:content-item-unpublished:"
                                + event.siteKey().value() + ":"
                                + event.contentTypeKey().value() + ":"
                                + event.key().value() + ":"
                                + event.occurredAt().toEpochMilli()))
                .action(AuditAction.UNPUBLISHED)
                .subject(AuditSubject.of("content-item", event.id().value(), event.key().value()))
                .actorId(event.actor().id())
                .occurredAt(event.occurredAt())
                .summary("Unpublished content item " + event.key().value()
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
                event.siteKey(), event.contentTypeKey(), event.key(), AuditAction.UNPUBLISHED);
    }
}
