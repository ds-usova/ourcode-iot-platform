package org.ourcode.failedevents.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.failedevents.api.service.Constants;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailedEventTest {

    @Test
    @DisplayName("Create FailedEvent with valid fields")
    void createFailedEventWithValidFields() {
        // Given: valid event data
        String id = "event-123";
        String origin = "device-collector";
        String type = "ValidationException";
        String payload = "{\"deviceId\":\"device-001\",\"error\":\"Invalid format\"}";
        Instant timestamp = Instant.parse("2026-01-13T10:30:45Z");

        // When: creating FailedEvent
        FailedEvent failedEvent = new FailedEvent(id, origin, type, payload, timestamp);

        // Then: all fields are correctly set
        assertThat(failedEvent.id()).isEqualTo(id);
        assertThat(failedEvent.origin()).isEqualTo(origin);
        assertThat(failedEvent.type()).isEqualTo(type);
        assertThat(failedEvent.payload()).isEqualTo(payload);
        assertThat(failedEvent.timestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Replace hierarchy separator in origin, type, and id fields")
    void replaceHierarchySeparatorInAffectedFields() {
        // Given: id containing hierarchy separator
        String id = "event" + Constants.HIERARCHY_SEPARATOR + "123";
        String origin = "device" + Constants.HIERARCHY_SEPARATOR + "collector";
        String type = "Validation" + Constants.HIERARCHY_SEPARATOR + "Exception";

        String payload = "{}";
        Instant timestamp = Instant.now();

        // When: creating FailedEvent
        FailedEvent failedEvent = new FailedEvent(id, origin, type, payload, timestamp);

        // Then: separator is replaced with underscore
        assertThat(failedEvent.id()).isEqualTo("event_123");
        assertThat(failedEvent.origin()).isEqualTo("device_collector");
        assertThat(failedEvent.type()).isEqualTo("Validation_Exception");
    }

    @Test
    @DisplayName("Replace multiple hierarchy separators in same field")
    void replaceMultipleHierarchySeparators() {
        // Given: id with multiple separators
        String idWithMultipleSeparators = "event" + Constants.HIERARCHY_SEPARATOR + "123" + Constants.HIERARCHY_SEPARATOR + "retry";
        String origin = "service";
        String type = "Error";
        String payload = "{}";
        Instant timestamp = Instant.now();

        // When: creating FailedEvent
        FailedEvent failedEvent = new FailedEvent(idWithMultipleSeparators, origin, type, payload, timestamp);

        // Then: all separators are replaced
        assertThat(failedEvent.id()).isEqualTo("event_123_retry");
    }

}

