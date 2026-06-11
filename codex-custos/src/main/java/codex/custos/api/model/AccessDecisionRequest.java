package codex.custos.api.model;

import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * Encapsulates the inputs to a single access-decision evaluation.
 * <p>
 * An {@code AccessDecisionRequest} carries two conceptually related but type-distinct
 * representations of the target:
 * <ul>
 *   <li>{@link #resource} — the concrete {@link ResourceRef} that will appear in the
 *       resulting {@link AccessDecision}. This is the "what object" the caller is asking
 *       about (e.g., a specific content item).</li>
 *   <li>{@link #targetScope} — the {@link ResourceScope} passed to {@link
 *       codex.custos.api.service.PermissionResolver} for the scope hierarchy walk.
 *       This is the "at which scope level" the permission is resolved.</li>
 * </ul>
 * <p>
 * These two fields are kept separate because {@code ResourceRef} and {@code ResourceScope}
 * are parallel but distinct hierarchies. A future implementation may use {@code ResourceRef}
 * to load resource data while {@code ResourceScope} drives the permission walk. Hiding this
 * difference would couple the two concerns prematurely.
 *
 * @see codex.custos.api.service.AccessDecisionService
 * @author jsanca/clio/elo
 */
public record AccessDecisionRequest(
        Actor actor,
        PermissionKey permission,
        ResourceRef resource,
        ResourceScope targetScope) {

    public AccessDecisionRequest {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(targetScope, "targetScope must not be null");
    }

    /**
     * Creates an {@link AccessDecisionRequest} from the given components.
     *
     * @param actor       the actor requesting the operation; must not be null
     * @param permission  the domain permission being requested; must not be null
     * @param resource    the concrete resource the operation targets; must not be null
     * @param targetScope the resource scope used for permission resolution; must not be null
     * @return an immutable request
     */
    public static AccessDecisionRequest of(final Actor actor, final PermissionKey permission,
                                           final ResourceRef resource, final ResourceScope targetScope) {
        return new AccessDecisionRequest(actor, permission, resource, targetScope);
    }

    /**
     * Creates a new builder for an {@link AccessDecisionRequest}.
     *
     * @return an empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized from an existing request.
     *
     * @param request the request to copy from; must not be null
     * @return a builder pre-populated with the request values
     */
    public static Builder copyOf(final AccessDecisionRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        return builder()
                .actor(request.actor)
                .permission(request.permission)
                .resource(request.resource)
                .targetScope(request.targetScope);
    }

    /**
     * Builder for {@link AccessDecisionRequest}.
     * <p>
     * Prefer this builder when constructing requests at call sites where the distinction
     * between {@link ResourceRef} and {@link ResourceScope} should remain explicit.
     */
    public static final class Builder {

        private Actor actor;
        private PermissionKey permission;
        private ResourceRef resource;
        private ResourceScope targetScope;

        private Builder() {
        }

        public Builder actor(final Actor actor) {
            this.actor = actor;
            return this;
        }

        public Builder permission(final PermissionKey permission) {
            this.permission = permission;
            return this;
        }

        public Builder resource(final ResourceRef resource) {
            this.resource = resource;
            return this;
        }

        public Builder targetScope(final ResourceScope targetScope) {
            this.targetScope = targetScope;
            return this;
        }

        public AccessDecisionRequest build() {
            return new AccessDecisionRequest(actor, permission, resource, targetScope);
        }
    }
}