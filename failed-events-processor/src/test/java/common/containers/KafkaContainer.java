package common.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class KafkaContainer {

    public static final org.testcontainers.kafka.KafkaContainer CONTAINER;

    static {
        CONTAINER = new org.testcontainers.kafka.KafkaContainer(
                DockerImageName.parse("apache/kafka:3.9.1")
                        .asCompatibleSubstituteFor("confluentinc/cp-kafka"))
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("kafka")
                .withListener("kafka:29092")
                .waitingFor(Wait.forListeningPort());
        CONTAINER.start();
    }

    private KafkaContainer() {}

}

