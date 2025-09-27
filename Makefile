.PHONY: help up down reset start-env-event-collector start-event-collector start-observability start-env-device-collector publish-avro-schemas

ARGS = $(filter-out $@,$(MAKECMDGOALS))

COMPOSE_FILE := ./architecture/infrastructure/docker-compose.yaml
ARTIFACTORY_HOST := artifactory:8001

help:
	@echo "Makefile commands:"
	@echo "  up                          - Start all services"
	@echo "  down                        - Stop all services"
	@echo "  reset                       - Reset all services (stop, remove volumes, and start)"

	@echo "\n  ===== Build and start services ====="
	@echo "  start-event-collector       - Start event collector and its dependencies"
	@echo "  start-device-collector      - Start event collector and its dependencies"

	@echo "\n  ===== Local Environment Setup ====="
	@echo "  start-env-event-collector   			   - Start local environment for event collector"
	@echo "  start-env-device-collector  			   - Start local environment for device collector"
	@echo "  start-env-device-collector observability  - Start local environment for device collector and observability stack"
	@echo "  start-observability                       - Start observability stack (Prometheus and Grafana)"
	@echo "  start-artifactory                         - Start artifactory"
	@echo "  publish-libraries                         - Publish Avro schemas to artifactory"

# No-op target to avoid errors when no target is specified
%:
	@:

up:
	@echo "Starting services..."
	docker compose -f $(COMPOSE_FILE) up -d

down:
	@echo "Stopping services..."
	docker compose -f $(COMPOSE_FILE) down

reset:
	@echo "Resetting services..."
	docker compose -f $(COMPOSE_FILE) down -v
	docker compose -f $(COMPOSE_FILE) up -d

start-env-event-collector:
	@echo "Starting local environment for event collector..."
	docker compose -f $(COMPOSE_FILE) up -d kafka kafka-init schema-registry cassandra cassandra-load-keyspace

start-event-collector:
	@echo "Starting event collector and required dependencies..."
	docker compose -f $(COMPOSE_FILE) up --build -d event-collector

start-observability:
	@echo "Starting observability stack..."
	docker compose -f $(COMPOSE_FILE) up -d prometheus grafana

start-env-device-collector: start-artifactory
	@echo "Starting local environment for device collector..."

	docker compose -f $(COMPOSE_FILE) up -d \
		kafka kafka-init schema-registry \
		postgres_shard_0 postgres_shard_1 \
		postgres_shard_0_replica postgres_shard_1_replica

start-device-collector: start-artifactory
	@echo "Starting device collector and required dependencies..."

	docker compose -f $(COMPOSE_FILE) up --build -d device-collector
	@if [ "$(ARGS)" = "observability" ]; then \
  			echo "Starting device collector exporters..."; \
    		$(MAKE) start-observability; \
    		docker compose -f $(COMPOSE_FILE) up -d \
    			postgres-exporter-shard-0 \
    			postgres-exporter-shard-1 \
    			postgres-exporter-shard-0-replica \
    			postgres-exporter-shard-1-replica \
    			kafka-exporter; \
    fi

start-artifactory:
	@echo "Starting artifactory..."
	docker compose -f $(COMPOSE_FILE) up -d artifactory

publish-libraries:
	@echo "Publishing to artifactory..."
	docker compose -f $(COMPOSE_FILE) up --build -d avro-schemas
