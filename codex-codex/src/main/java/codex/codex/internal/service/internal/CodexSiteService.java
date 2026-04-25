package codex.codex.internal.service.internal;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.model.value.SiteStatus;
import codex.codex.internal.repository.SiteRepository;
import codex.fundamentum.api.exception.NotFoundException;
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

    protected CodexSiteService(final SiteRepository siteRepository,
                            final Clock clock) {
        this (siteRepository, clock, new SiteIdentityGenerator());
    }

    protected CodexSiteService(final SiteRepository siteRepository,
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

        final Site site = Site.builder().createdAt(this.clock.instant())
                .key(siteKey)
                .aliases(createSiteCommand.aliases())
                .status(createSiteCommand.status())
                .displayName(createSiteCommand.displayName())
                .id(this.siteIdentityGenerator.nextIdentity(createSiteCommand))
                .build();

        LOGGER.debug("Creating site: {} by actor: {}", site, actor);

        return siteRepository.save(site);
    }

    private void throwSiteAlreadyExistException(final Site site) {

        throw new SiteAlreadyExistException(site);
    }

    @Override
    public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {

        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        LOGGER.debug("Finding site by key: {} by actor: {}", siteKey, actor);
        return siteRepository.findByKey(siteKey);
    }

    @Override
    public Site start(final StartSiteCommand startSiteCommand, final Actor actor) {

        Objects.requireNonNull(startSiteCommand, "startSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        final SiteKey siteKey = startSiteCommand.key();

        LOGGER.debug("Starting site: {} by actor: {}", siteKey, actor);
        return changeStatus(siteKey, SiteStatus.STARTED);
    }

    @Override
    public Site suspend(final SuspendSiteCommand suspendSiteCommand, final Actor actor) {

        Objects.requireNonNull(suspendSiteCommand, "suspendSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        final SiteKey siteKey = suspendSiteCommand.key();

        LOGGER.debug("Suspending site: {} by actor: {}", siteKey, actor);
        return changeStatus(siteKey, SiteStatus.SUSPENDED);
    }

    @Override
    public Site archive(final ArchiveSiteCommand archiveSiteCommand, final Actor actor) {

        Objects.requireNonNull(archiveSiteCommand, "archiveSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        final SiteKey siteKey = archiveSiteCommand.key();

        LOGGER.debug("Archiving site: {} by actor: {}", siteKey, actor);
        return changeStatus(siteKey, SiteStatus.ARCHIVED);
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand unarchiveSiteCommand, final Actor actor) {

        Objects.requireNonNull(unarchiveSiteCommand, "unarchiveSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        final SiteKey siteKey = unarchiveSiteCommand.key();

        LOGGER.debug("Unarchiving site: {} by actor: {}", siteKey, actor);
        return changeStatus(siteKey, SiteStatus.SUSPENDED);
    }

    private Site changeStatus(final SiteKey siteKey, final SiteStatus targetStatus) {

        final Site site = siteRepository.findByKey(siteKey)
                .orElseThrow(() -> new NotFoundException("Site not found: " + siteKey));

        if (site.status() == targetStatus) {
            return site;
        }

        if (!isValidTransition(site.status(), targetStatus)) {
            throw invalidTransition(site, targetStatus);
        }

        return siteRepository.save(Site.copyOf(site).status(targetStatus).build());
    }

    private boolean isValidTransition(final SiteStatus currentStatus, final SiteStatus targetStatus) {

        return switch (currentStatus) {
            case STARTED -> targetStatus == SiteStatus.SUSPENDED;
            case SUSPENDED -> targetStatus == SiteStatus.STARTED || targetStatus == SiteStatus.ARCHIVED;
            case ARCHIVED -> targetStatus == SiteStatus.SUSPENDED;
        };
    }

    private InvalidSiteStatusTransitionException invalidTransition(final Site site, final SiteStatus targetStatus) {

        return new InvalidSiteStatusTransitionException(
                "Cannot transition site " + site.key() + " from " + site.status() + " to " + targetStatus,
                site
        );
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
}
