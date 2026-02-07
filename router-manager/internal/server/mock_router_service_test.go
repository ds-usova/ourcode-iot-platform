package server

import (
	"context"
	"router-manager/internal/service"
)

type MockRouterService struct {
	SubmitCommandFunc      func(ctx context.Context, routerID, commandType, payload string) (*service.SubmitCommandResult, error)
	PollCommandsFunc       func(ctx context.Context, routerID string) ([]service.Command, error)
	AcknowledgeCommandFunc func(ctx context.Context, commandID, routerID string) error
}

func (m *MockRouterService) SubmitCommand(ctx context.Context, routerID, commandType, payload string) (*service.SubmitCommandResult, error) {
	if m.SubmitCommandFunc != nil {
		return m.SubmitCommandFunc(ctx, routerID, commandType, payload)
	}
	return nil, nil
}

func (m *MockRouterService) PollCommands(ctx context.Context, routerID string) ([]service.Command, error) {
	if m.PollCommandsFunc != nil {
		return m.PollCommandsFunc(ctx, routerID)
	}
	return nil, nil
}

func (m *MockRouterService) AcknowledgeCommand(ctx context.Context, commandID, routerID string) error {
	if m.AcknowledgeCommandFunc != nil {
		return m.AcknowledgeCommandFunc(ctx, commandID, routerID)
	}
	return nil
}
