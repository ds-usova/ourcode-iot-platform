package org.ourcode.devicecollector.api.events;

public record UpdateDeviceFailure(
        Reason reason,
        int count
) {

    public enum Reason {
        INVALID_INPUT,
        PROCESSING_ERROR,
    }

}