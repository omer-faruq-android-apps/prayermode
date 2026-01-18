@echo off
REM Android Prayer Mode - Docker Release Build Script
REM This script builds the release APK using Docker

echo ========================================
echo Prayer Mode - Docker Release Build
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

REM Check if keystore.properties exists
if not exist "keystore.properties" (
    echo WARNING: keystore.properties not found!
    echo Release build requires signing configuration.
    echo.
    choice /C YN /M "Continue with debug build instead"
    if errorlevel 2 exit /b 1
    if errorlevel 1 (
        docker run --rm -v "%CD%:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleDebug"
        echo.
        echo Debug APK: app\build\outputs\apk\debug\app-debug.apk
        pause
        exit /b 0
    )
)

echo [1/3] Building Docker image...
docker build -t prayermode-builder .
if errorlevel 1 (
    echo ERROR: Failed to build Docker image!
    pause
    exit /b 1
)

echo.
echo [2/3] Building Release APK...
docker run --rm -v "%CD%:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleRelease"
if errorlevel 1 (
    echo ERROR: Failed to build release APK!
    pause
    exit /b 1
)

echo.
echo [3/3] Release build completed successfully!
echo.
echo APK location: app\build\outputs\apk\release\app-release.apk
echo.
pause
