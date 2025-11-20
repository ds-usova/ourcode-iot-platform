package system_test;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static common.JsonUtils.readJsonResourceAsString;
import static common.containers.PostgresContainers.PROXY_0;
import static common.containers.PostgresContainers.PROXY_1;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AddDeviceTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Nested
    class ValidationTests {

        @ParameterizedTest(name = "{1}")
        @DisplayName("Validation tests for adding device")
        @CsvSource({
            "json/system_test/device/add/missing_id_400.json, id must not be null",
            "json/system_test/device/add/missing_type_400.json, type must not be null",
        })
        void testValidation(String path, String message) {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString(path))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(400)
                    .body("message", equalTo("Validation failed: " + message))
                    .body("code", notNullValue());
        }

    }

    @Nested
    class HappyPathTests {

        @Test
        @DisplayName("when add device with all required fields - then return 201 with device")
        void testAddDevice() {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(201)
                    .header("Location", equalTo(DeviceUtils.DEVICE_PATH + "/device_1"))
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo("sensor"))
                    .body("meta", equalTo("{\"temp\": \"22C\", \"humidity\": \"45%\"}"));
        }

        @Test
        @DisplayName("when add device without required fields - then return 201 with device")
        void testAddDeviceWithoutRequiredFields() {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_withoutRequired_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(201)
                    .header("Location", equalTo(DeviceUtils.DEVICE_PATH + "/device_1"))
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo("sensor"))
                    .body("meta", nullValue());
        }

    }

    @Nested
    class UnhappyPathTests {

        @Test
        @DisplayName("when device with given id already exists - then return 409")
        void testAddDeviceThatIsAlreadyExist() {
            given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH)
                    .then()
                    .statusCode(201);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(409)
                    .body("message", equalTo("Device with ID device_1 already exists"))
                    .body("code", notNullValue());
        }

        @Test
        @DisplayName("when database is down - then return 500")
        void testAddDeviceWhenDatabaseIsDown() throws IOException {
            ToxiProxyUtils.cutConnection(PROXY_0);
            ToxiProxyUtils.cutConnection(PROXY_1);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(readJsonResourceAsString("json/system_test/device/add/valid_201.json"))
                    .when()
                    .post(DeviceUtils.DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(500)
                    .body("message", equalTo("An internal error occurred. Please contact support if the issue persists"))
                    .body("code", notNullValue());
        }

    }

}
