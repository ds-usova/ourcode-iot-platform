package system_test;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static common.JsonUtils.readJsonResourceAsString;
import static common.containers.PostgresContainers.PROXY_0;
import static common.containers.PostgresContainers.PROXY_1;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class UpdateDeviceTest extends AbstractIntegrationTest {

    private final String PATCH_DEVICE_PATH = DeviceUtils.DEVICE_PATH + "/device_1";

    private static final String ORIGINAL_TYPE = "sensor";
    private static final String ORIGINAL_METADATA = "{\"temp\": \"22C\", \"humidity\": \"45%\"}";

    private static final String UPDATED_METADATA = "{\"temp\": \"25C\", \"humidity\": \"50%\"}";
    private static final String UPDATED_METADATA_ESCAPED = "{\\\"temp\\\": \\\"25C\\\", \\\"humidity\\\": \\\"50%\\\"}";
    private static final String UPDATED_TYPE = "updated_sensor";

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

    private String buildPatchBody(String type, String metadata) {
        List<String> params = new ArrayList<>();
        if (type != null) {
            params.add("""
                    "type" : "%s"\
                    """.formatted(type));
        }

        if (metadata != null) {
            params.add("""
                    "meta" : "%s"\
                    """.formatted(metadata));
        }

        String body = params.isEmpty() ? "" : ("{ " + String.join(", ", params) + " }");
        log.debug("Built PATCH body: {}", body);
        return body;
    }

    @Nested
    class ValidationTests {

        // Arguments: type, metadata, expectedMessage
        static Stream<Arguments> validationTestProvider() {
            return Stream.of(
                    Arguments.of(null, null, "malformed request body"),
                    // type validation
                    Arguments.of("", null, "type size must be between 1 and 255"),
                    Arguments.of("   ", null, "type must not be blank"),
                    Arguments.of("a".repeat(256), null, "type size must be between 1 and 255")
            );
        }

        @ParameterizedTest(name = "type = ({0}), metadata = ({1}), message = {2}")
        @DisplayName("Type and metadata validation tests for updating device")
        @MethodSource("validationTestProvider")
        void testValidation(String type, String metadata, String message) {
            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(buildPatchBody(type, metadata))
                    .when()
                    .patch(PATCH_DEVICE_PATH);

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
        @DisplayName("when update device with all fields - then return 200 with updated device")
        void testUpdateDevice() {
            fulfillPreconditions();

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(buildPatchBody(UPDATED_TYPE, UPDATED_METADATA_ESCAPED))
                    .when()
                    .patch(PATCH_DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(200)
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo(UPDATED_TYPE))
                    .body("meta", equalTo(UPDATED_METADATA));
        }

        @Test
        @DisplayName("when update device type - then return 200 with device")
        void testUpdateDeviceTypeOnly() {
            fulfillPreconditions();

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(buildPatchBody(UPDATED_TYPE, null))
                    .when()
                    .patch(PATCH_DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(200)
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo(UPDATED_TYPE))
                    .body("meta", equalTo(ORIGINAL_METADATA));
        }

        @Test
        @DisplayName("when update device metadata - then return 200 with device")
        void testUpdateDeviceMetadataOnly() {
            fulfillPreconditions();

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(buildPatchBody(null, UPDATED_METADATA_ESCAPED))
                    .when()
                    .patch(PATCH_DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(200)
                    .body("id", equalTo("device_1"))
                    .body("type", equalTo(ORIGINAL_TYPE))
                    .body("meta", equalTo(UPDATED_METADATA));
        }

    }

    @Nested
    class UnhappyPathTests {

        @Test
        @DisplayName("when database is down - then return 500")
        void testUpdateDeviceWhenDatabaseIsDown() throws IOException {
            ToxiProxyUtils.cutConnection(PROXY_0);
            ToxiProxyUtils.cutConnection(PROXY_1);

            Response response = given()
                    .port(port)
                    .contentType("application/json")
                    .body(buildPatchBody(UPDATED_TYPE, UPDATED_METADATA_ESCAPED))
                    .when()
                    .patch(PATCH_DEVICE_PATH);

            response.prettyPrint();

            response.then()
                    .statusCode(500)
                    .body("message", equalTo("An internal error occurred. Please contact support if the issue persists"))
                    .body("code", notNullValue());
        }

    }

}
