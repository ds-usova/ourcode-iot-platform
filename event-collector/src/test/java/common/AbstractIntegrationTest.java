package common;

import org.ourcode.eventcollector.EventCollectorApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

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
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*\\n", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        String schemaRegistryUrl = "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> schemaRegistryUrl);
        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> schemaRegistryUrl);
        registry.add("spring.cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("spring.cassandra.contact-points", cassandra::getHost);
    }

}
