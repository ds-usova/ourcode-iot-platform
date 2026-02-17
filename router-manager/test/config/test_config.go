package config

import (
	"path/filepath"
	"runtime"
	"time"
)

// TestDatabaseConfig holds database configuration constants used across all system tests
type TestDatabaseConfig struct {
	DBName   string
	User     string
	Password string
	Schema   string
}

// DefaultTestDBConfig returns the default database configuration for system tests
var DefaultTestDBConfig = TestDatabaseConfig{
	DBName:   "our_code_db",
	User:     "test_user",
	Password: "test_password",
	Schema:   "router",
}

// MigrationsPath is the absolute path to the Flyway migrations directory
var MigrationsPath = func() string {
	_, filename, _, _ := runtime.Caller(0)
	dir := filepath.Dir(filename)
	return filepath.Join(dir, "..", "..", "..", "architecture", "infrastructure", "db", "migrations", "router")
}()

// PostgreSQL container constants
const (
	PostgresImageVersion   = "postgres:17.5"
	PostgresNetworkAlias   = "postgres"
	PostgresBridgeNetwork  = "bridge"
	PostgresStartupTimeout = 60 * time.Second
)

// Flyway container constants
const (
	FlywayImageVersion     = "flyway/flyway:12.0-alpine"
	FlywayPostgresPort     = 5432
	FlywayMigrationTimeout = 60 * time.Second
	FlywayPollingInterval  = 500 * time.Millisecond
)
