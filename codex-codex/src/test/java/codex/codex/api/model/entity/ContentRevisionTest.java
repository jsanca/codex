package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.EditorialState;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentRevisionTest {

    @Test
    @DisplayName("Should create valid ContentRevision")
    void validRevision() {
        ContentRevisionId id = ContentRevisionId.generate();
        ContentItemId itemId = ContentItemId.generate();
        ActorId author = ActorId.of("user-1");

        ContentRevision revision = ContentRevision.builder()
                .id(id)
                .contentItemId(itemId)
                .revisionNumber(5)
                .title("My Revision")
                .createdBy(author)
                .dataFromStrings(Map.of("title", "Hello World"))
                .build();

        assertThat(revision.revisionNumber()).isEqualTo(5);
        assertThat(revision.title()).isEqualTo("My Revision");
        assertThat(revision.state()).isEqualTo(EditorialState.DRAFT);
        assertThat(revision.data()).containsKey(FieldKey.TITLE);
        assertThat(revision.data().get(FieldKey.TITLE)).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should throw exception for revision number < 1")
    void invalidRevisionNumber() {
        assertThatThrownBy(() -> ContentRevision.builder()
                .id(ContentRevisionId.generate())
                .contentItemId(ContentItemId.generate())
                .revisionNumber(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
