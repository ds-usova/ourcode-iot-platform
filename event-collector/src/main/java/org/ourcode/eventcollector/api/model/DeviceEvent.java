package org.ourcode.eventcollector.api.model;

public record DeviceEvent(
    String eventId,
    String deviceId,
    Long timestamp,
    String eventType,
    String payload
) {}
