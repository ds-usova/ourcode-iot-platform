package database

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
)

type Command struct {
	ID             string
	RouterID       string
	CommandType    string
	Payload        string
	Status         string
	CreatedAt      time.Time
	SentAt         time.Time
	AcknowledgedAt *time.Time
}

var (
	ErrRouterNotFound      = fmt.Errorf("router not found")
	ErrSentCommandNotFound = fmt.Errorf("sent command not found")
)

// CreateCommand inserts a new command into the database for a specific router.
// Returns the created Command and an error if the operation fails.
// If the router does not exist, returns ErrRouterNotFound.
func (r *CommandRepo) CreateCommand(ctx context.Context, routerID, commandType, payload string) (*Command, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback(ctx)

	query := `
		INSERT INTO commands (router_id, command_type, payload, status)
		VALUES ($1, $2, $3, 'PENDING')
		RETURNING id, router_id, command_type, payload, status, created_at
	`

	var cmd Command
	err = tx.QueryRow(ctx, query, routerID, commandType, payload).Scan(
		&cmd.ID,
		&cmd.RouterID,
		&cmd.CommandType,
		&cmd.Payload,
		&cmd.Status,
		&cmd.CreatedAt,
	)

	if err != nil {
		slog.Error("failed to insert command", "router_id", routerID, "command_type", commandType, "error", err)

		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) {
			switch pgErr.Code {
			case "23503": // foreign key violation
				return nil, ErrRouterNotFound
			}
		}
		return nil, fmt.Errorf("failed to create command: %w", err)
	}

	err = tx.Commit(ctx)
	if err != nil {
		return nil, err
	}

	slog.Info("command created", "command_id", cmd.ID, "router_id", cmd.RouterID, "command_type", cmd.CommandType)
	return &cmd, nil
}

// BroadcastCommand creates a command for all registered routers in the system.
// Returns the number of commands created (one per router) and an error if the operation fails.
// If no routers are found, returns ErrRouterNotFound.
func (r *CommandRepo) BroadcastCommand(ctx context.Context, commandType, payload string) (int64, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return 0, err
	}
	defer tx.Rollback(ctx)

	query := `
    	INSERT INTO commands (router_id, command_type, payload, status)
		SELECT id, $1, $2, 'PENDING' FROM routers
	`

	cmdTag, err := tx.Exec(ctx, query, commandType, payload)

	if err != nil {
		slog.Error("failed to broadcast command", "command_type", commandType, "error", err)
		return 0, fmt.Errorf("failed to broadcast command: %w", err)
	}

	rowsAffected := cmdTag.RowsAffected()
	if rowsAffected == 0 {
		slog.Warn("broadcast command skipped because no routers were found", "command_type", commandType)
		return 0, ErrRouterNotFound
	}

	err = tx.Commit(ctx)
	if err != nil {
		return 0, err
	}

	slog.Info("broadcast command created", "command_type", commandType, "router_count", rowsAffected)
	return rowsAffected, nil
}

// GetOutstandingCommands retrieves all commands for a specific router that are in 'PENDING' or 'SENT' status.
// It updates the status of these commands to 'SENT' and sets the sent_at timestamp to the current time.
func (r *CommandRepo) GetOutstandingCommands(ctx context.Context, routerID string) ([]Command, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback(ctx)

	updateQuery := `
		UPDATE commands
		SET status = 'SENT', sent_at = NOW()
		WHERE router_id = $1 AND status IN ('PENDING', 'SENT')
		RETURNING id, command_type, payload
	`

	rows, err := tx.Query(ctx, updateQuery, routerID)
	if err != nil {
		slog.Error("failed to query outstanding commands", "router_id", routerID, "error", err)
		return nil, fmt.Errorf("failed to query outstanding commands: %w", err)
	}

	var commands []Command
	for rows.Next() {
		var cmd Command
		err := rows.Scan(&cmd.ID, &cmd.CommandType, &cmd.Payload)
		if err != nil {
			rows.Close()
			slog.Error("failed to scan command row", "router_id", routerID, "error", err)
			return nil, fmt.Errorf("failed to scan command row: %w", err)
		}
		commands = append(commands, cmd)
	}
	rows.Close()

	if err := rows.Err(); err != nil {
		slog.Error("failed while iterating command rows", "router_id", routerID, "error", err)
		return nil, fmt.Errorf("error iterating command rows: %w", err)
	}

	err = tx.Commit(ctx)
	if err != nil {
		return nil, err
	}

	return commands, nil
}

// AcknowledgeCommand updates the status of a command to 'ACKED' and sets the acked_at timestamp to the current time.
func (r *CommandRepo) AcknowledgeCommand(ctx context.Context, commandID string, routerID string) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	query := `
		UPDATE commands
		SET status = 'ACKED', acked_at = NOW()
		WHERE id = $1 AND router_id = $2 AND status = 'SENT'
	`

	cmdTag, err := tx.Exec(ctx, query, commandID, routerID)
	if err != nil {
		slog.Error("failed to acknowledge command", "command_id", commandID, "router_id", routerID, "error", err)
		return fmt.Errorf("failed to acknowledge command: %w", err)
	}

	if cmdTag.RowsAffected() == 0 {
		slog.Warn("no sent command found to acknowledge", "command_id", commandID, "router_id", routerID)
		return ErrSentCommandNotFound
	}

	err = tx.Commit(ctx)
	if err != nil {
		return err
	}

	slog.Info("command acknowledged", "command_id", commandID, "router_id", routerID)
	return nil
}

// TouchRouter updates the last_seen_at timestamp of a router to the current time.
// Returns an error if the operation fails or if the router does not exist.
func (r *RouterRepo) TouchRouter(ctx context.Context, routerID string) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	query := `
		UPDATE routers
		SET last_seen_at = NOW()
		WHERE id = $1
	`

	cmdTag, err := tx.Exec(ctx, query, routerID)
	if err != nil {
		slog.Error("failed to touch router", "router_id", routerID, "error", err)
		return fmt.Errorf("failed to touch router: %w", err)
	}

	if cmdTag.RowsAffected() == 0 {
		slog.Warn("no router found to touch", "router_id", routerID)
		return ErrRouterNotFound
	}

	err = tx.Commit(ctx)
	if err != nil {
		return err
	}

	slog.Info("router touched", "router_id", routerID)
	return nil
}
