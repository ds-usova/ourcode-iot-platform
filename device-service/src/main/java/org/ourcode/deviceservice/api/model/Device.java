package org.ourcode.deviceservice.api.model;

public record Device(
        String id,
        String type,
        long timestamp,
        String metadata
) { }
