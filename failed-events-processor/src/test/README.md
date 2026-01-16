# Test Directory Structure

This guide outlines the structure and organization of the test directory for the `failed-events-processor` service.

## Table of Contents
- [Quick Reference for LLM Test Generation](#quick-reference-for-llm-test-generation)
- [Directory Layout](#directory-layout)
- [Test Types](#test-types)
- [Writing New Tests](#writing-new-tests)
- [Test Best Practices](#test-best-practices)
- [Test Containers](#test-containers)
- [Test Coverage](#test-coverage)
- [File Naming and Package Conventions](#file-naming-and-package-conventions)

## Quick Reference for LLM Test Generation

**When generating tests, always:**
1. Use the templates provided in the "Writing New Tests" section.
2. Follow the naming convention: `{scenario} - when {condition} - then {expected outcome}`.
3. Include Given-When-Then comments in test bodies.
4. Use AssertJ assertions (`assertThat()`) instead of JUnit assertions.
5. Mock dependencies with Mockito for unit tests.
6. Extend `AbstractIntegrationTest` for integration tests.
7. Include comprehensive Javadoc for integration/system tests. Javadoc should describe the Given-When-Then scenario.

## Directory Layout

```
test/
├── java/
│   ├── common/                                 # Shared test utilities
│   │   ├── AbstractIntegrationTest.java        # Base class for integration tests
│   │   ├── SchemaManager.java                  # Kafka schema registration
│   │   ├── TestMinioClient.java                # MinIO test client utilities
│   │   ├── TestProducers.java                  # Test Kafka producers
│   │   └── containers/                         # TestContainer configurations
│   │       ├── KafkaContainer.java
│   │       ├── MinioContainer.java
│   │       ├── Network.java
│   │       ├── SchemaRegistryContainer.java
│   │       └── ToxiproxyContainer.java
│   │
│   ├── integration/                            # Integration tests
│   │   ├── health/
│   │   │   ├── KafkaHealthIntegrationTest.java
│   │   │   └── MinioHealthIntegrationTest.java
│   │   └── minio/
│   │       └── MinioFailedEventGatewayIntegrationTest.java  
│   │
│   ├── org/ourcode/failedevents/               # Unit tests (mirror src structure)
│   │   └── minio/
│   │       ├── MinioFailedEventGatewayUnitTest.java
│   │       └── ObjectNameGeneratorTest.java        
│   │
│   └── system_test/                            # End-to-end system tests
│       ├── DeviceDltCollectionTest.java
│       ├── EventDltCollectionTest.java
│       └── ToxicTest.java
│
└── resources/
    ├── application-test.yaml                   # Test configuration
    └── logback-test.xml                        # Test logging configuration
```

## Test Types

### Decision Tree: Which Test Type?

```
┌─────────────────────────────────────┐
│ What are you testing?               │
└─────────────────────────────────────┘
                 │
         ┌───────┴────────┐
         │                │
    Single class     Multiple components
    with mocked      with real external
    dependencies?    systems?
         │                │
     UNIT TEST      ┌─────┴─────────┐
                    │               │
              Single service   Full workflow
              integration?     end-to-end?
                    │               │
               INTEGRATION     SYSTEM TEST
                   TEST
```

### 1. Unit Tests (`org/ourcode/failedevents/*`)
- **Purpose**: Fast, isolated tests with mocked dependencies
- **Location**: Mirrors main source structure
- **Dependencies**: Mockito for mocking
- **Execution Time**: Milliseconds per test
- **Docker Required**: No

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class MinioFailedEventGatewayUnitTest {
    
    @Mock
    private MinioClient minioClient;

    private MinioFailedEventGateway target;
    
}
```

### 2. Integration Tests (`integration/*`)
- **Purpose**: Test gateways/services with real external systems
- **Location**: `integration/` package
- **Dependencies**: TestContainers (MinIO, Kafka, etc.)
- **Execution Time**: Seconds per test
- **Docker Required**: Yes

**Example:**
```java
public class MinioFailedEventGatewayIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private MinioFailedEventGateway target;
    
}
```

### 3. System Tests (`system_test/*`)
- **Purpose**: End-to-end scenarios across multiple components
- **Location**: `system_test/` package
- **Dependencies**: All external services
- **Execution Time**: Seconds to minutes
- **Docker Required**: Yes

## Writing New Tests

### Unit Test Template

```java
package org.ourcode.failedevents.{package};

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private Dependency dependency;

    private MyService target;

    @BeforeEach
    void setUp() {
        target = new MyService(dependency);
    }

    @Test
    @DisplayName("{scenario} - when {condition} - then {expected outcome}")
    void shouldDoSomething() {
        // Given: Setup test data and mock behavior
        when(dependency.method()).thenReturn(value);

        // When: Execute the method under test
        var result = target.doSomething();

        // Then: Assert expected behavior
        assertThat(result).isEqualTo(expected);
        verify(dependency, times(1)).method();
    }
}
```

### Integration Test Template

```java
package integration.{package};

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class MyIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private MyService target;

    /**
     * Given:
     * - Describe preconditions and test data setup
     * <p>
     * When:
     * - Describe the action being tested
     * <p>
     * Then:
     * - Describe expected outcome and verification
     */
    @Test
    @DisplayName("{scenario} - when {condition} - then {expected outcome}")
    void testScenario() {
        // Given: Setup test data
        var input = createTestData();

        // When: Execute operation
        var result = target.process(input);

        // Then: Verify expected outcome
        assertThat(result).isEqualTo(expected);
    }
}
```

## Test Best Practices

### 1. Use Given-When-Then in tests
```java
// Given: Arrange test data
var event = new FailedEvent(...);

// When: Execute the code under test
gateway.save(event);

// Then: Assert expected behavior
assertThat(actualResult).isEqualTo(expectedResult);
```

### 2. Test names follow "{scenario} - when {condition} - then {expected outcome}" pattern.

Typical scenarios to cover:
- happy path
- unhappy path
- edge case

```java
@Test
@DisplayName("happy path - when failed event has JSON payload - then save it correctly")
void saveFailedEventWithJsonPayload() { }
```

### 3. Use AssertJ instead of JUnit assertions for better readability
```java
assertThat(actual).isEqualTo(expected);
assertThat(list).contains(item);
```

### 4. Clean Up Resources
```java
@AfterEach
void tearDown() {
    // Clean up test data
    repository.deleteAll();
}
```

## Test Containers

### Configured Containers

Tests use the following test containers (test/java/common/containers/):

| Container       | Purpose                    |
|-----------------|----------------------------|
| MinIO           | Object storage             |
| Kafka           | Message broker             |
| Schema Registry | Avro schemas               |
| Toxiproxy       | Network failure simulation |

### Container Lifecycle

- Containers start once before all tests (singleton pattern)
- Containers are reused across test classes

## Test Coverage

View [coverage reports](../../build/reports/jacoco/test/html/index.html).

**Target Coverage**: 90% minimum (unit + integration + system tests)

## File Naming and Package Conventions

### Unit Tests
- **Package**: Mirror the source package structure under `org/ourcode/failedevents/`
- **Class Name**: `{ClassName}Test` or `{ClassName}UnitTest`
- **Example**: `MinioFailedEventGateway.java` → `org/ourcode/failedevents/minio/MinioFailedEventGatewayUnitTest.java`

### Integration Tests
- **Package**: Use feature-based packages under `integration/`
- **Class Name**: `{ClassName}IntegrationTest`
- **Example**: `integration/minio/MinioFailedEventGatewayIntegrationTest.java`

### System Tests
- **Package**: `system_test/`
- **Class Name**: `{Feature}Test` (describe the workflow)
- **Example**: `system_test/EventDltCollectionTest.java`