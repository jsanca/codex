package codex.codex.internal.service;

/**
 * Metric name constants for {@link TimedSiteService}.
 *
 * <p>Package-private: used only by {@code TimedSiteService} within this package.</p>
 */
final class SiteServiceMetricNames {

    static final String CREATE_DURATION      = "services.site.create.duration";
    static final String CREATE_FAILED        = "services.site.create.failed";

    static final String START_DURATION       = "services.site.start.duration";
    static final String START_FAILED         = "services.site.start.failed";

    static final String SUSPEND_DURATION     = "services.site.suspend.duration";
    static final String SUSPEND_FAILED       = "services.site.suspend.failed";

    static final String ARCHIVE_DURATION     = "services.site.archive.duration";
    static final String ARCHIVE_FAILED       = "services.site.archive.failed";

    static final String UNARCHIVE_DURATION   = "services.site.unarchive.duration";
    static final String UNARCHIVE_FAILED     = "services.site.unarchive.failed";

    static final String FIND_BY_KEY_DURATION = "services.site.findByKey.duration";
    static final String FIND_BY_KEY_FAILED   = "services.site.findByKey.failed";

    static final String FIND_BY_ALIAS_DURATION = "services.site.findByAlias.duration";
    static final String FIND_BY_ALIAS_FAILED   = "services.site.findByAlias.failed";

    static final String FIND_ALL_DURATION    = "services.site.findAll.duration";
    static final String FIND_ALL_FAILED      = "services.site.findAll.failed";

    private SiteServiceMetricNames() {}
}
