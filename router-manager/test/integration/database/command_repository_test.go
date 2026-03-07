package database

import (
	"context"
	"errors"
	"log/slog"
	"testing"

	"router-manager/internal/database"
)

const (
	testRouterID     = "550e8400-e29b-41d4-a716-446655440000"
	testSerialNumber = "SN-TEST-12345"
	testCommandType  = "REBOOT"
	testPayload      = `{"timeout": 30, "force": true}`
)

// TestCreateCommand_WhenRouterExist_CreateCommand is an integration test that
// Given:
// - Test router is created
// When:
// - CreateCommand is called for the existing router
// Then:
// - The command is created in the database with status "PENDING"
func TestCreateCommand_WhenRouterExist_CreateCommand(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists in the database
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: Send create command for existing router
	slog.Info("creating command request", "router_id", testRouterID)
	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	// Then: Verify command is returned with correct fields
	slog.Info("verifying command creation", "command_id", cmd.ID)
	if cmd.ID == "" {
		t.Error("Expected non-empty command ID")
	}
	if cmd.Status != "PENDING" {
		t.Errorf("Expected status PENDING, got %s", cmd.Status)
	}
	if cmd.RouterID != testRouterID {
		t.Errorf("Expected router ID %s, got %s", testRouterID, cmd.RouterID)
	}

	// Then: verify command is stored in the database with correct fields
	if err := env.VerifyCommandInDatabase(cmd.ID, testRouterID, testCommandType, "PENDING"); err != nil {
		t.Fatalf("Failed to verify command in database: %v", err)
	}
}

// TestCreateCommand_WhenRouterDoesNotExist_ReturnError is an integration test that
// Given:
// - Router does not exist in the database
// When:
// - CreateCommand is called for the non-existing router
// Then:
// - ErrRouterNotFound is returned
func TestCreateCommand_WhenRouterDoesNotExist_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	nonExistentRouterID := "00000000-0000-0000-0000-000000000000"

	// When: Create command for a non-existent router
	_, err := env.DB.Commands().CreateCommand(ctx, nonExistentRouterID, testCommandType, testPayload)

	// Then: ErrRouterNotFound is returned
	if !errors.Is(err, database.ErrRouterNotFound) {
		t.Fatalf("Expected database.ErrRouterNotFound, got %v", err)
	}
}

// TestBroadcastCommand_WhenRoutersExist_CreateCommandsForAll is an integration test that
// Given:
// - Two test routers are created
// When:
// - BroadcastCommand is called
// Then:
// - Two commands are created in the database (one for each router)
func TestBroadcastCommand_WhenRoutersExist_CreateCommandsForAll(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	routerID1 := "550e8400-e29b-41d4-a716-446655440001"
	routerID2 := "550e8400-e29b-41d4-a716-446655440002"

	// Given: Two routers exist
	if err := env.CreateTestRouter(routerID1, "SN-TEST-1"); err != nil {
		t.Fatalf("Failed to create test router 1: %v", err)
	}
	if err := env.CreateTestRouter(routerID2, "SN-TEST-2"); err != nil {
		t.Fatalf("Failed to create test router 2: %v", err)
	}

	// When: Broadcast command
	slog.Info("broadcasting command to all routers", "command_type", testCommandType)
	count, err := env.DB.Commands().BroadcastCommand(ctx, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to broadcast command: %v", err)
	}

	// Then: Two records should be created
	if count != 2 {
		t.Errorf("Expected 2 commands created, got %d", count)
	}

	// Verify each router has a command
	for _, rid := range []string{routerID1, routerID2} {
		cCount, err := env.CountCommandsForRouter(rid)
		if err != nil {
			t.Errorf("Failed to count commands for router %s: %v", rid, err)
		}
		if cCount != 1 {
			t.Errorf("Expected 1 command for router %s, got %d", rid, cCount)
		}
	}
}

