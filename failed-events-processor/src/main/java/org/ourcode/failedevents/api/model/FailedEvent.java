package org.ourcode.failedevents.api.model;

import java.time.Instant;

public record FailedEvent(
        String id,
        String origin,
        String type,
        String payload,
        Instant timestamp
) { }
