package common;

import common.containers.PostgresContainers;
import common.containers.ToxiproxyContainer;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ourcode.deviceservice.DeviceServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DeviceServiceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class AbstractIntegrationTest {

    static {
        log.info("Toxi proxy is running: {}", ToxiproxyContainer.CONTAINER.isRunning());

        log.info("Postgres 0 is running: {}", PostgresContainers.CONTAINER_0.isRunning());
        log.info("Postgres 1 is running: {}", PostgresContainers.CONTAINER_1.isRunning());
    }

    protected static void setProperties(DynamicPropertyRegistry registry) {
        // no op - in case we need to add something later
    }

    @PostConstruct
    void init() {
        // no op - in case we need to add something later
    }

    @BeforeAll
    static void setUpBeforeAll() {
        PostgresContainers.setShardEnv();
    }

    @BeforeEach
    void setUp() {
        // no op - in case we need to add something later
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        PostgresContainers.resetProxies();
    }

}
