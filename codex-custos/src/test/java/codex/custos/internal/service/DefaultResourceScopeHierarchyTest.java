package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.SiteScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultResourceScopeHierarchyTest {

    private DefaultResourceScopeHierarchy hierarchy;

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final SiteKey SITE_B = SiteKey.of("site-b");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final ContentTypeKey NEWS = ContentTypeKey.of("news-item");
    private static final ContentItemKey ITEM_1 = ContentItemKey.of("welcome-post");
    private static final ContentItemKey ITEM_2 = ContentItemKey.of("second-post");

    @BeforeEach
    void setUp() {
        hierarchy = new DefaultResourceScopeHierarchy();
    }

    // -----------------------------------------------------------------------
    // parentOf
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("parentOf")
    class ParentOf {

        @Test
        @DisplayName("ContentItemScope → ContentTypeScope (same site and type)")
        void contentItemParent() {
            ContentItemScope scope = new ContentItemScope(SITE_A, BLOG, ITEM_1);
            assertThat(hierarchy.parentOf(scope))
                    .isEqualTo(new ContentTypeScope(SITE_A, BLOG));
        }

        @Test
        @DisplayName("ContentTypeScope → SiteScope (same site)")
        void contentTypeParent() {
            ContentTypeScope scope = new ContentTypeScope(SITE_A, BLOG);
            assertThat(hierarchy.parentOf(scope))
                    .isEqualTo(new SiteScope(SITE_A));
        }

        @Test
        @DisplayName("SiteScope → GlobalScope")
        void siteScopeParent() {
            assertThat(hierarchy.parentOf(new SiteScope(SITE_A)))
                    .isEqualTo(GlobalScope.INSTANCE);
        }

        @Test
        @DisplayName("GlobalScope → null (top of hierarchy)")
        void globalScopeParent() {
            assertThat(hierarchy.parentOf(GlobalScope.INSTANCE)).isNull();
        }

        @Test
        @DisplayName("site key is preserved when walking up from ContentItemScope")
        void siteKeyPreserved() {
            ContentItemScope scope = new ContentItemScope(SITE_B, NEWS, ITEM_1);
            assertThat(hierarchy.parentOf(scope))
                    .isEqualTo(new ContentTypeScope(SITE_B, NEWS));
        }
    }

    // -----------------------------------------------------------------------
    // covers
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("covers")
    class Covers {

        @Test
        @DisplayName("GlobalScope covers GlobalScope")
        void globalCoversGlobal() {
            assertThat(hierarchy.covers(GlobalScope.INSTANCE, GlobalScope.INSTANCE)).isTrue();
        }

        @Test
        @DisplayName("GlobalScope covers SiteScope")
        void globalCoversSite() {
            assertThat(hierarchy.covers(GlobalScope.INSTANCE, new SiteScope(SITE_A))).isTrue();
        }

        @Test
        @DisplayName("GlobalScope covers ContentTypeScope")
        void globalCoversContentType() {
            assertThat(hierarchy.covers(GlobalScope.INSTANCE, new ContentTypeScope(SITE_A, BLOG))).isTrue();
        }

        @Test
        @DisplayName("GlobalScope covers ContentItemScope")
        void globalCoversContentItem() {
            assertThat(hierarchy.covers(GlobalScope.INSTANCE,
                    new ContentItemScope(SITE_A, BLOG, ITEM_1))).isTrue();
        }

        @Test
        @DisplayName("SiteScope covers same SiteScope")
        void siteCoversSameSite() {
            assertThat(hierarchy.covers(new SiteScope(SITE_A), new SiteScope(SITE_A))).isTrue();
        }

        @Test
        @DisplayName("SiteScope covers ContentTypeScope in same site")
        void siteCoversSameContentType() {
            assertThat(hierarchy.covers(new SiteScope(SITE_A), new ContentTypeScope(SITE_A, BLOG))).isTrue();
        }

        @Test
        @DisplayName("SiteScope covers ContentItemScope in same site")
        void siteCoversSameContentItem() {
            assertThat(hierarchy.covers(new SiteScope(SITE_A),
                    new ContentItemScope(SITE_A, BLOG, ITEM_1))).isTrue();
        }

        @Test
        @DisplayName("SiteScope does NOT cover different SiteScope")
        void siteDoesNotCoverOtherSite() {
            assertThat(hierarchy.covers(new SiteScope(SITE_A), new SiteScope(SITE_B))).isFalse();
        }

        @Test
        @DisplayName("SiteScope does NOT cover GlobalScope (upward coverage forbidden)")
        void siteDoesNotCoverGlobal() {
            assertThat(hierarchy.covers(new SiteScope(SITE_A), GlobalScope.INSTANCE)).isFalse();
        }

        @Test
        @DisplayName("ContentTypeScope covers same ContentTypeScope")
        void contentTypeCoversSame() {
            ContentTypeScope scope = new ContentTypeScope(SITE_A, BLOG);
            assertThat(hierarchy.covers(scope, scope)).isTrue();
        }

        @Test
        @DisplayName("ContentTypeScope covers ContentItemScope within same site and type")
        void contentTypeCoversSameContentItem() {
            assertThat(hierarchy.covers(new ContentTypeScope(SITE_A, BLOG),
                    new ContentItemScope(SITE_A, BLOG, ITEM_1))).isTrue();
        }

        @Test
        @DisplayName("ContentTypeScope does NOT cover ContentItemScope with different type")
        void contentTypeDoesNotCoverDifferentType() {
            assertThat(hierarchy.covers(new ContentTypeScope(SITE_A, BLOG),
                    new ContentItemScope(SITE_A, NEWS, ITEM_1))).isFalse();
        }

        @Test
        @DisplayName("ContentTypeScope does NOT cover ContentItemScope in different site")
        void contentTypeDoesNotCoverDifferentSite() {
            assertThat(hierarchy.covers(new ContentTypeScope(SITE_A, BLOG),
                    new ContentItemScope(SITE_B, BLOG, ITEM_1))).isFalse();
        }

        @Test
        @DisplayName("ContentTypeScope does NOT cover SiteScope (upward coverage forbidden)")
        void contentTypeDoesNotCoverSite() {
            assertThat(hierarchy.covers(new ContentTypeScope(SITE_A, BLOG), new SiteScope(SITE_A))).isFalse();
        }

        @Test
        @DisplayName("ContentItemScope covers exact same ContentItemScope")
        void contentItemCoversSelf() {
            ContentItemScope scope = new ContentItemScope(SITE_A, BLOG, ITEM_1);
            assertThat(hierarchy.covers(scope, scope)).isTrue();
        }

        @Test
        @DisplayName("ContentItemScope does NOT cover a different ContentItemScope")
        void contentItemDoesNotCoverOtherItem() {
            assertThat(hierarchy.covers(
                    new ContentItemScope(SITE_A, BLOG, ITEM_1),
                    new ContentItemScope(SITE_A, BLOG, ITEM_2))).isFalse();
        }
    }
}
