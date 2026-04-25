package codex.codex.internal.service.internal;

import codex.codex.api.model.entity.Site;

public class SiteAlreadyExistException extends RuntimeException {
    public SiteAlreadyExistException(Site site) {
    }
}
