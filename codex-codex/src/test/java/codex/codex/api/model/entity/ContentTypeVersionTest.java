package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.api.model.value.FieldType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentTypeVersionTest {

    @Test
    @DisplayName("Should create valid ContentTypeVersion")
    void validCreation() {
        ContentTypeVersion version = ContentTypeVersion.builder()
                .id(ContentTypeVersionId.generate())
                .contentTypeId(ContentTypeId.generate())
                .version(1)
                .fields(List.of(
                        Field.builder()
                                .key(FieldKey.TITLE)
                                .type(FieldType.TEXT)
                                .required(true)
                                .build()
                ))
                .build();

        assertThat(version.version()).isEqualTo(1);
        assertThat(version.status()).isEqualTo(ContentTypeVersionStatus.DRAFT);
        assertThat(version.fields()).hasSize(1);
        assertThat(version.fields().get(0).key()).isEqualTo(FieldKey.TITLE);
    }

    @Test
    @DisplayName("Should throw exception for version < 1")
    void invalidVersion() {
        assertThatThrownBy(() -> ContentTypeVersion.builder()
                .id(ContentTypeVersionId.generate())
                .contentTypeId(ContentTypeId.generate())
                .version(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version must be >= 1");
    }
}
