package integration;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.ourcode.eventcollector.cassandra.CassandraDeviceEventGateway;
import org.ourcode.eventcollector.cassandra.DeviceEventEntity;
import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CassandraIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CassandraDeviceEventGateway deviceEventGateway;

    @Autowired
    private DeviceEventRepository deviceEventRepository;

    @Test
    void testSaveDeviceEvent() {
        // Given: device event
        DeviceEvent deviceEvent = new DeviceEvent(
                "event1",
                "device1",
                System.currentTimeMillis(),
                "temperature",
                "{\"value\": 22.5}"
        );

        // When: saving device event
        DeviceEvent actual = deviceEventGateway.save(deviceEvent);

        // Then: returned event matches input
        assertThat(actual).isEqualTo(deviceEvent);

        // Then: event is stored in Cassandra
        DeviceEventEntity entityFromDb = deviceEventRepository.findAll().getFirst();
        assertThat(actual.eventId()).isEqualTo(entityFromDb.getEventId());
        assertThat(actual.deviceId()).isEqualTo(entityFromDb.getDeviceId());
        assertThat(actual.timestamp()).isEqualTo(entityFromDb.getTimestamp());
        assertThat(actual.eventType()).isEqualTo(entityFromDb.getEventType());
        assertThat(actual.payload()).isEqualTo(entityFromDb.getPayload());
    }

}
