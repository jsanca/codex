package codex.codex.api.model.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueTests {

    @Test
    @DisplayName("Locale should trim and validate language tag")
    void localeValid() {
        Locale locale = Locale.of("  en-US  ");
        assertThat(locale.languageTag()).isEqualTo("en-US");
    }

    @Test
    @DisplayName("Slug should trim and validate value")
    void slugValid() {
        Slug slug = Slug.of("  my-post-slug  ");
        assertThat(slug.value()).isEqualTo("my-post-slug");
    }

    @Test
    @DisplayName("Slug should throw for blank value")
    void slugBlank() {
        assertThatThrownBy(() -> Slug.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
