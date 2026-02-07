package service

import (
	"context"
	"errors"
	"router-manager/internal/database"
	"testing"
)

const (
	testRouterID    = "router-123"
	testCommandType = "reboot"
	testPayload     = "{\"version\":\"1.2.3\"}"
)

// TestSubmitCommand_SingleRouterSuccess tests successful command submission to a specific router
func TestSubmitCommand_SingleRouterSuccess(t *testing.T) {
	// Given: a valid router ID, command type, and payload
	expectedCommand := &database.Command{
		ID:          "cmd-789",
		RouterID:    testRouterID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	mockRepo := &MockRepository{
		CreateCommandFunc: func(ctx context.Context, routerID, commandType, payload string) (*database.Command, error) {
			if routerID != testRouterID {
				t.Errorf("Expected routerID '%s', got '%s'", testRouterID, routerID)
			}
			if commandType != testCommandType {
				t.Errorf("Expected commandType '%s', got '%s'", testCommandType, commandType)
			}
			if payload != testPayload {
				t.Errorf("Expected payload '%s', got '%s'", testPayload, payload)
			}
			return expectedCommand, nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: SubmitCommand is called with a specific router ID
	result, err := service.SubmitCommand(context.Background(), testRouterID, testCommandType, testPayload)

	// Then: SubmitCommand should return the command result without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if result == nil {
		t.Fatal("Expected result, got nil")
	}
	if result.CommandID != expectedCommand.ID {
		t.Errorf("Expected CommandID '%s', got '%s'", expectedCommand.ID, result.CommandID)
	}
	if result.RoutersAffected != 1 {
		t.Errorf("Expected RoutersAffected 1, got %d", result.RoutersAffected)
	}
	if result.IsBroadcast {
		t.Error("Expected IsBroadcast to be false")
	}
}

// TestSubmitCommand_BroadcastSuccess tests successful command broadcast to all routers
func TestSubmitCommand_BroadcastSuccess(t *testing.T) {
	// Given: empty router ID (indicating broadcast) and expected count of routers affected
	expectedCount := int64(5)

	mockRepo := &MockRepository{
		BroadcastCommandFunc: func(ctx context.Context, commandType, payload string) (int64, error) {
			if commandType != testCommandType {
				t.Errorf("Expected commandType '%s', got '%s'", testCommandType, commandType)
			}
			if payload != testPayload {
				t.Errorf("Expected payload '%s', got '%s'", testPayload, payload)
			}
			return expectedCount, nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: SubmitCommand is called with empty router ID
	result, err := service.SubmitCommand(context.Background(), "", testCommandType, testPayload)

	// Then: SubmitCommand should return broadcast result without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if result == nil {
		t.Fatal("Expected result, got nil")
	}
	if result.CommandID != "" {
		t.Errorf("Expected empty CommandID for broadcast, got '%s'", result.CommandID)
	}
	if result.RoutersAffected != expectedCount {
		t.Errorf("Expected RoutersAffected %d, got %d", expectedCount, result.RoutersAffected)
	}
	if !result.IsBroadcast {
		t.Error("Expected IsBroadcast to be true")
	}
}

// TestSubmitCommand_SingleRouterError tests error handling when CreateCommand fails
func TestSubmitCommand_SingleRouterError(t *testing.T) {
	// Given: CreateCommand fails with an error
	expectedErr := database.ErrRouterNotFound

	mockRepo := &MockRepository{
		CreateCommandFunc: func(ctx context.Context, routerID, commandType, payload string) (*database.Command, error) {
			return nil, expectedErr
		},
	}

	service := NewRouterService(mockRepo)

	// When: SubmitCommand is called with a specific router ID
	result, err := service.SubmitCommand(context.Background(), testRouterID, testCommandType, testPayload)

	// Then: Error should be returned and result should be nil
	if err == nil {
		t.Error("Expected error, got nil")
	}
	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}
	if result != nil {
		t.Errorf("Expected nil result, got %v", result)
	}
}

// TestSubmitCommand_BroadcastError tests error handling when BroadcastCommand fails
func TestSubmitCommand_BroadcastError(t *testing.T) {
	// Given: BroadcastCommand fails with an error
	expectedErr := errors.New("database connection failed")

	mockRepo := &MockRepository{
		BroadcastCommandFunc: func(ctx context.Context, commandType, payload string) (int64, error) {
			return 0, expectedErr
		},
	}

	service := NewRouterService(mockRepo)

	// When: SubmitCommand is called with empty router ID
	result, err := service.SubmitCommand(context.Background(), "", testCommandType, testPayload)

	// Then: Error should be returned and result should be nil
	if err == nil {
		t.Error("Expected error, got nil")
	}
	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}
	if result != nil {
		t.Errorf("Expected nil result, got %v", result)
	}
}
