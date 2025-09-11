package common.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class PostgresContainers {

    public static final org.testcontainers.containers.PostgreSQLContainer<?> CONTAINER_0;
    public static final org.testcontainers.containers.PostgreSQLContainer<?> CONTAINER_1;

    static {
        CONTAINER_0 = new PostgreSQLContainer<>("postgres:17.5")
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("shard_0")
                .withDatabaseName("our_code_db")
                .withUsername("test")
                .withPassword("test")
                .waitingFor(Wait.forListeningPort());

        CONTAINER_1 = new PostgreSQLContainer<>("postgres:17.5")
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("shard_1")
                .withDatabaseName("our_code_db")
                .withUsername("test")
                .withPassword("test")
                .waitingFor(Wait.forListeningPort());

        CONTAINER_0.start();
        CONTAINER_1.start();
    }

    private PostgresContainers() { }

    public static void setShardEnv() {
        System.setProperty("SHARD_0_JDBC_URL", CONTAINER_0.getJdbcUrl());
        System.setProperty("SHARD_0_USER", CONTAINER_0.getUsername());
        System.setProperty("SHARD_0_PASSWORD", CONTAINER_0.getPassword());

        System.setProperty("SHARD_1_JDBC_URL", CONTAINER_1.getJdbcUrl());
        System.setProperty("SHARD_1_USER", CONTAINER_1.getUsername());
        System.setProperty("SHARD_1_PASSWORD", CONTAINER_1.getPassword());
    }

}
