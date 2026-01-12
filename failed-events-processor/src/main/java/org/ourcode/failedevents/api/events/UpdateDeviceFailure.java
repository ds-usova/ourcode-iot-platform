package org.ourcode.failedevents.api.events;

public record UpdateDeviceFailure(
        Reason reason,
        int count
) {

    public enum Reason {
        INVALID_INPUT,
        PROCESSING_ERROR,
    }

}