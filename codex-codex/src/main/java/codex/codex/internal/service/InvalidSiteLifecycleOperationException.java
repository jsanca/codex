package codex.codex.internal.service;

import codex.codex.api.model.entity.Site;

/**
 * Thrown when a lifecycle operation is requested on a site that does not participate
 * in the normal Codex lifecycle (i.e. whose
 * {@link codex.fundamentum.api.lifecycle.LifecycleParticipation} is not {@code MANAGED}).
 */
public class InvalidSiteLifecycleOperationException extends RuntimeException {

    private final Site site;

    public InvalidSiteLifecycleOperationException(final String message, final Site site) {
        super(message);
        this.site = site;
    }

    /**
     * Returns the site that rejected the lifecycle operation.
     *
     * @return the site; never null
     */
    public Site site() {
        return site;
    }
}
