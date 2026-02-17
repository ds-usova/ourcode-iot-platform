package config

import (
	"context"
	"fmt"
	"log"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

type PostgresConfig struct {
	Database       string
	User           string
	Password       string
	Schema         string
	NetworkName    string
	MigrationsPath string
}

// StartPostgresContainer starts a PostgreSQL container with the specified configuration and returns the container instance and connection string.
func StartPostgresContainer(ctx context.Context, config PostgresConfig) (*postgres.PostgresContainer, string, error) {
	req := testcontainers.ContainerRequest{
		NetworkAliases: map[string][]string{
			PostgresBridgeNetwork: {PostgresNetworkAlias},
		},
	}

	// Add custom network if provided
	var networks []string
	if config.NetworkName != "" {
		networks = append(networks, config.NetworkName)
		req.Networks = networks
		req.NetworkAliases = map[string][]string{
			config.NetworkName: {PostgresNetworkAlias},
		}
	}

	pgContainer, err := postgres.Run(ctx,
		PostgresImageVersion,
		postgres.WithDatabase(config.Database),
		postgres.WithUsername(config.User),
		postgres.WithPassword(config.Password),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).
				WithStartupTimeout(PostgresStartupTimeout),
		),
		testcontainers.CustomizeRequest(testcontainers.GenericContainerRequest{
			ContainerRequest: req,
		}),
	)
	if err != nil {
		return nil, "", fmt.Errorf("failed to start container: %w", err)
	}

	connString, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		return nil, "", fmt.Errorf("failed to get connection string: %w", err)
	}

	// Append search_path for schema
	connString = fmt.Sprintf("%s&search_path=%s", connString, config.Schema)

	log.Printf("PostgreSQL container started with connection string: %s", connString)
	return pgContainer, connString, nil
}
