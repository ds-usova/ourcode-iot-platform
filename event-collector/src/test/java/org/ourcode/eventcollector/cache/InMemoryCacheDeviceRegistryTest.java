package org.ourcode.eventcollector.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.eventcollector.api.model.DeviceEvent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryCacheDeviceRegistryTest {

    private InMemoryCacheDeviceRegistry target;

    @BeforeEach
    void setUp() {
        target = new InMemoryCacheDeviceRegistry();
    }

    @Test
    @DisplayName("register if not exists - should register new device and return true")
    void registerIfNotExists_DeviceNotRegistered() {
        // Given: device is not registered
        DeviceEvent deviceEvent = mock(DeviceEvent.class);
        when(deviceEvent.deviceId()).thenReturn("device-1");

        // When: register the device
        boolean registered = target.registerIfNotExists(deviceEvent);

        // Then: should return true
        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("register if not exists - should not register existing device and return false")
    void registerIfNotExists_DeviceAlreadyRegistered() {
        // Given: device is registered
        DeviceEvent deviceEvent = mock(DeviceEvent.class);
        when(deviceEvent.deviceId()).thenReturn("device-1");

        target.registerIfNotExists(deviceEvent);

        // When: try to register the same device again
        boolean secondRegistration = target.registerIfNotExists(deviceEvent);

        // Then: should return false
        assertThat(secondRegistration).isFalse();
    }

}