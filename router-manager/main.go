package main

import (
	"log/slog"
	"net"
	"os"
	"router-manager/internal/config"
	"router-manager/internal/database"
	"router-manager/internal/logging"
	"router-manager/internal/server"
	"router-manager/internal/service"
	pb "router-manager/proto"

	"google.golang.org/grpc"
)

func main() {
	cfg := config.Load()
	logging.ConfigureDefault(cfg.Logging)

	// Initialize database connection
	db, err := database.New(cfg.Database.ConnectionString())
	if err != nil {
		slog.Error("failed to connect to database", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	// Initialize service layer
	routerService := service.NewRouterService(db)

	// Setup gRPC server
	lis, err := net.Listen("tcp", cfg.Server.Port)
	if err != nil {
		slog.Error("failed to listen", "port", cfg.Server.Port, "error", err)
		os.Exit(1)
	}

	grpcServer := grpc.NewServer()
	routerServer := server.NewRouterServer(routerService)
	pb.RegisterRouterServiceServer(grpcServer, routerServer)

	slog.Info("gRPC server started",
		"port", cfg.Server.Port,
		"db_user", cfg.Database.User,
		"db_host", cfg.Database.Host,
		"db_port", cfg.Database.Port,
		"db_name", cfg.Database.Database,
		"db_schema", cfg.Database.Schema,
	)
	slog.Info("available endpoints",
		"submit_command", "POST /api.v1.RouterService/SubmitCommand",
		"poll_outstanding_commands", "POST /api.v1.RouterService/PollOutstandingCommands",
		"acknowledge_command", "POST /api.v1.RouterService/AcknowledgeCommand",
	)

	if err := grpcServer.Serve(lis); err != nil {
		slog.Error("failed to serve gRPC server", "error", err)
		os.Exit(1)
	}
}
