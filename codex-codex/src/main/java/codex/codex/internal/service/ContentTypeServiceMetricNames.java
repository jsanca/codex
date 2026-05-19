package codex.codex.internal.service;

/**
 * Metric name constants for {@link TimedContentTypeService}.
 *
 * <p>Package-private: used only by {@code TimedContentTypeService} within this package.</p>
 */
final class ContentTypeServiceMetricNames {

    static final String CREATE_DURATION          = "services.contentType.create.duration";
    static final String CREATE_FAILED            = "services.contentType.create.failed";

    static final String ACTIVATE_DURATION        = "services.contentType.activate.duration";
    static final String ACTIVATE_FAILED          = "services.contentType.activate.failed";

    static final String ARCHIVE_DURATION         = "services.contentType.archive.duration";
    static final String ARCHIVE_FAILED           = "services.contentType.archive.failed";

    static final String FIND_BY_KEY_DURATION     = "services.contentType.findByKey.duration";
    static final String FIND_BY_KEY_FAILED       = "services.contentType.findByKey.failed";

    static final String FIND_BY_SITE_KEY_DURATION = "services.contentType.findBySiteKey.duration";
    static final String FIND_BY_SITE_KEY_FAILED   = "services.contentType.findBySiteKey.failed";

    static final String FIND_ALL_DURATION        = "services.contentType.findAll.duration";
    static final String FIND_ALL_FAILED          = "services.contentType.findAll.failed";

    static final String ADD_FIELD_DURATION       = "services.contentType.addField.duration";
    static final String ADD_FIELD_FAILED         = "services.contentType.addField.failed";

    static final String REMOVE_FIELD_DURATION    = "services.contentType.removeField.duration";
    static final String REMOVE_FIELD_FAILED      = "services.contentType.removeField.failed";

    private ContentTypeServiceMetricNames() {}
}
