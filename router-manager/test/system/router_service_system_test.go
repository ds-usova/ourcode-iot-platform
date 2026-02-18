package system

import (
	"context"
	"fmt"
	"log"
	"testing"

	pb "router-manager/proto"
	"router-manager/test/config"
)

const (
	testRouterID     = "550e8400-e29b-41d4-a716-446655440000"
	testSerialNumber = "SN-TEST-12345"
	testCommandType  = "REBOOT"
	testPayload      = `{"timeout": 30, "force": true}`
)

// TestSendCommand_SystemTest is a system test that:
// Given:
// - Test environment with PostgreSQL and Flyway migrations set up
// - Test router is created
// When:
// - Sends a SendCommand request
// Then:
// - The command is created in the database with status "PENDING"
func TestSendCommand_SystemTest(t *testing.T) {
	ctx := context.Background()

	// Given: Test router exists
	log.Printf("Starting TestSendCommand_SystemTest with router ID: %s", testRouterID)
	if err := testEnv.CreateTestRouter(testRouterID, testSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// When: Send SendCommand request
	log.Println("Sending SendCommand request...")
	resp, err := testEnv.Client.SendCommand(ctx, &pb.SendCommandRequest{
		RouterId:    config.StringPtr(testRouterID),
		CommandType: testCommandType,
		Payload:     testPayload,
	})
	if err != nil {
		t.Fatalf("SendCommand failed: %v", err)
	}

	// Then: Verify response
	log.Println("Verifying response...")
	if resp.CommandId == "" {
		t.Error("Expected non-empty command ID in response")
	}

	expectedMessage := fmt.Sprintf("Command sent to router %s", testRouterID)
	if resp.Message != expectedMessage {
		t.Errorf("Expected message '%s', got '%s'", expectedMessage, resp.Message)
	}

	// Then: Verify command in database
	log.Println("Verifying command in database...")
	if err := testEnv.VerifyCommandInDatabase(resp.CommandId, testRouterID, testCommandType, "PENDING"); err != nil {
		t.Fatalf("Failed to verify command in database: %v", err)
	}
}
