@echo off
REM Disaster Resilience Hub - AI Service Start Script (Windows)

echo ==========================================
echo Disaster Resilience Hub - AI Service
echo ==========================================
echo.

REM Check Python installation
echo [INFO] Checking Python version...
python --version
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Please install Python 3.11 or higher.
    pause
    exit /b 1
)

REM Check if virtual environment exists
if not exist "venv" (
    echo [WARNING] Virtual environment not found. Creating one...
    python -m venv venv
    echo [INFO] Virtual environment created.
)

REM Activate virtual environment
echo [INFO] Activating virtual environment...
call venv\Scripts\activate.bat

REM Install/update dependencies
echo [INFO] Installing dependencies...
python -m pip install --quiet --upgrade pip
pip install --quiet -r requirements.txt

REM Create necessary directories
echo [INFO] Creating necessary directories...
if not exist "models" mkdir models
if not exist "logs" mkdir logs

REM Check if .env file exists
if not exist ".env" (
    echo [WARNING] .env file not found. Creating from .env.example...
    copy .env.example .env
    echo [WARNING] Please configure .env file with your settings
)

REM Parse command line arguments
set MODE=%1
set PORT=%2
set WORKERS=%3

if "%MODE%"=="" set MODE=production
if "%PORT%"=="" set PORT=8000
if "%WORKERS%"=="" set WORKERS=4

echo.
echo [INFO] Starting AI Prediction Service...
echo [INFO] Mode: %MODE%
echo [INFO] Port: %PORT%
echo [INFO] Workers: %WORKERS%
echo.

REM Start the service based on mode
if "%MODE%"=="development" goto dev
if "%MODE%"=="dev" goto dev
if "%MODE%"=="production" goto prod
if "%MODE%"=="prod" goto prod
if "%MODE%"=="test" goto test
if "%MODE%"=="train" goto train

echo [ERROR] Invalid mode: %MODE%
echo Usage: %0 [development^|production^|test^|train] [port] [workers]
pause
exit /b 1

:dev
echo [INFO] Starting in DEVELOPMENT mode with auto-reload...
python -m uvicorn disaster_predictor:app --host 0.0.0.0 --port %PORT% --reload
goto end

:prod
echo [INFO] Starting in PRODUCTION mode with Gunicorn...
REM Note: Gunicorn doesn't work on Windows, using uvicorn with multiple workers
echo [WARNING] Gunicorn not available on Windows. Using uvicorn instead.
python -m uvicorn disaster_predictor:app --host 0.0.0.0 --port %PORT% --workers %WORKERS%
goto end

:test
echo [INFO] Running tests...
pytest test_predictor.py -v --tb=short
goto end

:train
echo [INFO] Training model...
python model_trainer.py
goto end

:end
pause
