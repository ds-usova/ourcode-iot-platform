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
