package common;

import common.containers.CassandraContainer;
import common.containers.KafkaContainer;
import common.containers.SchemaRegistryContainer;
import common.containers.ToxiproxyContainer;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ourcode.eventcollector.EventCollectorApplication;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@ActiveProfiles("test")
@Import(TestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = EventCollectorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    protected static Proxy cassandraProxy;

    @Autowired
    private KafkaTopics kafkaTopics;

    @Autowired
    protected DatabaseManager databaseManager;

    protected TestConsumers testConsumers;
    protected TestProducers testProducers;

    static {
        log.info("Kafka is running: {}", KafkaContainer.CONTAINER.isRunning());
        log.info("Schema Registry is running: {}", SchemaRegistryContainer.CONTAINER.isRunning());
        log.info("Cassandra is running: {}", CassandraContainer.CONTAINER.isRunning());
        log.info("Toxi proxy is running: {}", ToxiproxyContainer.CONTAINER.isRunning());
    }

    protected static void setProperties(DynamicPropertyRegistry registry) {
        setProperties(registry, false);
    }

    @SneakyThrows
    protected static void setProperties(DynamicPropertyRegistry registry, boolean toxic) {
        registry.add("spring.kafka.bootstrap-servers", KafkaContainer.CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
        registry.add("spring.kafka.producer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);

        if (toxic) {
            proxyCassandra(registry);
        } else {
            registry.add("spring.cassandra.port", () -> CassandraContainer.CONTAINER.getMappedPort(9042));
            registry.add("spring.cassandra.contact-points", CassandraContainer.CONTAINER::getHost);
        }
    }

    @PostConstruct
    void init() {
        testConsumers = new TestConsumers(kafkaTopics, KafkaContainer.CONTAINER.getBootstrapServers(), schemaRegistryUrl());
        testProducers = new TestProducers(kafkaTopics, KafkaContainer.CONTAINER.getBootstrapServers(), schemaRegistryUrl());
    }

    @BeforeAll
    static void setUpBeforeAll() {
        new SchemaManager(schemaRegistryUrl()).registerSchemas();
    }

    @BeforeEach
    void setUp() {
        testConsumers.init();
    }

    @AfterEach
    void tearDown() {
        testConsumers.close();
        databaseManager.cleanUp();
    }

    @SneakyThrows
    private static void proxyCassandra(DynamicPropertyRegistry registry) {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(ToxiproxyContainer.CONTAINER.getHost(), ToxiproxyContainer.CONTAINER.getControlPort());

        cassandraProxy = toxiproxyClient.createProxy("cassandra-proxy", "0.0.0.0:8666", "cassandra:9042");

        registry.add("spring.cassandra.port", () -> ToxiproxyContainer.CONTAINER.getMappedPort(8666));
        registry.add("spring.cassandra.contact-points", ToxiproxyContainer.CONTAINER::getHost);
    }

    private static String schemaRegistryUrl() {
        return String.format("http://%s:%d", SchemaRegistryContainer.CONTAINER.getHost(), SchemaRegistryContainer.CONTAINER.getMappedPort(8081));
    }

}
