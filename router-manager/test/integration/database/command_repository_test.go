package system

import (
	"context"
	"log"
	"testing"

	intconfig "router-manager/test/integration/config"
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
	env, err := intconfig.SetupIntegrationTest(ctx)
	if err != nil {
		t.Fatalf("Failed to setup test environment: %v", err)
	}
	defer env.Cleanup()

	if err := env.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: Send create command for existing router
	log.Println("Creating command request...")
	cmd, err := env.DB.Commands().CreateCommand(ctx, testRouterID, testCommandType, testPayload)
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
	if err := env.VerifyCommandInDatabase(cmd.ID, testRouterID, testCommandType, "PENDING"); err != nil {
		t.Fatalf("Failed to verify command in database: %v", err)
	}
}
