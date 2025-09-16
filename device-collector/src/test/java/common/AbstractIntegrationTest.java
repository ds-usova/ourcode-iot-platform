package common;

import common.containers.KafkaContainer;
import common.containers.PostgresContainers;
import common.containers.SchemaRegistryContainer;
import common.containers.ToxiproxyContainer;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ourcode.devicecollector.DeviceCollectorApplication;
import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DeviceCollectorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class AbstractIntegrationTest {

    @Autowired
    private KafkaTopics kafkaTopics;

    protected TestConsumers testConsumers;
    protected TestProducers testProducers;

    static {
        log.info("Toxi proxy is running: {}", ToxiproxyContainer.CONTAINER.isRunning());

        log.info("Kafka is running: {}", KafkaContainer.CONTAINER.isRunning());
        log.info("Schema Registry is running: {}", SchemaRegistryContainer.CONTAINER.isRunning());

        log.info("Postgres 0 is running: {}", PostgresContainers.CONTAINER_0.isRunning());
        log.info("Postgres 1 is running: {}", PostgresContainers.CONTAINER_1.isRunning());
    }

    protected static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KafkaContainer.CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
        registry.add("spring.kafka.producer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
    }

    @PostConstruct
    void init() {
        testConsumers = new TestConsumers(kafkaTopics, KafkaContainer.CONTAINER.getBootstrapServers(), schemaRegistryUrl());
        testProducers = new TestProducers(kafkaTopics, KafkaContainer.CONTAINER.getBootstrapServers(), schemaRegistryUrl());
    }

    @BeforeAll
    static void setUpBeforeAll() {
        PostgresContainers.setShardEnv();
        new SchemaManager(schemaRegistryUrl()).registerSchemas();
    }

    @BeforeEach
    void setUp() {
        testConsumers.init();
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        testConsumers.close();
        PostgresContainers.resetProxies();
    }

    private static String schemaRegistryUrl() {
        return String.format("http://%s:%d", SchemaRegistryContainer.CONTAINER.getHost(), SchemaRegistryContainer.CONTAINER.getMappedPort(8081));
    }

}
