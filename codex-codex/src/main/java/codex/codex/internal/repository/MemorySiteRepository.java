package codex.codex.internal.repository;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MemorySiteRepository implements SiteRepository {

    private final MemoryStore<SiteKey, Site> store = new MemoryStore<>(Site::key);

    @Override
    public Site save(final Site site) {
        Objects.requireNonNull(site, "Site must not be null");
        return store.save(site);
    }

    @Override
    public Optional<Site> findByKey(final SiteKey key) {
        return store.findByKey(key);
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias) {
        Objects.requireNonNull(alias, "alias must not be null");
        return store.findFirstWhere(site -> site.aliases().contains(alias));
    }

    @Override
    public boolean existsByKey(final SiteKey siteKey) {
        return store.existsByKey(siteKey);
    }

    @Override
    public List<Site> findAll() {
        return store.findAll();
    }

    @Override
    public boolean deleteByKey(final SiteKey key) {
        return store.deleteByKey(key);
    }
}
