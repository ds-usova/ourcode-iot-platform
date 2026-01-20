# Avro Schemas

A project for managing and publishing Avro schemas used in the project.

## Project Structure

Avro schemas are stored in the `src/main/avro` directory. Each schema is organized into subdirectories based on the topic.

```plaintext
avro-schemas/
├── src/main/avro/
│   ├── device-ids/
│   └── events/
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker
- Nexus is running (see docker-compose.yaml)

### Generate Avro Classes

To generate Avro classes from the schemas, build the project with Gradle (build) and publish the schemas to Nexus (publish).