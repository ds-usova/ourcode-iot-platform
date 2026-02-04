package server

import (
	"context"
	"errors"
	"fmt"
	"log"
	"router-manager/internal/database"
	pb "router-manager/proto"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type RouterServer struct {
	pb.UnimplementedRouterServiceServer
	db *database.DB
}

func NewRouterServer(db *database.DB) *RouterServer {
	return &RouterServer{db: db}
}

func (s *RouterServer) SendCommand(ctx context.Context, req *pb.SendCommandRequest) (*pb.SendCommandResponse, error) {
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

	if routerID == "" {
		return &pb.SendCommandResponse{
			CommandId: "",
			Message:   "Command sent to all routers",
		}, nil
	}

	cmd, err := s.db.CreateCommand(ctx, routerID, req.GetCommandType(), req.GetPayload())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			return nil, status.Error(codes.InvalidArgument, "router not found")
		}
		return nil, status.Error(codes.Internal, err.Error())
	}

	return &pb.SendCommandResponse{
		CommandId: cmd.Id,
		Message:   fmt.Sprintf("Command sent to router %s", routerID),
	}, nil
}

func (s *RouterServer) PollOutstandingCommands(_ context.Context, req *pb.PollOutstandingCommandsRequest) (*pb.PollOutstandingCommandsResponse, error) {
	log.Printf("=========================================")
	log.Println("Received PollOutstandingCommands request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Println("=========================================")

	command := &pb.Command{
		CommandId:   "dec31cb2-0959-41d7-8194-bfe59ba4acb1",
		CommandType: "RESTART",
		Payload:     "Please restart the router.",
	}

	response := &pb.PollOutstandingCommandsResponse{
		Commands: []*pb.Command{command},
	}

	log.Printf("Sending response with %d commands", len(response.Commands))

	return response, nil
}

func (s *RouterServer) AcknowledgeCommand(_ context.Context, req *pb.AcknowledgeCommandRequest) (*pb.AcknowledgeCommandResponse, error) {
	log.Printf("=========================================")
	log.Println("Received AcknowledgeCommand request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Printf("  Command ID: %s", req.GetCommandId())
	log.Println("=========================================")

	response := &pb.AcknowledgeCommandResponse{
		Success: true,
		Message: "Command acknowledged successfully.",
	}

	log.Printf("Sending response: Success=%v, Message='%s'", response.Success, response.Message)

	return response, nil
}
