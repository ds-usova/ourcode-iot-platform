# Device Collector

## Architecture Overview

Device Collector is a microservice designed to collect and process data from various IoT devices. It ingests data through Kafka topics, processes the information, and stores it in a PostgreSQL database.

The service consists of the following components:

- **Data Storage**: PostgreSQL database (two shards with read replicas), Sharding Sphere JDBC
- **Messaging**: Kafka with Schema Registry
- **Observability**: Prometheus, Grafana

## Context Diagram

![Diagram](architecture/diagrams/image/context-diagram.png)

## Container Diagram

![Diagram](architecture/diagrams/image/container-diagram.png)

## Device Processing Flow

![Diagram](architecture/diagrams/image/consume-device-flow.png)

## Project Structure
```plaintext
device-collector/
├── architecture/
│   ├── diagrams/                    # C4 diagrams
│   │   ├── image/                   # Images generated from PlantUML
│   │   ├── consume-device-flow.puml
│   │   ├── containers.puml
│   │   └── context.puml
│   └── src/main/
│       ├── java/
│       │   ├── api/                 # Service API (doesn't depend on any other layers)
│       │   │   ├── events/          # Application events
│       │   │   ├── exception/
│       │   │   ├── gateway/         # Gateway interfaces (data providers/consumers)
│       │   │   ├── model/           # Model classes
│       │   │   └── service/         # Business logic interfaces
│       │   ├── application/         # Business logic implementations
│       │   ├── kafka/               
│       │   ├── metrics/               
│       │   ├── persistence/               
│       │   └── DeviceCollectorApplication.java
│       └── resources/
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker
- Install Kafka plugin for your IDE (e.g., IntelliJ IDEA)
- Create Artifactory repository (see README in root folder)
- (Optional) Switch to Linux terminal to run make commands if you're on Windows

### Starting the Platform

To start local environment with Kafka and PostgreSQL, run:

```bash
cd ..
make start-env-device-collector
```

To start the Device Collector service, run:

```bash
cd ..
make start-device-collector
```

To start Device Collector with observability tools (Prometheus and Grafana), run:

```bash
cd ..
make start-device-collector observability
```

### Smoke Test

* Register device-ids-value (Device.avsc) and device-ids-dlt-value (DeviceDeadLetter.avsc) schemas in Schema Registry with Kafka plugin (see src/main/avro)
* Produce test messages to `device-ids` topic using Kafka plugin or any Kafka producer tool
```json
{
  "deviceId" : "\bRrIj/h\u0018hA,;",
  "deviceType" : {
    "string" : "\u0002\u0016= .@0j6b"
  },
  "createdAt" : {
    "long" : 8625999633872044475
  },
  "meta" : {
    "string" : "\u0002\u0016= .@0j6b"
  }
}
```

* Open Device Collector dashboard in [Grafana](http://localhost:3000/dashboards)
* Verify that "Number of successfully processed devices" is 1

### Plans

* Automate schema creation
* Implement hot sharding rebalancing