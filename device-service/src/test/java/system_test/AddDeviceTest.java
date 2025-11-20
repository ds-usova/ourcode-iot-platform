package system_test;

import common.AbstractIntegrationTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.rest.DeviceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static common.JsonUtils.readJsonResourceAsString;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class AddDeviceTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Test
    void testAddDevice() {
        Response response = given()
                .port(port)
                .contentType("application/json")
                .body(readJsonResourceAsString("json/system_test/addDevice_201.json"))
        .when()
                .post(DeviceUtils.DEVICE_PATH);

        response.prettyPrint();

        response.then()
                .statusCode(201)
                .body("id", equalTo("device_1"))
                .body("type", equalTo("sensor"))
                .body("meta", equalTo("{\"temp\": \"22C\", \"humidity\": \"45%\"}"))
        ;
    }

}
