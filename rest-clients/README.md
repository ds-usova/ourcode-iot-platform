# Rest Clients

A project for managing and publishing Rest Clients to Nexus. 

## Project Structure

Open API interface files are located in the `src/main/resources/interface/` directory. The project structure is as follows:

```plaintext
rest-clients/
├── src/main/resources/interface/
│   └── device-service.yaml
└── README.md
```

## Setup Instructions

### Prerequisites

- Git
- Docker
- Nexus is running (see docker-compose.yaml)

### Generate Rest Clients

To generate Rest Clients and publish them to Nexus, run "make publish-libraries" command from the root directory.