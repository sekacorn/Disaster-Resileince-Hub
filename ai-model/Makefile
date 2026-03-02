# Makefile for Disaster Resilience Hub - AI Prediction Service

.PHONY: help install dev prod test train clean docker-build docker-run lint format

# Default target
help:
	@echo "Disaster Resilience Hub - AI Prediction Service"
	@echo ""
	@echo "Available targets:"
	@echo "  install       - Install dependencies"
	@echo "  dev           - Run in development mode"
	@echo "  prod          - Run in production mode"
	@echo "  test          - Run tests"
	@echo "  test-cov      - Run tests with coverage"
	@echo "  train         - Train the model"
	@echo "  example       - Run API client examples"
	@echo "  lint          - Run code linting"
	@echo "  format        - Format code"
	@echo "  clean         - Clean temporary files"
	@echo "  docker-build  - Build Docker image"
	@echo "  docker-run    - Run Docker container"
	@echo "  docker-dev    - Run Docker in development mode"
	@echo ""

# Install dependencies
install:
	python -m pip install --upgrade pip
	pip install -r requirements.txt
	mkdir -p models logs data

# Run in development mode
dev:
	python -m uvicorn disaster_predictor:app --host 0.0.0.0 --port 8000 --reload

# Run in production mode
prod:
	gunicorn disaster_predictor:app \
		--workers 4 \
		--worker-class uvicorn.workers.UvicornWorker \
		--bind 0.0.0.0:8000 \
		--timeout 120 \
		--access-logfile logs/access.log \
		--error-logfile logs/error.log

# Run tests
test:
	pytest test_predictor.py -v --tb=short

# Run tests with coverage
test-cov:
	pytest test_predictor.py -v --cov=. --cov-report=html --cov-report=term

# Train model
train:
	python model_trainer.py

# Run API client examples
example:
	python api_client.py

# Code linting
lint:
	flake8 *.py --max-line-length=120 --exclude=venv,__pycache__
	mypy *.py --ignore-missing-imports

# Format code
format:
	black *.py --line-length=120
	isort *.py

# Clean temporary files
clean:
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	find . -type f -name "*.pyo" -delete 2>/dev/null || true
	find . -type f -name "*.log" -delete 2>/dev/null || true
	rm -rf htmlcov/ .coverage 2>/dev/null || true

# Docker build
docker-build:
	docker build --target production -t disaster-predictor:latest .

# Docker build development
docker-build-dev:
	docker build --target development -t disaster-predictor:dev .

# Docker run
docker-run:
	docker run -p 8000:8000 --name disaster-predictor disaster-predictor:latest

# Docker run with volume mount
docker-run-dev:
	docker run -p 8000:8000 -v $(PWD):/app --name disaster-predictor-dev disaster-predictor:dev

# Docker compose up
docker-up:
	docker-compose up -d

# Docker compose down
docker-down:
	docker-compose down

# Docker compose logs
docker-logs:
	docker-compose logs -f

# Check Python version
check-python:
	@python --version
	@pip --version

# Create virtual environment
venv:
	python -m venv venv
	@echo "Virtual environment created. Activate with:"
	@echo "  source venv/bin/activate  (Linux/Mac)"
	@echo "  venv\\Scripts\\activate     (Windows)"

# Run all checks (lint, test)
check: lint test

# Development setup
setup: venv install
	@echo "Development environment setup complete!"
	@echo "Activate virtual environment and run 'make dev' to start"

# Quick start (install + dev)
start: install dev

# Production setup
setup-prod: install
	mkdir -p logs models data
	@echo "Production environment setup complete!"
	@echo "Run 'make prod' to start the service"
