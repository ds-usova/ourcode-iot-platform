package org.ourcode.failedevents.minio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.failedevents.api.model.FailedEvent;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectNameGeneratorTest {

    private final ObjectNameGenerator target = new ObjectNameGenerator();

    @Test
    @DisplayName("Generate object name with correct path structure")
    void generateObjectNameWithCorrectPath() {
        // Given: failed event
        String id = "id";
        Instant timestamp = Instant.parse("2026-01-13T10:30:45Z");

        FailedEvent failedEvent = new FailedEvent(
                id,
                "device-ids-dlt",
                "NullPointerException",
                "{\"temp\":25}",
                timestamp
        );

        // When:
        String objectName = target.toObjectName(failedEvent, "json");

        // Then: object name follows expected pattern
        assertThat(objectName).isEqualTo("device-ids-dlt/NullPointerException/2026/1/13/10-30-45-id.json");
    }

    @Test
    @DisplayName("Replace hierarchy separator in origin, type, and id fields")
    void replaceHierarchySeparatorInAffectedFields() {
        // Given: event with hierarchy separators in origin, type, and id
        String id = "event/123";
        String origin = "device/collector";
        String type = "Validation/Exception";
        String payload = "{}";
        Instant timestamp = Instant.parse("2026-01-13T10:30:45Z");

        FailedEvent failedEvent = new FailedEvent(id, origin, type, payload, timestamp);

        // When: generating object name
        String objectName = target.toObjectName(failedEvent, "json");

        // Then: separators are replaced with underscore in the path
        assertThat(objectName).isEqualTo("device_collector/Validation_Exception/2026/1/13/10-30-45-event_123.json");
    }

    @Test
    @DisplayName("Replace multiple hierarchy separators in same field")
    void replaceMultipleHierarchySeparators() {
        // Given: event with multiple separators in id
        String idWithMultipleSeparators = "event/123/retry";
        String origin = "service";
        String type = "Error";
        String payload = "{}";
        Instant timestamp = Instant.parse("2026-01-13T10:30:45Z");

        FailedEvent failedEvent = new FailedEvent(idWithMultipleSeparators, origin, type, payload, timestamp);

        // When: generating object name
        String objectName = target.toObjectName(failedEvent, "json");

        // Then: all separators are replaced
        assertThat(objectName).isEqualTo("service/Error/2026/1/13/10-30-45-event_123_retry.json");
    }

}

