package codex.custos.internal.service;

import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.ResourceScope;
import codex.custos.api.model.SiteScope;

/**
 * Default implementation of {@link ResourceScopeHierarchy}.
 * <p>
 * Scope hierarchy (most-specific → least-specific):
 * {@code ContentItemScope} → {@code ContentTypeScope} → {@code SiteScope} → {@code GlobalScope}
 * <p>
 * Site boundaries are never crossed during parent traversal: the parent of a
 * {@code ContentItemScope(site-a, ...)} is always a {@code ContentTypeScope(site-a, ...)}.
 */
final class DefaultResourceScopeHierarchy implements ResourceScopeHierarchy {

    @Override
    public ResourceScope parentOf(final ResourceScope scope) {
        return switch (scope) {
            case ContentItemScope s -> new ContentTypeScope(s.siteKey(), s.contentTypeKey());
            case ContentTypeScope s -> new SiteScope(s.siteKey());
            case SiteScope ignored  -> GlobalScope.INSTANCE;
            case GlobalScope ignored -> null;
        };
    }

    @Override
    public boolean covers(final ResourceScope assignmentScope, final ResourceScope targetScope) {
        return switch (assignmentScope) {
            case GlobalScope ignored -> true;
            case SiteScope s -> switch (targetScope) {
                case SiteScope ts       -> ts.siteKey().equals(s.siteKey());
                case ContentTypeScope cts -> cts.siteKey().equals(s.siteKey());
                case ContentItemScope cis -> cis.siteKey().equals(s.siteKey());
                case GlobalScope ignored  -> false;
            };
            case ContentTypeScope s -> switch (targetScope) {
                case ContentTypeScope cts -> cts.equals(s);
                case ContentItemScope cis -> cis.siteKey().equals(s.siteKey())
                        && cis.contentTypeKey().equals(s.contentTypeKey());
                default -> false;
            };
            case ContentItemScope s -> s.equals(targetScope);
        };
    }
}
