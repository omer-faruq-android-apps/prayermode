@echo off
REM Android Prayer Mode - Docker Build Script
REM This script builds the Android app using Docker without installing Android SDK locally

echo ========================================
echo Prayer Mode - Docker Build
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/3] Building Docker image...
docker build -t prayermode-builder .
if errorlevel 1 (
    echo ERROR: Failed to build Docker image!
    pause
    exit /b 1
)

echo.
echo [2/3] Building APK...
docker run --rm -v "%CD%:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleDebug"
if errorlevel 1 (
    echo ERROR: Failed to build APK!
    pause
    exit /b 1
)

echo.
echo [3/3] Build completed successfully!
echo.
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
pause
