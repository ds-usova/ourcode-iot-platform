package integration;

import common.AbstractIntegrationTest;
import common.ShardJdbcTemplateFactory;
import org.junit.jupiter.api.Test;
import org.ourcode.devicecollector.api.model.Device;
import org.ourcode.devicecollector.persistence.PostgresDeviceGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ShardingIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private PostgresDeviceGateway deviceGateway;

    @Test
    void testSaveNewDevice() {
        JdbcTemplate shard0JdbcTemplate = ShardJdbcTemplateFactory.createShard0Template();
        JdbcTemplate shard1JdbcTemplate = ShardJdbcTemplateFactory.createShard1Template();

        // Given: two devices with different IDs
        Device device1 = deviceWithId("device-1");
        Device device2 = deviceWithId("device-2");

        // When: saving the devices
        deviceGateway.upsert(device1);
        deviceGateway.upsert(device2);

        // Then: each shard should contain one device
        int countInShard0 = shard0JdbcTemplate.queryForObject("SELECT COUNT(*) FROM devices WHERE device_id = ?", Integer.class, device1.id());
        int countInShard1 = shard1JdbcTemplate.queryForObject("SELECT COUNT(*) FROM devices WHERE device_id = ?", Integer.class, device2.id());

        assertThat(countInShard0).isEqualTo(1);
        assertThat(countInShard1).isEqualTo(1);
    }

    private Device deviceWithId(String id) {
        return new Device(
                id,
                "sensor",
                1627849923L,
                "{\"location\":\"warehouse-1\",\"status\":\"active\"}"
        );
    }

}
