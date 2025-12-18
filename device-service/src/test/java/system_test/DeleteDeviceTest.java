package system_test;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static common.JsonUtils.readJsonResourceAsString;
import static common.containers.PostgresContainers.PROXY_0;
import static common.containers.PostgresContainers.PROXY_1;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class DeleteDeviceTest extends AbstractIntegrationTest {

    private final String DEVICE_PATH = DeviceUtils.DEVICE_PATH + "/device_1";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    private void fulfillPreconditions() {
        given()
                .port(port)
                .contentType("application/json")
                .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                .when()
                .post(DeviceUtils.DEVICE_PATH)
                .then()
                .statusCode(201);
    }

    @Nested
    class ValidationTests {

        @Test
        @DisplayName("when delete device with long id - then return 400")
        void testLongDeviceId() {
            String id = "a".repeat(256);
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .delete(DeviceUtils.DEVICE_PATH + "/" + id);

            response.prettyPrint();

            response.then()
                    .statusCode(400)
                    .body("message", equalTo("Validation failed: deviceId size must be between 1 and 255"))
                    .body("code", notNullValue());
        }

    }

    @Nested
    class HappyPathTests {

        @Test
        @DisplayName("when device exists - then return 204 and delete device - and 404 on subsequent get")
        void testDeleteDevice() {
            fulfillPreconditions();

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .delete(DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(204);

            // Then: verify device is deleted
            Response getResponse = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .get(DEVICE_PATH);

            getResponse.prettyPrint();

            getResponse.then()
                    .statusCode(404)
                    .body("message", equalTo("Device with ID device_1 not found"))
                    .body("code", notNullValue());
        }

        @Test
        @DisplayName("when no device exist - then return 404")
        void testDeleteNonExistingDevice() {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .delete(DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(404)
                    .body("message", equalTo("Device with ID device_1 not found"))
                    .body("code", notNullValue());
        }

    }

    @Nested
    class UnhappyPathTests {

        @Test
        @DisplayName("when database is down - then return 500")
        void testAddDeviceWhenDatabaseIsDown() throws IOException {
            fulfillPreconditions();

            ToxiProxyUtils.cutConnection(PROXY_0);
            ToxiProxyUtils.cutConnection(PROXY_1);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .delete(DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(500)
                    .body("message", equalTo("An internal error occurred. Please contact support if the issue persists"))
                    .body("code", notNullValue());
        }

    }

}
