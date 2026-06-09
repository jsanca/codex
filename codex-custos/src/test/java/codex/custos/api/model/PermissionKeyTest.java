package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionKeyTest {

    @Test
    @DisplayName("Should create a PermissionKey from a valid value")
    void createFromValidValue() {
        PermissionKey key = PermissionKey.of("contentItem.publish");
        assertThat(key.value()).isEqualTo("contentItem.publish");
    }

    @Test
    @DisplayName("Should trim whitespace on construction")
    void trimsWhitespace() {
        PermissionKey key = PermissionKey.of("  site.read  ");
        assertThat(key.value()).isEqualTo("site.read");
    }

    @Test
    @DisplayName("Should reject null value")
    void rejectsNull() {
        assertThatThrownBy(() -> PermissionKey.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject blank value")
    void rejectsBlank() {
        assertThatThrownBy(() -> PermissionKey.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString returns the raw value")
    void toStringReturnsValue() {
        assertThat(PermissionKey.of("role.assign").toString()).isEqualTo("role.assign");
    }

    @Test
    @DisplayName("Two keys with the same value are equal")
    void equality() {
        assertThat(PermissionKey.of("contentItem.update"))
                .isEqualTo(PermissionKey.of("contentItem.update"));
    }
}
