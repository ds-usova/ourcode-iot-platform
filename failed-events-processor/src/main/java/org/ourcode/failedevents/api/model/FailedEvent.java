package org.ourcode.failedevents.api.model;

import org.ourcode.failedevents.api.service.Constants;

import java.time.Instant;

public record FailedEvent(
        String id,
        String origin,
        String type,
        String payload,
        Instant timestamp
) {

    private static final String REPLACEMENT_CHAR = "_";

    public FailedEvent {
        if (id.contains(Constants.HIERARCHY_SEPARATOR)) {
            id = id.replace(Constants.HIERARCHY_SEPARATOR, REPLACEMENT_CHAR);
        }

        if (origin.contains(Constants.HIERARCHY_SEPARATOR)) {
            origin = origin.replace(Constants.HIERARCHY_SEPARATOR, REPLACEMENT_CHAR);
        }

        if (type.contains(Constants.HIERARCHY_SEPARATOR)) {
            type = type.replace(Constants.HIERARCHY_SEPARATOR, REPLACEMENT_CHAR);
        }
    }

}
