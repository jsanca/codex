package codex.codex.api.model.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTests {

    @Test
    @DisplayName("SiteId should be created with valid value")
    void siteIdValid() {
        SiteId id = SiteId.of("test-site");
        assertThat(id.value()).isEqualTo("test-site");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("SiteId should throw exception for blank value")
    void siteIdBlank(String value) {
        assertThatThrownBy(() -> SiteId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SiteId value cannot be blank");
    }

    @Test
    @DisplayName("SiteId should throw exception for null value")
    void siteIdNull() {
        assertThatThrownBy(() -> SiteId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SiteId.generate() should create unique IDs")
    void siteIdGenerate() {
        SiteId id1 = SiteId.generate();
        SiteId id2 = SiteId.generate();
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotBlank();
    }

    @Test
    @DisplayName("FieldKey should trim value")
    void fieldKeyTrim() {
        FieldKey key = FieldKey.of("  title  ");
        assertThat(key.value()).isEqualTo("title");
    }

    @Test
    @DisplayName("FieldKey.TITLE constant should be valid")
    void fieldKeyTitleConstant() {
        assertThat(FieldKey.TITLE.value()).isEqualTo("title");
    }
}
