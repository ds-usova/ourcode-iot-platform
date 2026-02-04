package database

import (
	"context"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
)

type Command struct {
	Id             string
	RouterID       string
	CommandType    string
	Payload        string
	Status         string
	CreatedAt      time.Time
	SentAt         time.Time
	AcknowledgedAt *time.Time
}

var (
	ErrRouterNotFound = fmt.Errorf("router not found")
)

// CreateCommand inserts a new command into the database
func (db *DB) CreateCommand(ctx context.Context, routerID, commandType, payload string) (*Command, error) {
	tx, err := db.Pool.Begin(ctx)
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
		&cmd.Id,
		&cmd.RouterID,
		&cmd.CommandType,
		&cmd.Payload,
		&cmd.Status,
		&cmd.CreatedAt,
	)

	if err != nil {
		log.Printf("Error inserting command: %v", err)

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

	log.Printf("Created command: %v", cmd)
	return &cmd, nil
}
