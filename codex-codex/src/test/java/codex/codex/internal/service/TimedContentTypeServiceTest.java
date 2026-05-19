package codex.codex.internal.service;

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
import codex.codex.api.model.value.FieldType;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.fundamentum.api.observance.InMemoryObservance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static codex.codex.internal.service.ContentTypeServiceMetricNames.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimedContentTypeService}.
 *
 * <p>Validates that every operation records its duration timer and — only on failure —
 * increments its failure counter, without changing return values, exception propagation,
 * or delegate call count.</p>
 */
class TimedContentTypeServiceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");

    private InMemoryObservance observance;
    private StubContentTypeService stub;
    private TimedContentTypeService service;

    // --- runtime integration ---

    private CodexRuntime runtime;

    @BeforeEach
    void setUp() {
        observance = new InMemoryObservance();
        stub = new StubContentTypeService();
        service = new TimedContentTypeService(stub, observance);
    }

    @AfterEach
    void tearDown() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new TimedContentTypeService(null, observance));
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new TimedContentTypeService(stub, null));
    }

    @Test
    void getDelegateReturnsDelegate() {
        assertSame(stub, service.getDelegate());
    }

    // --- create ---

    @Test
    void createRecordsDuration() {
        stub.nextContentType = emptyContentType();
        service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        assertEquals(1, observance.timerCount(CREATE_DURATION));
    }

    @Test
    void createReturnsDelegateResult() {
        final ContentType ct = emptyContentType();
        stub.nextContentType = ct;
        assertSame(ct, service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR));
    }

    @Test
    void createDelegatesExactlyOnce() {
        stub.nextContentType = emptyContentType();
        service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        assertEquals(1, stub.createCallCount);
    }

    @Test
    void createFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "create";
        assertThrows(RuntimeException.class,
                () -> service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR));
        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.counterValue(CREATE_FAILED));
    }

    @Test
    void createFailurePropagatesException() {
        stub.throwOn = "create";
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR));
        assertEquals("stub failure: create", ex.getMessage());
    }

    @Test
    void createSuccessDoesNotIncrementFailedCounter() {
        stub.nextContentType = emptyContentType();
        service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        assertEquals(0, observance.counterValue(CREATE_FAILED));
    }

    // --- activate ---

    @Test
    void activateRecordsDuration() {
        stub.nextContentType = emptyContentType();
        service.activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
        assertEquals(1, observance.timerCount(ACTIVATE_DURATION));
    }

    @Test
    void activateReturnsDelegateResult() {
        final ContentType ct = emptyContentType();
        stub.nextContentType = ct;
        assertSame(ct, service.activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR));
    }

    @Test
    void activateFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "activate";
        assertThrows(RuntimeException.class,
                () -> service.activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR));
        assertEquals(1, observance.timerCount(ACTIVATE_DURATION));
        assertEquals(1, observance.counterValue(ACTIVATE_FAILED));
    }

    // --- archive ---

    @Test
    void archiveRecordsDuration() {
        stub.nextContentType = emptyContentType();
        service.archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
    }

    @Test
    void archiveFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "archive";
        assertThrows(RuntimeException.class,
                () -> service.archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR));
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
        assertEquals(1, observance.counterValue(ARCHIVE_FAILED));
    }

    // --- findByKey ---

    @Test
    void findByKeyRecordsDuration() {
        stub.nextOptional = Optional.of(emptyContentType());
        service.findByKey(SITE_KEY, CT_KEY, ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
    }

    @Test
    void findByKeyReturnsDelegateResult() {
        final Optional<ContentType> result = Optional.of(emptyContentType());
        stub.nextOptional = result;
        assertSame(result, service.findByKey(SITE_KEY, CT_KEY, ACTOR));
    }

    @Test
    void findByKeyFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findByKey";
        assertThrows(RuntimeException.class,
                () -> service.findByKey(SITE_KEY, CT_KEY, ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_KEY_FAILED));
    }

    // --- findBySiteKey ---

    @Test
    void findBySiteKeyRecordsDuration() {
        service.findBySiteKey(SITE_KEY, ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_SITE_KEY_DURATION));
    }

    @Test
    void findBySiteKeyReturnsDelegateResult() {
        final List<ContentType> result = List.of(emptyContentType());
        stub.nextList = result;
        assertSame(result, service.findBySiteKey(SITE_KEY, ACTOR));
    }

    @Test
    void findBySiteKeyFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findBySiteKey";
        assertThrows(RuntimeException.class,
                () -> service.findBySiteKey(SITE_KEY, ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_SITE_KEY_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_SITE_KEY_FAILED));
    }

    // --- findAll ---

    @Test
    void findAllRecordsDuration() {
        service.findAll(ACTOR);
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
    }

    @Test
    void findAllFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findAll";
        assertThrows(RuntimeException.class, () -> service.findAll(ACTOR));
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
        assertEquals(1, observance.counterValue(FIND_ALL_FAILED));
    }

    // --- addField ---

    @Test
    void addFieldRecordsDuration() {
        stub.nextContentType = emptyContentType();
        service.addField(addFieldCommand(), ACTOR);
        assertEquals(1, observance.timerCount(ADD_FIELD_DURATION));
    }

    @Test
    void addFieldReturnsDelegateResult() {
        final ContentType ct = emptyContentType();
        stub.nextContentType = ct;
        assertSame(ct, service.addField(addFieldCommand(), ACTOR));
    }

    @Test
    void addFieldFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "addField";
        assertThrows(RuntimeException.class, () -> service.addField(addFieldCommand(), ACTOR));
        assertEquals(1, observance.timerCount(ADD_FIELD_DURATION));
        assertEquals(1, observance.counterValue(ADD_FIELD_FAILED));
    }

    // --- removeField ---

    @Test
    void removeFieldRecordsDuration() {
        stub.nextContentType = emptyContentType();
        service.removeField(RemoveContentTypeFieldCommand.of(SITE_KEY, CT_KEY, FieldKey.TITLE), ACTOR);
        assertEquals(1, observance.timerCount(REMOVE_FIELD_DURATION));
    }

    @Test
    void removeFieldFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "removeField";
        assertThrows(RuntimeException.class,
                () -> service.removeField(RemoveContentTypeFieldCommand.of(SITE_KEY, CT_KEY, FieldKey.TITLE), ACTOR));
        assertEquals(1, observance.timerCount(REMOVE_FIELD_DURATION));
        assertEquals(1, observance.counterValue(REMOVE_FIELD_FAILED));
    }

    // --- no cross-contamination ---

    @Test
    void eachOperationUsesItsOwnMetrics() {
        stub.nextContentType = emptyContentType();
        stub.nextOptional = Optional.empty();

        service.create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        service.findByKey(SITE_KEY, CT_KEY, ACTOR);

        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
        assertEquals(0, observance.timerCount(ACTIVATE_DURATION));
    }

    // --- runtime integration ---

    @Test
    void runtimeCreatedContentTypeServiceRecordsDuration() {
        final InMemoryObservance runtimeObservance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(runtimeObservance);

        runtime.siteService().create(
                codex.codex.api.model.command.CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);

        assertEquals(1, runtimeObservance.timerCount(CREATE_DURATION));
    }

    @Test
    void runtimeNoArgFactoryStillWorks() {
        runtime = CodexRuntime.inMemory();
        runtime.siteService().create(
                codex.codex.api.model.command.CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        final ContentType ct = runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        assertNotNull(ct);
    }

    // --- private helpers ---

    private static ContentType emptyContentType() {
        final ActorId actorId = ActorId.of("user-1");
        return ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(SITE_KEY)
                .key(CT_KEY)
                .displayName("Article")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
    }

    private static AddContentTypeFieldCommand addFieldCommand() {
        return AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                Field.builder()
                        .key(FieldKey.TITLE)
                        .displayName("Title")
                        .type(FieldType.TEXT)
                        .required(true)
                        .build());
    }

    // --- inner stub ---

    private static final class StubContentTypeService implements ContentTypeService {

        ContentType nextContentType;
        Optional<ContentType> nextOptional = Optional.empty();
        List<ContentType> nextList = List.of();
        String throwOn;
        int createCallCount;

        private void maybeThrow(final String op) {
            if (op.equals(throwOn)) {
                throw new RuntimeException("stub failure: " + op);
            }
        }

        @Override
        public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
            maybeThrow("create");
            createCallCount++;
            return nextContentType;
        }

        @Override
        public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
            maybeThrow("activate");
            return nextContentType;
        }

        @Override
        public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
            maybeThrow("archive");
            return nextContentType;
        }

        @Override
        public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key,
                                               final Actor actor) {
            maybeThrow("findByKey");
            return nextOptional;
        }

        @Override
        public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
            maybeThrow("findBySiteKey");
            return nextList;
        }

        @Override
        public List<ContentType> findAll(final Actor actor) {
            maybeThrow("findAll");
            return nextList;
        }

        @Override
        public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
            maybeThrow("addField");
            return nextContentType;
        }

        @Override
        public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
            maybeThrow("removeField");
            return nextContentType;
        }
    }
}
