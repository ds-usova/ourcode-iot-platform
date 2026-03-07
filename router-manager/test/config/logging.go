package config

import (
	"log/slog"
	"os"
	"router-manager/internal/logging"
	"strings"
)

// ConfigureTestLogging sets default slog behavior for system/integration tests.
func ConfigureTestLogging() {
	level := getEnvWithFallback("TEST_LOG_LEVEL", getEnvWithFallback("LOG_LEVEL", "warn"))
	format := getEnvWithFallback("TEST_LOG_FORMAT", getEnvWithFallback("LOG_FORMAT", "text"))
	slog.SetDefault(slog.New(logging.NewHandler(level, format)))
}

func getEnvWithFallback(key, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(key)); value != "" {
		return value
	}
	return fallback
}
