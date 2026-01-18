@echo off
REM Android Prayer Mode - Docker Clean Script
REM This script cleans build files using Docker

echo ========================================
echo Prayer Mode - Docker Clean
echo ========================================
echo.

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    pause
    exit /b 1
)

echo Cleaning build files...
docker run --rm -v "%CD%:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon clean"

echo.
echo Clean completed!
pause