// TestBroadcastCommand_WhenNoRoutersExist_ReturnError is an integration test that
// Given:
// - No routers exist in the database
// When:
// - BroadcastCommand is called
// Then:
// - ErrRouterNotFound is returned
func TestBroadcastCommand_WhenNoRoutersExist_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// When: Broadcast command when no routers exist
	slog.Info("broadcasting command when no routers exist", "command_type", testCommandType)
	_, err := env.DB.Commands().BroadcastCommand(ctx, testCommandType, testPayload)

	// Then: Verify that ErrRouterNotFound is returned
	if !errors.Is(err, database.ErrRouterNotFound) {
		t.Fatalf("Expected database.ErrRouterNotFound, got %v", err)
	}

	slog.Warn("received expected error", "error", err)
}

// TestGetOutstandingCommands_WhenPendingCommandExists_ReturnCommand is an integration test that
// Given:
// - Router is created
// - Command is created with status "PENDING"
// When:
// - GetOutstandingCommands is called
// Then:
// - The correct command is returned and its status is updated to "SENT"
func TestGetOutstandingCommands_WhenPendingCommandExists_ReturnCommand(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and a pending command is created
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create pending command: %v", err)
	}

	// When: Get outstanding commands
	slog.Info("retrieving outstanding commands", "router_id", testRouterID)
	commands, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)
	if err != nil {
		t.Fatalf("Failed to get outstanding commands: %v", err)
	}

	// Then: Verify the correct command is returned
	if len(commands) != 1 {
		t.Errorf("Expected 1 outstanding command, got %d", len(commands))
	}

	foundCmd := commands[0]
	if foundCmd.ID != cmd.ID {
		t.Errorf("Expected command ID %s, got %s", cmd.ID, foundCmd.ID)
	}
	if foundCmd.CommandType != testCommandType {
		t.Errorf("Expected command type %s, got %s", testCommandType, foundCmd.CommandType)
	}

	// Then: Verify status in database is updated to "SENT" and sent_at is set
	if err := env.VerifyCommandInDatabase(cmd.ID, testRouterID, testCommandType, "SENT"); err != nil {
		t.Fatalf("Failed to verify command status updated to SENT: %v", err)
	}

	if err := env.VerifyCommandSentAt(cmd.ID); err != nil {
		t.Fatalf("Failed to verify sent_at timestamp: %v", err)
	}
}

// TestGetOutstandingCommands_WhenCommandIsSent_ReturnCommandAgain is an integration test that
// Given:
// - Router is created
// - Command is created with status "PENDING"
// - Command is retrieved with GetOutstandingCommands (status becomes "SENT")
// When:
// - GetOutstandingCommands is called again
// Then:
// - The command is still returned
func TestGetOutstandingCommands_WhenCommandIsSent_ReturnCommandAgain(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and a pending command is created
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create pending command: %v", err)
	}

	// Given: Command is retrieved once (status becomes SENT)
	_, err = env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)
	if err != nil {
		t.Fatalf("Failed to get outstanding commands for the first time: %v", err)
	}

	// When: Get outstanding commands again
	slog.Info("retrieving outstanding commands again", "router_id", testRouterID)
	commands, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)
	if err != nil {
		t.Fatalf("Failed to get outstanding commands again: %v", err)
	}

	// Then: The command should still be returned
	if len(commands) != 1 {
		t.Errorf("Expected 1 outstanding command on second call, got %d", len(commands))
	}

	if commands[0].ID != cmd.ID {
		t.Errorf("Expected command ID %s, got %s", cmd.ID, commands[0].ID)
	}
}

// TestGetOutstandingCommands_WhenNoCommandsForRouter_ReturnEmpty is an integration test that
// Given:
// - Router exists but has no commands
// When:
// - GetOutstandingCommands is called
// Then:
// - An empty list is returned with no error
func TestGetOutstandingCommands_WhenNoCommandsForRouter_ReturnEmpty(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists but has no commands
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: Get outstanding commands
	commands, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)

	// Then: No error and empty list returned
	if err != nil {
		t.Fatalf("Expected no error for router with no commands, got %v", err)
	}
	if len(commands) != 0 {
		t.Errorf("Expected 0 commands for router with no commands, got %d", len(commands))
	}
}

