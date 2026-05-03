package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.internal.repository.ContentItemRepository;
import codex.codex.internal.repository.ContentRevisionRepository;
import codex.codex.internal.repository.ContentTypeRepository;
import codex.codex.internal.repository.ContentTypeVersionRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Core implementation of {@link ContentItemService}.
 * <p>
 * Validates content items against the latest published {@link ContentTypeVersion}, ensuring
 * that field checks happen against an immutable schema snapshot rather than the mutable
 * draft schema in {@link ContentType#fields()}.
 * <p>
 * Creating a content item creates revision {@code 1} as {@link ContentRevisionStatus#WORKING}
 * and sets {@link ContentItem#currentWorkingRevisionId()} to that revision.
 * <p>
 * Publishing is pointer-based: values remain in {@link codex.codex.api.model.entity.ContentRevision}.
 * The first publish transitions the working revision to {@link ContentRevisionStatus#PUBLISHED} and
 * sets both {@link ContentItem#currentWorkingRevisionId()} and
 * {@link ContentItem#currentPublishedRevisionId()} to the same revision.
 * Future edit support will create a new working revision that diverges from the published one.
 * Publish events and transaction management are future work.
 */
public final class CodexContentItemService implements ContentItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexContentItemService.class);

    private final ContentItemRepository repository;
    private final ContentRevisionRepository revisionRepository;
    private final ContentTypeRepository contentTypeRepository;
    private final ContentTypeVersionRepository contentTypeVersionRepository;
    private final Clock clock;

    /**
     * Creates a new {@link CodexContentItemService}.
     *
     * @param repository                   the content item repository; must not be null
     * @param revisionRepository           the content revision repository; must not be null
     * @param contentTypeRepository        the content type repository; must not be null
     * @param contentTypeVersionRepository the content type version repository; must not be null
     * @param clock                        the clock for timestamp generation; must not be null
     */
    public CodexContentItemService(final ContentItemRepository repository,
                                    final ContentRevisionRepository revisionRepository,
                                    final ContentTypeRepository contentTypeRepository,
                                    final ContentTypeVersionRepository contentTypeVersionRepository,
                                    final Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.revisionRepository = Objects.requireNonNull(revisionRepository, "revisionRepository must not be null");
        this.contentTypeRepository = Objects.requireNonNull(contentTypeRepository, "contentTypeRepository must not be null");
        this.contentTypeVersionRepository = Objects.requireNonNull(contentTypeVersionRepository, "contentTypeVersionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Creating content item {}/{}/{} by actor: {}",
                command.siteKey(), command.contentTypeKey(), command.key(), actor);

        if (repository.existsByKey(command.siteKey(), command.contentTypeKey(), command.key())) {
            throw new ContentItemAlreadyExistsException(command.key(), command.contentTypeKey(), command.siteKey());
        }

        final ContentType contentType = findContentTypeRequired(command.siteKey(), command.contentTypeKey());
        validateContentTypeCanCreateItems(contentType);

        final ContentTypeVersionId versionId = contentType.latestPublishedVersionId();
        final ContentTypeVersion version = findVersionRequired(versionId);
        validateVersionCanCreateItems(version, command.contentTypeKey());

        validateValues(command.values(), version, command.contentTypeKey());

        final ContentItemId itemId = ContentItemId.forItem(command.siteKey(), command.contentTypeKey(), command.key());
        final ContentRevisionId revisionId = ContentRevisionId.forRevision(
                command.siteKey(), command.contentTypeKey(), command.key(), 1);
        final Instant now = clock.instant();

        final ContentItem item = ContentItem.builder()
                .id(itemId)
                .siteKey(command.siteKey())
                .contentTypeKey(command.contentTypeKey())
                .contentTypeVersionId(versionId)
                .key(command.key())
                .status(ContentItemStatus.DRAFT)
                .currentWorkingRevisionId(revisionId)
                .currentPublishedRevisionId(null)
                .owner(actor.id())
                .createdBy(actor.id())
                .updatedBy(actor.id())
                .createdAt(now)
                .updatedAt(now)
                .build();

        final ContentRevision revision = ContentRevision.builder()
                .id(revisionId)
                .contentItemId(itemId)
                .siteKey(command.siteKey())
                .contentTypeKey(command.contentTypeKey())
                .contentTypeVersionId(versionId)
                .contentItemKey(command.key())
                .revisionNumber(1)
                .status(ContentRevisionStatus.WORKING)
                .values(command.values())
                .createdBy(actor.id())
                .createdAt(now)
                .build();

        revisionRepository.save(revision);
        final ContentItem saved = repository.save(item);

        LOGGER.info("Created content item {}/{}/{} (revision 1) by actor: {}",
                command.siteKey(), command.contentTypeKey(), command.key(), actor);

        return saved;
    }

    @Override
    public Optional<ContentItem> findByKey(final SiteKey siteKey,
                                            final ContentTypeKey contentTypeKey,
                                            final ContentItemKey key,
                                            final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding content item {}/{}/{} by actor: {}", siteKey, contentTypeKey, key, actor);
        return repository.findByKey(siteKey, contentTypeKey, key);
    }

    @Override
    public List<ContentItem> findByContentType(final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding content items for {}/{} by actor: {}", siteKey, contentTypeKey, actor);
        return repository.findByContentType(siteKey, contentTypeKey);
    }

    @Override
    public List<ContentItem> findAll(final Actor actor) {
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding all content items by actor: {}", actor);
        return repository.findAll();
    }

    @Override
    public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {

        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Publishing content item {}/{}/{} by actor: {}",
                command.siteKey(), command.contentTypeKey(), command.key(), actor);

        final ContentItem item = repository.findByKey(command.siteKey(), command.contentTypeKey(), command.key())
                .orElseThrow(() -> new NotFoundException("ContentItem not found: "
                        + command.siteKey() + "/" + command.contentTypeKey() + "/" + command.key()));

        if (item.status() == ContentItemStatus.ARCHIVED) {
            throw new InvalidContentItemPublishException(item.key(),
                    "item is ARCHIVED");
        }

        if (item.currentWorkingRevisionId() == null) {
            throw new InvalidContentItemPublishException(item.key(),
                    "it has no working revision");
        }

        final ContentRevision working = revisionRepository.findById(item.currentWorkingRevisionId())
                .orElseThrow(() -> new NotFoundException("ContentRevision not found: "
                        + item.currentWorkingRevisionId().value()));

        if (working.status() == ContentRevisionStatus.ARCHIVED) {
            throw new InvalidContentItemPublishException(item.key(),
                    "the working revision is ARCHIVED");
        }

        if (item.status() == ContentItemStatus.PUBLISHED
                && item.currentPublishedRevisionId() != null
                && item.currentPublishedRevisionId().equals(item.currentWorkingRevisionId())
                && working.status() == ContentRevisionStatus.PUBLISHED) {
            LOGGER.debug("Content item {}/{}/{} is already published (idempotent)",
                    command.siteKey(), command.contentTypeKey(), command.key());
            return item;
        }

        final ContentRevision published = ContentRevision.copyOf(working)
                .status(ContentRevisionStatus.PUBLISHED)
                .build();
        revisionRepository.save(published);

        final ContentItem updated = ContentItem.copyOf(item)
                .status(ContentItemStatus.PUBLISHED)
                .currentPublishedRevisionId(published.id())
                .currentWorkingRevisionId(published.id())
                .updatedBy(actor.id())
                .updatedAt(clock.instant())
                .build();
        final ContentItem saved = repository.save(updated);

        LOGGER.info("Published content item {}/{}/{} (revision {}) by actor: {}",
                command.siteKey(), command.contentTypeKey(), command.key(),
                published.revisionNumber(), actor);

        return saved;
    }

    private ContentType findContentTypeRequired(final SiteKey siteKey, final ContentTypeKey contentTypeKey) {
        return contentTypeRepository.findByKey(siteKey, contentTypeKey)
                .orElseThrow(() -> new NotFoundException("ContentType not found: " + siteKey + "/" + contentTypeKey));
    }

    private ContentTypeVersion findVersionRequired(final ContentTypeVersionId versionId) {
        return contentTypeVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("ContentTypeVersion not found: " + versionId.value()));
    }

    private void validateContentTypeCanCreateItems(final ContentType contentType) {
        if (contentType.status() != ContentTypeStatus.ACTIVE) {
            throw new InvalidContentItemCreationException(
                    "Cannot create content item for content type " + contentType.key().value()
                            + " with status " + contentType.status()
                            + " — content type must be ACTIVE");
        }
        if (contentType.latestPublishedVersionId() == null) {
            throw new InvalidContentItemCreationException(
                    "Cannot create content item for content type " + contentType.key().value()
                            + " — no published version available");
        }
    }

    private void validateVersionCanCreateItems(final ContentTypeVersion version,
                                                final ContentTypeKey contentTypeKey) {
        if (version.status() != ContentTypeVersionStatus.PUBLISHED) {
            throw new InvalidContentItemCreationException(
                    "Cannot create content item against content type version "
                            + version.id().value() + " with status " + version.status()
                            + " — version must be PUBLISHED");
        }
    }

    private void validateValues(final Map<FieldKey, Object> values,
                                 final ContentTypeVersion version,
                                 final ContentTypeKey contentTypeKey) {
        validateNoUnknownFields(values, version, contentTypeKey);
        validateRequiredFields(values, version, contentTypeKey);
    }

    private void validateNoUnknownFields(final Map<FieldKey, Object> values,
                                          final ContentTypeVersion version,
                                          final ContentTypeKey contentTypeKey) {
        for (final FieldKey fieldKey : values.keySet()) {
            if (!version.fields().containsKey(fieldKey)) {
                throw new ContentItemFieldValidationException(
                        "Unknown field " + fieldKey.value(), contentTypeKey);
            }
        }
    }

    private void validateRequiredFields(final Map<FieldKey, Object> values,
                                         final ContentTypeVersion version,
                                         final ContentTypeKey contentTypeKey) {
        for (final Map.Entry<FieldKey, Field> entry : version.fields().entrySet()) {
            if (entry.getValue().required() && !values.containsKey(entry.getKey())) {
                throw new ContentItemFieldValidationException(
                        "Missing required field " + entry.getKey().value(), contentTypeKey);
            }
        }
    }
}
