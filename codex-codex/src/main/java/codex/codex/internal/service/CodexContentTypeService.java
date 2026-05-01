package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.internal.repository.ContentTypeRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.IdentityGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Core implementation of {@link ContentTypeService}.
 * <p>
 * Owns business semantics: duplicate-key prevention, status transition rules,
 * and deterministic identity generation. Designed to be wrapped by decorators
 * for event publishing following the same pattern used for {@code SiteService}.
 */
public final class CodexContentTypeService implements ContentTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexContentTypeService.class);

    private final ContentTypeRepository repository;
    private final Clock clock;
    private final IdentityGenerator<CreateContentTypeCommand, ContentTypeId> identityGenerator;

    public CodexContentTypeService(final ContentTypeRepository repository, final Clock clock) {
        this(repository, clock, new ContentTypeIdentityGenerator());
    }

    public CodexContentTypeService(final ContentTypeRepository repository,
                                   final Clock clock,
                                   final IdentityGenerator<CreateContentTypeCommand, ContentTypeId> identityGenerator) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.identityGenerator = Objects.requireNonNull(identityGenerator, "identityGenerator must not be null");
    }

    @Override
    public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Creating content type key: {} by actor: {}", command.key(), actor);

        if (repository.existsByKey(command.key())) {
            throw new ContentTypeAlreadyExistsException(command.key());
        }

        final ContentType contentType = ContentType.builder()
                .id(identityGenerator.nextIdentity(command))
                .key(command.key())
                .displayName(command.displayName())
                .status(ContentTypeStatus.DRAFT)
                .createdAt(clock.instant())
                .build();

        return repository.save(contentType);
    }

    @Override
    public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Activating content type key: {} by actor: {}", command.key(), actor);

        final ContentType contentType = loadOrThrow(command.key());

        if (contentType.status() == ContentTypeStatus.ACTIVE) {
            return contentType;
        }

        if (contentType.status() == ContentTypeStatus.ARCHIVED) {
            throw new InvalidContentTypeStatusTransitionException(
                    "Cannot activate archived content type: " + command.key(), contentType);
        }

        return repository.save(ContentType.copyOf(contentType)
                .status(ContentTypeStatus.ACTIVE)
                .updatedAt(clock.instant())
                .build());
    }

    @Override
    public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Archiving content type key: {} by actor: {}", command.key(), actor);

        final ContentType contentType = loadOrThrow(command.key());

        if (contentType.status() == ContentTypeStatus.ARCHIVED) {
            return contentType;
        }

        return repository.save(ContentType.copyOf(contentType)
                .status(ContentTypeStatus.ARCHIVED)
                .updatedAt(clock.instant())
                .build());
    }

    @Override
    public Optional<ContentType> findByKey(final ContentTypeKey key, final Actor actor) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding content type by key: {} by actor: {}", key, actor);
        return repository.findByKey(key);
    }

    @Override
    public List<ContentType> findAll(final Actor actor) {
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding all content types by actor: {}", actor);
        return repository.findAll();
    }

    private ContentType loadOrThrow(final ContentTypeKey key) {
        return repository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("ContentType not found: " + key));
    }
}
