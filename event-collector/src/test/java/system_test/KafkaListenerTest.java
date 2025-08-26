package system_test;

import com.ourcode.avro.DeviceEvent;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.ourcode.eventcollector.EventCollectorApplication;
import org.ourcode.eventcollector.cassandra.DeviceEventEntity;
import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@ActiveProfiles("test")
@SuppressWarnings("resource")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = EventCollectorApplication.class)
public class KafkaListenerTest {

    private static final Network network = Network.newNetwork();

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.9.1").asCompatibleSubstituteFor("confluentinc/cp-kafka")
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

    @Autowired
    private DeviceEventRepository repository;

    @Test
    public void testKafkaListener() throws Exception {
        // Create initial records in Cassandra
        repository.insert(new DeviceEventEntity("event1", "device1", System.currentTimeMillis(), "type1", "{\"key\":\"value\"}"));

        int eventSchemaId = registerSchema(DeviceEvent.getClassSchema(), "events");
        System.out.println("Registered schema with ID: " + eventSchemaId);

        // Create Kafka producer properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        
        String schemaRegistryUrl = "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
        props.put("schema.registry.url", schemaRegistryUrl);

        // Create the producer
        try (KafkaProducer<String, DeviceEvent> producer = new KafkaProducer<>(props)) {
            // Create a test event
            DeviceEvent event = DeviceEvent.newBuilder()
                    .setEventId("test-event-1")
                    .setDeviceId("test-device-1")
                    .setTimestamp(System.currentTimeMillis())
                    .setType("TEST_TYPE")
                    .setPayload("{\"test\":\"value\"}")
                    .build();

            System.out.println("Sending event: " + event);

            ProducerRecord<String, DeviceEvent> record = new ProducerRecord<>("events", event.getDeviceId(), event);
            producer.send(record).get();
        }
    }

    private static int registerSchema(Schema schema, String topic) {
        String schemaRegistryUrl = "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort();
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 1000);

        String subject = topic + "-value";

        try {
            return schemaRegistryClient.register(subject, new AvroSchema(schema));
        } catch (IOException | RestClientException e) {
            throw new RuntimeException("Failed to register schema", e);
        }
    }

}