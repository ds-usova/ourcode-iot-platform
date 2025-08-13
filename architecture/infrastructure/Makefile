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