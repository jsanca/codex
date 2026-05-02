package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.internal.repository.ContentTypeRepository;
import codex.codex.internal.repository.ContentTypeVersionRepository;
import codex.codex.internal.repository.MemoryContentTypeVersionRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.IdentityGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Core implementation of {@link ContentTypeService}.
 * <p>
 * Owns business semantics: scoped duplicate-key prevention, status transition rules,
 * ownership metadata, and deterministic identity generation.
 * Designed to be wrapped by decorators for event publishing following the same pattern
 * used for {@code SiteService}.
 */
public final class CodexContentTypeService implements ContentTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexContentTypeService.class);

    private final ContentTypeRepository repository;
    private final ContentTypeVersionRepository versionRepository;
    private final Clock clock;
    private final IdentityGenerator<CreateContentTypeCommand, ContentTypeId> identityGenerator;


    // Visible for testing
    CodexContentTypeService(final ContentTypeRepository repository, final Clock clock) {
        this(repository, new MemoryContentTypeVersionRepository(), clock, new ContentTypeIdentityGenerator());
    }

    public CodexContentTypeService(final ContentTypeRepository repository,
                                   final ContentTypeVersionRepository versionRepository,
                                   final Clock clock) {
        this(repository, versionRepository, clock, new ContentTypeIdentityGenerator());
    }

    public CodexContentTypeService(final ContentTypeRepository repository,
                                   final ContentTypeVersionRepository versionRepository,
                                   final Clock clock,
                                   final IdentityGenerator<CreateContentTypeCommand, ContentTypeId> identityGenerator) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.versionRepository = Objects.requireNonNull(versionRepository, "versionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.identityGenerator = Objects.requireNonNull(identityGenerator, "identityGenerator must not be null");
    }

    @Override
    public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Creating content type {}/{} by actor: {}", command.siteKey(), command.key(), actor);

        if (repository.existsByKey(command.siteKey(), command.key())) {
            throw new ContentTypeAlreadyExistsException(command.siteKey(), command.key());
        }

        final ContentType contentType = ContentType.builder()
                .id(identityGenerator.nextIdentity(command))
                .siteKey(command.siteKey())
                .key(command.key())
                .displayName(command.displayName())
                .status(ContentTypeStatus.DRAFT)
                .owner(actor.id())
                .createdBy(actor.id())
                .updatedBy(actor.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build();

        return repository.save(contentType);
    }

    @Override
    public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Activating content type {}/{} by actor: {}", command.siteKey(), command.key(), actor);

        final ContentType contentType = loadOrThrow(command.siteKey(), command.key());

        if (contentType.status() == ContentTypeStatus.ACTIVE) {
            return contentType;
        }

        if (contentType.status() == ContentTypeStatus.ARCHIVED) {
            throw new InvalidContentTypeStatusTransitionException(
                    "Cannot activate archived content type: " + command.siteKey() + "/" + command.key(),
                    contentType);
        }

        final int nextVersion = contentType.latestPublishedVersion() == null
                ? 1
                : contentType.latestPublishedVersion() + 1;

        final ContentTypeVersionId versionId = ContentTypeVersionId.forVersion(
                command.siteKey(), command.key(), nextVersion);

        final ContentTypeVersion snapshot = ContentTypeVersion.builder()
                .id(versionId)
                .contentTypeId(contentType.id())
                .siteKey(contentType.siteKey())
                .contentTypeKey(contentType.key())
                .version(nextVersion)
                .fields(contentType.fields())
                .status(ContentTypeVersionStatus.PUBLISHED)
                .createdBy(actor.id())
                .createdAt(clock.instant())
                .build();

        versionRepository.save(snapshot);

        LOGGER.info("Published ContentTypeVersion {}/{} v{} by actor: {}",
                command.siteKey(), command.key(), nextVersion, actor);

        return repository.save(ContentType.copyOf(contentType)
                .status(ContentTypeStatus.ACTIVE)
                .latestPublishedVersionId(versionId)
                .latestPublishedVersion(nextVersion)
                .updatedBy(actor.id())
                .updatedAt(clock.instant())
                .build());
    }

    @Override
    public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Archiving content type {}/{} by actor: {}", command.siteKey(), command.key(), actor);

        final ContentType contentType = loadOrThrow(command.siteKey(), command.key());

        if (contentType.status() == ContentTypeStatus.ARCHIVED) {
            return contentType;
        }

        return repository.save(ContentType.copyOf(contentType)
                .status(ContentTypeStatus.ARCHIVED)
                .updatedBy(actor.id())
                .updatedAt(clock.instant())
                .build());
    }

    @Override
    public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding content type {}/{} by actor: {}", siteKey, key, actor);
        return repository.findByKey(siteKey, key);
    }

    @Override
    public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding content types for site {} by actor: {}", siteKey, actor);
        return repository.findBySiteKey(siteKey);
    }

    @Override
    public List<ContentType> findAll(final Actor actor) {
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding all content types by actor: {}", actor);
        return repository.findAll();
    }

    @Override
    public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Adding field {} to content type {}/{} by actor: {}",
                command.field().key(), command.siteKey(), command.contentTypeKey(), actor);

        final ContentType contentType = loadOrThrow(command.siteKey(), command.contentTypeKey());
        validateSchemaCanBeModified(contentType);
        validateFieldDoesNotExist(contentType, command.field().key());

        final Map<FieldKey, Field> updatedFields = new HashMap<>(contentType.fields());
        updatedFields.put(command.field().key(), command.field());

        return repository.save(ContentType.copyOf(contentType)
                .fields(updatedFields)
                .updatedBy(actor.id())
                .updatedAt(clock.instant())
                .build());
    }

    @Override
    public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Removing field {} from content type {}/{} by actor: {}",
                command.fieldKey(), command.siteKey(), command.contentTypeKey(), actor);

        final ContentType contentType = loadOrThrow(command.siteKey(), command.contentTypeKey());
        validateSchemaCanBeModified(contentType);
        validateFieldExists(contentType, command.fieldKey());

        final Map<FieldKey, Field> updatedFields = new HashMap<>(contentType.fields());
        updatedFields.remove(command.fieldKey());

        return repository.save(ContentType.copyOf(contentType)
                .fields(updatedFields)
                .updatedBy(actor.id())
                .updatedAt(clock.instant())
                .build());
    }

    private ContentType loadOrThrow(final SiteKey siteKey, final ContentTypeKey key) {
        return repository.findByKey(siteKey, key)
                .orElseThrow(() -> new NotFoundException("ContentType not found: " + siteKey + "/" + key));
    }

    private void validateSchemaCanBeModified(final ContentType contentType) {
        if (contentType.status() != ContentTypeStatus.DRAFT) {
            throw new InvalidContentTypeSchemaModificationException(
                    "Cannot modify fields for content type " + contentType.key().value()
                            + " while status is " + contentType.status(),
                    contentType);
        }
    }

    private void validateFieldDoesNotExist(final ContentType contentType, final FieldKey fieldKey) {
        if (contentType.fields().containsKey(fieldKey)) {
            throw new ContentTypeFieldAlreadyExistsException(fieldKey, contentType.key());
        }
    }

    private void validateFieldExists(final ContentType contentType, final FieldKey fieldKey) {
        if (!contentType.fields().containsKey(fieldKey)) {
            throw new ContentTypeFieldNotFoundException(fieldKey, contentType.key());
        }
    }
}
