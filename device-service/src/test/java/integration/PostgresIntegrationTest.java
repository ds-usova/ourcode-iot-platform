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

    @Nested
    public class TestGetDeviceById {

        @Test
        @DisplayName("happy path - device is retrieved successfully")
        void testGetDeviceById() {
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

            // When: retrieving the device by ID
            Device retrievedDevice = deviceGateway.getBy(device.id()).orElseThrow(
                    () -> new IllegalStateException("Device not found in DB")
            );

            // Then: retrieved device matches saved device
            assertThat(retrievedDevice).isNotNull();
            assertThat(retrievedDevice.id()).isEqualTo(device.id());
            assertThat(retrievedDevice.type()).isEqualTo(device.type());
            assertThat(retrievedDevice.metadata()).isEqualTo(device.metadata());
        }

        @Test
        @DisplayName("when device does not exist - then empty Optional is returned")
        void testGetDeviceById_deviceDoesNotExist() {
            // Given: no device is saved yet
            String nonExistentDeviceId = "non-existent-device-456";

            // When & Then: retrieving the device by ID returns empty Optional
            assertThat(deviceGateway.getBy(nonExistentDeviceId))
                    .withFailMessage("device must not exist")
                    .isEmpty();
        }

    }

}
