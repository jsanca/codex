package codex.custos.api.service;

import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.internal.service.DefaultAccessDecisionService;
import codex.custos.internal.service.DefaultContentItemPermissionsService;
import codex.custos.internal.service.DefaultContentTypePermissionsService;
import codex.custos.internal.service.DefaultPermissionResolver;
import codex.custos.internal.service.DefaultSitePermissionsService;
import codex.custos.internal.service.SecuredContentItemService;
import codex.custos.internal.service.SecuredContentTypeService;
import codex.custos.internal.service.SecuredSiteService;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Public factory for wrapping raw Codex services with Custos authorization decorators.
 *
 * <p>Contains zero authorization logic — all policy decisions are encoded in the injected
 * permissions services and the internal default implementations they delegate to.</p>
 *
 * <p>This class is the sole public bridge between external modules (e.g. {@code codex-concilium})
 * and Custos internal service implementations. Callers receive service interface types only;
 * the concrete {@code Secured*Service} and {@code Default*PermissionsService} classes remain
 * internal to this module.</p>
 */
public final class SecuredServiceComposer {

    private SecuredServiceComposer() {}

    // --- default authorization stack factories ---

    /**
     * Creates the default Custos authorization decision service (default resolver + evaluator).
     *
     * @return a new {@link AccessDecisionService} backed by {@code DefaultPermissionResolver}; never null
     */
    public static AccessDecisionService defaultAccessDecisionService() {
        return new DefaultAccessDecisionService(new DefaultPermissionResolver());
    }

    /**
     * Creates the default site permissions service backed by the given decision service.
     *
     * @param decisionService the decision service to delegate to; must not be null
     * @return a new {@link SitePermissionsService}; never null
     */
    public static SitePermissionsService defaultSitePermissionsService(
            final AccessDecisionService decisionService) {
        Objects.requireNonNull(decisionService, "decisionService must not be null");
        return new DefaultSitePermissionsService(decisionService);
    }

    /**
     * Creates the default content-type permissions service backed by the given decision service.
     *
     * @param decisionService the decision service to delegate to; must not be null
     * @return a new {@link ContentTypePermissionsService}; never null
     */
    public static ContentTypePermissionsService defaultContentTypePermissionsService(
            final AccessDecisionService decisionService) {
        Objects.requireNonNull(decisionService, "decisionService must not be null");
        return new DefaultContentTypePermissionsService(decisionService);
    }

    /**
     * Creates the default content-item permissions service backed by the given decision service.
     *
     * @param decisionService the decision service to delegate to; must not be null
     * @return a new {@link ContentItemPermissionsService}; never null
     */
    public static ContentItemPermissionsService defaultContentItemPermissionsService(
            final AccessDecisionService decisionService) {
        Objects.requireNonNull(decisionService, "decisionService must not be null");
        return new DefaultContentItemPermissionsService(decisionService);
    }

    // --- service wrapper factories ---

    /**
     * Wraps the given {@link SiteService} with an authorization-enforcing decorator.
     *
     * @param delegate           the raw service to delegate authorized operations to; must not be null
     * @param permissionsService the permissions service for authorization checks; must not be null
     * @param snapshotProvider   provides the current permission snapshot per operation; must not be null
     * @return an authorization-enforcing {@link SiteService}; never null
     */
    public static SiteService wrapSiteService(
            final SiteService delegate,
            final SitePermissionsService permissionsService,
            final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
        return new SecuredSiteService(delegate, permissionsService, snapshotProvider);
    }

    /**
     * Wraps the given {@link ContentTypeService} with an authorization-enforcing decorator.
     *
     * @param delegate           the raw service to delegate authorized operations to; must not be null
     * @param permissionsService the permissions service for authorization checks; must not be null
     * @param snapshotProvider   provides the current permission snapshot per operation; must not be null
     * @return an authorization-enforcing {@link ContentTypeService}; never null
     */
    public static ContentTypeService wrapContentTypeService(
            final ContentTypeService delegate,
            final ContentTypePermissionsService permissionsService,
            final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
        return new SecuredContentTypeService(delegate, permissionsService, snapshotProvider);
    }

    /**
     * Wraps the given {@link ContentItemService} with an authorization-enforcing decorator.
     *
     * @param delegate           the raw service to delegate authorized operations to; must not be null
     * @param permissionsService the permissions service for authorization checks; must not be null
     * @param snapshotProvider   provides the current permission snapshot per operation; must not be null
     * @return an authorization-enforcing {@link ContentItemService}; never null
     */
    public static ContentItemService wrapContentItemService(
            final ContentItemService delegate,
            final ContentItemPermissionsService permissionsService,
            final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
        return new SecuredContentItemService(delegate, permissionsService, snapshotProvider);
    }
}
