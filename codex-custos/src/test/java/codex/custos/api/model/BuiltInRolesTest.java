package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInRolesTest {

    // --- canonical role keys ---

    @Test
    @DisplayName("SUPER_ADMIN has key SUPER_ADMIN")
    void superAdminKey() {
        assertThat(BuiltInRoles.SUPER_ADMIN.key()).isEqualTo(RoleKey.of("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("SITE_ADMIN has key SITE_ADMIN")
    void siteAdminKey() {
        assertThat(BuiltInRoles.SITE_ADMIN.key()).isEqualTo(RoleKey.of("SITE_ADMIN"));
    }

    @Test
    @DisplayName("EDITOR has key EDITOR")
    void editorKey() {
        assertThat(BuiltInRoles.EDITOR.key()).isEqualTo(RoleKey.of("EDITOR"));
    }

    @Test
    @DisplayName("COPYWRITER has key COPYWRITER")
    void copywriterKey() {
        assertThat(BuiltInRoles.COPYWRITER.key()).isEqualTo(RoleKey.of("COPYWRITER"));
    }

    @Test
    @DisplayName("REVIEWER has key REVIEWER")
    void reviewerKey() {
        assertThat(BuiltInRoles.REVIEWER.key()).isEqualTo(RoleKey.of("REVIEWER"));
    }

    @Test
    @DisplayName("VIEWER has key VIEWER")
    void viewerKey() {
        assertThat(BuiltInRoles.VIEWER.key()).isEqualTo(RoleKey.of("VIEWER"));
    }

    // --- canonical permission sets ---

    @Test
    @DisplayName("SUPER_ADMIN holds all 20 built-in permissions")
    void superAdminHasAllPermissions() {
        assertThat(BuiltInRoles.SUPER_ADMIN.permissions()).hasSize(20);
        assertThat(BuiltInRoles.SUPER_ADMIN.permissions()).contains(
                Permissions.PERMISSION_GRANT,
                Permissions.PERMISSION_REVOKE,
                Permissions.ROLE_ASSIGN,
                Permissions.ROLE_REVOKE,
                Permissions.SITE_CREATE);
    }

    @Test
    @DisplayName("SITE_ADMIN has the expected 17 permissions")
    void siteAdminPermissions() {
        assertThat(BuiltInRoles.SITE_ADMIN.permissions()).hasSize(17);
        assertThat(BuiltInRoles.SITE_ADMIN.permissions()).contains(
                Permissions.SITE_READ,
                Permissions.CONTENT_TYPE_DELETE,
                Permissions.CONTENT_ITEM_PUBLISH,
                Permissions.PERMISSION_READ,
                Permissions.ROLE_ASSIGN,
                Permissions.ROLE_REVOKE);
        assertThat(BuiltInRoles.SITE_ADMIN.permissions())
                .doesNotContain(Permissions.SITE_CREATE);
    }

    @Test
    @DisplayName("EDITOR has read, create, update, publish, unpublish, archive")
    void editorPermissions() {
        assertThat(BuiltInRoles.EDITOR.permissions()).containsExactlyInAnyOrder(
                Permissions.CONTENT_ITEM_READ,
                Permissions.CONTENT_ITEM_CREATE,
                Permissions.CONTENT_ITEM_UPDATE,
                Permissions.CONTENT_ITEM_PUBLISH,
                Permissions.CONTENT_ITEM_UNPUBLISH,
                Permissions.CONTENT_ITEM_ARCHIVE);
    }

    @Test
    @DisplayName("COPYWRITER has read, create, update — no publish")
    void copywriterPermissions() {
        assertThat(BuiltInRoles.COPYWRITER.permissions()).containsExactlyInAnyOrder(
                Permissions.CONTENT_ITEM_READ,
                Permissions.CONTENT_ITEM_CREATE,
                Permissions.CONTENT_ITEM_UPDATE);
        assertThat(BuiltInRoles.COPYWRITER.permissions())
                .doesNotContain(Permissions.CONTENT_ITEM_PUBLISH);
    }

    @Test
    @DisplayName("REVIEWER has read, publish, unpublish — no update")
    void reviewerPermissions() {
        assertThat(BuiltInRoles.REVIEWER.permissions()).containsExactlyInAnyOrder(
                Permissions.CONTENT_ITEM_READ,
                Permissions.CONTENT_ITEM_PUBLISH,
                Permissions.CONTENT_ITEM_UNPUBLISH);
        assertThat(BuiltInRoles.REVIEWER.permissions())
                .doesNotContain(Permissions.CONTENT_ITEM_UPDATE);
    }

    @Test
    @DisplayName("VIEWER has only contentItem.read")
    void viewerPermissions() {
        assertThat(BuiltInRoles.VIEWER.permissions()).containsExactly(Permissions.CONTENT_ITEM_READ);
    }

    // --- no actor, no scope ---

    @Test
    @DisplayName("Built-in roles are Role instances with no actor or scope")
    void rolesAreBlueprints() {
        assertThat(Role.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("actor")
                        || f.getName().toLowerCase().contains("scope"));
    }

    // --- non-instantiable utility class ---

    @Test
    @DisplayName("BuiltInRoles has only a private constructor")
    void hasOnlyPrivateConstructor() throws NoSuchMethodException {
        var constructor = BuiltInRoles.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(BuiltInRoles.class.getDeclaredConstructors()).hasSize(1);
    }

    // --- SUPER_ADMIN bypass contract ---

    @Test
    @DisplayName("SUPER_ADMIN is a regular Role — bypass logic is not present in the blueprint")
    void superAdminIsRegularRole() {
        assertThat(BuiltInRoles.SUPER_ADMIN).isInstanceOf(Role.class);
        assertThat(BuiltInRoles.SUPER_ADMIN.getClass().getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("bypass") || m.getName().equals("isAdmin"));
    }

    /**
     * Documents the expected evaluator contract for SUPER_ADMIN.
     * This test does NOT exercise a resolver (none exists yet). It verifies that the
     * blueprint itself provides no bypass mechanism, and that the documented evaluation
     * order (hard rules → agent denial → bypass → normal resolution) is understood
     * as belonging entirely to the future PermissionResolver / policy layer.
     */
    @Test
    @DisplayName("SUPER_ADMIN blueprint carries all permissions but encodes no bypass, agent denial, or invariant skip")
    void superAdminBlueprintContractIsDocumented() {
        // Blueprint contains all permissions — introspectable by UI and resolver
        assertThat(BuiltInRoles.SUPER_ADMIN.permissions()).hasSize(20);

        // Blueprint is a plain Role record — no subclass, no special type
        assertThat(BuiltInRoles.SUPER_ADMIN.getClass()).isEqualTo(Role.class);

        // No method on Role encodes bypass or agent restriction — those live in the evaluator
        assertThat(Role.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("bypass")
                        || m.getName().equals("skipScopeCheck")
                        || m.getName().equals("isSuperAdmin"));
    }
}
