package codex.codex.internal.service;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.model.value.SiteStatus;
import codex.codex.internal.repository.SiteRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.lifecycle.LifecycleParticipation;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.IdentityGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CodexSiteService implements SiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexSiteService.class);

    private final SiteRepository siteRepository;
    private final Clock clock;
    private final IdentityGenerator<CreateSiteCommand, SiteId> siteIdentityGenerator;

    public CodexSiteService(final SiteRepository siteRepository,
                            final Clock clock) {
        this(siteRepository, clock, new SiteIdentityGenerator());
    }

    public CodexSiteService(final SiteRepository siteRepository,
                            final Clock clock,
                            final IdentityGenerator<CreateSiteCommand, SiteId> siteIdentityGenerator) {
        this.siteRepository = Objects.requireNonNull(siteRepository, "siteRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.siteIdentityGenerator = Objects.requireNonNull(siteIdentityGenerator, "siteIdentityGenerator must not be null");
    }

    @Override
    public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
        Objects.requireNonNull(createSiteCommand, "createSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        final SiteKey siteKey = createSiteCommand.key();

        LOGGER.debug("Creating site key: {} by actor: {}", siteKey, actor);
        siteRepository.findByKey(siteKey).ifPresent(this::throwSiteAlreadyExistException);

        final Site site = Site.builder()
                .createdAt(clock.instant())
                .key(siteKey)
                .aliases(createSiteCommand.aliases())
                .status(createSiteCommand.status())
                .displayName(createSiteCommand.displayName())
                .id(siteIdentityGenerator.nextIdentity(createSiteCommand))
                .build();

        LOGGER.debug("Creating site: {} by actor: {}", site, actor);
        return siteRepository.save(site);
    }

    @Override
    public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding site by key: {} by actor: {}", siteKey, actor);
        return siteRepository.findByKey(siteKey);
    }

    @Override
    public Site start(final StartSiteCommand command, final Actor actor) {
        Objects.requireNonNull(command, "startSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Starting site: {} by actor: {}", command.key(), actor);
        return changeStatus(command.key(), SiteStatus.STARTED, SiteOperation.START);
    }

    @Override
    public Site suspend(final SuspendSiteCommand command, final Actor actor) {
        Objects.requireNonNull(command, "suspendSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Suspending site: {} by actor: {}", command.key(), actor);
        return changeStatus(command.key(), SiteStatus.SUSPENDED, SiteOperation.SUSPEND);
    }

    @Override
    public Site archive(final ArchiveSiteCommand command, final Actor actor) {
        Objects.requireNonNull(command, "archiveSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Archiving site: {} by actor: {}", command.key(), actor);
        return changeStatus(command.key(), SiteStatus.ARCHIVED, SiteOperation.ARCHIVE);
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
        Objects.requireNonNull(command, "unarchiveSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Unarchiving site: {} by actor: {}", command.key(), actor);
        return changeStatus(command.key(), SiteStatus.SUSPENDED, SiteOperation.UNARCHIVE);
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
        Objects.requireNonNull(alias, "alias must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding site by alias: {} by actor: {}", alias, actor);
        return siteRepository.findByAlias(alias);
    }

    @Override
    public List<Site> findAll(final Actor actor) {
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding all sites by actor: {}", actor);
        return siteRepository.findAll();
    }

    private Site changeStatus(final SiteKey siteKey, final SiteStatus targetStatus, final SiteOperation operation) {
        final Site site = siteRepository.findByKey(siteKey)
                .orElseThrow(() -> new NotFoundException("Site not found: " + siteKey));
        runValidation(site, targetStatus, operation);
        if (site.status() == targetStatus) {
            return site;
        }
        return siteRepository.save(Site.copyOf(site).status(targetStatus).build());
    }

    private void runValidation(final Site site, final SiteStatus targetStatus, final SiteOperation operation) {
        if (site.lifecycleParticipation() != LifecycleParticipation.MANAGED) {
            throw new InvalidSiteLifecycleOperationException(
                    "Site " + site.key() + " does not participate in the normal lifecycle operation: " + operation,
                    site);
        }
        if (site.status() != targetStatus && !isValidTransition(site.status(), targetStatus)) {
            throw invalidTransition(site, targetStatus);
        }
    }

    private boolean isValidTransition(final SiteStatus currentStatus, final SiteStatus targetStatus) {
        return switch (currentStatus) {
            case STARTED  -> targetStatus == SiteStatus.SUSPENDED;
            case SUSPENDED -> targetStatus == SiteStatus.STARTED || targetStatus == SiteStatus.ARCHIVED;
            case ARCHIVED  -> targetStatus == SiteStatus.SUSPENDED;
        };
    }

    private InvalidSiteStatusTransitionException invalidTransition(final Site site, final SiteStatus targetStatus) {
        return new InvalidSiteStatusTransitionException(
                "Cannot transition site " + site.key() + " from " + site.status() + " to " + targetStatus,
                site);
    }

    private void throwSiteAlreadyExistException(final Site site) {
        throw new SiteAlreadyExistException(site);
    }

    private enum SiteOperation {
        START, SUSPEND, ARCHIVE, UNARCHIVE
    }
}
