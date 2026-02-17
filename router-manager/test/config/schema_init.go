package config

import (
	"context"
	"fmt"
	"log"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// InitializeSchema creates the database schema and runs Flyway migrations
func InitializeSchema(ctx context.Context, pgContainer *postgres.PostgresContainer, config PostgresConfig) error {
	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		return fmt.Errorf("failed to get connection string: %w", err)
	}

	if err := createDatabaseSchema(ctx, connStr, config.Schema); err != nil {
		return err
	}

	if err := executeMigrations(ctx, pgContainer, config); err != nil {
		return err
	}

	log.Println("Schema initialized successfully")
	return nil
}

// createDatabaseSchema connects to the database and creates the specified schema if it doesn't exist
func createDatabaseSchema(ctx context.Context, connStr string, schemaName string) error {
	pool, err := pgxpool.New(ctx, connStr)
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}
	defer pool.Close()

	query := fmt.Sprintf("CREATE SCHEMA IF NOT EXISTS %s", schemaName)
	if _, err := pool.Exec(ctx, query); err != nil {
		return fmt.Errorf("failed to create schema: %w", err)
	}

	log.Printf("Schema '%s' ensured in the database", schemaName)
	return nil
}

// executeMigrations runs the Flyway migrations using the testcontainer
func executeMigrations(ctx context.Context, pgContainer *postgres.PostgresContainer, config PostgresConfig) error {
	log.Println("Running Flyway migrations...")
	if err := RunFlywayMigrations(ctx, pgContainer, config); err != nil {
		return fmt.Errorf("failed to run Flyway migrations: %w", err)
	}
	return nil
}
