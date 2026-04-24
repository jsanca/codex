package codex.codex.internal.service.internal;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

public class CodexSiteService implements SiteService {



    @Override
    public Site create(Site site, Actor actor) {
        return null;
    }

    @Override
    public Optional<Site> findByKey(SiteKey siteKey, Actor actor) {
        return Optional.empty();
    }

    @Override
    public Site start(SiteKey siteKey, Actor actor) {
        return null;
    }

    @Override
    public Site suspend(SiteKey siteKey, Actor actor) {
        return null;
    }

    @Override
    public Site archive(SiteKey siteKey, Actor actor) {
        return null;
    }

    @Override
    public Optional<Site> findByAlias(SiteAlias alias, Actor actor) {
        return Optional.empty();
    }

    @Override
    public List<Site> findAll(Actor actor) {
        return List.of();
    }
}
