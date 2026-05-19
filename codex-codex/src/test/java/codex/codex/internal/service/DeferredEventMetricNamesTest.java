package codex.codex.internal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeferredEventMetricNamesTest {

    @Test
    void bufferedName() {
        assertEquals("deferred.events.buffered.ContentItemPublishedEvent",
                DeferredEventMetricNames.buffered("ContentItemPublishedEvent"));
    }

    @Test
    void dispatchedImmediatelyName() {
        assertEquals("deferred.events.dispatchedImmediately.ContentItemPublishedEvent",
                DeferredEventMetricNames.dispatchedImmediately("ContentItemPublishedEvent"));
    }

    @Test
    void committedConstant() {
        assertEquals("deferred.events.committed", DeferredEventMetricNames.COMMITTED);
    }

    @Test
    void discardedOnRollbackConstant() {
        assertEquals("deferred.events.discardedOnRollback", DeferredEventMetricNames.DISCARDED_ON_ROLLBACK);
    }

    @Test
    void commitDurationConstant() {
        assertEquals("deferred.commit.duration", DeferredEventMetricNames.COMMIT_DURATION);
    }

    @Test
    void nullEventNameRejectedForBuffered() {
        assertThrows(NullPointerException.class, () -> DeferredEventMetricNames.buffered(null));
    }

    @Test
    void nullEventNameRejectedForDispatchedImmediately() {
        assertThrows(NullPointerException.class, () -> DeferredEventMetricNames.dispatchedImmediately(null));
    }
}
