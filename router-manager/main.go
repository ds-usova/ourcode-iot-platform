package main

import (
	"log"
	"net"
	"router-manager/internal/config"
	"router-manager/internal/database"
	"router-manager/internal/server"
	pb "router-manager/proto"

	"google.golang.org/grpc"
)

func main() {
	cfg := config.Load()

	// Initialize database connection
	db, err := database.New(cfg.Database.ConnectionString())
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Setup gRPC server
	lis, err := net.Listen("tcp", cfg.Server.Port)
	if err != nil {
		log.Fatalf("Failed to listen on port %s: %v", cfg.Server.Port, err)
	}

	grpcServer := grpc.NewServer()
	routerServer := server.NewRouterServer(db)
	pb.RegisterRouterServiceServer(grpcServer, routerServer)

	log.Println("========================================")
	log.Printf("gRPC Server started successfully!")
	log.Printf("Listening on port %s", cfg.Server.Port)
	log.Printf("️Database: %s@%s:%s/%s (schema: %s)", cfg.Database.User, cfg.Database.Host, cfg.Database.Port, cfg.Database.Database, cfg.Database.Schema)
	log.Println("========================================")
	log.Println("Available endpoints:")
	log.Println("  POST /api.v1.RouterService/SendCommand")
	log.Println("  POST /api.v1.RouterService/PollOutstandingCommands")
	log.Println("  POST /api.v1.RouterService/AcknowledgeCommand")
	log.Println("========================================")
	log.Println("Waiting for requests...")

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
