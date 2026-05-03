package codex.codex.api.index;

import codex.codex.api.model.identity.SiteKey;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A neutral projection document suitable for writing into any search or index backend.
 * <p>
 * {@code IndexDocument} is not a domain entity — it is a projection of domain state into a
 * flat, searchable representation. It can be translated into OpenSearch documents,
 * Elasticsearch documents, Lucene documents, myIR records, embedding inputs, or
 * in-memory test structures.
 * <p>
 * Fields:
 * <ul>
 *   <li>{@code title} — short human-readable label; useful for admin search and result display</li>
 *   <li>{@code body} — full searchable text; built from published content values by a future subscriber</li>
 *   <li>{@code fields} — structured values for filtering, sorting, and backend-specific indexing;
 *       should remain backend-neutral</li>
 *   <li>{@code metadata} — string pairs for tracing and debugging (e.g. contentTypeKey, contentRevisionId)</li>
 * </ul>
 * <p>
 * Content values are excluded from this class. They must be projected from a
 * {@code ContentRevision} by an indexing subscriber, not embedded here.
 *
 * @param id           the stable document identifier; must not be null
 * @param resourceType the kind of Codex resource this document represents; must not be null
 * @param siteKey      the site scope; must not be null
 * @param title        short label; defaults to empty string if null; trimmed
 * @param body         full searchable text; defaults to empty string if null; trimmed
 * @param fields       structured values; defaults to empty map if null; null keys/values rejected
 * @param metadata     string metadata; defaults to empty map if null; null keys/values rejected
 * @param updatedAt    the timestamp of the most recent update; defaults to {@link Instant#now()} if null
 */
public record IndexDocument(
        IndexDocumentId id,
        IndexResourceType resourceType,
        SiteKey siteKey,
        String title,
        String body,
        Map<String, Object> fields,
        Map<String, String> metadata,
        Instant updatedAt
) {

    /**
     * Canonical constructor for {@link IndexDocument}.
     */
    public IndexDocument {
        Objects.requireNonNull(id, "IndexDocument id cannot be null");
        Objects.requireNonNull(resourceType, "IndexDocument resourceType cannot be null");
        Objects.requireNonNull(siteKey, "IndexDocument siteKey cannot be null");

        title = title == null ? "" : title.trim();
        body = body == null ? "" : body.trim();

        if (fields == null) {
            fields = Map.of();
        } else {
            for (final var entry : fields.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "fields map key cannot be null");
                Objects.requireNonNull(entry.getValue(), "fields map value cannot be null");
            }
            fields = Map.copyOf(fields);
        }

        if (metadata == null) {
            metadata = Map.of();
        } else {
            for (final var entry : metadata.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "metadata map key cannot be null");
                Objects.requireNonNull(entry.getValue(), "metadata map value cannot be null");
            }
            metadata = Map.copyOf(metadata);
        }

        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    /**
     * Creates a new {@link Builder} for {@link IndexDocument}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link IndexDocument}.
     *
     * @param document the source; must not be null
     * @return a builder pre-populated with all fields from {@code document}
     */
    public static Builder copyOf(final IndexDocument document) {
        Objects.requireNonNull(document, "document cannot be null");
        return builder()
                .id(document.id())
                .resourceType(document.resourceType())
                .siteKey(document.siteKey())
                .title(document.title())
                .body(document.body())
                .fields(document.fields())
                .metadata(document.metadata())
                .updatedAt(document.updatedAt());
    }

    @Override
    public String toString() {
        return "IndexDocument{" +
                "id=" + id +
                ", resourceType=" + resourceType +
                ", siteKey=" + siteKey +
                ", title='" + title + '\'' +
                ", fields=" + fields.size() +
                ", metadata=" + metadata.size() +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Builder for {@link IndexDocument}.
     */
    public static class Builder {
        private IndexDocumentId id;
        private IndexResourceType resourceType;
        private SiteKey siteKey;
        private String title;
        private String body;
        private Map<String, Object> fields;
        private Map<String, String> metadata;
        private Instant updatedAt;

        public Builder id(IndexDocumentId id) { this.id = id; return this; }
        public Builder resourceType(IndexResourceType resourceType) { this.resourceType = resourceType; return this; }
        public Builder siteKey(SiteKey siteKey) { this.siteKey = siteKey; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder fields(Map<String, Object> fields) { this.fields = fields == null ? null : new HashMap<>(fields); return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata == null ? null : new HashMap<>(metadata); return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        /**
         * Builds a new {@link IndexDocument} instance.
         *
         * @return a validated {@code IndexDocument}
         */
        public IndexDocument build() {
            return new IndexDocument(id, resourceType, siteKey, title, body, fields, metadata, updatedAt);
        }
    }
}
