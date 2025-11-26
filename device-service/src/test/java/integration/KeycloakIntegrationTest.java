package integration;

import common.AbstractIntegrationTest;
import common.containers.KeycloakContainer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Base64;

import static common.JsonUtils.readJsonResourceAsString;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
        registry.add("app.apiProtected", () -> "true");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KeycloakContainer.KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/our-code");
    }

    private String token;

    private void fulfillPreconditions(String clientId, String clientSecret) {
        String tokenEndpoint = KeycloakContainer.KEYCLOAK_CONTAINER.getAuthServerUrl()
                + "/realms/our-code/protocol/openid-connect/token";

        String authorization = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        Response response = given()
                .contentType("application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + authorization)
                .formParam("grant_type", "client_credentials")
                .formParam("scope", "device-service-roles")
                .post(tokenEndpoint);

        response.prettyPrint();

        response
                .then()
                .statusCode(200)
                .extract()
                .response();

        token = response.jsonPath().getString("access_token");
        assertThat(token)
                .withFailMessage("Token couldn't be acquired from Keycloak")
                .isNotNull();
    }

    @Nested
    class TestReadOnlyRights {

        private static final String CLIENT_ID = "device-reader";
        private static final String CLIENT_SECRET = "ekKnotKMQSSH9vAHLirPb98GoVbZI3nf";

        @Test
        @DisplayName("when client has read only rights - then GET is allowed")
        void testGetIsAllowed() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("when client has read only rights - then POST is forbidden")
        void testPostIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("when client has read only rights - then PATCH is forbidden")
        void testPatchIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .body("{\"type\": \"new_type\"}")
                    .when()
                    .patch(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("when client has read only rights - then DELETE is forbidden")
        void testDeleteIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(403);
        }

    }

    @Nested
    class TestWriteRights {

        private static final String CLIENT_ID = "device-writer";
        private static final String CLIENT_SECRET = "AVaha5HFcTXAcjXBgwgVu9dsIAB4Pz8H";

        @Test
        @DisplayName("when client has write rights - then GET is allowed")
        void testGetIsAllowed() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("when client has write rights - then POST is allowed")
        void testPostIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(201);
        }

        @Test
        @DisplayName("when client has write rights - then PATCH is allowed")
        void testPatchIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .body("{\"type\": \"new_type\"}")
                    .when()
                    .patch(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("when client has write rights - then DELETE is allowed")
        void testDeleteIsForbidden() {
            fulfillPreconditions(CLIENT_ID, CLIENT_SECRET);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(404);
        }

    }

}
