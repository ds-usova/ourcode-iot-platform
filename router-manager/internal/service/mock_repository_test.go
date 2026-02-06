package service

import (
	"context"
	"router-manager/internal/database"
)

// MockRepository is a mock implementation of database.Repository for testing
type MockRepository struct {
	CreateCommandFunc          func(ctx context.Context, routerID, commandType, payload string) (*database.Command, error)
	BroadcastCommandFunc       func(ctx context.Context, commandType, payload string) (int64, error)
	GetOutstandingCommandsFunc func(ctx context.Context, routerID string) ([]database.Command, error)
	AcknowledgeCommandFunc     func(ctx context.Context, commandID, routerID string) error
	TouchRouterFunc            func(ctx context.Context, routerID string) error
}

func (m *MockRepository) CreateCommand(ctx context.Context, routerID, commandType, payload string) (*database.Command, error) {
	if m.CreateCommandFunc != nil {
		return m.CreateCommandFunc(ctx, routerID, commandType, payload)
	}
	return nil, nil
}

func (m *MockRepository) BroadcastCommand(ctx context.Context, commandType, payload string) (int64, error) {
	if m.BroadcastCommandFunc != nil {
		return m.BroadcastCommandFunc(ctx, commandType, payload)
	}
	return 0, nil
}

func (m *MockRepository) GetOutstandingCommands(ctx context.Context, routerID string) ([]database.Command, error) {
	if m.GetOutstandingCommandsFunc != nil {
		return m.GetOutstandingCommandsFunc(ctx, routerID)
	}
	return nil, nil
}

func (m *MockRepository) AcknowledgeCommand(ctx context.Context, commandID, routerID string) error {
	if m.AcknowledgeCommandFunc != nil {
		return m.AcknowledgeCommandFunc(ctx, commandID, routerID)
	}
	return nil
}

func (m *MockRepository) TouchRouter(ctx context.Context, routerID string) error {
	if m.TouchRouterFunc != nil {
		return m.TouchRouterFunc(ctx, routerID)
	}
	return nil
}