// TestGetOutstandingCommands_WhenRouterNotFound_ReturnEmpty is an integration test that
// Given:
// - No router exists in the database
// When:
// - GetOutstandingCommands is called with a non-existent router ID
// Then:
// - An empty list is returned with no error (the UPDATE matches 0 rows)
func TestGetOutstandingCommands_WhenRouterNotFound_ReturnEmpty(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	nonExistentRouterID := "00000000-0000-0000-0000-000000000000"

	// When: Get outstanding commands for a non-existent router
	commands, err := env.DB.Commands().GetOutstandingCommands(ctx, nonExistentRouterID)

	// Then: No error and empty list returned
	if err != nil {
		t.Fatalf("Expected no error for non-existent router, got %v", err)
	}
	if len(commands) != 0 {
		t.Errorf("Expected 0 commands for non-existent router, got %d", len(commands))
	}
}

// TestAcknowledgeCommand_WhenValidIDs_UpdateStatus is an integration test that
// Given:
// - Router exists
// - Command was retrieved with GetOutstandingCommands (status "SENT")
// When:
// - AcknowledgeCommand is called with correct router and command ID
// Then:
// - Command status is updated to "ACKED"
// - acked_at is set
func TestAcknowledgeCommand_WhenValidIDs_UpdateStatus(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and command is in "SENT" status
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	if _, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID); err != nil {
		t.Fatalf("Failed to set command status to SENT: %v", err)
	}

	// When: Acknowledge command with correct IDs
	slog.Info("acknowledging command", "router_id", testRouterID, "command_id", cmd.ID)
	if err := env.DB.Commands().AcknowledgeCommand(ctx, cmd.ID, testRouterID); err != nil {
		t.Fatalf("Failed to acknowledge command: %v", err)
	}

	// Then: Status is updated to ACKED
	if err := env.VerifyCommandInDatabase(cmd.ID, testRouterID, testCommandType, "ACKED"); err != nil {
		t.Fatalf("Failed to verify command status updated to ACKED: %v", err)
	}

	// Then: acked_at is set
	if err := env.VerifyCommandAckedAt(cmd.ID); err != nil {
		t.Fatalf("Failed to verify acked_at timestamp: %v", err)
	}
}

// TestAcknowledgeCommand_WhenWrongRouterID_ReturnError is an integration test that
// Given:
// - Two routers exist
// - Command is created for router A and is in "SENT" status
// When:
// - AcknowledgeCommand is called with router B's ID (exists but doesn't own the command)
// Then:
// - ErrSentCommandNotFound is returned
func TestAcknowledgeCommand_WhenWrongRouterID_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	otherRouterID := "550e8400-e29b-41d4-a716-446655449999"

	// Given: Both routers exist, command belongs to testRouterID and is in "SENT" status
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}
	if err := env.CreateTestRouter(otherRouterID, "SN-OTHER"); err != nil {
		t.Fatalf("Failed to create other router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	if _, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID); err != nil {
		t.Fatalf("Failed to set command status to SENT: %v", err)
	}

	// When: Acknowledge with a router that exists but does not own the command
	err = env.DB.Commands().AcknowledgeCommand(ctx, cmd.ID, otherRouterID)

	// Then: ErrSentCommandNotFound is returned
	if !errors.Is(err, database.ErrSentCommandNotFound) {
		t.Fatalf("Expected database.ErrSentCommandNotFound, got %v", err)
	}
}

// TestAcknowledgeCommand_WhenNonExistentCommandID_ReturnError is an integration test that
// Given:
// - Router exists
// When:
// - AcknowledgeCommand is called with non-existent command ID
// Then:
// - ErrSentCommandNotFound is returned
func TestAcknowledgeCommand_WhenNonExistentCommandID_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	nonExistentCmdID := "00000000-0000-0000-0000-000000000000"

	// When: Acknowledge with non-existent command ID
	slog.Info("acknowledging non-existent command", "router_id", testRouterID, "command_id", nonExistentCmdID)
	err := env.DB.Commands().AcknowledgeCommand(ctx, nonExistentCmdID, testRouterID)

	// Then: ErrSentCommandNotFound is returned
	if !errors.Is(err, database.ErrSentCommandNotFound) {
		t.Fatalf("Expected database.ErrSentCommandNotFound, got %v", err)
	}
}

