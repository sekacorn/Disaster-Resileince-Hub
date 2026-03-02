#!/bin/bash

# Disaster Resilience Hub - AI Service Start Script

set -e

echo "=========================================="
echo "Disaster Resilience Hub - AI Service"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check Python version
print_info "Checking Python version..."
python_version=$(python --version 2>&1 | awk '{print $2}')
print_info "Python version: $python_version"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    print_warning "Virtual environment not found. Creating one..."
    python -m venv venv
    print_info "Virtual environment created."
fi

# Activate virtual environment
print_info "Activating virtual environment..."
if [ -f "venv/bin/activate" ]; then
    source venv/bin/activate
elif [ -f "venv/Scripts/activate" ]; then
    source venv/Scripts/activate
else
    print_error "Could not find virtual environment activation script"
    exit 1
fi

# Install/update dependencies
print_info "Installing dependencies..."
pip install --quiet --upgrade pip
pip install --quiet -r requirements.txt

# Create necessary directories
print_info "Creating necessary directories..."
mkdir -p models
mkdir -p logs

# Check if .env file exists
if [ ! -f ".env" ]; then
    print_warning ".env file not found. Creating from .env.example..."
    cp .env.example .env
    print_warning "Please configure .env file with your settings"
fi

# Check if model exists
MODEL_PATH="${MODEL_PATH:-models/disaster_risk_model.pth}"
if [ ! -f "$MODEL_PATH" ]; then
    print_warning "Model not found at $MODEL_PATH"
    read -p "Do you want to train a model now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Starting model training..."
        python model_trainer.py
    else
        print_warning "Service will start with untrained model"
    fi
fi

# Parse command line arguments
MODE="${1:-production}"
PORT="${2:-8000}"
WORKERS="${3:-4}"

echo ""
print_info "Starting AI Prediction Service..."
print_info "Mode: $MODE"
print_info "Port: $PORT"
print_info "Workers: $WORKERS"
echo ""

# Start the service based on mode
if [ "$MODE" = "development" ] || [ "$MODE" = "dev" ]; then
    print_info "Starting in DEVELOPMENT mode with auto-reload..."
    python -m uvicorn disaster_predictor:app --host 0.0.0.0 --port "$PORT" --reload
elif [ "$MODE" = "production" ] || [ "$MODE" = "prod" ]; then
    print_info "Starting in PRODUCTION mode with Gunicorn..."
    gunicorn disaster_predictor:app \
        --workers "$WORKERS" \
        --worker-class uvicorn.workers.UvicornWorker \
        --bind "0.0.0.0:$PORT" \
        --timeout 120 \
        --access-logfile logs/access.log \
        --error-logfile logs/error.log \
        --log-level info
elif [ "$MODE" = "test" ]; then
    print_info "Running tests..."
    pytest test_predictor.py -v --tb=short
elif [ "$MODE" = "train" ]; then
    print_info "Training model..."
    python model_trainer.py
else
    print_error "Invalid mode: $MODE"
    echo "Usage: $0 [development|production|test|train] [port] [workers]"
    exit 1
fi
