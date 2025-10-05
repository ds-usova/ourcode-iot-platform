package integration;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.api.model.Device;
import org.ourcode.deviceservice.persistence.PostgresDeviceGateway;
import org.ourcode.deviceservice.persistence.entity.DeviceEntity;
import org.ourcode.deviceservice.persistence.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PostgresIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private PostgresDeviceGateway deviceGateway;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void testSaveNewDevice() {
        // Given: no device is saved yet
        Device device = new Device(
                "device-123",
                "sensor",
                1627849923L,
                "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
        );

        assertThat(deviceRepository.findById(device.id()))
                .withFailMessage("devices table must be empty before test")
                .isEmpty();

        // When: saving a new device
        deviceGateway.upsertAll(List.of(device));

        // Then: saved event matches input
        DeviceEntity entityFromDb = deviceRepository.findById(device.id()).orElseThrow(
                () -> new IllegalStateException("Device not found in DB")
        );
        assertThat(device.id()).isEqualTo(entityFromDb.getId());
        assertThat(device.timestamp()).isEqualTo(entityFromDb.getCreatedAt());
        assertThat(device.type()).isEqualTo(entityFromDb.getType());
        assertThat(device.metadata()).isEqualTo(entityFromDb.getMetadata());
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
