package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionsTest {

    @Test
    @DisplayName("Site permissions have the expected canonical values")
    void sitePermissions() {
        assertThat(Permissions.SITE_READ.value()).isEqualTo("site.read");
        assertThat(Permissions.SITE_CREATE.value()).isEqualTo("site.create");
        assertThat(Permissions.SITE_START.value()).isEqualTo("site.start");
        assertThat(Permissions.SITE_SUSPEND.value()).isEqualTo("site.suspend");
        assertThat(Permissions.SITE_ARCHIVE.value()).isEqualTo("site.archive");
    }

    @Test
    @DisplayName("Content type permissions have the expected canonical values")
    void contentTypePermissions() {
        assertThat(Permissions.CONTENT_TYPE_READ.value()).isEqualTo("contentType.read");
        assertThat(Permissions.CONTENT_TYPE_CREATE.value()).isEqualTo("contentType.create");
        assertThat(Permissions.CONTENT_TYPE_UPDATE.value()).isEqualTo("contentType.update");
        assertThat(Permissions.CONTENT_TYPE_ARCHIVE.value()).isEqualTo("contentType.archive");
    }

    @Test
    @DisplayName("Content item permissions have the expected canonical values")
    void contentItemPermissions() {
        assertThat(Permissions.CONTENT_ITEM_READ.value()).isEqualTo("contentItem.read");
        assertThat(Permissions.CONTENT_ITEM_CREATE.value()).isEqualTo("contentItem.create");
        assertThat(Permissions.CONTENT_ITEM_UPDATE.value()).isEqualTo("contentItem.update");
        assertThat(Permissions.CONTENT_ITEM_PUBLISH.value()).isEqualTo("contentItem.publish");
        assertThat(Permissions.CONTENT_ITEM_UNPUBLISH.value()).isEqualTo("contentItem.unpublish");
        assertThat(Permissions.CONTENT_ITEM_ARCHIVE.value()).isEqualTo("contentItem.archive");
    }

    @Test
    @DisplayName("Permission management keys have the expected canonical values")
    void permissionManagementPermissions() {
        assertThat(Permissions.PERMISSION_READ.value()).isEqualTo("permission.read");
        assertThat(Permissions.PERMISSION_GRANT.value()).isEqualTo("permission.grant");
        assertThat(Permissions.PERMISSION_REVOKE.value()).isEqualTo("permission.revoke");
    }

    @Test
    @DisplayName("Role management keys have the expected canonical values")
    void roleManagementPermissions() {
        assertThat(Permissions.ROLE_ASSIGN.value()).isEqualTo("role.assign");
        assertThat(Permissions.ROLE_REVOKE.value()).isEqualTo("role.revoke");
    }

    @Test
    @DisplayName("All built-in permission keys are distinct")
    void allPermissionsAreDistinct() {
        List<PermissionKey> all = allConstants();
        Set<String> values = all.stream().map(PermissionKey::value).collect(Collectors.toSet());
        assertThat(values).hasSize(all.size());
    }

    @Test
    @DisplayName("Catalog contains exactly 20 built-in permissions")
    void catalogSize() {
        assertThat(allConstants()).hasSize(20);
    }

    @Test
    @DisplayName("Permissions has only a private no-arg constructor")
    void hasOnlyPrivateConstructor() throws NoSuchMethodException {
        var constructor = Permissions.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(Permissions.class.getDeclaredConstructors()).hasSize(1);
    }

    // --- helpers ---

    private static List<PermissionKey> allConstants() {
        return Arrays.stream(Permissions.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers())
                        && f.getType() == PermissionKey.class)
                .map(f -> {
                    try {
                        return (PermissionKey) f.get(null);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .toList();
    }
}
