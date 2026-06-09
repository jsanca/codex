package codex.custos.api.model;

/**
 * Represents the scope at which a permission or role is assigned.
 * <p>
 * A permission assigned within one site never applies to another site. Scope resolution
 * walks from most-specific to least-specific: content item → content type → site → global.
 *
 * @see ResourceRef for the concrete resource being evaluated
 */
public sealed interface ResourceScope
        permits GlobalScope, SiteScope, ContentTypeScope, ContentItemScope {
}
