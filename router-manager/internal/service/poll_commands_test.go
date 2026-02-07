package service

import (
	"context"
	"errors"
	"router-manager/internal/database"
	"testing"
)

const (
	testPollRouterID = "router-123"
)

// TestPollCommands_Success tests the successful retrieval of outstanding commands for a router
func TestPollCommands_Success(t *testing.T) {
	// Given: expected commands for the router
	expectedCommands := []database.Command{
		{ID: "cmd-1", RouterID: testPollRouterID, CommandType: "reboot", Payload: ""},
		{ID: "cmd-2", RouterID: testPollRouterID, CommandType: "update", Payload: "{\"version\":\"1.2.3\"}"},
	}

	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			if routerID != testPollRouterID {
				t.Errorf("Expected TouchRouter to be called with '%s', got '%s'", testPollRouterID, routerID)
			}
			return nil
		},
		GetOutstandingCommandsFunc: func(ctx context.Context, routerID string) ([]database.Command, error) {
			if routerID != testPollRouterID {
				t.Errorf("Expected GetOutstandingCommands to be called with '%s', got '%s'", testPollRouterID, routerID)
			}
			return expectedCommands, nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: PollCommands is called for the router
	commands, err := service.PollCommands(context.Background(), testPollRouterID)

	// Then: PollCommands should return the list of outstanding commands without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if len(commands) != len(expectedCommands) {
		t.Fatalf("Expected %d commands, got %d", len(expectedCommands), len(commands))
	}

	for i, expectedDBCmd := range expectedCommands {
		actual := commands[i]
		expected := Command{
			ID:          expectedDBCmd.ID,
			RouterID:    expectedDBCmd.RouterID,
			CommandType: expectedDBCmd.CommandType,
			Payload:     expectedDBCmd.Payload,
		}
		assertCommandEqual(t, i, expected, actual)
	}
}

// TestPollCommands_TouchRouterError tests error handling when TouchRouter fails
func TestPollCommands_TouchRouterError(t *testing.T) {
	// Given: TouchRouter fails with an error
	expectedErr := database.ErrRouterNotFound
	getOutstandingCommandsCalled := false

	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			return expectedErr
		},
		GetOutstandingCommandsFunc: func(ctx context.Context, routerID string) ([]database.Command, error) {
			getOutstandingCommandsCalled = true
			return nil, nil
		},
	}

	service := NewRouterService(mockRepo)

	// When: PollCommands is called for the router
	_, err := service.PollCommands(context.Background(), testPollRouterID)

	// Then: Error should be returned and GetOutstandingCommands should not be called
	if err == nil {
		t.Error("Expected error, got nil")
	}
	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}
	if getOutstandingCommandsCalled {
		t.Error("GetOutstandingCommands should not be called when TouchRouter fails")
	}
}

// TestPollCommands_GetOutstandingCommandsError tests error handling when GetOutstandingCommands fails
func TestPollCommands_GetOutstandingCommandsError(t *testing.T) {
	// Given: GetOutstandingCommands fails with an error
	expectedErr := errors.New("database is down")

	mockRepo := &MockRepository{
		TouchRouterFunc: func(ctx context.Context, routerID string) error {
			return nil
		},
		GetOutstandingCommandsFunc: func(ctx context.Context, routerID string) ([]database.Command, error) {
			return nil, expectedErr
		},
	}

	service := NewRouterService(mockRepo)

	// When: PollCommands is called for the router
	_, err := service.PollCommands(context.Background(), testPollRouterID)

	// Then: Error should be returned
	if err == nil {
		t.Error("Expected error, got nil")
	}
	if !errors.Is(err, expectedErr) {
		t.Errorf("Expected error '%v', got '%v'", expectedErr, err)
	}
}

// assertCommandEqual a helper function to assert that two commands are equal
func assertCommandEqual(t *testing.T, index int, expected, actual Command) {
	t.Helper()
	if actual.ID != expected.ID {
		t.Errorf("Command[%d]: Expected ID '%s', got '%s'", index, expected.ID, actual.ID)
	}
	if actual.RouterID != expected.RouterID {
		t.Errorf("Command[%d]: Expected RouterID '%s', got '%s'", index, expected.RouterID, actual.RouterID)
	}
	if actual.CommandType != expected.CommandType {
		t.Errorf("Command[%d]: Expected CommandType '%s', got '%s'", index, expected.CommandType, actual.CommandType)
	}
	if actual.Payload != expected.Payload {
		t.Errorf("Command[%d]: Expected Payload '%s', got '%s'", index, expected.Payload, actual.Payload)
	}
}
