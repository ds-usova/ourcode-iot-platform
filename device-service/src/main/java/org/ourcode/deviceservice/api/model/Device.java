package org.ourcode.deviceservice.api.model;

public record Device(
        String id,
        String type,
        Long timestamp,
        String metadata
) { }