// TestAcknowledgeCommand_WhenCommandNotSent_ReturnError is an integration test that
// Given:
// - A command exists in "PENDING" status (never polled)
// When:
// - AcknowledgeCommand is called before the command was retrieved via GetOutstandingCommands
// Then:
// - ErrSentCommandNotFound is returned (command must be in SENT status to be acknowledged)
func TestAcknowledgeCommand_WhenCommandNotSent_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and command is in "PENDING" status (not yet polled)
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	// When: Acknowledge command that is still PENDING (was never polled)
	err = env.DB.Commands().AcknowledgeCommand(ctx, cmd.ID, testRouterID)

	// Then: ErrSentCommandNotFound is returned
	if !errors.Is(err, database.ErrSentCommandNotFound) {
		t.Fatalf("Expected database.ErrSentCommandNotFound, got %v", err)
	}
}

// TestAcknowledgeCommand_WhenCommandAlreadyAcked_ReturnError is an integration test that
// Given:
// - Router exists
// - Command has been acknowledged (status "ACKED")
// When:
// - AcknowledgeCommand is called again for the same command
// Then:
// - ErrSentCommandNotFound is returned (WHERE status = 'SENT' does not match ACKED)
func TestAcknowledgeCommand_WhenCommandAlreadyAcked_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and command is acknowledged (ACKED)
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	if _, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID); err != nil {
		t.Fatalf("Failed to set command status to SENT: %v", err)
	}

	if err := env.DB.Commands().AcknowledgeCommand(ctx, cmd.ID, testRouterID); err != nil {
		t.Fatalf("Failed to acknowledge command: %v", err)
	}

	// When: Acknowledge the same command again
	err = env.DB.Commands().AcknowledgeCommand(ctx, cmd.ID, testRouterID)

	// Then: ErrSentCommandNotFound is returned
	if !errors.Is(err, database.ErrSentCommandNotFound) {
		t.Fatalf("Expected database.ErrSentCommandNotFound, got %v", err)
	}
}

// TestCommandLifecycle_FromBroadcastToAck_RetrievedCorrectly is an integration test that
// Given:
// - Router exists
// - Command was broadcasted
// - The command was polled with GetOutstandingCommands (status "SENT")
// - Command was acknowledged (status "ACKED")
// When:
// - It is polled again
// Then:
// - No command is returned
func TestCommandLifecycle_FromBroadcastToAck_RetrievedCorrectly(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists and command is broadcasted
	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	_, err := env.DB.Commands().BroadcastCommand(ctx, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to broadcast command: %v", err)
	}

	// Given: Command is polled once (becomes SENT)
	commands, err := env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)
	if err != nil {
		t.Fatalf("Failed to poll outstanding commands: %v", err)
	}
	if len(commands) != 1 {
		t.Fatalf("Expected 1 outstanding command, got %d", len(commands))
	}
	cmdID := commands[0].ID

	// Given: Command is acknowledged (becomes ACKED)
	if err := env.DB.Commands().AcknowledgeCommand(ctx, cmdID, testRouterID); err != nil {
		t.Fatalf("Failed to acknowledge command: %v", err)
	}

	// When: Polled again
	slog.Info("polling after acknowledgment", "router_id", testRouterID)
	commands, err = env.DB.Commands().GetOutstandingCommands(ctx, testRouterID)
	if err != nil {
		t.Fatalf("Failed to poll outstanding commands again: %v", err)
	}

	// Then: No command is returned (since status is ACKED)
	if len(commands) != 0 {
		t.Errorf("Expected 0 outstanding commands after acknowledgment, got %d", len(commands))
	}

	// Verify final state in database
	if err := env.VerifyCommandInDatabase(cmdID, testRouterID, testCommandType, "ACKED"); err != nil {
		t.Fatalf("Failed to verify final status is ACKED: %v", err)
	}
}
