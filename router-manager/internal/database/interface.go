package database

import "context"

// CommandRepository command-related database operations
type CommandRepository interface {
	// CreateCommand inserts a new command into the database for a specific router
	CreateCommand(ctx context.Context, routerID, commandType, payload string) (*Command, error)

	// BroadcastCommand creates a command for all registered routers in the system
	BroadcastCommand(ctx context.Context, commandType, payload string) (int64, error)

	// GetOutstandingCommands retrieves all commands for a specific router that are in 'PENDING' or 'SENT' status
	GetOutstandingCommands(ctx context.Context, routerID string) ([]Command, error)

	// AcknowledgeCommand updates the status of a command to 'ACKED'
	AcknowledgeCommand(ctx context.Context, commandID, routerID string) error
}

// RouterRepository router-related database operations
type RouterRepository interface {
	// TouchRouter updates the last_seen_at timestamp of a router to the current time
	TouchRouter(ctx context.Context, routerID string) error
}

type Repository interface {
	CommandRepository
	RouterRepository
}
