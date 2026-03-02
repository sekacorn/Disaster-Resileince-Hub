# Makefile for Disaster Resilience Hub API Gateway

.PHONY: help build test run clean docker-build docker-run docker-stop install

# Default target
help:
	@echo "Available targets:"
	@echo "  install       - Install dependencies"
	@echo "  build         - Build the application"
	@echo "  test          - Run tests"
	@echo "  run           - Run the application locally"
	@echo "  clean         - Clean build artifacts"
	@echo "  docker-build  - Build Docker image"
	@echo "  docker-run    - Run with Docker Compose"
	@echo "  docker-stop   - Stop Docker containers"
	@echo "  docker-logs   - View Docker logs"
	@echo "  verify        - Run full verification (build + test)"

# Install dependencies
install:
	mvn dependency:resolve

# Build the application
build:
	mvn clean package -DskipTests

# Run tests
test:
	mvn test

# Run integration tests
integration-test:
	mvn verify

# Run the application locally
run:
	mvn spring-boot:run

# Clean build artifacts
clean:
	mvn clean
	rm -rf logs/

# Build Docker image
docker-build:
	docker build -t disaster-hub/api-gateway:latest .

# Run with Docker Compose
docker-run:
	docker-compose up -d

# Stop Docker containers
docker-stop:
	docker-compose down

# View Docker logs
docker-logs:
	docker-compose logs -f api-gateway

# Full verification
verify:
	mvn clean verify

# Format code
format:
	mvn formatter:format

# Check code style
checkstyle:
	mvn checkstyle:check

# Generate test coverage report
coverage:
	mvn clean test jacoco:report

# Package for production
package:
	mvn clean package -DskipTests -Pprod

# Start Redis for local development
redis-start:
	docker run -d --name gateway-redis -p 6379:6379 redis:7-alpine

# Stop Redis
redis-stop:
	docker stop gateway-redis
	docker rm gateway-redis
