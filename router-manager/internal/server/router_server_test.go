package server

import (
	"context"
	"errors"
	"router-manager/internal/database"
	"router-manager/internal/service"
	pb "router-manager/proto"
	"testing"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

const (
	testCommandType = "reboot"
	testPayload     = "{\"version\":\"1.2.3\"}"
	testCommandID   = "cmd-789"
)

// TestSendCommand_SingleRouterSuccess tests successful command submission to a specific router
func TestSendCommand_SingleRouterSuccess(t *testing.T) {
	// Given: a valid request for a specific router
	routerID := "router-123"
	req := &pb.SendCommandRequest{
		RouterId:    &routerID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	expectedResult := &service.SubmitCommandResult{
		CommandID:       testCommandID,
		RoutersAffected: 1,
		IsBroadcast:     false,
	}

	mockService := &MockRouterService{
		SubmitCommandFunc: func(ctx context.Context, routerIDParam, commandType, payload string) (*service.SubmitCommandResult, error) {
			if routerIDParam != routerID {
				t.Errorf("Expected routerID '%s', got '%s'", routerID, routerIDParam)
			}
			if commandType != testCommandType {
				t.Errorf("Expected commandType '%s', got '%s'", testCommandType, commandType)
			}
			if payload != testPayload {
				t.Errorf("Expected payload '%s', got '%s'", testPayload, payload)
			}
			return expectedResult, nil
		},
	}

	server := NewRouterServer(mockService)

	// When: SendCommand is called
	resp, err := server.SendCommand(context.Background(), req)

	// Then: Response should contain command ID and success message without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if resp == nil {
		t.Fatal("Expected response, got nil")
	}
	if resp.CommandId != testCommandID {
		t.Errorf("Expected CommandId '%s', got '%s'", testCommandID, resp.CommandId)
	}
	expectedMessage := "Command sent to router router-123"
	if resp.Message != expectedMessage {
		t.Errorf("Expected message '%s', got '%s'", expectedMessage, resp.Message)
	}
}

// TestSendCommand_BroadcastSuccess tests successful command broadcast to all routers
func TestSendCommand_BroadcastSuccess(t *testing.T) {
	// Given: a valid request with empty router ID (broadcast)
	emptyRouterID := ""
	req := &pb.SendCommandRequest{
		RouterId:    &emptyRouterID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	expectedResult := &service.SubmitCommandResult{
		CommandID:       "",
		RoutersAffected: 5,
		IsBroadcast:     true,
	}

	mockService := &MockRouterService{
		SubmitCommandFunc: func(ctx context.Context, routerID, commandType, payload string) (*service.SubmitCommandResult, error) {
			if routerID != "" {
				t.Errorf("Expected empty routerID, got '%s'", routerID)
			}
			if commandType != testCommandType {
				t.Errorf("Expected commandType '%s', got '%s'", testCommandType, commandType)
			}
			if payload != testPayload {
				t.Errorf("Expected payload '%s', got '%s'", testPayload, payload)
			}
			return expectedResult, nil
		},
	}

	server := NewRouterServer(mockService)

	// When: SendCommand is called with empty router ID
	resp, err := server.SendCommand(context.Background(), req)

	// Then: Response should contain broadcast message without error
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if resp == nil {
		t.Fatal("Expected response, got nil")
	}
	if resp.CommandId != "" {
		t.Errorf("Expected empty CommandId for broadcast, got '%s'", resp.CommandId)
	}
	expectedMessage := "Command sent to 5 router(s)"
	if resp.Message != expectedMessage {
		t.Errorf("Expected message '%s', got '%s'", expectedMessage, resp.Message)
	}
}

// TestSendCommand_RouterNotFound tests error handling when router is not found
func TestSendCommand_RouterNotFound(t *testing.T) {
	// Given: SubmitCommand fails with ErrRouterNotFound
	routerID := "router-123"
	req := &pb.SendCommandRequest{
		RouterId:    &routerID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	mockService := &MockRouterService{
		SubmitCommandFunc: func(ctx context.Context, routerIDParam, commandType, payload string) (*service.SubmitCommandResult, error) {
			return nil, database.ErrRouterNotFound
		},
	}

	server := NewRouterServer(mockService)

	// When: SendCommand is called
	resp, err := server.SendCommand(context.Background(), req)

	// Then: Should return InvalidArgument error with appropriate message
	if resp != nil {
		t.Errorf("Expected nil response, got %v", resp)
	}
	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	st, ok := status.FromError(err)
	if !ok {
		t.Fatal("Expected gRPC status error")
	}
	if st.Code() != codes.InvalidArgument {
		t.Errorf("Expected code InvalidArgument, got %v", st.Code())
	}
	expectedMessage := "router with ID router-123 not found"
	if st.Message() != expectedMessage {
		t.Errorf("Expected message '%s', got '%s'", expectedMessage, st.Message())
	}
}

// TestSendCommand_BroadcastNoRoutersFound tests error handling when no routers exist for broadcast
func TestSendCommand_BroadcastNoRoutersFound(t *testing.T) {
	// Given: SubmitCommand fails with ErrRouterNotFound for broadcast
	emptyRouterID := ""
	req := &pb.SendCommandRequest{
		RouterId:    &emptyRouterID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	mockService := &MockRouterService{
		SubmitCommandFunc: func(ctx context.Context, routerID, commandType, payload string) (*service.SubmitCommandResult, error) {
			return nil, database.ErrRouterNotFound
		},
	}

	server := NewRouterServer(mockService)

	// When: SendCommand is called with empty router ID
	resp, err := server.SendCommand(context.Background(), req)

	// Then: Should return InvalidArgument error with broadcast-specific message
	if resp != nil {
		t.Errorf("Expected nil response, got %v", resp)
	}
	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	st, ok := status.FromError(err)
	if !ok {
		t.Fatal("Expected gRPC status error")
	}
	if st.Code() != codes.InvalidArgument {
		t.Errorf("Expected code InvalidArgument, got %v", st.Code())
	}
	expectedMessage := "no routers found to send the command"
	if st.Message() != expectedMessage {
		t.Errorf("Expected message '%s', got '%s'", expectedMessage, st.Message())
	}
}

// TestSendCommand_InternalError tests error handling for internal service errors
func TestSendCommand_InternalError(t *testing.T) {
	// Given: SubmitCommand fails with an internal error
	routerID := "router-123"
	req := &pb.SendCommandRequest{
		RouterId:    &routerID,
		CommandType: testCommandType,
		Payload:     testPayload,
	}

	expectedErr := errors.New("database connection failed")

	mockService := &MockRouterService{
		SubmitCommandFunc: func(ctx context.Context, routerIDParam, commandType, payload string) (*service.SubmitCommandResult, error) {
			return nil, expectedErr
		},
	}

	server := NewRouterServer(mockService)

	// When: SendCommand is called
	resp, err := server.SendCommand(context.Background(), req)

	// Then: Should return Internal error
	if resp != nil {
		t.Errorf("Expected nil response, got %v", resp)
	}
	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	st, ok := status.FromError(err)
	if !ok {
		t.Fatal("Expected gRPC status error")
	}
	if st.Code() != codes.Internal {
		t.Errorf("Expected code Internal, got %v", st.Code())
	}
	if st.Message() != expectedErr.Error() {
		t.Errorf("Expected message '%s', got '%s'", expectedErr.Error(), st.Message())
	}
}
