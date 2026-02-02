#!/bin/bash

# The script generates Go code from proto files

set -e
echo "🔧 Generating Go code from proto files..."

# Ensure GOPATH/bin is in PATH
export GOPATH=$(go env GOPATH)
export PATH="$PATH:$GOPATH/bin"

echo "Using GOPATH: $GOPATH"
echo "PATH includes: $GOPATH/bin"
echo ""

# Check if protoc is installed
if ! command -v protoc &> /dev/null; then
    echo "❌ Error: protoc is not installed"
    echo "Please install protoc:"
    echo "  - Ubuntu/Debian: sudo apt install protobuf-compiler"
    echo "  - macOS: brew install protobuf"
    echo "  - Windows: Download from https://github.com/protocolbuffers/protobuf/releases"
    exit 1
fi

echo "✅ protoc is installed: $(protoc --version)"
echo ""

# Check if protoc-gen-go is installed
if ! command -v protoc-gen-go &> /dev/null; then
    echo "⚠️  protoc-gen-go is not installed"
    echo "Installing protoc-gen-go..."
    go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
    echo "✅ protoc-gen-go installed"
else
    echo "✅ protoc-gen-go is already installed"
fi

# Check if protoc-gen-go-grpc is installed
if ! command -v protoc-gen-go-grpc &> /dev/null; then
    echo "⚠️  protoc-gen-go-grpc is not installed"
    echo "Installing protoc-gen-go-grpc..."
    go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
    echo "✅ protoc-gen-go-grpc installed"
else
    echo "✅ protoc-gen-go-grpc is already installed"
fi

echo ""

# Verify plugins are accessible
echo "Verifying plugin installations..."
if [ ! -f "$GOPATH/bin/protoc-gen-go" ]; then
    echo "❌ Error: protoc-gen-go not found at $GOPATH/bin/protoc-gen-go"
    echo "Please run: go install google.golang.org/protobuf/cmd/protoc-gen-go@latest"
    exit 1
fi

if [ ! -f "$GOPATH/bin/protoc-gen-go-grpc" ]; then
    echo "❌ Error: protoc-gen-go-grpc not found at $GOPATH/bin/protoc-gen-go-grpc"
    echo "Please run: go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest"
    exit 1
fi

echo "✅ All plugins verified"
echo ""

# Generate proto files
echo "📦 Generating proto files..."
protoc --experimental_allow_proto3_optional \
    --plugin=protoc-gen-go="$GOPATH/bin/protoc-gen-go" \
    --plugin=protoc-gen-go-grpc="$GOPATH/bin/protoc-gen-go-grpc" \
    --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    proto/router_service.proto

echo "✅ Proto files generated successfully!"
echo ""
echo "Generated files:"
echo "  - proto/router_service.pb.go"
echo "  - proto/router_service_grpc.pb.go"
echo ""
echo "You can now run the server with: go run main.go"
