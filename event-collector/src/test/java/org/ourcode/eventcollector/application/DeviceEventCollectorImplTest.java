package org.ourcode.eventcollector.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ourcode.eventcollector.api.gateway.DeviceEventGateway;
import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.gateway.DeviceRegistry;
import org.ourcode.eventcollector.api.model.DeviceEvent;

import static org.mockito.Mockito.*;

class DeviceEventCollectorImplTest {

    private DeviceEventGateway deviceEventGateway;

    private DeviceRegistry deviceRegistry;

    private DevicePublisher devicePublisher;

    private DeviceEventCollectorImpl deviceEventCollector;

    @BeforeEach
    void setUp() {
        deviceEventGateway = mock(DeviceEventGateway.class);
        deviceRegistry = mock(DeviceRegistry.class);
        devicePublisher = mock(DevicePublisher.class);

        deviceEventCollector = new DeviceEventCollectorImpl(deviceEventGateway, deviceRegistry, devicePublisher);
    }

    @Test
    void collect_NewDeviceEvent_ShouldSaveAndPublish() {
        // Given: a new device event
        DeviceEvent deviceEvent = mock(DeviceEvent.class);
        when(deviceRegistry.registerIfNotExists(deviceEvent)).thenReturn(true);

        // When: collecting the device event
        deviceEventCollector.collect(deviceEvent);

        // Then: verify that the event was saved and published
        verify(deviceEventGateway, times(1)).save(deviceEvent);
        verify(devicePublisher, times(1)).publish(deviceEvent);
    }

    @Test
    void collect_ExistingDeviceEvent_ShouldOnlySave() {
        // Given: an existing device event
        DeviceEvent deviceEvent = mock(DeviceEvent.class);
        when(deviceRegistry.registerIfNotExists(deviceEvent)).thenReturn(false);

        // When: collecting the device event
        deviceEventCollector.collect(deviceEvent);

        // Then: verify that the event was saved but not published
        verify(deviceEventGateway, times(1)).save(deviceEvent);
        verify(devicePublisher, never()).publish(deviceEvent);
    }

}