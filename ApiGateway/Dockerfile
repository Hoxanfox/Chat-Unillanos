# Build stage
FROM golang:1.21-alpine AS builder

WORKDIR /app

# Copy go mod and sum files
COPY go.mod ./
# COPY go.sum ./ # No dependencies yet, so go.sum might not exist

# Download all dependencies. Dependencies will be cached if the go.mod and go.sum files are not changed
# RUN go mod download # Skipped as we have no external deps yet

# Copy the source from the current directory to the Working Directory inside the container
COPY . .

# Build the Go app
RUN go build -o api-gateway .

# Final stage
FROM alpine:latest

WORKDIR /root/

# Copy the Pre-built binary from the previous stage
COPY --from=builder /app/api-gateway .

# Expose port 8080 to the outside world
EXPOSE 8080

# Command to run the executable
CMD ["./api-gateway"]
