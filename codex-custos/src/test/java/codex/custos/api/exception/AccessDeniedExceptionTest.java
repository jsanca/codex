package codex.custos.api.exception;

import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.GlobalResourceRef;
import codex.custos.api.model.PermissionKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessDeniedExceptionTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Alice");
    private static final PermissionKey PERMISSION = PermissionKey.of("contentItem.publish");

    @Test
    @DisplayName("Should construct with a message")
    void constructWithMessage() {
        AccessDeniedException ex = new AccessDeniedException("operation not allowed");
        assertThat(ex.getMessage()).isEqualTo("operation not allowed");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should construct with a message and cause")
    void constructWithMessageAndCause() {
        Throwable cause = new IllegalStateException("upstream failure");
        AccessDeniedException ex = new AccessDeniedException("operation not allowed", cause);
        assertThat(ex.getMessage()).isEqualTo("operation not allowed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("decision() is empty when constructed from a raw message")
    void decisionEmptyForRawMessageConstructor() {
        AccessDeniedException ex = new AccessDeniedException("bare message");
        assertThat(ex.decision()).isEmpty();
    }

    @Test
    @DisplayName("Should carry the AccessDecision.Denied when constructed from a decision")
    void constructWithDecision() {
        AccessDecision.Denied denied = AccessDecision.denied(ACTOR, PERMISSION, GlobalResourceRef.INSTANCE, "no grant found");

        AccessDeniedException ex = new AccessDeniedException(denied);

        assertThat(ex.decision()).isPresent();
        assertThat(ex.decision().get()).isSameAs(denied);
        assertThat(ex.getMessage()).contains("Access denied");
        assertThat(ex.getMessage()).contains("no grant found");
    }

    @Test
    @DisplayName("Constructor with null decision rejects the argument")
    void constructWithNullDecisionThrows() {
        assertThatThrownBy(() -> new AccessDeniedException((AccessDecision.Denied) null))
                .isInstanceOf(NullPointerException.class);
    }
}
