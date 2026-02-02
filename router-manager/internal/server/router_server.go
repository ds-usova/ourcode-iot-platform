package server

import (
	"context"
	"log"
	pb "router-manager/proto"
)

type RouterServer struct {
	pb.UnimplementedRouterServiceServer
}

func NewRouterServer() *RouterServer {
	return &RouterServer{}
}

func (s *RouterServer) SendCommand(_ context.Context, req *pb.SendCommandRequest) (*pb.SendCommandResponse, error) {
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

	var message = "Command sent to router " + routerID

	response := &pb.SendCommandResponse{
		Success: true,
		Message: message,
	}

	log.Printf("Sending response: Success=%v, Message='%s'", response.Success, response.Message)

	return response, nil
}

func (s *RouterServer) PollOutstandingCommands(_ context.Context, req *pb.PollOutstandingCommandsRequest) (*pb.PollOutstandingCommandsResponse, error) {
	log.Printf("=========================================")
	log.Println("Received PollOutstandingCommands request:")
	log.Printf("  Router ID: %s", req.GetRouterId())
	log.Println("=========================================")

	command := &pb.Command{
		CommandId:   "cmd-12345",
		CommandType: "RESTART",
		Payload:     "Please restart the router.",
	}

	response := &pb.PollOutstandingCommandsResponse{
		Commands: []*pb.Command{command},
	}

	log.Printf("Sending response with %d commands", len(response.Commands))

	return response, nil
}
