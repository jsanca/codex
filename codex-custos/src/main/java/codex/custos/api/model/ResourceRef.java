package codex.custos.api.model;

/**
 * Points to a concrete Codex resource being evaluated for authorization.
 * <p>
 * A resource reference carries enough site context to prevent accidental cross-site
 * permission lookups. Permission resolution never crosses site boundaries.
 * Use pattern matching on the sealed subtypes to extract site or resource details.
 *
 * @see ResourceScope for where a permission or role is assigned
 */
public sealed interface ResourceRef
        permits GlobalResourceRef, SiteResourceRef, ContentTypeResourceRef, ContentItemResourceRef {
}
