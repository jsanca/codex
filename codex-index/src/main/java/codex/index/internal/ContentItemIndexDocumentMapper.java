package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexResourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Maps a published {@link ContentItem} and its {@link ContentRevision} into a backend-neutral
 * {@link IndexDocument}.
 * <p>
 * The index document id is deterministic and stable:
 * {@code content-item:{siteKey}:{contentTypeKey}:{contentItemKey}}.
 * Re-publishing a new revision upserts the same document — the index always reflects the
 * current published state.
 */
public final class ContentItemIndexDocumentMapper {

    /**
     * Converts a published content item and its revision into an {@link IndexDocument}.
     *
     * @param item     the published content item; must not be null
     * @param revision the published revision; must not be null
     * @return a backend-neutral index document
     * @throws NullPointerException     if {@code item} or {@code revision} is null
     * @throws IllegalArgumentException if the item/revision identity fields are inconsistent,
     *                                  or if the revision is not {@link ContentRevisionStatus#PUBLISHED}
     */
    public IndexDocument toDocument(final ContentItem item, final ContentRevision revision) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(revision, "revision must not be null");
        validateConsistency(item, revision);

        final IndexDocumentId docId = IndexDocumentId.of(
                "content-item:" + item.siteKey().value()
                        + ":" + item.contentTypeKey().value()
                        + ":" + item.key().value());

        return IndexDocument.builder()
                .id(docId)
                .resourceType(IndexResourceType.CONTENT_ITEM)
                .siteKey(item.siteKey())
                .title(extractTitle(item, revision))
                .body(buildBody(revision))
                .fields(buildFields(revision))
                .metadata(buildMetadata(item, revision))
                .updatedAt(item.updatedAt())
                .build();
    }

    private void validateConsistency(final ContentItem item, final ContentRevision revision) {
        if (!item.id().equals(revision.contentItemId())) {
            throw new IllegalArgumentException(
                    "item id " + item.id() + " does not match revision contentItemId " + revision.contentItemId());
        }
        if (!item.siteKey().equals(revision.siteKey())) {
            throw new IllegalArgumentException(
                    "item siteKey " + item.siteKey() + " does not match revision siteKey " + revision.siteKey());
        }
        if (!item.contentTypeKey().equals(revision.contentTypeKey())) {
            throw new IllegalArgumentException(
                    "item contentTypeKey " + item.contentTypeKey()
                            + " does not match revision contentTypeKey " + revision.contentTypeKey());
        }
        if (!item.contentTypeVersionId().equals(revision.contentTypeVersionId())) {
            throw new IllegalArgumentException(
                    "item contentTypeVersionId " + item.contentTypeVersionId()
                            + " does not match revision contentTypeVersionId " + revision.contentTypeVersionId());
        }
        if (revision.status() != ContentRevisionStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "revision " + revision.id() + " must be PUBLISHED but was " + revision.status());
        }
    }

    private String extractTitle(final ContentItem item, final ContentRevision revision) {
        final Object titleValue = revision.values().get(FieldKey.TITLE);
        if (titleValue instanceof String string) {
            return string;
        }
        return item.key().value();
    }

    private String buildBody(final ContentRevision revision) {
        final StringJoiner joiner = new StringJoiner(" ");
        for (final Map.Entry<FieldKey, Object> entry : revision.values().entrySet()) {
            final Object val = entry.getValue();
            if (isScalar(val)) {
                joiner.add(val.toString());
            }
        }
        return joiner.toString();
    }

    private Map<String, Object> buildFields(final ContentRevision revision) {
        final Map<String, Object> fields = new HashMap<>();
        for (final Map.Entry<FieldKey, Object> entry : revision.values().entrySet()) {
            final Object val = entry.getValue();
            if (isScalar(val)) {
                fields.put(entry.getKey().value(), val);
            }
        }
        return fields;
    }

    private Map<String, String> buildMetadata(final ContentItem item, final ContentRevision revision) {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("contentItemId", item.id().value());
        metadata.put("contentItemKey", item.key().value());
        metadata.put("contentTypeKey", item.contentTypeKey().value());
        metadata.put("contentTypeVersionId", item.contentTypeVersionId().value());
        metadata.put("contentRevisionId", revision.id().value());
        metadata.put("revisionNumber", String.valueOf(revision.revisionNumber()));
        metadata.put("revisionStatus", revision.status().name());
        return metadata;
    }

    private boolean isScalar(final Object val) {
        return val instanceof String || val instanceof Number || val instanceof Boolean;
    }
}
