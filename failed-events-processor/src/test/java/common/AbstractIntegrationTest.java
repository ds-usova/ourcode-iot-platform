package common;

import common.containers.KafkaContainer;
import common.containers.SchemaRegistryContainer;
import common.containers.ToxiproxyContainer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ourcode.devicecollector.FailedEventsProcessorApplication;
import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = FailedEventsProcessorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @Autowired
    private KafkaTopics kafkaTopics;

    protected TestProducers testProducers;

    static {
        log.info("Toxi proxy is running: {}", ToxiproxyContainer.CONTAINER.isRunning());

        log.info("Kafka is running: {}", KafkaContainer.CONTAINER.isRunning());
        log.info("Schema Registry is running: {}", SchemaRegistryContainer.CONTAINER.isRunning());
    }

    protected static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KafkaContainer.CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
        registry.add("spring.kafka.producer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
    }

    @PostConstruct
    void init() {
        testProducers = new TestProducers(kafkaTopics, KafkaContainer.CONTAINER.getBootstrapServers(), schemaRegistryUrl());
    }

    @BeforeAll
    static void setUpBeforeAll() {
        new SchemaManager(schemaRegistryUrl()).registerSchemas();
    }

    @BeforeEach
    void setUp() {
        // No-op for now
    }

    @AfterEach
    void tearDown() {
        // No-op for now
    }

    private static String schemaRegistryUrl() {
        return "http://%s:%d".formatted(
                SchemaRegistryContainer.CONTAINER.getHost(),
                SchemaRegistryContainer.CONTAINER.getMappedPort(8081)
        );
    }

}
