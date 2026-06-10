package codex.custos.internal.service;

import codex.custos.api.model.PermissionKey;
import codex.custos.api.model.Permissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPermissionImplicationRulesTest {

    private DefaultPermissionImplicationRules rules;

    @BeforeEach
    void setUp() {
        rules = new DefaultPermissionImplicationRules();
    }

    @Test
    @DisplayName("direct permission match returns true")
    void directMatch() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_READ), Permissions.CONTENT_ITEM_READ)).isTrue();
    }

    @Test
    @DisplayName("empty role permissions returns false")
    void emptyPermissions() {
        assertThat(rules.permits(Set.of(), Permissions.CONTENT_ITEM_READ)).isFalse();
    }

    @Test
    @DisplayName("contentItem.update implies contentItem.read")
    void updateImpliesRead() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_UPDATE), Permissions.CONTENT_ITEM_READ)).isTrue();
    }

    @Test
    @DisplayName("contentItem.publish implies contentItem.read")
    void publishImpliesRead() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_PUBLISH), Permissions.CONTENT_ITEM_READ)).isTrue();
    }

    @Test
    @DisplayName("contentItem.publish does NOT imply contentItem.update")
    void publishDoesNotImplyUpdate() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_PUBLISH), Permissions.CONTENT_ITEM_UPDATE)).isFalse();
    }

    @Test
    @DisplayName("contentItem.update does NOT imply contentItem.publish")
    void updateDoesNotImplyPublish() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_UPDATE), Permissions.CONTENT_ITEM_PUBLISH)).isFalse();
    }

    @Test
    @DisplayName("implication from publish does not extend to unrelated permissions like site.read")
    void publishDoesNotImplySiteRead() {
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_PUBLISH), Permissions.SITE_READ)).isFalse();
    }

    @Test
    @DisplayName("role with unrelated permission does not grant contentItem.read via implication")
    void unrelatedPermissionDoesNotGrantRead() {
        assertThat(rules.permits(Set.of(Permissions.SITE_READ), Permissions.CONTENT_ITEM_READ)).isFalse();
    }

    @Test
    @DisplayName("role with both update and publish grants read via either implication")
    void bothUpdateAndPublishGrantRead() {
        assertThat(rules.permits(
                Set.of(Permissions.CONTENT_ITEM_UPDATE, Permissions.CONTENT_ITEM_PUBLISH),
                Permissions.CONTENT_ITEM_READ)).isTrue();
    }

    @Test
    @DisplayName("custom permission key matched directly when present")
    void customKeyDirectMatch() {
        PermissionKey custom = PermissionKey.of("custom.action");
        assertThat(rules.permits(Set.of(custom), custom)).isTrue();
    }

    @Test
    @DisplayName("custom permission key not implied by anything")
    void customKeyNotImplied() {
        PermissionKey custom = PermissionKey.of("custom.action");
        assertThat(rules.permits(Set.of(Permissions.CONTENT_ITEM_UPDATE), custom)).isFalse();
    }
}
