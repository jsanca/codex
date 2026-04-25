package codex.codex.internal.service.internal;

import codex.codex.api.model.entity.Site;
import codex.fundamentum.api.exception.InvalidStateTransitionException;

public class InvalidSiteStatusTransitionException extends InvalidStateTransitionException {

    private final Site site;

    public InvalidSiteStatusTransitionException(Site site) {
        this.site = site;
    }

    public InvalidSiteStatusTransitionException(String message, Site site) {
        super(message);
        this.site = site;
    }

    public InvalidSiteStatusTransitionException(String message, Throwable cause, Site site) {
        super(message, cause);
        this.site = site;
    }

    public InvalidSiteStatusTransitionException(Throwable cause, Site site) {
        super(cause);
        this.site = site;
    }

    public InvalidSiteStatusTransitionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Site site) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.site = site;
    }
}
