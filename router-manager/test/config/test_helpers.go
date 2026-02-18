package config

import (
	"fmt"
	"log"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// CreateTestRouter inserts a test router into the database for testing purposes
func (env *TestEnvironment) CreateTestRouter(routerID, serialNumber string) error {
	pool, err := pgxpool.New(env.Ctx, env.ConnString)
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}
	defer pool.Close()

	_, err = pool.Exec(env.Ctx, `
		INSERT INTO router.routers (id, serial_number, last_seen_at, created_at)
		VALUES ($1, $2, now(), now())
	`, routerID, serialNumber)
	if err != nil {
		return fmt.Errorf("failed to insert test router: %w", err)
	}

	log.Printf("Successfully created test router: ID=%s, SerialNumber=%s", routerID, serialNumber)
	return nil
}

// VerifyCommandInDatabase checks that a command was created correctly in the database
func (env *TestEnvironment) VerifyCommandInDatabase(commandID, routerID, expectedType, expectedStatus string) error {
	pool, err := pgxpool.New(env.Ctx, env.ConnString)
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}
	defer pool.Close()

	var cmd struct {
		ID          string
		RouterID    string
		CommandType string
		Payload     string
		Status      string
	}

	err = pool.QueryRow(env.Ctx, `
		SELECT id, router_id, command_type, payload::text, status
		FROM router.commands
		WHERE id = $1
	`, commandID).Scan(&cmd.ID, &cmd.RouterID, &cmd.CommandType, &cmd.Payload, &cmd.Status)
	if err != nil {
		return fmt.Errorf("command not found in database: %w", err)
	}

	// Verify command fields
	if cmd.RouterID != routerID {
		return fmt.Errorf("expected router_id '%s', got '%s'", routerID, cmd.RouterID)
	}
	if cmd.CommandType != expectedType {
		return fmt.Errorf("expected command_type '%s', got '%s'", expectedType, cmd.CommandType)
	}
	if cmd.Status != expectedStatus {
		return fmt.Errorf("expected status '%s', got '%s'", expectedStatus, cmd.Status)
	}

	return nil
}

// StringPtr returns a pointer to the given string
func StringPtr(s string) *string {
	return &s
}

func randomString(n int) string {
	const letters = "abcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, n)
	for i := range b {
		b[i] = letters[time.Now().UnixNano()%int64(len(letters))]
	}
	return string(b)
}

// CountCommandsForRouter counts how many commands exist for a specific router in the database
func (env *TestEnvironment) CountCommandsForRouter(routerID string) (int, error) {
	pool, err := pgxpool.New(env.Ctx, env.ConnString)
	if err != nil {
		return 0, fmt.Errorf("failed to connect to database: %w", err)
	}
	defer pool.Close()

	var count int
	err = pool.QueryRow(env.Ctx, `
		SELECT COUNT(*)
		FROM router.commands
		WHERE router_id = $1
	`, routerID).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("failed to count commands: %w", err)
	}

	return count, nil
}

// ClearDatabase deletes all routers and commands from the database
func (env *TestEnvironment) ClearDatabase() error {
	pool, err := pgxpool.New(env.Ctx, env.ConnString)
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}
	defer pool.Close()

	if _, err := pool.Exec(env.Ctx, "TRUNCATE router.commands, router.routers RESTART IDENTITY CASCADE"); err != nil {
		return fmt.Errorf("failed to clear database: %w", err)
	}

	return nil
}
