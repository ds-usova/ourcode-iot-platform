package common.containers;

import org.testcontainers.cassandra.wait.CassandraQueryWaitStrategy;

public class CassandraContainer {

    public static final org.testcontainers.cassandra.CassandraContainer CONTAINER;

    static {
        CONTAINER = new org.testcontainers.cassandra.CassandraContainer("cassandra:5.0")
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("cassandra")
                .withExposedPorts(9042)
                .withEnv("CASSANDRA_CLUSTER_NAME", "cluster")
                .withInitScript("init-cassandra.cql")
                .waitingFor(new CassandraQueryWaitStrategy());

        CONTAINER.start();
    }

    private CassandraContainer() {}
}
