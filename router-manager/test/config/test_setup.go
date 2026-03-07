package config

import (
	"context"
	"fmt"
	"log/slog"

	"router-manager/internal/database"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// TestEnvironment holds the base resources needed for tests (database, network)
type TestEnvironment struct {
	PgContainer *postgres.PostgresContainer
	ConnString  string
	DB          *database.DB
	Ctx         context.Context
	Network     testcontainers.Network
	Cleanup     func()
}

// SetupBaseEnvironment initializes the base infrastructure (network, postgres, migrations, connection)
func SetupBaseEnvironment(ctx context.Context) (*TestEnvironment, error) {
	dbConfig := DefaultTestDBConfig

	network, networkName, err := CreateTestNetwork(ctx)
	if err != nil {
		return nil, err
	}

	pgContainer, connString, err := SetupPostgresContainer(ctx, dbConfig, networkName)
	if err != nil {
		_ = network.Remove(ctx)
		return nil, err
	}

	if err := InitializeDatabaseSchema(ctx, pgContainer, dbConfig, networkName); err != nil {
		_ = pgContainer.Terminate(ctx)
		_ = network.Remove(ctx)
		return nil, err
	}

	db, err := CreateDatabaseConnection(ctx, connString, pgContainer, network)
	if err != nil {
		return nil, err
	}

	cleanup := CreateCleanupFunction(db, pgContainer, network, ctx)

	return &TestEnvironment{
		PgContainer: pgContainer,
		ConnString:  connString,
		DB:          db,
		Ctx:         ctx,
		Network:     network,
		Cleanup:     cleanup,
	}, nil
}

// CreateTestNetwork creates a custom network for test isolation
func CreateTestNetwork(ctx context.Context) (testcontainers.Network, string, error) {
	networkName := fmt.Sprintf("router-test-net-%s", randomString(8))
	network, err := testcontainers.GenericNetwork(ctx, testcontainers.GenericNetworkRequest{
		NetworkRequest: testcontainers.NetworkRequest{
			Name: networkName,
		},
	})
	if err != nil {
		return nil, "", fmt.Errorf("failed to create network: %w", err)
	}
	return network, networkName, nil
}

// SetupPostgresContainer starts the PostgreSQL container
func SetupPostgresContainer(ctx context.Context, dbConfig TestDatabaseConfig, networkName string) (*postgres.PostgresContainer, string, error) {
	pgConfig := PostgresConfig{
		Database:       dbConfig.DBName,
		User:           dbConfig.User,
		Password:       dbConfig.Password,
		Schema:         dbConfig.Schema,
		NetworkName:    networkName,
		MigrationsPath: MigrationsPath,
	}
	pgContainer, connString, err := StartPostgresContainer(ctx, pgConfig)
	if err != nil {
		return nil, "", fmt.Errorf("failed to start PostgreSQL container: %w", err)
	}
	return pgContainer, connString, nil
}

// InitializeDatabaseSchema runs Flyway migrations
func InitializeDatabaseSchema(ctx context.Context, pgContainer *postgres.PostgresContainer, dbConfig TestDatabaseConfig, networkName string) error {
	pgConfig := PostgresConfig{
		Database:       dbConfig.DBName,
		User:           dbConfig.User,
		Password:       dbConfig.Password,
		Schema:         dbConfig.Schema,
		NetworkName:    networkName,
		MigrationsPath: MigrationsPath,
	}
	if err := InitializeSchema(ctx, pgContainer, pgConfig); err != nil {
		if termErr := pgContainer.Terminate(ctx); termErr != nil {
			return fmt.Errorf("failed to initialize schema: %w (also failed to terminate container: %v)", err, termErr)
		}
		return fmt.Errorf("failed to initialize schema: %w", err)
	}
	return nil
}

// CreateDatabaseConnection establishes connection to the database
func CreateDatabaseConnection(ctx context.Context, connString string, pgContainer *postgres.PostgresContainer, network testcontainers.Network) (*database.DB, error) {
	db, err := database.New(connString)
	if err != nil {
		if termErr := pgContainer.Terminate(ctx); termErr != nil {
			return nil, fmt.Errorf("failed to connect to database: %w (also failed to terminate container: %v)", err, termErr)
		}
		if netErr := network.Remove(ctx); netErr != nil {
			return nil, fmt.Errorf("failed to connect to database: %w (also failed to remove network: %v)", err, netErr)
		}
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}
	return db, nil
}

// CreateCleanupFunction returns a function that cleans up all test resources
func CreateCleanupFunction(db *database.DB, pgContainer *postgres.PostgresContainer, network testcontainers.Network, ctx context.Context) func() {
	return func() {
		if db != nil {
			db.Close()
		}
		if pgContainer != nil {
			if err := pgContainer.Terminate(ctx); err != nil {
				slog.Warn("failed to terminate PostgreSQL container", "error", err)
			}
		}
		if network != nil {
			if err := network.Remove(ctx); err != nil {
				slog.Warn("failed to remove test network", "error", err)
			}
		}
	}
}
