package codex.index.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexMetricNamesTest {

    @Test
    void upsertCallsConstant() {
        assertEquals("index.upsert.calls", IndexMetricNames.UPSERT_CALLS);
    }

    @Test
    void upsertDurationConstant() {
        assertEquals("index.upsert.duration", IndexMetricNames.UPSERT_DURATION);
    }

    @Test
    void upsertFailedConstant() {
        assertEquals("index.upsert.failed", IndexMetricNames.UPSERT_FAILED);
    }

    @Test
    void deleteCallsConstant() {
        assertEquals("index.delete.calls", IndexMetricNames.DELETE_CALLS);
    }

    @Test
    void deleteDurationConstant() {
        assertEquals("index.delete.duration", IndexMetricNames.DELETE_DURATION);
    }

    @Test
    void deleteFailedConstant() {
        assertEquals("index.delete.failed", IndexMetricNames.DELETE_FAILED);
    }
}
