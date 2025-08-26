# Event Collector

## Architecture Overview

The Event Collector collects event data from IoT devices, processes it, and stores it in a database for further analysis.

The service consists of the following components:

- **Data Storage**: Cassandra
- **Messaging**: Kafka with Schema Registry
- **Observability**: Prometheus, Grafana

## Context Diagram

![Diagram](architecture/diagrams/image/context-diagram.png)

## Container Diagram

![Diagram](architecture/diagrams/image/container-diagram.png)

## Project Structure

```plaintext
event-collector/
├── architecture/
│   ├── diagrams/                    # C4 diagrams
│   │   ├── image/                   # Images generated from PlantUML
│   │   ├── containers.puml
│   │   └── context.puml
│   └── src/main/
│       ├── avro/                    # Avro schemas
│       ├── java/
│       │   ├── api/                 # Service API (doens't depend on any other layers)
│       │   │   ├── gateway/         # Gateway interfaces (data providers/consumers)
│       │   │   ├── model/           # Model classes
│       │   │   └── service/         # Business logic interfaces
│       │   ├── application/         # Business logic implementations
│       │   ├── cache/               
│       │   ├── cassandra/           
│       │   ├── kafka/               
│       │   └── EventCollectorApplication.java
│       └── resources/
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker

### Starting the Platform

TODO