package common.containers;

import common.ToxiProxyUtils;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.SneakyThrows;

import java.io.IOException;

public class KeycloakContainer {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.3.0";

    public static final dasniko.testcontainers.keycloak.KeycloakContainer KEYCLOAK_CONTAINER;

    private static final Proxy PROXY;

    static {
        KEYCLOAK_CONTAINER = new dasniko.testcontainers.keycloak.KeycloakContainer(KEYCLOAK_IMAGE)
                .withNetwork(Network.NETWORK)
                .withNetworkAliases("keycloak")
                .withRealmImportFile("our-code-realm.json");

        KEYCLOAK_CONTAINER.start();

        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
                ToxiproxyContainer.CONTAINER.getHost(),
                ToxiproxyContainer.CONTAINER.getControlPort()
        );

        try {
           PROXY = toxiproxyClient.createProxy("keycloak-proxy", "0.0.0.0:8668", "keycloak:8080");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakContainer() { }

    public static String getAuthServerUrl() {
        return "http://%s:%s".formatted(
                ToxiproxyContainer.CONTAINER.getHost(),
                ToxiproxyContainer.CONTAINER.getMappedPort(8668)
        );
    }

    @SneakyThrows
    public static void cutConnection() {
        ToxiProxyUtils.cutConnection(PROXY);
    }

    public static void restoreConnection() {
        ToxiProxyUtils.removeAllToxics(PROXY);
    }

}
