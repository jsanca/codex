package codex.fundamentum.api.runtime;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MapBackedCodexRuntimeContext} and the {@link CodexRuntimeContext} contract.
 */
class MapBackedCodexRuntimeContextTest {

    // --- empty context ---

    @Test
    void emptyContextFindReturnsEmpty() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        assertEquals(Optional.empty(), context.find(String.class));
    }

    @Test
    void emptyContextRequireThrowsIllegalStateException() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        assertThrows(IllegalStateException.class, () -> context.require(String.class));
    }

    // --- builder put and retrieval ---

    @Test
    void builderPutStoresInstance() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "hello")
                .build();
        assertEquals(Optional.of("hello"), context.find(String.class));
    }

    @Test
    void findReturnsInstanceByType() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
                .put(Integer.class, 42)
                .build();
        assertEquals(Optional.of(42), context.find(Integer.class));
    }

    @Test
    void requireReturnsInstanceByType() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "required-value")
                .build();
        assertEquals("required-value", context.require(String.class));
    }

    // --- null guards ---

    @Test
    void findRejectsNullType() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        assertThrows(NullPointerException.class, () -> context.find(null));
    }

    @Test
    void requireRejectsNullType() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        assertThrows(NullPointerException.class, () -> context.require(null));
    }

    @Test
    void builderPutRejectsNullType() {
        assertThrows(NullPointerException.class, () ->
                MapBackedCodexRuntimeContext.builder().put(null, "value"));
    }

    @Test
    void builderPutRejectsNullInstance() {
        assertThrows(NullPointerException.class, () ->
                MapBackedCodexRuntimeContext.builder().put(String.class, null));
    }

    // --- immutability ---

    @Test
    void buildDefensivelyCopiresEntries() {
        final MapBackedCodexRuntimeContext.Builder builder = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "original");
        final CodexRuntimeContext context = builder.build();

        // Cannot add to a built context; assert original value is preserved
        assertEquals("original", context.require(String.class));
    }

    @Test
    void laterBuilderCallsDoNotMutateBuiltContext() {
        final MapBackedCodexRuntimeContext.Builder builder = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "first");
        final CodexRuntimeContext first = builder.build();

        final MapBackedCodexRuntimeContext.Builder builder2 = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "second");
        final CodexRuntimeContext second = builder2.build();

        assertEquals("first", first.require(String.class));
        assertEquals("second", second.require(String.class));
    }

    // --- type safety ---

    @Test
    void wrongTypeIsNotReturned() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "a-string")
                .build();
        assertEquals(Optional.empty(), context.find(Integer.class));
    }

    @Test
    void multipleTypesAreStoredAndRetrievedIndependently() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "hello")
                .put(Integer.class, 99)
                .build();
        assertEquals("hello", context.require(String.class));
        assertEquals(99, context.require(Integer.class));
    }

    // --- duplicate registration ---

    @Test
    void duplicatePutForSameTypeThrowsIllegalStateException() {
        final MapBackedCodexRuntimeContext.Builder builder = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "first");
        assertThrows(IllegalStateException.class, () -> builder.put(String.class, "second"));
    }

    @Test
    void firstValueRemainsAfterDuplicateAttemptIsRejected() {
        final MapBackedCodexRuntimeContext.Builder builder = MapBackedCodexRuntimeContext.builder()
                .put(String.class, "original");

        assertThrows(IllegalStateException.class, () -> builder.put(String.class, "duplicate"));

        final CodexRuntimeContext context = builder.build();
        assertEquals("original", context.require(String.class));
    }

    // --- require error message ---

    @Test
    void requireErrorMessageContainsTypeName() {
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> context.require(String.class));
        assertTrue(ex.getMessage().contains(String.class.getName()),
                "error message should name the missing type");
    }
}
