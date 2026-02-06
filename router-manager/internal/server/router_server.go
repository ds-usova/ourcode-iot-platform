package server

import (
	"context"
	"errors"
	"fmt"
	"log"
	"router-manager/internal/database"
	"router-manager/internal/service"
	pb "router-manager/proto"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type RouterServer struct {
	pb.UnimplementedRouterServiceServer
	routerService *service.RouterService
}

func NewRouterServer(routerService *service.RouterService) *RouterServer {
	return &RouterServer{routerService: routerService}
}

func (s *RouterServer) SendCommand(ctx context.Context, req *pb.SendCommandRequest) (*pb.SendCommandResponse, error) {
	log.Println("========================================")
	log.Println("Received SubmitCommand request:")

	routerID := req.GetRouterId()
	if routerID == "" {
		log.Printf("  Router ID: <ALL ROUTERS>")
	} else {
		log.Printf("  Router ID: %s", routerID)
	}

	log.Printf("  Command Type: %s", req.GetCommandType())
	log.Printf("  Payload: %s", req.GetPayload())
	log.Println("========================================")

	result, err := s.routerService.SubmitCommand(ctx, routerID, req.GetCommandType(), req.GetPayload())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			notFoundMessage := "no routers found to send the command"
			if routerID != "" {
				notFoundMessage = fmt.Sprintf("router with ID %s not found", routerID)
			}
			return nil, status.Error(codes.InvalidArgument, notFoundMessage)
		}
		return nil, status.Error(codes.Internal, err.Error())
	}

	var message string
	if result.IsBroadcast {
		message = fmt.Sprintf("Command sent to %d router(s)", result.RoutersAffected)
	} else {
		message = fmt.Sprintf("Command sent to router %s", routerID)
	}

	return &pb.SendCommandResponse{
		CommandId: result.CommandID,
		Message:   message,
	}, nil
}

func (s *RouterServer) PollOutstandingCommands(ctx context.Context, req *pb.PollOutstandingCommandsRequest) (*pb.PollOutstandingCommandsResponse, error) {
	log.Printf("=========================================")
	log.Println("Received PollOutstandingCommands request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Println("=========================================")

	commands, err := s.routerService.PollCommands(ctx, req.GetRouterId())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			return nil, status.Error(codes.InvalidArgument, "router not found")
		}
		return nil, status.Error(codes.Internal, err.Error())
	}

	grpcCommands := make([]*pb.Command, len(commands))
	for i, cmd := range commands {
		grpcCommands[i] = &pb.Command{
			CommandId:   cmd.ID,
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

func (s *RouterServer) AcknowledgeCommand(ctx context.Context, req *pb.AcknowledgeCommandRequest) (*pb.AcknowledgeCommandResponse, error) {
	log.Printf("=========================================")
	log.Println("Received AcknowledgeCommand request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Printf("  Command ID: %s", req.GetCommandId())
	log.Println("=========================================")

	err := s.routerService.AcknowledgeCommand(ctx, req.GetCommandId(), req.GetRouterId())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			return nil, status.Error(codes.InvalidArgument, "router not found")
		}
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
