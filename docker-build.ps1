# Android Prayer Mode - Docker Build Script (PowerShell)
# This script builds the Android app using Docker without installing Android SDK locally

Write-Host "========================================"
Write-Host "Prayer Mode - Docker Build"
Write-Host "========================================"
Write-Host ""

# Check if Docker is running
try {
    docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running"
    }
} catch {
    Write-Host "ERROR: Docker is not running!" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and try again."
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[1/3] Building Docker image..."
docker build -t prayermode-builder .
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to build Docker image!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[2/3] Building APK..."
docker run --rm -v "${PWD}:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleDebug"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to build APK!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[3/3] Build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "APK location: app\build\outputs\apk\debug\app-debug.apk"
Write-Host ""
Read-Host "Press Enter to exit"
