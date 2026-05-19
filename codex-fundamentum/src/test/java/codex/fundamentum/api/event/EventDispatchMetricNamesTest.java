package codex.fundamentum.api.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventDispatchMetricNamesTest {

    @Test
    void eventsDispatchedName() {
        assertEquals("events.dispatched.ContentItemPublishedEvent",
                EventDispatchMetricNames.eventsDispatched("ContentItemPublishedEvent"));
    }

    @Test
    void subscribersInvokedName() {
        assertEquals("subscribers.invoked.ContentItemPublishedIndexingSubscriber",
                EventDispatchMetricNames.subscribersInvoked("ContentItemPublishedIndexingSubscriber"));
    }

    @Test
    void subscribersDurationName() {
        assertEquals("subscribers.duration.ContentItemPublishedIndexingSubscriber",
                EventDispatchMetricNames.subscribersDuration("ContentItemPublishedIndexingSubscriber"));
    }

    @Test
    void subscribersFailedName() {
        assertEquals("subscribers.failed.ContentItemPublishedIndexingSubscriber",
                EventDispatchMetricNames.subscribersFailed("ContentItemPublishedIndexingSubscriber"));
    }

    @Test
    void nullEventNameRejectedForEventsDispatched() {
        assertThrows(NullPointerException.class, () -> EventDispatchMetricNames.eventsDispatched(null));
    }

    @Test
    void nullSubscriberNameRejectedForSubscribersInvoked() {
        assertThrows(NullPointerException.class, () -> EventDispatchMetricNames.subscribersInvoked(null));
    }

    @Test
    void nullSubscriberNameRejectedForSubscribersDuration() {
        assertThrows(NullPointerException.class, () -> EventDispatchMetricNames.subscribersDuration(null));
    }

    @Test
    void nullSubscriberNameRejectedForSubscribersFailed() {
        assertThrows(NullPointerException.class, () -> EventDispatchMetricNames.subscribersFailed(null));
    }
}
