package codex.codex.internal.repository;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MemorySiteRepository implements SiteRepository {

    private final Map<SiteKey, Site> sites = new ConcurrentHashMap<>();

    @Override
    public Site save(final Site site) {
        Objects.requireNonNull(site, "Site must not be null");
        sites.put(site.key(), site);
        return site;
    }

    @Override
    public Optional<Site> findByKey(final SiteKey key) {
        Objects.requireNonNull(key, "Key must not be null");
        return Optional.ofNullable(sites.get(key));
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias) {
        Objects.requireNonNull(alias, "alias must not be null");
        return sites.values().stream()
                .filter(site -> site.aliases().contains(alias))
                .findFirst();
    }

    @Override
    public boolean existsByKey(final SiteKey siteKey) {
        return this.sites.containsKey(Objects.requireNonNull(siteKey, "Key must not be null"));
    }

    @Override
    public List<Site> findAll() {
        return List.copyOf(sites.values());
    }

    @Override
    public boolean deleteByKey(final SiteKey key) {
        Objects.requireNonNull(key, "Key must not be null");
        return sites.remove(key) != null;
    }
}