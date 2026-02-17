package config

import (
	"context"
	"router-manager/test/config"
)

// SetupIntegrationTest initializes only the database layer for integration tests:
// - PostgreSQL container
// - Flyway migrations
// - Database connection
// Returns a TestEnvironment with cleanup function
func SetupIntegrationTest(ctx context.Context) (*config.TestEnvironment, error) {
	return config.SetupBaseEnvironment(ctx)
}
