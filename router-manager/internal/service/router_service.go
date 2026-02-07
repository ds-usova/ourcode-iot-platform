package service

import (
	"context"
	"router-manager/internal/database"
)

type RouterService struct {
	repo database.Repository
}

func NewRouterService(repo database.Repository) *RouterService {
	return &RouterService{repo: repo}
}

type Command struct {
	ID          string
	RouterID    string
	CommandType string
	Payload     string
}

// SubmitCommandResult represents the result of a send command operation
type SubmitCommandResult struct {
	CommandID       string
	RoutersAffected int64
	IsBroadcast     bool
}

// SubmitCommand sends a command to a specific router or broadcasts to all routers if routerID is empty.
// Returns the command result with ID (for single router) or count (for broadcast)
func (s *RouterService) SubmitCommand(ctx context.Context, routerID, commandType, payload string) (*SubmitCommandResult, error) {
	if routerID == "" {
		count, err := s.repo.BroadcastCommand(ctx, commandType, payload)
		if err != nil {
			return nil, err
		}
		return &SubmitCommandResult{
			CommandID:       "",
			RoutersAffected: count,
			IsBroadcast:     true,
		}, nil
	}

	// Send to specific router
	cmd, err := s.repo.CreateCommand(ctx, routerID, commandType, payload)
	if err != nil {
		return nil, err
	}
	return &SubmitCommandResult{
		CommandID:       cmd.ID,
		RoutersAffected: 1,
		IsBroadcast:     false,
	}, nil
}

// PollCommands retrieves outstanding commands for a router and updates its last seen timestamp
func (s *RouterService) PollCommands(ctx context.Context, routerID string) ([]Command, error) {
	if err := s.repo.TouchRouter(ctx, routerID); err != nil {
		return nil, err
	}

	dbCommands, err := s.repo.GetOutstandingCommands(ctx, routerID)
	if err != nil {
		return nil, err
	}

	commands := make([]Command, len(dbCommands))
	for i, cmd := range dbCommands {
		commands[i] = Command{
			ID:          cmd.ID,
			RouterID:    cmd.RouterID,
			CommandType: cmd.CommandType,
			Payload:     cmd.Payload,
		}
	}

	return commands, nil
}

// AcknowledgeCommand marks a command as acknowledged and updates router's last seen timestamp
func (s *RouterService) AcknowledgeCommand(ctx context.Context, commandID, routerID string) error {
	if err := s.repo.TouchRouter(ctx, routerID); err != nil {
		return err
	}

	return s.repo.AcknowledgeCommand(ctx, commandID, routerID)
}
