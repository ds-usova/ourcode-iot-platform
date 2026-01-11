package common.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class SchemaRegistryContainer {
    public static final GenericContainer<?> CONTAINER;

    static {
        CONTAINER = new GenericContainer<>(DockerImageName.parse("bitnami/schema-registry:8.0"))
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("schema-registry")
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .withEnv("SCHEMA_REGISTRY_KAFKA_BROKERS", "PLAINTEXT://kafka:29092")
                .withEnv("SCHEMA_REGISTRY_AVRO_COMPATIBILY_LEVEL", "BACKWARD")
                .dependsOn(KafkaContainer.CONTAINER)
                .withEnv("SCHEMA_REGISTRY_DEBUG", "true")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

        CONTAINER.start();
    }

    private SchemaRegistryContainer() {}

}

