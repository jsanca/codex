package codex.custos.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.ContentItemStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecuredContentItemServiceTest {

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final ContentItemKey WELCOME = ContentItemKey.of("welcome-post");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final Actor BOT = Actor.agent("indexer");

    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision GRANTED =
            AccessDecision.granted(ALICE, Permissions.CONTENT_ITEM_READ, GlobalResourceRef.INSTANCE, "stub grant");
    private static final AccessDecision DENIED =
            AccessDecision.denied(ALICE, Permissions.CONTENT_ITEM_READ, GlobalResourceRef.INSTANCE, "stub deny");

    private StubContentItemPermissionsService permissionsStub;
    private SpyContentItemService delegateSpy;
    private SecuredContentItemService service;

    @BeforeEach
    void setUp() {
        permissionsStub = new StubContentItemPermissionsService(GRANTED);
        delegateSpy = new SpyContentItemService();
        service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);
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
                    new SecuredContentItemService(null, permissionsStub, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null permissionsService is rejected")
        void nullPermissionsService() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredContentItemService(delegateSpy, null, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshotProvider is rejected")
        void nullSnapshotProvider() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredContentItemService(delegateSpy, permissionsStub, null));
        }
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        private final CreateContentItemCommand CREATE_CMD =
                CreateContentItemCommand.of(SITE_A, BLOG, WELCOME, null);

        @Test
        @DisplayName("checks canCreateContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.create(CREATE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canCreateContentItem");
        }

        @Test
        @DisplayName("denied create does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentItemPermissionsService(DENIED);
            service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

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
    // findByKey
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findByKey")
    class FindByKey {

        @Test
        @DisplayName("checks canReadContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.findByKey(SITE_A, BLOG, WELCOME, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canReadContentItem");
        }

        @Test
        @DisplayName("denied findByKey does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentItemPermissionsService(DENIED);
            service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.findByKey(SITE_A, BLOG, WELCOME, ALICE));
            assertThat(delegateSpy.findByKeyCallCount()).isZero();
        }

        @Test
        @DisplayName("granted findByKey delegates exactly once")
        void grantedDelegatesOnce() {
            service.findByKey(SITE_A, BLOG, WELCOME, ALICE);
            assertThat(delegateSpy.findByKeyCallCount()).isOne();
        }

        @Test
        @DisplayName("delegate return value is preserved")
        void delegateReturnValuePreserved() {
            ContentItem stubItem = minimalContentItem();
            delegateSpy.stubFindByKeyResult(Optional.of(stubItem));

            Optional<ContentItem> result = service.findByKey(SITE_A, BLOG, WELCOME, ALICE);

            assertThat(result).containsSame(stubItem);
        }
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("update")
    class Update {

        private final UpdateContentItemCommand UPDATE_CMD =
                UpdateContentItemCommand.of(SITE_A, BLOG, WELCOME, null);

        @Test
        @DisplayName("checks canUpdateContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.update(UPDATE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canUpdateContentItem");
        }

        @Test
        @DisplayName("denied update does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentItemPermissionsService(DENIED);
            service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.update(UPDATE_CMD, ALICE));
            assertThat(delegateSpy.updateCallCount()).isZero();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.update(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // publish
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("publish")
    class Publish {

        private final PublishContentItemCommand PUBLISH_CMD =
                PublishContentItemCommand.of(SITE_A, BLOG, WELCOME);

        @Test
        @DisplayName("checks canPublishContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.publish(PUBLISH_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canPublishContentItem");
        }

        @Test
        @DisplayName("denied publish does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubContentItemPermissionsService(DENIED);
            service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.publish(PUBLISH_CMD, ALICE));
            assertThat(delegateSpy.publishCallCount()).isZero();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.publish(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // unpublish
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("unpublish")
    class Unpublish {

        private final UnpublishContentItemCommand UNPUBLISH_CMD =
                UnpublishContentItemCommand.of(SITE_A, BLOG, WELCOME);

        @Test
        @DisplayName("checks canUnpublishContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.unpublish(UNPUBLISH_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canUnpublishContentItem");
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.unpublish(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // archive
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("archive")
    class Archive {

        private final ArchiveContentItemCommand ARCHIVE_CMD =
                ArchiveContentItemCommand.of(SITE_A, BLOG, WELCOME);

        @Test
        @DisplayName("checks canArchiveContentItem before delegating")
        void checksPermissionBeforeDelegate() {
            service.archive(ARCHIVE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canArchiveContentItem");
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.archive(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // delete — fail closed
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("delete — fail closed")
    class Delete {

        private final DeleteContentItemCommand DELETE_CMD =
                DeleteContentItemCommand.of(SITE_A, BLOG, WELCOME);

        @Test
        @DisplayName("delete throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> service.delete(DELETE_CMD, ALICE));
        }

        @Test
        @DisplayName("delete does not call the delegate")
        void doesNotCallDelegate() {
            try { service.delete(DELETE_CMD, ALICE); } catch (UnsupportedOperationException ignored) {}
            assertThat(delegateSpy.deleteCallCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // restore — fail closed
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("restore — fail closed")
    class Restore {

        private final RestoreContentItemCommand RESTORE_CMD =
                RestoreContentItemCommand.of(SITE_A, BLOG, WELCOME);

        @Test
        @DisplayName("restore throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> service.restore(RESTORE_CMD, ALICE));
        }

        @Test
        @DisplayName("restore does not call the delegate")
        void doesNotCallDelegate() {
            try { service.restore(RESTORE_CMD, ALICE); } catch (UnsupportedOperationException ignored) {}
            assertThat(delegateSpy.restoreCallCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // List operations — pass-through
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("list operations — pass-through")
    class ListOperations {

        @Test
        @DisplayName("findByContentType delegates without permission check")
        void findByContentTypeDelegates() {
            service.findByContentType(SITE_A, BLOG, ALICE);
            assertThat(delegateSpy.findByContentTypeCallCount()).isOne();
        }

        @Test
        @DisplayName("findAll delegates without permission check")
        void findAllDelegates() {
            service.findAll(ALICE);
            assertThat(delegateSpy.findAllCallCount()).isOne();
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
            permissionsStub = new StubContentItemPermissionsService(DENIED);
            service = new SecuredContentItemService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.create(
                            CreateContentItemCommand.of(SITE_A, BLOG, WELCOME, null), ALICE));
        }

        @Test
        @DisplayName("CustosAgentSuperAdminInvariantViolationException propagates uncaught")
        void invariantExceptionPropagates() {
            CustosAgentSuperAdminInvariantViolationException invariantViolation =
                    new CustosAgentSuperAdminInvariantViolationException(BOT);
            permissionsStub.throwInstead(invariantViolation);

            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.create(
                            CreateContentItemCommand.of(SITE_A, BLOG, WELCOME, null), BOT))
                    .satisfies(ex -> assertThat(ex.actor()).isEqualTo(BOT));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ContentItem minimalContentItem() {
        ActorId actorId = ActorId.of("user-alice");
        return ContentItem.builder()
                .id(ContentItemId.forItem(SITE_A, BLOG, WELCOME))
                .siteKey(SITE_A)
                .contentTypeKey(BLOG)
                .contentTypeVersionId(ContentTypeVersionId.of("ctv-1"))
                .key(WELCOME)
                .status(ContentItemStatus.DRAFT)
                .currentWorkingRevisionId(ContentRevisionId.of("rev-1"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(Instant.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // Spy delegate
    // -----------------------------------------------------------------------

    private static final class SpyContentItemService implements ContentItemService {

        private int createCount;
        private int findByKeyCount;
        private int updateCount;
        private int publishCount;
        private int deleteCount;
        private int restoreCount;
        private int findByContentTypeCount;
        private int findAllCount;
        private Optional<ContentItem> findByKeyResult = Optional.empty();

        void stubFindByKeyResult(final Optional<ContentItem> result) {
            this.findByKeyResult = result;
        }

        int createCallCount() { return createCount; }
        int findByKeyCallCount() { return findByKeyCount; }
        int updateCallCount() { return updateCount; }
        int publishCallCount() { return publishCount; }
        int deleteCallCount() { return deleteCount; }
        int restoreCallCount() { return restoreCount; }
        int findByContentTypeCallCount() { return findByContentTypeCount; }
        int findAllCallCount() { return findAllCount; }

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            createCount++;
            return null;
        }

        @Override
        public Optional<ContentItem> findByKey(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key, final Actor actor) {
            findByKeyCount++;
            return findByKeyResult;
        }

        @Override
        public List<ContentItem> findByContentType(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                                   final Actor actor) {
            findByContentTypeCount++;
            return List.of();
        }

        @Override
        public List<ContentItem> findAll(final Actor actor) {
            findAllCount++;
            return List.of();
        }

        @Override
        public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
            updateCount++;
            return null;
        }

        @Override
        public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
            return null;
        }

        @Override
        public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
            return null;
        }

        @Override
        public void delete(final DeleteContentItemCommand command, final Actor actor) {
            deleteCount++;
        }

        @Override
        public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
            restoreCount++;
            return null;
        }

        @Override
        public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
            publishCount++;
            return null;
        }
    }
}
