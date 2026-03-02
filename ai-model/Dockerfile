# Multi-stage Docker build for AI Prediction Service
FROM python:3.11-slim as base

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    software-properties-common \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directory for models
RUN mkdir -p models

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV MODEL_PATH=/app/models/disaster_risk_model.pth
ENV HOST=0.0.0.0
ENV PORT=8000
ENV WORKERS=1

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["python", "-m", "uvicorn", "disaster_predictor:app", "--host", "0.0.0.0", "--port", "8000"]


# Production stage with gunicorn
FROM base as production

# Install gunicorn for production deployment
RUN pip install --no-cache-dir gunicorn

# Use gunicorn for production
CMD ["gunicorn", "disaster_predictor:app", \
     "--workers", "4", \
     "--worker-class", "uvicorn.workers.UvicornWorker", \
     "--bind", "0.0.0.0:8000", \
     "--timeout", "120", \
     "--access-logfile", "-", \
     "--error-logfile", "-"]


# Development stage
FROM base as development

# Install development dependencies
RUN pip install --no-cache-dir pytest pytest-asyncio pytest-cov black flake8 mypy

# Use uvicorn with reload for development
CMD ["uvicorn", "disaster_predictor:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]
