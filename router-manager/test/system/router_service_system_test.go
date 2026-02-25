package system

import (
	"context"
	"fmt"
	"testing"

	pb "router-manager/proto"
	"router-manager/test/config"
	sysconfig "router-manager/test/system/config"
)

const (
	testRouterID      = "550e8400-e29b-41d4-a716-446655440000"
	testSerialNumber  = "SN-TEST-12345"
	testRouterID2     = "550e8400-e29b-41d4-a716-446655440001"
	testSerialNumber2 = "SN-TEST-67890"
	testCommandType   = "REBOOT"
	testPayload       = `{"timeout": 30, "force": true}`
)

// TestCommandLifecycle_SingleRouter is a system test that:
// Given:
// - A test router exists
// When:
// - A command is sent to the router via SendCommand
// - The router polls outstanding commands
// - The router acknowledges the command
// - The router polls outstanding commands again
// Then:
// - The first poll returns the command
// - After acknowledgement, the second poll returns no commands
func TestCommandLifecycle_SingleRouter(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: test router exists
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: send command to router
	sendResp, err := env.Client.SendCommand(ctx, &pb.SendCommandRequest{
		RouterId:    config.StringPtr(testRouterID),
		CommandType: testCommandType,
		Payload:     testPayload,
	})
	if err != nil {
		t.Fatalf("SendCommand failed: %v", err)
	}

	// Then: response contains a command ID and the expected message
	if sendResp.CommandId == "" {
		t.Fatal("Expected non-empty command ID in SendCommand response")
	}
	expectedMsg := fmt.Sprintf("Command sent to router %s", testRouterID)
	if sendResp.Message != expectedMsg {
		t.Errorf("Expected message %q, got %q", expectedMsg, sendResp.Message)
	}

	// When/Then: router polls, acknowledges, and confirms no outstanding commands remain
	pollAcknowledgeAndAssertDone(t, ctx, env, testRouterID, testCommandType)
}

// TestCommandLifecycle_BroadcastToMultipleRouters is a system test that:
// Given:
// - Two test routers exist
// When:
// - A command is broadcast to all routers via SendCommand (no router ID)
// - Each router polls outstanding commands
// - Each router acknowledges its command
// - Each router polls outstanding commands again
// Then:
// - Each router's first poll returns the broadcast command
// - After acknowledgement, each router's second poll returns no commands
func TestCommandLifecycle_BroadcastToMultipleRouters(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: two test routers exist
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create first test router: %v", err)
	}
	if err := env.CreateTestRouter(testRouterID2, testSerialNumber2); err != nil {
		t.Fatalf("Failed to create second test router: %v", err)
	}

	// When: broadcast command to all routers (no router ID)
	sendResp, err := env.Client.SendCommand(ctx, &pb.SendCommandRequest{
		CommandType: testCommandType,
		Payload:     testPayload,
	})
	if err != nil {
		t.Fatalf("Broadcast SendCommand failed: %v", err)
	}

	// Then: broadcast response reflects 2 routers affected
	expectedMsg := "Command sent to 2 router(s)"
	if sendResp.Message != expectedMsg {
		t.Errorf("Expected message %q, got %q", expectedMsg, sendResp.Message)
	}

	// When/Then: each router polls, acknowledges, and confirms no outstanding commands remain
	pollAcknowledgeAndAssertDone(t, ctx, env, testRouterID, testCommandType)
	pollAcknowledgeAndAssertDone(t, ctx, env, testRouterID2, testCommandType)
}

// pollAcknowledgeAndAssertDone polls outstanding commands for a router, asserts exactly one
// command of the expected type is returned, acknowledges it, then asserts no commands remain.
func pollAcknowledgeAndAssertDone(t *testing.T, ctx context.Context, env *sysconfig.SystemTestEnvironment, routerID, expectedCommandType string) {
	t.Helper()

	// Poll outstanding commands
	pollResp, err := env.Client.PollOutstandingCommands(ctx, &pb.PollOutstandingCommandsRequest{
		RouterId: routerID,
	})
	if err != nil {
		t.Fatalf("PollOutstandingCommands for router %s failed: %v", routerID, err)
	}
	if len(pollResp.Commands) != 1 {
		t.Fatalf("Expected 1 outstanding command for router %s, got %d", routerID, len(pollResp.Commands))
	}
	cmd := pollResp.Commands[0]
	if cmd.CommandType != expectedCommandType {
		t.Errorf("Expected command type %s for router %s, got %s", expectedCommandType, routerID, cmd.CommandType)
	}

	// Acknowledge the command
	if _, err := env.Client.AcknowledgeCommand(ctx, &pb.AcknowledgeCommandRequest{
		RouterId:  routerID,
		CommandId: cmd.CommandId,
	}); err != nil {
		t.Fatalf("AcknowledgeCommand for router %s failed: %v", routerID, err)
	}

	// Poll again and assert no commands remain
	pollResp2, err := env.Client.PollOutstandingCommands(ctx, &pb.PollOutstandingCommandsRequest{
		RouterId: routerID,
	})
	if err != nil {
		t.Fatalf("Second PollOutstandingCommands for router %s failed: %v", routerID, err)
	}
	if len(pollResp2.Commands) != 0 {
		t.Errorf("Expected 0 outstanding commands for router %s after acknowledgement, got %d", routerID, len(pollResp2.Commands))
	}
}
