package system

import (
	"context"
	"log"
	"os"
	"testing"

	sysconfig "router-manager/test/system/config"
)

var testEnv *sysconfig.SystemTestEnvironment

func TestMain(m *testing.M) {
	ctx := context.Background()

	// Setup: Start containers once for the whole package
	var err error
	testEnv, err = sysconfig.SetupSystemTest(ctx)
	if err != nil {
		log.Fatalf("Failed to setup test suite: %v", err)
	}

	// Run all tests in the package
	code := m.Run()

	// Teardown: Cleanup after all tests are done
	testEnv.Cleanup()

	os.Exit(code)
}
