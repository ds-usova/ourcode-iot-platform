package system_test;

import common.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.devicecollector.persistence.entity.DeviceEntity;
import org.ourcode.devicecollector.persistence.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class DeviceCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    protected DeviceRepository deviceRepository;

    /**
     * Given:
     * - Device Avro schema is registered in Schema Registry
     * <p>
     * When:
     * - Two valid devices are sent to "device-ids" topic
     * <p>
     * Then:
     * - Both devices are saved in database
     */
    @Test
    @DisplayName("Happy path: devices are collected")
    void deviceIsCollected() {
        Device device1 = Device.newBuilder()
                .setDeviceId("device-1")
                .setDeviceType("sensor")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{\"location\":\"warehouse-1\"}")
                .build();

        Device device2 = Device.newBuilder()
                .setDeviceId("device-2")
                .setDeviceType("sensor")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{\"location\":\"warehouse-1\"}")
                .build();

        testProducers.sendDevices(List.of(device1, device2));

        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            List<DeviceEntity> savedDevices = deviceRepository.findAll();

            assertThat(savedDevices).hasSize(2);
            assertThat(savedDevices)
                    .extracting(DeviceEntity::getId)
                    .containsExactlyInAnyOrder("device-1", "device-2");
        });
    }

    /**
     * Given:
     * - Device Avro schema is registered in Schema Registry
     * <p>
     * When:
     * - Device is sent to "device-ids" topic twice with the same deviceId
     * <p>
     * Then:
     * - Device is updated in the database (not duplicated)
     */
    @Test
    @DisplayName("Happy path: devices are updated")
    void deviceIsUpdated() {
        // Given: a device is saved in the database
        Device device = Device.newBuilder()
                .setDeviceId("device-1")
                .setDeviceType("sensor")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{\"location\":\"warehouse-1\"}")
                .build();

        testProducers.sendDevices(List.of(device));

        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            List<DeviceEntity> savedDevices = deviceRepository.findAll();
            assertThat(savedDevices).hasSize(1);
        });

        // When: the same device is sent again with updated fields
        Device updatedDevice = Device.newBuilder()
                .setDeviceId(device.getDeviceId())
                .setDeviceType("tracker")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{\"location\":\"warehouse-2\"}")
                .build();

        testProducers.sendDevices(List.of(updatedDevice));

        // Then: the device is updated in the database
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            List<DeviceEntity> savedDevices = deviceRepository.findAll();
            assertThat(savedDevices).hasSize(1);

            DeviceEntity deviceEntity = savedDevices.getFirst();
            assertThat(deviceEntity.getId()).isEqualTo(updatedDevice.getDeviceId());
            assertThat(deviceEntity.getType()).isEqualTo(updatedDevice.getDeviceType());
            assertThat(deviceEntity.getCreatedAt()).isEqualTo(updatedDevice.getCreatedAt());
            assertThat(deviceEntity.getMetadata()).isEqualTo(updatedDevice.getMeta());
        });
    }

    /**
     * Given:
     * - Invalid Avro message is sent to "device-ids" topic
     * <p>
     * When:
     * - The message is consumed
     * <p>
     * Then:
     * - The message is sent to the DLT topic with an error message
     * - No device is saved to the database
     */
    @Test
    @DisplayName("Invalid Avro: device is not collected if schema is invalid")
    void deviceIsNotCollectedOnInvalidAvro() {
        // Given: payload that does not conform to the Avro schema
        byte[] invalidPayload = "poison-pills".getBytes();

        // When: Send invalid Avro bytes to the events topic
        testProducers.sendRawEventBytes("invalid-key", invalidPayload);

        // Then: The event is sent to the DLT
        Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Map<String, DeviceDeadLetter> devices = testConsumers.readDlt();

            assertThat(devices).withFailMessage("Expected exactly one event in DLT").hasSize(1);
            assertThat(devices).containsKey("invalid-key");
            assertThat(devices.get("invalid-key").getErrorMessage()).isEqualTo("failed to deserialize");
            assertThat(devices.get("invalid-key").getRawEvent()).isEqualTo(Base64.getEncoder().encodeToString(invalidPayload));

            List<DeviceEntity> savedEvents = deviceRepository.findAll();
            assertThat(savedEvents).isEmpty();
        });
    }

}
