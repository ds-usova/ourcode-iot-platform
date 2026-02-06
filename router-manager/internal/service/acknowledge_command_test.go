package service

import (
	"context"
	"errors"
	"router-manager/internal/database"
	"testing"
)

// TestAcknowledgeCommand_Success tests successful command acknowledgment
func TestAcknowledgeCommand_Success(t *testing.T) {
	// Given: command with ID "cmd-456" exists for router "router-123"
	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			if routerID != "router-123" {
				t.Errorf("Expected routerID 'router-123', got '%s'", routerID)
			}
			return nil
		},
		AcknowledgeCommandFunc: func(ctx context.Context, commandID, routerID string) error {
			if commandID != "cmd-456" {
				t.Errorf("Expected commandID 'cmd-456', got '%s'", commandID)
			}
			if routerID != "router-123" {
				t.Errorf("Expected routerID 'router-123', got '%s'", routerID)
			}
			return nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: AcknowledgeCommand is called with valid command and router IDs
	err := service.AcknowledgeCommand(context.Background(), "cmd-456", "router-123")

	// Then: AcknowledgeCommand should succeed without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
}

// TestAcknowledgeCommand_TouchRouterError tests error handling when TouchRouter fails
func TestAcknowledgeCommand_TouchRouterError(t *testing.T) {
	// Given: TouchRouter fails with an error
	expectedErr := database.ErrRouterNotFound
	ackCommandCalled := false

	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			return expectedErr
		},
		AcknowledgeCommandFunc: func(ctx context.Context, commandID, routerID string) error {
			ackCommandCalled = true
			return nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: AcknowledgeCommand is called
	err := service.AcknowledgeCommand(context.Background(), "cmd-456", "router-123")

	// Then: Error should be returned and AcknowledgeCommand should not be called
	if err == nil {
		t.Error("Expected error, got nil")
	}

	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}

	if ackCommandCalled {
		t.Error("AcknowledgeCommand should not be called when TouchRouter fails")
	}
}

// TestAcknowledgeCommand_AcknowledgeError tests error handling when AcknowledgeCommand fails
func TestAcknowledgeCommand_AcknowledgeError(t *testing.T) {
	// Given: AcknowledgeCommand fails with an error
	expectedErr := database.ErrSentCommandNotFound
	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			return nil
		},
		AcknowledgeCommandFunc: func(ctx context.Context, commandID, routerID string) error {
			return expectedErr
		},
	}

	service := NewRouterService(mockRepo)

	// When: AcknowledgeCommand is called
	err := service.AcknowledgeCommand(context.Background(), "cmd-456", "router-123")

	// Then: Error should be returned
	if err == nil {
		t.Error("Expected error, got nil")
	}

	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}
}
