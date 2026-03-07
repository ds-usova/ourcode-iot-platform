package system

import (
	"context"
	"log/slog"
	"os"
	"testing"

	testconfig "router-manager/test/config"
	sysconfig "router-manager/test/system/config"
)

var testEnv *sysconfig.SystemTestEnvironment

func TestMain(m *testing.M) {
	testconfig.ConfigureTestLogging()
	ctx := context.Background()

	// Setup: Start containers once for the whole package
	var err error
	testEnv, err = sysconfig.SetupSystemTest(ctx)
	if err != nil {
		slog.Error("failed to setup test suite", "error", err)
		os.Exit(1)
	}

	// Run all tests in the package
	code := m.Run()

	// Teardown: Cleanup after all tests are done
	testEnv.Cleanup()

	os.Exit(code)
}

// SetupTest prepares the environment for a single test case.
// It returns the shared testEnv and registers a cleanup function to
// clear the database after the test finishes.
func SetupTest(t *testing.T) *sysconfig.SystemTestEnvironment {
	t.Helper()

	// Clear before test to ensure a clean start
	if err := testEnv.ClearDatabase(); err != nil {
		t.Fatalf("Failed to clear database before test: %v", err)
	}

	t.Cleanup(func() {
		if err := testEnv.ClearDatabase(); err != nil {
			t.Errorf("Failed to clear database after test: %v", err)
		}
	})

	return testEnv
}
