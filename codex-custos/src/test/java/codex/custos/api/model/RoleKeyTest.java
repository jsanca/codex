package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleKeyTest {

    @Test
    @DisplayName("Should create a RoleKey from a valid value")
    void createFromValidValue() {
        RoleKey key = RoleKey.of("EDITOR");
        assertThat(key.value()).isEqualTo("EDITOR");
    }

    @Test
    @DisplayName("Should preserve canonical value exactly — no trimming or casing change")
    void preservesCanonicalValue() {
        assertThat(RoleKey.of("SITE_ADMIN").value()).isEqualTo("SITE_ADMIN");
        assertThat(RoleKey.of("custom-role").value()).isEqualTo("custom-role");
    }

    @Test
    @DisplayName("Should reject null value")
    void rejectsNull() {
        assertThatThrownBy(() -> RoleKey.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject empty string")
    void rejectsEmpty() {
        assertThatThrownBy(() -> RoleKey.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject blank string")
    void rejectsBlank() {
        assertThatThrownBy(() -> RoleKey.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString returns the raw value")
    void toStringReturnsValue() {
        assertThat(RoleKey.of("REVIEWER").toString()).isEqualTo("REVIEWER");
    }

    @Test
    @DisplayName("Two keys with the same value are equal")
    void equality() {
        assertThat(RoleKey.of("COPYWRITER")).isEqualTo(RoleKey.of("COPYWRITER"));
    }

    @Test
    @DisplayName("Keys with different values are not equal")
    void inequality() {
        assertThat(RoleKey.of("EDITOR")).isNotEqualTo(RoleKey.of("VIEWER"));
    }
}
