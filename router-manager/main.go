package main

import (
	"log"
	"net"
	pb "router-manager/proto"

	"router-manager/internal/server"

	"google.golang.org/grpc"
)

const (
	port = ":50051"
)

func main() {
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("Failed to listen on port %s: %v", port, err)
	}

	grpcServer := grpc.NewServer()
	routerServer := server.NewRouterServer()
	pb.RegisterRouterServiceServer(grpcServer, routerServer)

	log.Println("========================================")
	log.Printf("🚀 gRPC Server started successfully!")
	log.Printf("📡 Listening on port %s", port)
	log.Println("========================================")
	log.Println("Available endpoints:")
	log.Println("  POST /api.v1.RouterService/SendCommand")
	log.Println("========================================")
	log.Println("Waiting for requests...")

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
