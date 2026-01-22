# Failed Events Processor

## Architecture Overview

Failed Events Processor is a microservice designed to handle and process events that have failed during their initial processing in a data pipeline. 
It listens to designated Kafka topics for failed events and stores them in an object storage for further analysis.

The service consists of the following components:

- **Data Storage**: MinIO
- **Messaging**: Kafka with Schema Registry
- **Observability**: Prometheus, Grafana

## Context Diagram

![Diagram](architecture/diagrams/image/context-diagram.png)

## Container Diagram

![Diagram](architecture/diagrams/image/container-diagram.png)

## Device Processing Flow

![Diagram](architecture/diagrams/image/process-device-dlt.png)

## Project Structure
```plaintext
failed-events-processor/
├── architecture/
│   └── diagrams/                    # C4 diagrams
│       ├── image/                   # Images generated from PlantUML
│       ├── consume-device-dlt.puml
│       ├── containers.puml
│       └── context.puml
├── src/main/
│   ├── java/
│   │   └── org/ourcode/failedevents/
│   │       ├── api/                 # Service API (doesn't depend on any other layers)
│   │       │   ├── exception/
│   │       │   ├── gateway/         # Gateway interfaces (data providers/consumers)
│   │       │   ├── model/           # Model classes
│   │       │   └── service/         # Business logic interfaces
│   │       ├── application/         # Business logic implementations
│   │       ├── kafka/               
│   │       │   ├── configuration/
│   │       │   ├── consumer/
│   │       │   ├── health/
│   │       │   └── producer/
│   │       ├── metrics/               
│   │       ├── minio/
│   │       │   ├── configuration/
│   │       │   └── health/
│   │       ├── retry/               
│   │       └── FailedEventsProcessorApplication.java
│   └── resources/
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker
- Install Kafka plugin for your IDE (e.g., IntelliJ IDEA)
- Run Nexus (see README in root folder)
- (Optional) Switch to Linux terminal to run make commands if you're on Windows

### Starting the Platform

To start local environment with Kafka and MinIO, run:

```bash
cd ..
make start-env-failed-events-processor
```

To start the Failed Events Processor service, run:

```bash
cd ..
make start-failed-events-processor
```

To start Failed Events Processor with observability tools (Prometheus and Grafana), run:

```bash
cd ..
make start-failed-events-processor observability
```

### Smoke Test

* Register events-dlt-value (DeviceEventDeadLetter.avsc) and device-ids-dlt-value (DeviceDeadLetter.avsc) schemas in Schema Registry with Kafka plugin (see src/main/avro)
* Produce test messages to `device-ids-dlt` topic using Kafka plugin or any Kafka producer tool
```json
{
  "deviceId": null,
  "deviceType": null,
  "createdAt": null,
  "meta": null,
  "exception": "DeserializationException",
  "errorMessage": "failed to deserialize",
  "rawEvent": {
    "string": "dGhpcy1tZXNzYWdlLW11c3QtZW5kLXVwLWluLW1pbmlv"
  },
  "$$$SchemaName$$$": "org.ourcode.avro.DeviceDeadLetter"
}
```

* Open MinIO UI at [http://localhost:9001](http://localhost:9001)
* Navigate to `failed-events` bucket
* Verify that a new object with the following object name is created: 
    `device-ids-dlt/DeserializationException/{year}/{month}/{day}/{hour}-{minute}-{second}-{uuid}.json`

### Plans

* Automate schema creation