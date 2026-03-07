package database

import (
	"context"
	"errors"
	"log/slog"
	"testing"
	"time"

	"router-manager/internal/database"
)

const (
	routerTestID           = "550e8400-e29b-41d4-a716-446655447777"
	routerTestSerialNumber = "SN-ROUTER-TEST"
)

// TestTouchRouter_WhenRouterExist_UpdateLastSeenAt is an integration test that
// Given:
// - Router exists
// When:
// - TouchRouter is called
// Then:
// - last_seen_at is updated to current time
func TestTouchRouter_WhenRouterExist_UpdateLastSeenAt(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	// Given: Router exists
	if err := env.CreateTestRouter(routerTestID, routerTestSerialNumber); err != nil {
		t.Fatalf("Failed to create test router: %v", err)
	}

	// Capture initial last_seen_at
	initialLastSeen, err := env.GetRouterLastSeenAt(routerTestID)
	if err != nil {
		t.Fatalf("Failed to get initial last_seen_at: %v", err)
	}

	// Wait a bit to ensure a different timestamp if the DB resolution is high
	time.Sleep(100 * time.Millisecond)

	// When: Touch router
	slog.Info("touching router", "router_id", routerTestID)
	if err := env.DB.Routers().TouchRouter(ctx, routerTestID); err != nil {
		t.Fatalf("Failed to touch router: %v", err)
	}

	// Then: last_seen_at is updated
	updatedLastSeen, err := env.GetRouterLastSeenAt(routerTestID)
	if err != nil {
		t.Fatalf("Failed to get updated last_seen_at: %v", err)
	}

	if !updatedLastSeen.After(initialLastSeen) {
		t.Errorf("Expected updated last_seen_at (%v) to be after initial last_seen_at (%v)", updatedLastSeen, initialLastSeen)
	}
}

// TestTouchRouter_WhenRouterDoesNotExist_ReturnError is an integration test that
// Given:
// - Router doesn't exist
// When:
// - TouchRouter is called
// Then:
// - ErrRouterNotFound is returned
func TestTouchRouter_WhenRouterDoesNotExist_ReturnError(t *testing.T) {
	ctx := context.Background()
	env := SetupTest(t)

	nonExistentRouterID := "00000000-0000-0000-0000-000000009999"

	// When: Touch non-existent router
	slog.Info("touching non-existent router", "router_id", nonExistentRouterID)
	err := env.DB.Routers().TouchRouter(ctx, nonExistentRouterID)

	// Then: ErrRouterNotFound is returned
	if !errors.Is(err, database.ErrRouterNotFound) {
		t.Fatalf("Expected database.ErrRouterNotFound, got %v", err)
	}
}
