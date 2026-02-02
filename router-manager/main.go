package main

import (
	"context"
	"log"
	"net"
	pb "router-manager/proto"

	"google.golang.org/grpc"
)

const (
	port = ":50051"
)

type server struct {
	pb.UnimplementedRouterServiceServer
}

func (s *server) SendCommand(_ context.Context, req *pb.SendCommandRequest) (*pb.SendCommandResponse, error) {
	log.Println("========================================")
	log.Println("Received SendCommand request:")

	routerID := req.GetRouterId()
	if routerID == "" {
		log.Printf("  Router ID: <ALL ROUTERS>")
	} else {
		log.Printf("  Router ID: %s", routerID)
	}

	log.Printf("  Command Type: %s", req.GetCommandType())
	log.Printf("  Payload: %s", req.GetPayload())
	log.Println("========================================")

	// Determine if sending to all routers or a specific one
	var routersAffected int32 = 1
	var message = "Command sent to router " + routerID

	response := &pb.SendCommandResponse{
		Success:         true,
		Message:         message,
		RoutersAffected: routersAffected,
	}

	log.Printf("Sending response: Success=%v, Message='%s', RoutersAffected=%d\n",
		response.Success, response.Message, response.RoutersAffected)

	return response, nil
}

func main() {
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("Failed to listen on port %s: %v", port, err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterRouterServiceServer(grpcServer, &server{})

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
