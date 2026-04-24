package codex.codex.api.model.entity;

import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiteTest {

    @Test
    @DisplayName("Should create Site with valid builder data")
    void siteBuilderValid() {
        final SiteId id = SiteId.generate();
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
        SiteId id = SiteId.generate();
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
                .id(SiteId.generate())
                .key("test")
                .displayName("Test")
                .build();
        
        assertThat(site.status()).isEqualTo(SiteStatus.STARTED);
    }

    @Test
    @DisplayName("Should handle aliases from strings")
    void siteAliasesFromStrings() {
        Site site = Site.builder()
                .id(SiteId.generate())
                .key("test")
                .displayName("Test")
                .aliasesFromStrings(Set.of("example.com", "www.example.com"))
                .build();
        
        assertThat(site.aliases()).hasSize(2);
        assertThat(site.aliases()).extracting(SiteAlias::value)
                .containsExactlyInAnyOrder("example.com", "www.example.com");
    }
}
