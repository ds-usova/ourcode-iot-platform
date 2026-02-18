package system

import (
	"context"
	"log"
	"testing"
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

	// Given: Router exists in the database
	if err := testEnv.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: Send create command for existing router
	log.Println("Creating command request...")
	cmd, err := testEnv.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
	if err != nil {
		t.Fatalf("Failed to create command: %v", err)
	}

	// Then: Verify command is returned with correct fields
	log.Println("Verifying command creation...")
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
	if err := testEnv.VerifyCommandInDatabase(cmd.ID, testRouterID, testCommandType, "PENDING"); err != nil {
		t.Fatalf("Failed to verify command in database: %v", err)
	}
}

// TestCreateCommand_WhenRouterDoesNotExist_ReturnError is an integration test that
// Given:
// - Router does not exist in the database
// When:
// - CreateCommand is called for the non-existing router
// Then:
// - An error is returned
func TestCreateCommand_WhenRouterDoesNotExist_ReturnError(t *testing.T) {
	ctx := context.Background()

	nonExistentRouterID := "00000000-0000-0000-0000-000000000000"

	// When: Send create command for a non-existing router
	log.Println("Creating command request for non-existent router...")
	_, err := testEnv.DB.Commands().CreateCommand(ctx, nonExistentRouterID, testCommandType, testPayload)

	// Then: Verify that an error is returned
	if err == nil {
		t.Fatal("Expected error when creating command for non-existent router, but got nil")
	}

	log.Printf("Received expected error: %v", err)
}
