package org.ourcode.devicecollector.api.gateway;

/**
 * Interface for collecting metrics related to device processing.
 * All methods must throw no exceptions.
 */
public interface MetricsCollector {

    void incrementInvalidDeviceInput(int count);

    void incrementDeviceProcessingErrors(int count);

    void incrementSuccessfulDevices(int count);

}
