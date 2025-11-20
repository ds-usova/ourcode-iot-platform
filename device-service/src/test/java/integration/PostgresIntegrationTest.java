package integration;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.api.exception.DuplicateException;
import org.ourcode.deviceservice.api.model.Device;
import org.ourcode.deviceservice.persistence.PostgresDeviceGateway;
import org.ourcode.deviceservice.persistence.entity.DeviceEntity;
import org.ourcode.deviceservice.persistence.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PostgresIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private PostgresDeviceGateway deviceGateway;

    @Autowired
    private DeviceRepository deviceRepository;

    @Nested
    public class TestSaveNewDevice {

        @Test
        @DisplayName("happy path - device is saved successfully")
        void testSaveNewDevice() {
            // Given: no device is saved yet
            Device device = new Device(
                    "device-123",
                    "sensor",
                    null,
                    "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
            );

            assertThat(deviceRepository.findById(device.id()))
                    .withFailMessage("devices table must be empty before test")
                    .isEmpty();

            // When: saving a new device
            deviceGateway.create(device);

            // Then: saved event matches input
            DeviceEntity entityFromDb = deviceRepository.findById(device.id()).orElseThrow(
                    () -> new IllegalStateException("Device not found in DB")
            );
            assertThat(entityFromDb.getId()).isEqualTo(device.id());
            assertThat(entityFromDb).isNotNull();
            assertThat(entityFromDb.getType()).isEqualTo(device.type());
            assertThat(entityFromDb.getMetadata()).isEqualTo(device.metadata());
        }

        @Test
        @DisplayName("when device already exists - then DuplicateException is thrown")
        void testSaveNewDevice_deviceAlreadyExists() {
            // Given: device is already saved
            Device device = new Device(
                    "device-123",
                    "sensor",
                    null,
                    "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
            );

            deviceGateway.create(device);
            assertThat(deviceRepository.findById(device.id()))
                    .withFailMessage("device must exist in DB before test")
                    .isNotEmpty();

            // When: saving a new device
            assertThatThrownBy(() -> deviceGateway.create(device))
                    .isInstanceOf(DuplicateException.class);
        }

    }

    @Test
    void testUpdateDevice() {
        // Given: device is already saved
        Device device = new Device(
                "device-123",
                "sensor",
                1627849923L,
                "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
        );

        deviceGateway.upsertAll(List.of(device));

        device = new Device(
                device.id(),
                device.type() + "-updated",
                device.timestamp() + 100,
                ""
        );

        // When: updating a device
        deviceGateway.upsertAll(List.of(device));

        // Then: returned event matches input
        DeviceEntity entityFromDb = deviceRepository.findById(device.id()).orElseThrow(
                () -> new IllegalStateException("Device not found in DB")
        );
        assertThat(device.id()).isEqualTo(entityFromDb.getId());
        assertThat(device.timestamp()).isEqualTo(entityFromDb.getCreatedAt());
        assertThat(device.type()).isEqualTo(entityFromDb.getType());
        assertThat(device.metadata()).isEqualTo(entityFromDb.getMetadata());
    }

}
