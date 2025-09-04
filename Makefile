.PHONY: help up down reset start-env-event-collector start-event-collector start-observability

COMPOSE_FILE := ./architecture/infrastructure/docker-compose.yaml

help:
	@echo "Makefile commands:"
	@echo "  up                          - Start all services"
	@echo "  down                        - Stop all services"
	@echo "  reset                       - Reset all services (stop, remove volumes, and start)"
	@echo "  start-env-event-collector   - Start environment event collector and its dependencies"
	@echo "  start-event-collector       - Start event collector and its dependencies"
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
	@echo "Starting environment event collector..."
	docker compose -f $(COMPOSE_FILE) up -d kafka kafka-init schema-registry cassandra cassandra-load-keyspace

start-event-collector:
	@echo "Starting event collector and required dependencies..."
	docker compose -f $(COMPOSE_FILE) up --build -d event-collector

start-observability:
	@echo "Starting observability stack..."
	docker compose -f $(COMPOSE_FILE) up -d prometheus grafana