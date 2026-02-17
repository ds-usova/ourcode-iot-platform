package config

import (
	"context"
	"fmt"
	"net"

	"router-manager/internal/database"
	"router-manager/internal/server"
	"router-manager/internal/service"
	pb "router-manager/proto"
	"router-manager/test/config"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// SystemTestEnvironment extends the base TestEnvironment with gRPC resources
type SystemTestEnvironment struct {
	*config.TestEnvironment
	GRPCServer   *grpc.Server
	GRPCListener net.Listener
	ClientConn   *grpc.ClientConn
	Client       pb.RouterServiceClient
}

// SetupSystemTest initializes the full application stack for system tests:
// - All resources from SetupBaseEnvironment (DB, network)
// - gRPC server
// - gRPC client
func SetupSystemTest(ctx context.Context) (*SystemTestEnvironment, error) {
	baseEnv, err := config.SetupBaseEnvironment(ctx)
	if err != nil {
		return nil, err
	}

	env := &SystemTestEnvironment{
		TestEnvironment: baseEnv,
	}

	if err := env.initializeGRPCServer(ctx); err != nil {
		env.Cleanup()
		return nil, err
	}

	if err := env.initializeGRPCClient(); err != nil {
		env.Cleanup()
		return nil, err
	}

	env.wrapCleanup()

	return env, nil
}

func (env *SystemTestEnvironment) initializeGRPCServer(ctx context.Context) error {
	server, listener, err := SetupGRPCServer(ctx, env.DB, env.PgContainer, env.Network, GRPCServerPort)
	if err != nil {
		return err
	}
	env.GRPCServer = server
	env.GRPCListener = listener
	return nil
}

func (env *SystemTestEnvironment) initializeGRPCClient() error {
	conn, client, err := SetupGRPCClient(GRPCServerPort)
	if err != nil {
		return err
	}
	env.ClientConn = conn
	env.Client = client
	return nil
}

func (env *SystemTestEnvironment) wrapCleanup() {
	baseCleanup := env.Cleanup
	env.Cleanup = func() {
		if env.ClientConn != nil {
			_ = env.ClientConn.Close()
		}
		if env.GRPCServer != nil {
			env.GRPCServer.Stop()
		}
		baseCleanup()
	}
}

// SetupGRPCServer initializes and starts the gRPC server
func SetupGRPCServer(ctx context.Context, db database.Repository, pgContainer *postgres.PostgresContainer, network testcontainers.Network, serverPort string) (*grpc.Server, net.Listener, error) {
	routerService := service.NewRouterService(db)
	grpcServer := grpc.NewServer()
	routerServer := server.NewRouterServer(routerService)
	pb.RegisterRouterServiceServer(grpcServer, routerServer)

	listener, err := net.Listen("tcp", serverPort)
	if err != nil {
		if termErr := pgContainer.Terminate(ctx); termErr != nil {
			return nil, nil, fmt.Errorf("failed to create gRPC listener: %w (also failed to terminate container: %v)", err, termErr)
		}
		if netErr := network.Remove(ctx); netErr != nil {
			return nil, nil, fmt.Errorf("failed to create gRPC listener: %w (also failed to remove network: %v)", err, netErr)
		}
		return nil, nil, fmt.Errorf("failed to create gRPC listener: %w", err)
	}

	// Start gRPC server in background
	go func() {
		if err := grpcServer.Serve(listener); err != nil {
			fmt.Printf("gRPC server error: %v\n", err)
		}
	}()

	return grpcServer, listener, nil
}

// SetupGRPCClient initializes the gRPC client
func SetupGRPCClient(serverPort string) (*grpc.ClientConn, pb.RouterServiceClient, error) {
	conn, err := grpc.NewClient(
		"localhost"+serverPort,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create gRPC client: %w", err)
	}

	client := pb.NewRouterServiceClient(conn)
	return conn, client, nil
}
