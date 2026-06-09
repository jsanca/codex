package codex.custos.api.model;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceRefTest {

    private static final SiteKey SITE = SiteKey.of("site-a");
    private static final ContentTypeKey CT = ContentTypeKey.of("blog-post");
    private static final ContentItemKey CI = ContentItemKey.of("hello-world");

    @Test
    @DisplayName("GlobalResourceRef singleton is a ResourceRef")
    void globalRefIsResourceRef() {
        ResourceRef ref = GlobalResourceRef.INSTANCE;
        assertThat(ref).isInstanceOf(GlobalResourceRef.class);
    }

    @Test
    @DisplayName("SiteResourceRef exposes its site key")
    void siteRefExposesSiteKey() {
        SiteResourceRef ref = new SiteResourceRef(SITE);
        assertThat(ref.siteKey()).isEqualTo(SITE);
    }

    @Test
    @DisplayName("SiteResourceRef rejects null site key")
    void siteRefRejectsNull() {
        assertThatThrownBy(() -> new SiteResourceRef(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ContentTypeResourceRef exposes its fields")
    void contentTypeRefExposesFields() {
        ContentTypeResourceRef ref = new ContentTypeResourceRef(SITE, CT);
        assertThat(ref.siteKey()).isEqualTo(SITE);
        assertThat(ref.contentTypeKey()).isEqualTo(CT);
    }

    @Test
    @DisplayName("ContentTypeResourceRef rejects null arguments")
    void contentTypeRefRejectsNulls() {
        assertThatThrownBy(() -> new ContentTypeResourceRef(null, CT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentTypeResourceRef(SITE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ContentItemResourceRef exposes all three keys")
    void contentItemRefExposesFields() {
        ContentItemResourceRef ref = new ContentItemResourceRef(SITE, CT, CI);
        assertThat(ref.siteKey()).isEqualTo(SITE);
        assertThat(ref.contentTypeKey()).isEqualTo(CT);
        assertThat(ref.contentItemKey()).isEqualTo(CI);
    }

    @Test
    @DisplayName("ContentItemResourceRef rejects null arguments")
    void contentItemRefRejectsNulls() {
        assertThatThrownBy(() -> new ContentItemResourceRef(null, CT, CI))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentItemResourceRef(SITE, null, CI))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContentItemResourceRef(SITE, CT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
