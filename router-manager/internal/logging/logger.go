package logging

import (
	"log/slog"
	"os"
	"router-manager/internal/config"
	"strings"
)

// ConfigureDefault sets the application-wide slog default logger from config.
func ConfigureDefault(cfg config.LoggingConfig) {
	slog.SetDefault(slog.New(NewHandler(cfg.Level, cfg.Format)))
}

// NewHandler creates a slog handler with the specified level and format.
func NewHandler(level, format string) slog.Handler {
	opts := &slog.HandlerOptions{Level: ParseLevel(level)}

	if strings.EqualFold(format, "json") {
		return slog.NewJSONHandler(os.Stdout, opts)
	}

	return slog.NewTextHandler(os.Stdout, opts)
}

// ParseLevel converts a string log level to slog.Level.
func ParseLevel(raw string) slog.Level {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "debug":
		return slog.LevelDebug
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}
