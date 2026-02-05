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
		count, err := s.db.BroadcastCommand(ctx, req.GetCommandType(), req.GetPayload())
		if err != nil {
			if errors.Is(err, database.ErrRouterNotFound) {
				return nil, status.Error(codes.InvalidArgument, "no routers found to send the command")
			}
			return nil, status.Error(codes.Internal, err.Error())
		}

		return &pb.SendCommandResponse{
			CommandId: "",
			Message:   fmt.Sprintf("Command sent to %d router(s)", count),
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

func (s *RouterServer) PollOutstandingCommands(ctx context.Context, req *pb.PollOutstandingCommandsRequest) (*pb.PollOutstandingCommandsResponse, error) {
	log.Printf("=========================================")
	log.Println("Received PollOutstandingCommands request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Println("=========================================")

	commands, err := s.db.GetOutstandingCommands(ctx, req.GetRouterId())
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}

	grpcCommands := make([]*pb.Command, len(commands))
	for i, cmd := range commands {
		grpcCommands[i] = &pb.Command{
			CommandId:   cmd.Id,
			CommandType: cmd.CommandType,
			Payload:     cmd.Payload,
		}
	}

	response := &pb.PollOutstandingCommandsResponse{
		Commands: grpcCommands,
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

	err := s.db.AcknowledgeCommand(context.Background(), req.GetCommandId(), req.GetRouterId())
	if err != nil {
		if errors.Is(err, database.ErrSentCommandNotFound) {
			return nil, status.Error(codes.InvalidArgument, "sent command not found")
		}
		return nil, status.Error(codes.Internal, err.Error())
	}

	response := &pb.AcknowledgeCommandResponse{
		Message: "Command acknowledged successfully.",
	}

	log.Printf("Acknowledged command %s for router %s", req.GetCommandId(), req.GetRouterId())
	return response, nil
}
