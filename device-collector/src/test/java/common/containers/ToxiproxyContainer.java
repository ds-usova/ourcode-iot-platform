package common.containers;

import org.testcontainers.containers.wait.strategy.Wait;

public class ToxiproxyContainer {

    public static final org.testcontainers.containers.ToxiproxyContainer CONTAINER;

    static {
        CONTAINER = new org.testcontainers.containers.ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.12.0")
                .withNetwork(Network.NETWORK)
                .waitingFor(Wait.forListeningPort());

        CONTAINER.start();
    }

    private ToxiproxyContainer() {}

}

