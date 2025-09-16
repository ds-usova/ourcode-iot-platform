package common.containers;

import common.ToxiProxyUtils;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.SneakyThrows;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.stream.Stream;

public class PostgresContainers {

    public static final org.testcontainers.containers.PostgreSQLContainer<?> CONTAINER_0;
    public static final org.testcontainers.containers.PostgreSQLContainer<?> CONTAINER_1;

    public static Proxy PROXY_0;
    public static Proxy PROXY_1;

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

    public static void resetProxies() {
        Stream.of(PROXY_0, PROXY_1).forEach(ToxiProxyUtils::removeAllToxics);
    }

    @SneakyThrows
    public static void setShardEnv() {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(ToxiproxyContainer.CONTAINER.getHost(), ToxiproxyContainer.CONTAINER.getControlPort());

        if (PROXY_0 == null) {
            PROXY_0 = toxiproxyClient.createProxy("shard-0-proxy", "0.0.0.0:8666", "shard_0:5432");
        }

        if (PROXY_1 == null) {
            PROXY_1 = toxiproxyClient.createProxy("shard-1-proxy", "0.0.0.0:8667", "shard_1:5432");
        }

        // Shard 0
        String urlTemplate = "jdbc:postgresql://%s:%d/our_code_db?loggerLevel=OFF";
        System.setProperty("SHARD_0_JDBC_URL",
                urlTemplate.formatted(
                        ToxiproxyContainer.CONTAINER.getHost(),
                        ToxiproxyContainer.CONTAINER.getMappedPort(8666)
                )
        );
        System.setProperty("SHARD_0_USER", CONTAINER_0.getUsername());
        System.setProperty("SHARD_0_PASSWORD", CONTAINER_0.getPassword());
        System.setProperty("SHARD_0_CONNECTION_TIMEOUT", "" + 3000);

        // Shard 1
        System.setProperty("SHARD_1_JDBC_URL",
                urlTemplate.formatted(
                        ToxiproxyContainer.CONTAINER.getHost(),
                        ToxiproxyContainer.CONTAINER.getMappedPort(8667)
                )
        );
        System.setProperty("SHARD_1_USER", CONTAINER_1.getUsername());
        System.setProperty("SHARD_1_PASSWORD", CONTAINER_1.getPassword());
        System.setProperty("SHARD_1_CONNECTION_TIMEOUT", "" + 3000);
    }

}
