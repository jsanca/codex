package codex.custos.internal.service;

import codex.custos.api.model.ResourceScope;

/**
 * Knows the parent/ancestry relationships between {@link ResourceScope} levels.
 * <p>
 * Two concerns are captured here:
 * <ul>
 *   <li>{@link #parentOf} — walks one step up the hierarchy toward {@code GlobalScope}.</li>
 *   <li>{@link #covers} — decides whether an assignment at one scope applies to a target
 *       at an equal or more-specific scope, without crossing site boundaries.</li>
 * </ul>
 */
interface ResourceScopeHierarchy {

    /**
     * Returns the immediate parent scope of {@code scope}, or {@code null} if {@code scope}
     * is {@code GlobalScope} (the top of the hierarchy).
     *
     * @param scope the current scope; must not be null
     * @return the parent scope, or {@code null} at the top of the hierarchy
     */
    ResourceScope parentOf(ResourceScope scope);

    /**
     * Returns {@code true} if {@code assignmentScope} is an ancestor of or equal to
     * {@code targetScope}, without crossing site boundaries.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code GlobalScope} covers everything.</li>
     *   <li>{@code SiteScope(site-a)} covers any scope within site-a.</li>
     *   <li>{@code SiteScope(site-a)} does <em>not</em> cover {@code SiteScope(site-b)}.</li>
     *   <li>{@code SiteScope(site-a)} does <em>not</em> cover {@code GlobalScope}.</li>
     * </ul>
     *
     * @param assignmentScope the scope of the role assignment
     * @param targetScope     the scope of the resource being accessed
     * @return {@code true} if the assignment scope covers the target scope
     */
    boolean covers(ResourceScope assignmentScope, ResourceScope targetScope);
}
