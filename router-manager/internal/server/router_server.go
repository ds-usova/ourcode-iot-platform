package server

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"router-manager/internal/database"
	"router-manager/internal/service"
	pb "router-manager/proto"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type RouterServiceInterface interface {
	SubmitCommand(ctx context.Context, routerID, commandType, payload string) (*service.SubmitCommandResult, error)
	PollCommands(ctx context.Context, routerID string) ([]service.Command, error)
	AcknowledgeCommand(ctx context.Context, commandID, routerID string) error
}

type RouterServer struct {
	pb.UnimplementedRouterServiceServer
	routerService RouterServiceInterface
}

func NewRouterServer(routerService RouterServiceInterface) *RouterServer {
	return &RouterServer{routerService: routerService}
}

func (s *RouterServer) SendCommand(ctx context.Context, req *pb.SendCommandRequest) (*pb.SendCommandResponse, error) {
	routerID := req.GetRouterId()
	if routerID == "" {
		routerID = "<ALL ROUTERS>"
	}

	slog.Info("received SubmitCommand request",
		"router_id", routerID,
		"command_type", req.GetCommandType(),
	)
	slog.Debug("SubmitCommand payload", "payload", req.GetPayload())

	result, err := s.routerService.SubmitCommand(ctx, req.GetRouterId(), req.GetCommandType(), req.GetPayload())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			notFoundMessage := "no routers found to send the command"
			if req.GetRouterId() != "" {
				notFoundMessage = fmt.Sprintf("router with ID %s not found", req.GetRouterId())
			}
			slog.Warn("submit command rejected", "router_id", req.GetRouterId(), "error", err)
			return nil, status.Error(codes.InvalidArgument, notFoundMessage)
		}
		slog.Error("submit command failed", "router_id", req.GetRouterId(), "error", err)
		return nil, status.Error(codes.Internal, err.Error())
	}

	var message string
	if result.IsBroadcast {
		message = fmt.Sprintf("Command sent to %d router(s)", result.RoutersAffected)
	} else {
		message = fmt.Sprintf("Command sent to router %s", req.GetRouterId())
	}

	return &pb.SendCommandResponse{
		CommandId: result.CommandID,
		Message:   message,
	}, nil
}

func (s *RouterServer) PollOutstandingCommands(ctx context.Context, req *pb.PollOutstandingCommandsRequest) (*pb.PollOutstandingCommandsResponse, error) {
	slog.Info("received PollOutstandingCommands request", "router_id", req.GetRouterId())

	commands, err := s.routerService.PollCommands(ctx, req.GetRouterId())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			slog.Warn("poll outstanding commands rejected", "router_id", req.GetRouterId(), "error", err)
			return nil, status.Error(codes.InvalidArgument, "router not found")
		}
		slog.Error("poll outstanding commands failed", "router_id", req.GetRouterId(), "error", err)
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

	slog.Info("sending PollOutstandingCommands response", "router_id", req.GetRouterId(), "command_count", len(response.Commands))
	return response, nil
}

func (s *RouterServer) AcknowledgeCommand(ctx context.Context, req *pb.AcknowledgeCommandRequest) (*pb.AcknowledgeCommandResponse, error) {
	slog.Info("received AcknowledgeCommand request",
		"router_id", req.GetRouterId(),
		"command_id", req.GetCommandId(),
	)

	err := s.routerService.AcknowledgeCommand(ctx, req.GetCommandId(), req.GetRouterId())
	if err != nil {
		if errors.Is(err, database.ErrRouterNotFound) {
			slog.Warn("acknowledge command rejected: router not found", "router_id", req.GetRouterId(), "error", err)
			return nil, status.Error(codes.InvalidArgument, "router not found")
		}
		if errors.Is(err, database.ErrSentCommandNotFound) {
			slog.Warn("acknowledge command rejected: sent command not found", "router_id", req.GetRouterId(), "command_id", req.GetCommandId(), "error", err)
			return nil, status.Error(codes.InvalidArgument, "sent command not found")
		}
		slog.Error("acknowledge command failed", "router_id", req.GetRouterId(), "command_id", req.GetCommandId(), "error", err)
		return nil, status.Error(codes.Internal, err.Error())
	}

	response := &pb.AcknowledgeCommandResponse{
		Message: "Command acknowledged successfully.",
	}

	slog.Info("command acknowledged", "command_id", req.GetCommandId(), "router_id", req.GetRouterId())
	return response, nil
}
