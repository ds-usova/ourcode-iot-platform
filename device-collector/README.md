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

## Event Processing Flow

{Todo}
![Diagram](architecture/diagrams/image/consume-device-flow.png)

## Project Structure
Todo: update project structure
```plaintext
device-collector/
├── architecture/
│   ├── diagrams/                    # C4 diagrams
│   │   ├── image/                   # Images generated from PlantUML
│   │   ├── containers.puml
│   │   └── context.puml
│   └── src/main/
│       ├── avro/                    # Avro schemas
│       ├── java/
│       │   ├── api/                 # Service API (doesn't depend on any other layers)
│       │   │   ├── exception/
│       │   │   ├── gateway/         # Gateway interfaces (data providers/consumers)
│       │   │   ├── model/           # Model classes
│       │   │   └── service/         # Business logic interfaces
│       │   ├── application/         # Business logic implementations
│       │   ├── cache/               
│       │   ├── kafka/               
│       │   └── DeviceCollectorApplication.java
│       └── resources/
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker
- Install Kafka plugin for your IDE (e.g., IntelliJ IDEA)
- (Optional) Switch to Linux terminal to run make commands if you're on Windows

### Starting the Platform

{Todo}

### Plans

{Todo}