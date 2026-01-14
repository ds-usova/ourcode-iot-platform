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

}

