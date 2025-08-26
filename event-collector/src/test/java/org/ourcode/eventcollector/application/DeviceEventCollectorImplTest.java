package org.ourcode.eventcollector.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.eventcollector.api.gateway.DeviceEventGateway;
import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.gateway.DeviceRegistry;
import org.ourcode.eventcollector.api.model.DeviceEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceEventCollectorImplTest {

    @Mock
    private DeviceEventGateway deviceEventGateway;

    @Mock
    private DeviceRegistry deviceRegistry;

    @Mock
    private DevicePublisher devicePublisher;

    private DeviceEventCollectorImpl deviceEventCollector;

    @BeforeEach
    void setUp() {
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