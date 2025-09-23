.PHONY: help up down reset start-env-event-collector start-event-collector start-observability start-env-device-collector

COMPOSE_FILE := ./architecture/infrastructure/docker-compose.yaml

help:
	@echo "Makefile commands:"
	@echo "  up                          - Start all services"
	@echo "  down                        - Stop all services"
	@echo "  reset                       - Reset all services (stop, remove volumes, and start)"
	@echo "  start-env-event-collector   - Start local environment for event collector"
	@echo "  start-event-collector       - Start event collector and its dependencies"
	@echo "  start-env-device-collector  - Start local environment for device collector"
	@echo "  start-observability         - Start observability stack (Prometheus and Grafana)"

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

start-env-device-collector:
	@echo "Starting local environment for device collector..."
	docker compose -f $(COMPOSE_FILE) up -d kafka kafka-init schema-registry postgres_shard_0 postgres_shard_1 postgres_shard_0_replica postgres_shard_1_replica