package integration;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.ourcode.devicecollector.api.model.Device;
import org.ourcode.devicecollector.persistence.PostgresDeviceGateway;
import org.ourcode.devicecollector.persistence.entity.DeviceEntity;
import org.ourcode.devicecollector.persistence.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
        // Given: device
        Device device = new Device(
                "device-123",
                "sensor",
                1627849923L,
                "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
        );

        // When: saving a new device
        Device actual = deviceGateway.upsert(device);

        // Then: returned event matches input
        assertThat(actual).isEqualTo(device);

        // Then: event is stored in PostgreSQL
        DeviceEntity entityFromDb = deviceRepository.findById(device.id()).orElseThrow(
                () -> new IllegalStateException("Device not found in DB")
        );
        assertThat(actual.id()).isEqualTo(entityFromDb.getId());
        assertThat(actual.timestamp()).isEqualTo(entityFromDb.getCreatedAt());
        assertThat(actual.type()).isEqualTo(entityFromDb.getType());
        assertThat(actual.metadata()).isEqualTo(entityFromDb.getMetadata());
    }

    @Test
    void testUpdateDevice() {
        // Given: device
        Device device = new Device(
                "device-123",
                "sensor",
                1627849923L,
                "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
        );

        // When: updating a device
        Device saved = deviceGateway.upsert(device);

        device = new Device(
                saved.id(),
                saved.type() + "-updated",
                saved.timestamp() + 100,
                ""
        );

        Device actual = deviceGateway.upsert(device);

        // Then: returned event matches input
        assertThat(actual).isEqualTo(device);

        // Then: event is updated in PostgreSQL
        DeviceEntity entityFromDb = deviceRepository.findById(device.id()).orElseThrow(
                () -> new IllegalStateException("Device not found in DB")
        );
        assertThat(actual.id()).isEqualTo(entityFromDb.getId());
        assertThat(actual.timestamp()).isEqualTo(entityFromDb.getCreatedAt());
        assertThat(actual.type()).isEqualTo(entityFromDb.getType());
        assertThat(actual.metadata()).isEqualTo(entityFromDb.getMetadata());
    }

}
