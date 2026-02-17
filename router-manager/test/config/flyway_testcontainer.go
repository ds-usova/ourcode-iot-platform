package config

import (
	"context"
	"fmt"
	"log"
	"time"

	"path/filepath"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// RunFlywayMigrations executes Flyway migrations
func RunFlywayMigrations(ctx context.Context, pgContainer *postgres.PostgresContainer, config PostgresConfig) error {
	networks, err := pgContainer.Networks(ctx)
	if err != nil {
		return fmt.Errorf("failed to get postgres networks: %w", err)
	}

	flywayContainer, err := createFlywayContainer(ctx, networks, config)
	if err != nil {
		return err
	}

	if err := flywayContainer.Start(ctx); err != nil {
		return fmt.Errorf("failed to start Flyway container: %w", err)
	}

	if err := waitForMigrationCompletion(ctx, flywayContainer); err != nil {
		return err
	}

	if err := verifyMigrationSuccess(ctx, flywayContainer); err != nil {
		return err
	}

	if err := flywayContainer.Terminate(ctx); err != nil {
		log.Printf("Warning: failed to terminate Flyway container: %v", err)
	}

	log.Println("Flyway migrations completed successfully")
	return nil
}

// createFlywayContainer creates and configures a Flyway container
func createFlywayContainer(ctx context.Context, networks []string, config PostgresConfig) (testcontainers.Container, error) {
	dbUrl := fmt.Sprintf("jdbc:postgresql://%s:%d/%s?currentSchema=%s", PostgresNetworkAlias, FlywayPostgresPort, config.Database, config.Schema)

	log.Printf("Flyway migrations path: %s", config.MigrationsPath)
	log.Printf("Flyway connecting to database at: %s", dbUrl)

	req := buildFlywayContainerRequest(networks, config, dbUrl)

	flywayContainer, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: req,
		Started:          false,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create Flyway container: %w", err)
	}

	return flywayContainer, nil
}

// buildFlywayContainerRequest creates the container request configuration
func buildFlywayContainerRequest(networks []string, config PostgresConfig, dbUrl string) testcontainers.ContainerRequest {
	return testcontainers.ContainerRequest{
		Image: FlywayImageVersion,
		Cmd: []string{
			"-connectRetries=10",
			"-baselineOnMigrate=true",
			"-locations=filesystem:/flyway/sql",
			"migrate",
		},
		Env: map[string]string{
			"FLYWAY_URL":      dbUrl,
			"FLYWAY_USER":     config.User,
			"FLYWAY_PASSWORD": config.Password,
			"FLYWAY_SCHEMAS":  config.Schema,
		},
		Mounts: testcontainers.Mounts(
			testcontainers.BindMount(filepath.ToSlash(config.MigrationsPath), "/flyway/sql"),
		),
		Networks: networks,
	}
}

// waitForMigrationCompletion waits for the Flyway container to finish running
func waitForMigrationCompletion(ctx context.Context, container testcontainers.Container) error {
	log.Println("Waiting for Flyway migrations to complete...")
	waitCtx, cancel := context.WithTimeout(ctx, FlywayMigrationTimeout)
	defer cancel()

	ticker := time.NewTicker(FlywayPollingInterval)
	defer ticker.Stop()

	for {
		select {
		case <-waitCtx.Done():
			return fmt.Errorf("flyway migration timed out after 60 seconds")
		case <-ticker.C:
			state, err := container.State(ctx)
			if err != nil {
				return fmt.Errorf("failed to get Flyway container state: %w", err)
			}

			if !state.Running {
				return nil
			}
		}
	}
}

// verifyMigrationSuccess checks the exit code and logs the output
func verifyMigrationSuccess(ctx context.Context, container testcontainers.Container) error {
	state, err := container.State(ctx)
	if err != nil {
		return fmt.Errorf("failed to get Flyway container state: %w", err)
	}

	printContainerLogs(ctx, container)

	if state.ExitCode != 0 {
		return fmt.Errorf("flyway migration failed with exit code %d", state.ExitCode)
	}

	return nil
}

// printContainerLogs reads and prints the Flyway container logs
func printContainerLogs(ctx context.Context, container testcontainers.Container) {
	logs, err := container.Logs(ctx)
	if err != nil {
		log.Printf("Warning: failed to get Flyway logs: %v", err)
		return
	}
	defer func() {
		if closeErr := logs.Close(); closeErr != nil {
			log.Printf("Warning: failed to close Flyway logs: %v", closeErr)
		}
	}()

	buf := make([]byte, 1024)
	for {
		n, readErr := logs.Read(buf)
		if n > 0 {
			log.Printf("Flyway: %s", string(buf[:n]))
		}
		if readErr != nil {
			break
		}
	}
}
