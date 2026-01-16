package system_test;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

public class EventDltCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Test
    @DisplayName("happy path - when a event dl is sent - then it's logged")
    void eventDltIsProcessed() throws InterruptedException {
        DeviceEventDeadLetter dl = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-456")
                .setDeviceId("device-123")
                .setException("IllegalArgumentException")
                .setErrorMessage("Simulated error for testing")
                .build();

        testProducers.sendEventDeadLetters(List.of(dl));

        Thread.sleep(5000);
    }

}
