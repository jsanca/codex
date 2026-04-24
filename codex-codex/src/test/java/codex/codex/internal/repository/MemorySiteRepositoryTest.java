package codex.codex.internal.repository;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemorySiteRepositoryTest {

    private MemorySiteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MemorySiteRepository();
    }

    @Test
    @DisplayName("Should save and find a site by key")
    void saveAndFindByKey() {
        SiteKey key = SiteKey.of("test-site");
        Site site = createSite(key);

        repository.save(site);
        Optional<Site> found = repository.findByKey(key);

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(site);
    }

    @Test
    @DisplayName("Should return empty optional for non-existent key")
    void findNonExistent() {
        Optional<Site> found = repository.findByKey(SiteKey.of("missing"));
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if site exists by key")
    void existsByKey() {
        SiteKey key = SiteKey.of("exist-check");
        repository.save(createSite(key));

        assertThat(repository.existsByKey(key)).isTrue();
        assertThat(repository.existsByKey(SiteKey.of("other"))).isFalse();
    }

    @Test
    @DisplayName("Should return all saved sites")
    void findAll() {
        repository.save(createSite(SiteKey.of("site-1")));
        repository.save(createSite(SiteKey.of("site-2")));

        List<Site> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should delete site by key")
    void deleteByKey() {
        SiteKey key = SiteKey.of("to-delete");
        repository.save(createSite(key));

        boolean deleted = repository.deleteByKey(key);
        assertThat(deleted).isTrue();
        assertThat(repository.existsByKey(key)).isFalse();
    }

    @Test
    @DisplayName("Should return false when deleting non-existent site")
    void deleteNonExistent() {
        boolean deleted = repository.deleteByKey(SiteKey.of("none"));
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when saving null site")
    void saveNull() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Site must not be null");
    }

    private Site createSite(SiteKey key) {
        return Site.builder()
                .id(SiteId.generate())
                .key(key)
                .displayName("Site " + key.value())
                .build();
    }
}
