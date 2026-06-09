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
    @DisplayName("Denied.requireGranted throws AccessDeniedException")
    void deniedThrowsOnRequireGranted() {
        AccessDecision decision = AccessDecision.denied(ACTOR, PERMISSION, RESOURCE, "no matching grant");

        assertThatThrownBy(decision::requireGranted)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("AccessDecision carries actor, permission, resource, and reason")
    void carriessPayload() {
        AccessDecision decision = AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, "granted by policy");

        assertThat(decision.actor()).isEqualTo(ACTOR);
        assertThat(decision.permission()).isEqualTo(PERMISSION);
        assertThat(decision.resource()).isEqualTo(RESOURCE);
        assertThat(decision.reason()).isEqualTo("granted by policy");
    }

    @Test
    @DisplayName("Granted rejects null arguments")
    void grantedRejectsNulls() {
        assertThatThrownBy(() -> AccessDecision.granted(null, PERMISSION, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, null, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, PERMISSION, null, "reason"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessDecision.granted(ACTOR, PERMISSION, RESOURCE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Denied rejects null arguments")
    void deniedRejectsNulls() {
        assertThatThrownBy(() -> AccessDecision.denied(null, PERMISSION, RESOURCE, "reason"))
                .isInstanceOf(NullPointerException.class);
    }
}
