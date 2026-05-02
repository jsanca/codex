package codex.codex.api.model.entity;

import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.FieldSettingKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldConstraintType;
import codex.codex.api.model.value.FieldType;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeSchemaTest {

    private final ActorId actorId = ActorId.of("system:test");
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey ctKey = ContentTypeKey.of("blog-post");

    // --- Field tests ---

    @Test
    @DisplayName("Field rejects null key")
    void fieldRejectsNullKey() {
        assertThrows(NullPointerException.class, () ->
                Field.builder().type(FieldType.TEXT).build());
    }

    @Test
    @DisplayName("Field rejects null type")
    void fieldRejectsNullType() {
        assertThrows(NullPointerException.class, () ->
                Field.builder().key("title").build());
    }

    @Test
    @DisplayName("Field trims displayName")
    void fieldTrimsDisplayName() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).displayName("  Title  ").build();
        assertEquals("Title", field.displayName());
    }

    @Test
    @DisplayName("Field defaults blank displayName to key value")
    void fieldDefaultsBlankDisplayNameToKeyValue() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).displayName("   ").build();
        assertEquals("title", field.displayName());
    }

    @Test
    @DisplayName("Field constraints are defensively copied")
    void fieldConstraintsAreDefensivelyCopied() {
        final List<FieldConstraint> original = new ArrayList<>();
        final Field field = Field.builder().key("title").type(FieldType.TEXT)
                .constraints(original).build();
        original.add(FieldConstraint.of(FieldConstraintType.MIN_LENGTH, 5));
        assertEquals(0, field.constraints().size());
    }

    @Test
    @DisplayName("Field settings are defensively copied")
    void fieldSettingsAreDefensivelyCopied() {
        final List<FieldSetting> original = new ArrayList<>();
        final Field field = Field.builder().key("title").type(FieldType.TEXT)
                .settings(original).build();
        original.add(FieldSetting.of(FieldSettingKey.PLACEHOLDER, "Enter title"));
        assertEquals(0, field.settings().size());
    }

    // --- ContentType fields tests ---

    @Test
    @DisplayName("ContentType fields default to empty map when null")
    void contentTypeFieldsDefaultToEmptyMapWhenNull() {
        final ContentType ct = buildContentType(null);
        assertNotNull(ct.fields());
        assertTrue(ct.fields().isEmpty());
    }

    @Test
    @DisplayName("ContentType fields are defensively copied")
    void contentTypeFieldsAreDefensivelyCopied() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final Map<FieldKey, Field> original = new HashMap<>();
        original.put(titleKey, titleField);

        final ContentType ct = buildContentType(original);
        original.put(FieldKey.of("body"), Field.builder().key("body").type(FieldType.TEXT).build());

        assertEquals(1, ct.fields().size());
    }

    @Test
    @DisplayName("ContentType fields map is immutable from public accessor")
    void contentTypeFieldsMapIsImmutable() {
        final ContentType ct = buildContentType(null);
        assertThrows(UnsupportedOperationException.class, () ->
                ct.fields().put(FieldKey.of("title"), Field.builder().key("title").type(FieldType.TEXT).build()));
    }

    @Test
    @DisplayName("ContentType builder supports fields")
    void contentTypeBuilderSupportsFields() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentType ct = buildContentType(Map.of(titleKey, titleField));
        assertEquals(1, ct.fields().size());
        assertEquals(titleField, ct.fields().get(titleKey));
    }

    @Test
    @DisplayName("ContentType copyOf preserves fields")
    void contentTypeCopyOfPreservesFields() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentType original = buildContentType(Map.of(titleKey, titleField));

        final ContentType copy = ContentType.copyOf(original).build();
        assertEquals(1, copy.fields().size());
        assertEquals(titleField, copy.fields().get(titleKey));
    }

    @Test
    @DisplayName("ContentType constructor rejects fields where map key differs from field key")
    void contentTypeRejectsFieldsWhereMapKeyDiffersFromFieldKey() {
        final FieldKey mapKey = FieldKey.of("title");
        final Field bodyField = Field.builder().key("body").type(FieldType.TEXT).build();
        final Map<FieldKey, Field> badFields = Map.of(mapKey, bodyField);
        assertThrows(IllegalArgumentException.class, () -> buildContentType(badFields));
    }

    @Test
    @DisplayName("ContentType toString includes field count not full details")
    void contentTypeToStringIncludesFieldCount() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentType ct = buildContentType(Map.of(titleKey, titleField));
        final String str = ct.toString();
        assertTrue(str.contains("fields=1"));
        assertFalse(str.contains("FieldKey"));
    }

    // --- ContentType latest published version metadata tests ---

    @Test
    @DisplayName("new content type may have null latestPublishedVersionId")
    void newContentTypeMayHaveNullLatestPublishedVersionId() {
        assertNull(buildContentType(null).latestPublishedVersionId());
    }

    @Test
    @DisplayName("new content type may have null latestPublishedVersion")
    void newContentTypeMayHaveNullLatestPublishedVersion() {
        assertNull(buildContentType(null).latestPublishedVersion());
    }

    @Test
    @DisplayName("rejects latestPublishedVersion less than 1")
    void rejectsLatestPublishedVersionLessThan1() {
        assertThrows(IllegalArgumentException.class, () ->
                ContentType.builder()
                        .id(ContentTypeId.generate())
                        .siteKey(siteKey)
                        .key(ctKey)
                        .displayName("Blog Post")
                        .owner(actorId)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .latestPublishedVersion(0)
                        .build());
    }

    @Test
    @DisplayName("builder supports latestPublishedVersionId")
    void builderSupportsLatestPublishedVersionId() {
        final ContentTypeVersionId versionId = ContentTypeVersionId.forVersion("acme", "blog-post", 1);
        final ContentType ct = ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(ctKey)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .latestPublishedVersionId(versionId)
                .latestPublishedVersion(1)
                .build();
        assertEquals(versionId, ct.latestPublishedVersionId());
    }

    @Test
    @DisplayName("builder supports latestPublishedVersion")
    void builderSupportsLatestPublishedVersion() {
        final ContentType ct = ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(ctKey)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .latestPublishedVersion(1)
                .build();
        assertEquals(1, ct.latestPublishedVersion());
    }

    @Test
    @DisplayName("copyOf preserves latest published metadata")
    void copyOfPreservesLatestPublishedMetadata() {
        final ContentTypeVersionId versionId = ContentTypeVersionId.forVersion("acme", "blog-post", 1);
        final ContentType original = ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(ctKey)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .latestPublishedVersionId(versionId)
                .latestPublishedVersion(1)
                .build();
        final ContentType copy = ContentType.copyOf(original).build();
        assertEquals(versionId, copy.latestPublishedVersionId());
        assertEquals(1, copy.latestPublishedVersion());
    }

    // --- AddContentTypeFieldCommand tests ---

    @Test
    @DisplayName("AddContentTypeFieldCommand requires non-null siteKey")
    void addFieldCommandRequiresNonNullSiteKey() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).build();
        assertThrows(NullPointerException.class, () ->
                AddContentTypeFieldCommand.of((SiteKey) null, ctKey, field));
    }

    @Test
    @DisplayName("AddContentTypeFieldCommand requires non-null contentTypeKey")
    void addFieldCommandRequiresNonNullContentTypeKey() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).build();
        assertThrows(NullPointerException.class, () ->
                AddContentTypeFieldCommand.of(siteKey, (ContentTypeKey) null, field));
    }

    @Test
    @DisplayName("AddContentTypeFieldCommand requires non-null field")
    void addFieldCommandRequiresNonNullField() {
        assertThrows(NullPointerException.class, () ->
                AddContentTypeFieldCommand.of(siteKey, ctKey, null));
    }

    @Test
    @DisplayName("AddContentTypeFieldCommand typed factory works")
    void addFieldCommandTypedFactory() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).build();
        final AddContentTypeFieldCommand cmd = AddContentTypeFieldCommand.of(siteKey, ctKey, field);
        assertEquals(siteKey, cmd.siteKey());
        assertEquals(ctKey, cmd.contentTypeKey());
        assertEquals(field, cmd.field());
    }

    @Test
    @DisplayName("AddContentTypeFieldCommand string factory works")
    void addFieldCommandStringFactory() {
        final Field field = Field.builder().key("title").type(FieldType.TEXT).build();
        final AddContentTypeFieldCommand cmd = AddContentTypeFieldCommand.of("acme", "blog-post", field);
        assertEquals(siteKey, cmd.siteKey());
        assertEquals(ctKey, cmd.contentTypeKey());
    }

    // --- RemoveContentTypeFieldCommand tests ---

    @Test
    @DisplayName("RemoveContentTypeFieldCommand requires non-null siteKey")
    void removeFieldCommandRequiresNonNullSiteKey() {
        assertThrows(NullPointerException.class, () ->
                RemoveContentTypeFieldCommand.of((SiteKey) null, ctKey, FieldKey.of("title")));
    }

    @Test
    @DisplayName("RemoveContentTypeFieldCommand requires non-null contentTypeKey")
    void removeFieldCommandRequiresNonNullContentTypeKey() {
        assertThrows(NullPointerException.class, () ->
                RemoveContentTypeFieldCommand.of(siteKey, (ContentTypeKey) null, FieldKey.of("title")));
    }

    @Test
    @DisplayName("RemoveContentTypeFieldCommand requires non-null fieldKey")
    void removeFieldCommandRequiresNonNullFieldKey() {
        assertThrows(NullPointerException.class, () ->
                RemoveContentTypeFieldCommand.of(siteKey, ctKey, (FieldKey) null));
    }

    @Test
    @DisplayName("RemoveContentTypeFieldCommand typed factory works")
    void removeFieldCommandTypedFactory() {
        final FieldKey fieldKey = FieldKey.of("title");
        final RemoveContentTypeFieldCommand cmd = RemoveContentTypeFieldCommand.of(siteKey, ctKey, fieldKey);
        assertEquals(siteKey, cmd.siteKey());
        assertEquals(ctKey, cmd.contentTypeKey());
        assertEquals(fieldKey, cmd.fieldKey());
    }

    @Test
    @DisplayName("RemoveContentTypeFieldCommand string factory works")
    void removeFieldCommandStringFactory() {
        final RemoveContentTypeFieldCommand cmd = RemoveContentTypeFieldCommand.of("acme", "blog-post", "title");
        assertEquals(siteKey, cmd.siteKey());
        assertEquals(ctKey, cmd.contentTypeKey());
        assertEquals(FieldKey.of("title"), cmd.fieldKey());
    }

    // --- helpers ---

    private ContentType buildContentType(final Map<FieldKey, Field> fields) {
        return ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(ctKey)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .fields(fields)
                .build();
    }
}
