package common;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ourcode.eventcollector.EventCollectorApplication;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.cassandra.wait.CassandraQueryWaitStrategy;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("resource")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = EventCollectorApplication.class)
public abstract class AbstractIntegrationTest {

    private static final Network network = Network.newNetwork();

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.9.1")
                    .asCompatibleSubstituteFor("confluentinc/cp-kafka")
    )
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener("kafka:29092")
            .waitingFor(Wait.forListeningPort());

    @Container
    static final GenericContainer<?> schemaRegistry = new GenericContainer<>(DockerImageName.parse("bitnami/schema-registry:8.0"))
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKA_BROKERS", "PLAINTEXT://kafka:29092")
            .withEnv("SCHEMA_REGISTRY_AVRO_COMPATIBILY_LEVEL", "BACKWARD")
            .withEnv("SCHEMA_REGISTRY_DEBUG", "true")
            .dependsOn(kafka)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @Container
    static final CassandraContainer cassandra = new CassandraContainer("cassandra:5.0")
            .withNetwork(network)
            .withNetworkAliases("cassandra")
            .withExposedPorts(9042)
            .withEnv("CASSANDRA_CLUSTER_NAME", "cluster")
            .withInitScript("init-cassandra.cql")
            .waitingFor(new CassandraQueryWaitStrategy());

    @Autowired
    private KafkaTopics kafkaTopics;

    protected TestConsumers testConsumers;
    protected TestProducers testProducers;

    protected static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
        registry.add("spring.kafka.producer.properties.schema.registry.url", AbstractIntegrationTest::schemaRegistryUrl);
        registry.add("spring.cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("spring.cassandra.contact-points", cassandra::getHost);
    }

    @PostConstruct
    void init() {
        testConsumers = new TestConsumers(kafkaTopics, kafka.getBootstrapServers(), schemaRegistryUrl());
        testProducers = new TestProducers(kafkaTopics, kafka.getBootstrapServers(), schemaRegistryUrl());
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
    }

    private static String schemaRegistryUrl() {
        return String.format("http://%s:%d", schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));
    }

}

