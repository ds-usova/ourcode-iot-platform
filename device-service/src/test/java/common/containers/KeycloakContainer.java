package common.containers;

public class KeycloakContainer {

    public static final dasniko.testcontainers.keycloak.KeycloakContainer KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new dasniko.testcontainers.keycloak.KeycloakContainer("quay.io/keycloak/keycloak:26.3.0")
                .withRealmImportFile("our-code-realm.json");

        KEYCLOAK_CONTAINER.start();
    }

    private KeycloakContainer() { }

}
