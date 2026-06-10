package codex.custos.api.model;

import codex.custos.api.exception.AccessDeniedException;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessDecisionTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Alice");
    private static final PermissionKey PERMISSION = PermissionKey.of("contentItem.publish");
    private static final ResourceRef RESOURCE = GlobalResourceRef.INSTANCE;

    @Test
    @DisplayName("Granted decision reports isGranted true and requireGranted is a no-op")
    void grantedBehavior() {
        AccessDecision decision = AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, "user has EDITOR role");

        assertThat(decision.isGranted()).isTrue();
        assertThat(decision).isInstanceOf(AccessDecision.Granted.class);
        decision.requireGranted(); // must not throw
    }

    @Test
    @DisplayName("Denied decision reports isGranted false")
    void deniedIsNotGranted() {
        AccessDecision decision = AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, "no matching grant");

        assertThat(decision.isGranted()).isFalse();
        assertThat(decision).isInstanceOf(AccessDecision.Denied.class);
    }

    @Test
    @DisplayName("Denied.requireGranted throws AccessDeniedException carrying the same Denied instance")
    void deniedThrowsAndCarriesDecision() {
        AccessDecision.Denied denied = AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, "no matching grant");

        AccessDeniedException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                AccessDeniedException.class, denied::requireGranted);

        assertThat(thrown).hasMessageContaining("Access denied");
        assertThat(thrown.decision()).isPresent();
        assertThat(thrown.decision().get()).isSameAs(denied);
    }

    @Test
    @DisplayName("Denied preserves the reason in the thrown exception message")
    void deniedReasonAppearsInMessage() {
        AccessDecision.Denied denied = AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, "rate limit exceeded");

        assertThatThrownBy(denied::requireGranted)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    @DisplayName("AccessDecision carries actor, permission, resource, and reason")
    void carriesPayload() {
        AccessDecision decision = AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, "granted by policy");

        assertThat(decision.actor()).isEqualTo(ACTOR);
        assertThat(decision.permission()).isEqualTo(PERMISSION);
        assertThat(decision.resource()).isEqualTo(RESOURCE);
        assertThat(decision.reason()).isEqualTo("granted by policy");
    }

    @Test
    @DisplayName("Granted rejects null and blank reason")
    void grantedRejectsInvalidReason() {
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Denied rejects null and blank reason")
    void deniedRejectsInvalidReason() {
        assertThatThrownBy(() -> AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Granted rejects null actor, permission, and resource")
    void grantedRejectsNullFields() {
        assertThatThrownBy(() -> AccessDecision.granted(null, PERMISSION, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, null, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, PERMISSION, null, "reason"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Denied rejects null actor, permission, and resource")
    void deniedRejectsNullFields() {
        assertThatThrownBy(() -> AccessDecision.denied(null, PERMISSION, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.denied(ACTOR, null, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.denied(ACTOR, PERMISSION, null, "reason"))
                .isInstanceOf(NullPointerException.class);
    }
}
