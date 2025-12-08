package integration;

import common.AbstractIntegrationTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class MalformedRestRequestTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Test
    @DisplayName("Test call non-existing endpoint")
    public void testCallNonExistingEndpoint() {
        Response response = given()
                .port(port)
                .when()
                .get("/non-existing-endpoint");

        response.prettyPrint();

        response.then()
                .statusCode(404)
                .body("message", equalTo("Resource non-existing-endpoint not found"))
                .body("code", notNullValue());
    }

    @Test
    @DisplayName("Test method is not supported")
    public void testMethodIsNotSupported() {
        Response response = given()
                .port(port)
                .when()
                .get(DeviceUtils.DEVICE_PATH);

        response.prettyPrint();

        response.then()
                .statusCode(404)
                .body("message", equalTo("Method GET is not supported"))
                .body("code", notNullValue());
    }

}
