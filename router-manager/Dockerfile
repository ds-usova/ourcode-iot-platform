# ---- Build stage ----
FROM golang:1.25-alpine AS builder

WORKDIR /app

# Download dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source and build a statically linked binary
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o bin/router-manager main.go

# ---- Runtime stage ----
FROM scratch

COPY --from=builder /app/bin/router-manager /router-manager

EXPOSE 50051

ENTRYPOINT ["/router-manager"]
