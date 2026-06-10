package codex.custos.api.model;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionGrantTest {

    private static final SiteKey SITE = SiteKey.of("site-a");
    private static final ContentTypeKey CT = ContentTypeKey.of("blog-post");
    private static final ContentItemKey CI = ContentItemKey.of("hello-world");

    @Test
    @DisplayName("Creates a grant with the given permission and scope")
    void createGrant() {
        ResourceScope scope = new SiteScope(SITE);
        PermissionGrant grant = PermissionGrant.of(Permissions.SITE_READ, scope);

        assertThat(grant.permission()).isEqualTo(Permissions.SITE_READ);
        assertThat(grant.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Rejects null permission")
    void rejectsNullPermission() {
        assertThatThrownBy(() -> PermissionGrant.of(null, GlobalScope.INSTANCE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects null scope")
    void rejectsNullScope() {
        assertThatThrownBy(() -> PermissionGrant.of(Permissions.SITE_READ, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Preserves permission exactly")
    void preservesPermission() {
        PermissionGrant grant = PermissionGrant.of(Permissions.CONTENT_ITEM_PUBLISH, GlobalScope.INSTANCE);
        assertThat(grant.permission()).isSameAs(Permissions.CONTENT_ITEM_PUBLISH);
    }

    @Test
    @DisplayName("Preserves scope exactly")
    void preservesScope() {
        SiteScope scope = new SiteScope(SITE);
        PermissionGrant grant = PermissionGrant.of(Permissions.SITE_ARCHIVE, scope);
        assertThat(grant.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Equality works by value — two grants with same permission and scope are equal")
    void equalityByValue() {
        PermissionGrant grant1 = PermissionGrant.of(Permissions.CONTENT_ITEM_READ, new SiteScope(SITE));
        PermissionGrant grant2 = PermissionGrant.of(Permissions.CONTENT_ITEM_READ, new SiteScope(SITE));
        assertThat(grant1).isEqualTo(grant2);
    }

    @Test
    @DisplayName("Can grant contentItem.update on ContentTypeScope")
    void grantUpdateOnContentTypeScope() {
        ContentTypeScope scope = new ContentTypeScope(SITE, CT);
        PermissionGrant grant = PermissionGrant.of(Permissions.CONTENT_ITEM_UPDATE, scope);

        assertThat(grant.permission()).isEqualTo(Permissions.CONTENT_ITEM_UPDATE);
        assertThat(grant.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Can grant contentItem.publish on ContentItemScope")
    void grantPublishOnContentItemScope() {
        ContentItemScope scope = new ContentItemScope(SITE, CT, CI);
        PermissionGrant grant = PermissionGrant.of(Permissions.CONTENT_ITEM_PUBLISH, scope);

        assertThat(grant.permission()).isEqualTo(Permissions.CONTENT_ITEM_PUBLISH);
        assertThat(grant.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Does not contain an Actor field")
    void hasNoActorField() {
        assertThat(PermissionGrant.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("actor"));
    }

    @Test
    @DisplayName("Does not contain a Role field")
    void hasNoRoleField() {
        assertThat(PermissionGrant.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("role"));
    }

    @Test
    @DisplayName("Does not evaluate access — has no evaluate or isGranted method")
    void doesNotEvaluateAccess() {
        assertThat(PermissionGrant.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("evaluate") || m.getName().equals("isGranted"));
    }
}
