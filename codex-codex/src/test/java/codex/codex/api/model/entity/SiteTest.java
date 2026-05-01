package codex.codex.api.model.entity;

import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;
import codex.fundamentum.api.lifecycle.LifecycleParticipation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiteTest {

    @Test
    @DisplayName("Should create Site with valid builder data")
    void siteBuilderValid() {
        final SiteId id = SiteId.of(UUID.randomUUID().toString());
        final SiteKey key = SiteKey.of("main-site");
        Site site = Site.builder()
                .id(id)
                .key(key)
                .displayName("Main Site")
                .status(SiteStatus.STARTED)
                .build();

        assertThat(site.id()).isEqualTo(id);
        assertThat(site.key()).isEqualTo(key);
        assertThat(site.displayName()).isEqualTo("Main Site");
        assertThat(site.status()).isEqualTo(SiteStatus.STARTED);
        assertThat(site.aliases()).isEmpty();
        assertThat(site.attributes()).isEmpty();
        assertThat(site.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when key is blank")
    void siteKeyBlank() {
        SiteId id = SiteId.of(UUID.randomUUID().toString());
        assertThatThrownBy(() -> Site.builder()
                .id(id)
                .key(" ")
                .displayName("Name")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("site key must have at least 2 characters");
    }

    @Test
    @DisplayName("Should default status to STARTED if not provided")
    void siteDefaultStatus() {
        Site site = Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key("test")
                .displayName("Test")
                .build();
        
        assertThat(site.status()).isEqualTo(SiteStatus.STARTED);
    }

    @Test
    @DisplayName("regular site defaults lifecycle participation to MANAGED")
    void siteDefaultsLifecycleParticipationToManaged() {
        final Site site = Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key("test")
                .displayName("Test")
                .build();
        assertThat(site.lifecycleParticipation()).isEqualTo(LifecycleParticipation.MANAGED);
    }

    @Test
    @DisplayName("builder can set lifecycle participation")
    void siteBuilderSetsLifecycleParticipation() {
        final Site site = Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key("test")
                .displayName("Test")
                .lifecycleParticipation(LifecycleParticipation.READ_ONLY)
                .build();
        assertThat(site.lifecycleParticipation()).isEqualTo(LifecycleParticipation.READ_ONLY);
    }

    @Test
    @DisplayName("Site.copyOf preserves lifecycle participation")
    void siteCopyOfPreservesLifecycleParticipation() {
        final Site original = Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key("test")
                .displayName("Test")
                .lifecycleParticipation(LifecycleParticipation.EXTERNAL)
                .build();
        final Site copy = Site.copyOf(original).build();
        assertThat(copy.lifecycleParticipation()).isEqualTo(LifecycleParticipation.EXTERNAL);
    }

    @Test
    @DisplayName("Site.system() returns a site with key SiteKey.SYSTEM")
    void systemSiteHasSystemKey() {
        assertThat(Site.system().key()).isEqualTo(SiteKey.SYSTEM);
    }

    @Test
    @DisplayName("Site.system() returns a site with status STARTED")
    void systemSiteHasStatusStarted() {
        assertThat(Site.system().status()).isEqualTo(SiteStatus.STARTED);
    }

    @Test
    @DisplayName("Site.system() returns lifecycle participation SYSTEM_MANAGED")
    void systemSiteHasSystemManagedParticipation() {
        assertThat(Site.system().lifecycleParticipation()).isEqualTo(LifecycleParticipation.SYSTEM_MANAGED);
    }

    @Test
    @DisplayName("Should handle aliases from strings")
    void siteAliasesFromStrings() {
        Site site = Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key("test")
                .displayName("Test")
                .aliasesFromStrings(Set.of("example.com", "www.example.com"))
                .build();
        
        assertThat(site.aliases()).hasSize(2);
        assertThat(site.aliases()).extracting(SiteAlias::value)
                .containsExactlyInAnyOrder("example.com", "www.example.com");
    }
}
