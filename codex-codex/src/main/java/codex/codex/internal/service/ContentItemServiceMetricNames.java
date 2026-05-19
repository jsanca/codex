package codex.codex.internal.service;

/**
 * Metric name constants for {@link TimedContentItemService}.
 *
 * <p>Package-private: used only by {@code TimedContentItemService} within this package.</p>
 */
final class ContentItemServiceMetricNames {

    static final String CREATE_DURATION              = "services.contentItem.create.duration";
    static final String CREATE_FAILED                = "services.contentItem.create.failed";

    static final String FIND_BY_KEY_DURATION         = "services.contentItem.findByKey.duration";
    static final String FIND_BY_KEY_FAILED           = "services.contentItem.findByKey.failed";

    static final String FIND_BY_CONTENT_TYPE_DURATION = "services.contentItem.findByContentType.duration";
    static final String FIND_BY_CONTENT_TYPE_FAILED   = "services.contentItem.findByContentType.failed";

    static final String FIND_ALL_DURATION            = "services.contentItem.findAll.duration";
    static final String FIND_ALL_FAILED              = "services.contentItem.findAll.failed";

    static final String UPDATE_DURATION              = "services.contentItem.update.duration";
    static final String UPDATE_FAILED                = "services.contentItem.update.failed";

    static final String ARCHIVE_DURATION             = "services.contentItem.archive.duration";
    static final String ARCHIVE_FAILED               = "services.contentItem.archive.failed";

    static final String UNPUBLISH_DURATION           = "services.contentItem.unpublish.duration";
    static final String UNPUBLISH_FAILED             = "services.contentItem.unpublish.failed";

    static final String DELETE_DURATION              = "services.contentItem.delete.duration";
    static final String DELETE_FAILED                = "services.contentItem.delete.failed";

    static final String RESTORE_DURATION             = "services.contentItem.restore.duration";
    static final String RESTORE_FAILED               = "services.contentItem.restore.failed";

    static final String PUBLISH_DURATION             = "services.contentItem.publish.duration";
    static final String PUBLISH_FAILED               = "services.contentItem.publish.failed";

    private ContentItemServiceMetricNames() {}
}
