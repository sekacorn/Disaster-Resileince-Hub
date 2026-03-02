# Quick Start Guide - AI Prediction Service

Get the Disaster Resilience Hub AI Prediction Service up and running in minutes.

## Prerequisites

- Python 3.11 or higher
- pip (Python package manager)
- 4GB+ RAM recommended
- (Optional) Docker for containerized deployment

## Quick Start Options

### Option 1: Using Start Scripts (Recommended)

#### Linux/Mac:
```bash
chmod +x start.sh
./start.sh development
```

#### Windows:
```cmd
start.bat development
```

The service will be available at `http://localhost:8000`

### Option 2: Manual Setup

1. **Create Virtual Environment** (Recommended)
```bash
python -m venv venv

# Activate it
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows
```

2. **Install Dependencies**
```bash
pip install -r requirements.txt
```

3. **Run the Service**
```bash
# Development mode (with auto-reload)
python -m uvicorn disaster_predictor:app --host 0.0.0.0 --port 8000 --reload

# Production mode
gunicorn disaster_predictor:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000
```

### Option 3: Using Docker

```bash
# Build the image
docker build --target production -t disaster-predictor:latest .

# Run the container
docker run -p 8000:8000 disaster-predictor:latest
```

### Option 4: Using Docker Compose

```bash
# Start the service
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the service
docker-compose down
```

### Option 5: Using Makefile

```bash
# Install dependencies
make install

# Run in development mode
make dev

# Or run in production mode
make prod
```

## Verify Installation

1. **Check Health**
```bash
curl http://localhost:8000/health
```

Expected response:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "device": "cpu"
}
```

2. **Check System Resources**
```bash
curl http://localhost:8000/resources/check
```

3. **View Interactive Documentation**

Open in your browser:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## First API Request

### Predict Earthquake Risk

```bash
curl -X POST http://localhost:8000/predict/risk \
  -H "Content-Type: application/json" \
  -d '{
    "location": {
      "latitude": 37.7749,
      "longitude": -122.4194
    },
    "disaster_type": "earthquake",
    "environmental_data": {
      "seismic_activity": 6.5,
      "temperature": 20,
      "humidity": 60
    }
  }'
```

### Generate Evacuation Plan

```bash
curl -X POST http://localhost:8000/predict/evacuation \
  -H "Content-Type: application/json" \
  -d '{
    "location": {
      "latitude": 34.0522,
      "longitude": -118.2437
    },
    "disaster_type": "wildfire",
    "population_at_risk": 5000
  }'
```

## Run Examples

Test the API with comprehensive examples:

```bash
python api_client.py
```

This will run multiple examples demonstrating:
- Health checks
- Risk predictions for different disasters
- Evacuation planning
- Resource monitoring

## Train a Custom Model (Optional)

If you have historical disaster data:

```bash
python model_trainer.py
```

This will:
1. Generate synthetic training data (or load your CSV)
2. Train a PyTorch neural network
3. Save the model to `models/disaster_risk_model.pth`
4. Create training history and metadata files

## Run Tests

Ensure everything is working correctly:

```bash
# Run all tests
pytest test_predictor.py -v

# Run with coverage report
pytest test_predictor.py --cov=. --cov-report=html

# Or using Makefile
make test
```

## Configuration

### Environment Variables

Create a `.env` file (copy from `.env.example`):

```bash
cp .env.example .env
```

Edit `.env` to configure:
```env
HOST=0.0.0.0
PORT=8000
WORKERS=4
MODEL_PATH=models/disaster_risk_model.pth
LOG_LEVEL=INFO
```

### Key Configuration Options

| Variable | Default | Description |
|----------|---------|-------------|
| `HOST` | 0.0.0.0 | Server host |
| `PORT` | 8000 | Server port |
| `WORKERS` | 1 | Number of worker processes (-1 for auto) |
| `MODEL_PATH` | models/disaster_risk_model.pth | Path to trained model |
| `LOG_LEVEL` | INFO | Logging level |
| `TORCH_DEVICE` | auto | PyTorch device (cpu/cuda) |

## Troubleshooting

### Port Already in Use
```bash
# Kill process using port 8000
lsof -ti:8000 | xargs kill -9  # Mac/Linux
netstat -ano | findstr :8000   # Windows (then kill PID)

# Or use a different port
PORT=8001 python disaster_predictor.py
```

### Dependencies Installation Fails
```bash
# Upgrade pip first
python -m pip install --upgrade pip

# Install with verbose output
pip install -r requirements.txt -v
```

### PyTorch Installation Issues
```bash
# Install CPU-only version (lighter)
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
```

### Module Not Found Errors
```bash
# Ensure virtual environment is activated
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows

# Reinstall dependencies
pip install -r requirements.txt
```

### Out of Memory Errors
- Reduce batch size in `config.py`
- Use fewer workers
- Close other applications

### CUDA Not Available (GPU)
```bash
# Check CUDA installation
python -c "import torch; print(torch.cuda.is_available())"

# If False, service will automatically use CPU
```

## Production Deployment

### Recommended Settings

```bash
# Use multiple workers
WORKERS=4  # Or number of CPU cores

# Use gunicorn
gunicorn disaster_predictor:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000 \
  --timeout 120 \
  --access-logfile logs/access.log \
  --error-logfile logs/error.log
```

### Using PM2 (Process Manager)

```bash
# Install PM2
npm install -g pm2

# Start service
pm2 start "gunicorn disaster_predictor:app --workers 4 --worker-class uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000" --name disaster-ai

# Monitor
pm2 status
pm2 logs disaster-ai

# Auto-restart on system boot
pm2 startup
pm2 save
```

### Using systemd (Linux)

Create `/etc/systemd/system/disaster-ai.service`:

```ini
[Unit]
Description=Disaster AI Prediction Service
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/ai-model
Environment="PATH=/path/to/venv/bin"
ExecStart=/path/to/venv/bin/gunicorn disaster_predictor:app --workers 4 --worker-class uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable disaster-ai
sudo systemctl start disaster-ai
sudo systemctl status disaster-ai
```

## Next Steps

1. **Integrate with Backend**: Connect the AI service to your main backend
2. **Train Custom Model**: Use your historical disaster data
3. **Monitor Performance**: Set up monitoring and alerting
4. **Scale Horizontally**: Deploy multiple instances behind a load balancer
5. **Optimize**: Profile and optimize prediction speed

## Resources

- **Full Documentation**: See `README.md`
- **API Reference**: See `API_DOCUMENTATION.md`
- **Configuration**: See `config.py`
- **Examples**: Run `python api_client.py`

## Support

For issues or questions:
1. Check the documentation
2. Review error logs
3. Run tests to isolate issues
4. Refer to the main project repository

---

**Congratulations!** Your AI Prediction Service is now running. Visit http://localhost:8000/docs for interactive API documentation.
