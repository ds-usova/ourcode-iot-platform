package system_test;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.DeviceDeadLetter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

public class DeviceDltCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Test
    @DisplayName("happy path - when a device dl is sent - then it's logged")
    void deviceDltIsProcessed() throws InterruptedException {
        DeviceDeadLetter dl = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-123")
                .setErrorMessage("Simulated error for testing")
                .build();

        testProducers.sendDeviceDeadLetters(List.of(dl));

        Thread.sleep(5000);
    }

}
