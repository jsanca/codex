package codex.custos.api.model;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceScopeTest {

    private static final SiteKey SITE = SiteKey.of("site-a");
    private static final ContentTypeKey CT = ContentTypeKey.of("blog-post");
    private static final ContentItemKey CI = ContentItemKey.of("hello-world");

    @Test
    @DisplayName("GlobalScope singleton is a ResourceScope")
    void globalScopeIsResourceScope() {
        ResourceScope scope = GlobalScope.INSTANCE;
        assertThat(scope).isInstanceOf(GlobalScope.class);
    }

    @Test
    @DisplayName("SiteScope exposes its site key")
    void siteScopeExposesSiteKey() {
        SiteScope scope = new SiteScope(SITE);
        assertThat(scope.siteKey()).isEqualTo(SITE);
    }

    @Test
    @DisplayName("SiteScope rejects null site key")
    void siteScopeRejectsNull() {
        assertThatThrownBy(() -> new SiteScope(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ContentTypeScope exposes its fields")
    void contentTypeScopeExposesFields() {
        ContentTypeScope scope = new ContentTypeScope(SITE, CT);
        assertThat(scope.siteKey()).isEqualTo(SITE);
        assertThat(scope.contentTypeKey()).isEqualTo(CT);
    }

    @Test
    @DisplayName("ContentTypeScope rejects null arguments")
    void contentTypeScopeRejectsNulls() {
        assertThatThrownBy(() -> new ContentTypeScope(null, CT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentTypeScope(SITE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ContentItemScope exposes all three keys")
    void contentItemScopeExposesFields() {
        ContentItemScope scope = new ContentItemScope(SITE, CT, CI);
        assertThat(scope.siteKey()).isEqualTo(SITE);
        assertThat(scope.contentTypeKey()).isEqualTo(CT);
        assertThat(scope.contentItemKey()).isEqualTo(CI);
    }

    @Test
    @DisplayName("ContentItemScope rejects null arguments")
    void contentItemScopeRejectsNulls() {
        assertThatThrownBy(() -> new ContentItemScope(null, CT, CI))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentItemScope(SITE, null, CI))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentItemScope(SITE, CT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
