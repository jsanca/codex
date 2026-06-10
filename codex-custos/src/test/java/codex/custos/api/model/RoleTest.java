package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    private static final RoleKey EDITOR = RoleKey.of("EDITOR");

    @Test
    @DisplayName("Role.of(key, Set) creates a role with the given key and permissions")
    void createWithSet() {
        Set<PermissionKey> permissions = Set.of(Permissions.CONTENT_ITEM_READ, Permissions.CONTENT_ITEM_UPDATE);
        Role role = Role.of(EDITOR, permissions);

        assertThat(role.key()).isEqualTo(EDITOR);
        assertThat(role.permissions()).containsExactlyInAnyOrder(
                Permissions.CONTENT_ITEM_READ, Permissions.CONTENT_ITEM_UPDATE);
    }

    @Test
    @DisplayName("Role.of(key, varargs) creates a role with the given permissions")
    void createWithVarargs() {
        Role role = Role.of(EDITOR, Permissions.CONTENT_ITEM_READ, Permissions.CONTENT_ITEM_PUBLISH);

        assertThat(role.permissions()).containsExactlyInAnyOrder(
                Permissions.CONTENT_ITEM_READ, Permissions.CONTENT_ITEM_PUBLISH);
    }

    @Test
    @DisplayName("Empty permission set is allowed — role with no permissions is valid")
    void emptyPermissionsAllowed() {
        Role role = Role.of(EDITOR, Set.of());
        assertThat(role.permissions()).isEmpty();
    }

    @Test
    @DisplayName("Role rejects null key")
    void rejectsNullKey() {
        assertThatThrownBy(() -> Role.of(null, Set.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Role rejects null permission set")
    void rejectsNullPermissionSet() {
        assertThatThrownBy(() -> Role.of(EDITOR, (Set<PermissionKey>) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Role rejects null element inside permission set")
    void rejectsNullPermissionElement() {
        Set<PermissionKey> withNull = new HashSet<>();
        withNull.add(Permissions.CONTENT_ITEM_READ);
        withNull.add(null);

        assertThatThrownBy(() -> Role.of(EDITOR, withNull))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Role defensively copies the permission set — external mutation does not affect the role")
    void defensiveCopy() {
        Set<PermissionKey> mutable = new HashSet<>();
        mutable.add(Permissions.CONTENT_ITEM_READ);
        Role role = Role.of(EDITOR, mutable);

        mutable.add(Permissions.CONTENT_ITEM_PUBLISH);

        assertThat(role.permissions()).containsOnly(Permissions.CONTENT_ITEM_READ);
    }

    @Test
    @DisplayName("Exposed permission set is immutable")
    void permissionsAreImmutable() {
        Role role = Role.of(EDITOR, Permissions.CONTENT_ITEM_READ);

        assertThatThrownBy(() -> role.permissions().add(Permissions.CONTENT_ITEM_PUBLISH))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Role has no scope field")
    void hasNoScopeField() {
        assertThat(Role.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("scope"));
    }

    @Test
    @DisplayName("Role has no actor field")
    void hasNoActorField() {
        assertThat(Role.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("actor"));
    }

    @Test
    @DisplayName("Role can contain built-in Permissions constants")
    void canContainBuiltInPermissions() {
        Role editor = Role.of(EDITOR,
                Permissions.CONTENT_ITEM_READ,
                Permissions.CONTENT_ITEM_UPDATE,
                Permissions.CONTENT_ITEM_PUBLISH);

        assertThat(editor.permissions()).contains(
                Permissions.CONTENT_ITEM_READ,
                Permissions.CONTENT_ITEM_UPDATE,
                Permissions.CONTENT_ITEM_PUBLISH);
    }
}
