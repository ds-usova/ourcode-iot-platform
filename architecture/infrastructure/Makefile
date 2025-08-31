.PHONY: up down reset

up:
	@echo "Starting services..."
	docker compose up -d

down:
	@echo "Stopping services..."
	docker compose down

reset:
	@echo "Resetting services..."
	docker compose down -v
	docker compose up -d

start-env-event-collector:
	@echo "Starting environment event collector..."
	docker compose up -d kafka kafka-init schema-registry cassandra cassandra-load-keyspace