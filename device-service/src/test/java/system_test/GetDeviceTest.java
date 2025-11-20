package system_test;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import io.restassured.response.Response;
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

public class GetDeviceTest extends AbstractIntegrationTest {

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
        @DisplayName("when get device with long id - then return 400")
        void testLongDeviceId() {
            String id = "a".repeat(256);
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .get(DeviceUtils.DEVICE_PATH + "/" + id);

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
        @DisplayName("when device exists - then return device with 200")
        void testGetDevice() {
            fulfillPreconditions();

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .get(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(200)
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo("sensor"))
                    .body("meta", equalTo("{\"temp\": \"22C\", \"humidity\": \"45%\"}"));
        }

        @Test
        @DisplayName("when no device exist - then return 404")
        void testAddDeviceWithoutRequiredFields() {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .when()
                    .get(DeviceUtils.DEVICE_PATH + "/device_1");

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
                    .get(DeviceUtils.DEVICE_PATH + "/device_1");

            response.prettyPrint();

            response.then()
                    .statusCode(500)
                    .body("message", equalTo("An internal error occurred. Please contact support if the issue persists"))
                    .body("code", notNullValue());
        }

    }

}
