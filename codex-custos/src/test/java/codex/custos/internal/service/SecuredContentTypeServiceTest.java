package codex.custos.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.FieldType;
import codex.custos.api.exception.AccessDeniedException;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.GlobalResourceRef;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.RoleAssignment;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecuredContentTypeServiceTest {

    private static final SiteKey    SITE_A = SiteKey.of("site-a");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final Actor BOT   = Actor.agent("indexer");

    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision GRANTED =
            AccessDecision.granted(ALICE, Permissions.CONTENT_TYPE_READ, GlobalResourceRef.INSTANCE, "stub grant");
    private static final AccessDecision DENIED =
            AccessDecision.denied(ALICE, Permissions.CONTENT_TYPE_READ, GlobalResourceRef.INSTANCE, "stub deny");

    private StubContentTypePermissionsService permissionsStub;
    private SpyContentTypeService delegateSpy;
    private SecuredContentTypeService service;

    @BeforeEach
    void setUp() {
        permissionsStub = new StubContentTypePermissionsService(GRANTED);
        delegateSpy = new SpyContentTypeService();
        service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);
    }

    // -----------------------------------------------------------------------
    // Constructor null guards
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("null guard — constructor")
    class ConstructorNullGuards {

        @Test
        @DisplayName("null delegate is rejected")
        void nullDelegate() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredContentTypeService(null, permissionsStub, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null permissionsService is rejected")
        void nullPermissionsService() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredContentTypeService(delegateSpy, null, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshotProvider is rejected")
        void nullSnapshotProvider() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredContentTypeService(delegateSpy, permissionsStub, null));
        }
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        private final CreateContentTypeCommand CREATE_CMD =
                CreateContentTypeCommand.of(SITE_A, BLOG, "Blog Post");

        @Test
        @DisplayName("checks canCreateContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.create(CREATE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canCreateContentType");
        }

        @Test
        @DisplayName("denied create does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.create(CREATE_CMD, ALICE));
            assertThat(delegateSpy.createCallCount()).isZero();
        }

        @Test
        @DisplayName("granted create delegates exactly once")
        void grantedDelegatesOnce() {
            service.create(CREATE_CMD, ALICE);
            assertThat(delegateSpy.createCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.create(null, ALICE));
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() -> service.create(CREATE_CMD, null));
        }
    }

    // -----------------------------------------------------------------------
    // activate
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("activate")
    class Activate {

        private final ActivateContentTypeCommand ACTIVATE_CMD =
                ActivateContentTypeCommand.of(SITE_A, BLOG);

        @Test
        @DisplayName("checks canUpdateContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.activate(ACTIVATE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canUpdateContentType");
        }

        @Test
        @DisplayName("denied activate does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.activate(ACTIVATE_CMD, ALICE));
            assertThat(delegateSpy.activateCallCount()).isZero();
        }

        @Test
        @DisplayName("granted activate delegates exactly once")
        void grantedDelegatesOnce() {
            service.activate(ACTIVATE_CMD, ALICE);
            assertThat(delegateSpy.activateCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.activate(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // archive
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("archive")
    class Archive {

        private final ArchiveContentTypeCommand ARCHIVE_CMD =
                ArchiveContentTypeCommand.of(SITE_A, BLOG);

        @Test
        @DisplayName("checks canArchiveContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.archive(ARCHIVE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canArchiveContentType");
        }

        @Test
        @DisplayName("denied archive does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.archive(ARCHIVE_CMD, ALICE));
            assertThat(delegateSpy.archiveCallCount()).isZero();
        }

        @Test
        @DisplayName("granted archive delegates exactly once")
        void grantedDelegatesOnce() {
            service.archive(ARCHIVE_CMD, ALICE);
            assertThat(delegateSpy.archiveCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.archive(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // findByKey
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findByKey")
    class FindByKey {

        @Test
        @DisplayName("checks canReadContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.findByKey(SITE_A, BLOG, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canReadContentType");
        }

        @Test
        @DisplayName("denied findByKey does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.findByKey(SITE_A, BLOG, ALICE));
            assertThat(delegateSpy.findByKeyCallCount()).isZero();
        }

        @Test
        @DisplayName("granted findByKey delegates exactly once")
        void grantedDelegatesOnce() {
            service.findByKey(SITE_A, BLOG, ALICE);
            assertThat(delegateSpy.findByKeyCallCount()).isOne();
        }

        @Test
        @DisplayName("delegate return value is preserved")
        void delegateReturnValuePreserved() {
            ContentType stub = minimalContentType();
            delegateSpy.stubFindByKeyResult(Optional.of(stub));

            Optional<ContentType> result = service.findByKey(SITE_A, BLOG, ALICE);

            assertThat(result).containsSame(stub);
        }

        @Test
        @DisplayName("null siteKey is rejected")
        void nullSiteKey() {
            assertThatNullPointerException().isThrownBy(() -> service.findByKey(null, BLOG, ALICE));
        }

        @Test
        @DisplayName("null key is rejected")
        void nullKey() {
            assertThatNullPointerException().isThrownBy(() -> service.findByKey(SITE_A, null, ALICE));
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() -> service.findByKey(SITE_A, BLOG, null));
        }
    }

    // -----------------------------------------------------------------------
    // addField
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("addField")
    class AddField {

        private final AddContentTypeFieldCommand ADD_FIELD_CMD =
                AddContentTypeFieldCommand.of(SITE_A, BLOG,
                        Field.builder().key(FieldKey.of("title")).type(FieldType.TEXT).displayName("Title").build());

        @Test
        @DisplayName("checks canUpdateContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.addField(ADD_FIELD_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canUpdateContentType");
        }

        @Test
        @DisplayName("denied addField does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.addField(ADD_FIELD_CMD, ALICE));
            assertThat(delegateSpy.addFieldCallCount()).isZero();
        }

        @Test
        @DisplayName("granted addField delegates exactly once")
        void grantedDelegatesOnce() {
            service.addField(ADD_FIELD_CMD, ALICE);
            assertThat(delegateSpy.addFieldCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.addField(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // removeField
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("removeField")
    class RemoveField {

        private final RemoveContentTypeFieldCommand REMOVE_FIELD_CMD =
                RemoveContentTypeFieldCommand.of(SITE_A, BLOG, FieldKey.of("title"));

        @Test
        @DisplayName("checks canUpdateContentType before delegating")
        void checksPermissionBeforeDelegate() {
            service.removeField(REMOVE_FIELD_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canUpdateContentType");
        }

        @Test
        @DisplayName("denied removeField does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.removeField(REMOVE_FIELD_CMD, ALICE));
            assertThat(delegateSpy.removeFieldCallCount()).isZero();
        }

        @Test
        @DisplayName("granted removeField delegates exactly once")
        void grantedDelegatesOnce() {
            service.removeField(REMOVE_FIELD_CMD, ALICE);
            assertThat(delegateSpy.removeFieldCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.removeField(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // Cross-cutting: AccessDeniedException and invariant propagation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("authorization outcome")
    class AuthorizationOutcome {

        @Test
        @DisplayName("denied decision throws AccessDeniedException")
        void deniedThrowsAccessDeniedException() {
            permissionsStub = new StubContentTypePermissionsService(DENIED);
            service = new SecuredContentTypeService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.create(
                            CreateContentTypeCommand.of(SITE_A, BLOG, "Blog Post"), ALICE));
        }

        @Test
        @DisplayName("CustosAgentSuperAdminInvariantViolationException propagates uncaught")
        void invariantExceptionPropagates() {
            CustosAgentSuperAdminInvariantViolationException invariantViolation =
                    new CustosAgentSuperAdminInvariantViolationException(BOT);
            permissionsStub.throwInstead(invariantViolation);

            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.create(
                            CreateContentTypeCommand.of(SITE_A, BLOG, "Blog Post"), BOT))
                    .satisfies(ex -> assertThat(ex.actor()).isEqualTo(BOT));
        }
    }

    // -----------------------------------------------------------------------
    // Vocabulary check
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("vocabulary invariants")
    class VocabularyInvariants {

        @Test
        @DisplayName("no delete vocabulary in SecuredContentTypeService method names")
        void noDeleteVocabulary() {
            List<String> methodNames = Arrays.stream(SecuredContentTypeService.class.getDeclaredMethods())
                    .map(Method::getName)
                    .toList();
            assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("delete"));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ContentType minimalContentType() {
        ActorId actorId = ActorId.of("user-alice");
        return ContentType.builder()
                .id(ContentTypeId.of("ct-1"))
                .siteKey(SITE_A)
                .key(BLOG)
                .displayName("Blog Post")
                .status(ContentTypeStatus.DRAFT)
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(Instant.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // Spy delegate
    // -----------------------------------------------------------------------

    private static final class SpyContentTypeService implements ContentTypeService {

        private int createCount;
        private int activateCount;
        private int archiveCount;
        private int findByKeyCount;
        private int addFieldCount;
        private int removeFieldCount;
        private Optional<ContentType> findByKeyResult = Optional.empty();

        void stubFindByKeyResult(final Optional<ContentType> result) {
            this.findByKeyResult = result;
        }

        int createCallCount()      { return createCount; }
        int activateCallCount()    { return activateCount; }
        int archiveCallCount()     { return archiveCount; }
        int findByKeyCallCount()   { return findByKeyCount; }
        int addFieldCallCount()    { return addFieldCount; }
        int removeFieldCallCount() { return removeFieldCount; }

        @Override
        public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
            createCount++;
            return null;
        }

        @Override
        public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
            activateCount++;
            return null;
        }

        @Override
        public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
            archiveCount++;
            return null;
        }

        @Override
        public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key, final Actor actor) {
            findByKeyCount++;
            return findByKeyResult;
        }

        @Override
        public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
            return List.of();
        }

        @Override
        public List<ContentType> findAll(final Actor actor) {
            return List.of();
        }

        @Override
        public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
            addFieldCount++;
            return null;
        }

        @Override
        public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
            removeFieldCount++;
            return null;
        }
    }
}
