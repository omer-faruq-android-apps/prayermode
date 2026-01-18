# Android Prayer Mode - Docker Release Build Script (PowerShell)
# This script builds the release APK using Docker

Write-Host "========================================"
Write-Host "Prayer Mode - Docker Release Build"
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

# Check if keystore.properties exists
if (-not (Test-Path "keystore.properties")) {
    Write-Host "WARNING: keystore.properties not found!" -ForegroundColor Yellow
    Write-Host "Release build requires signing configuration."
    Write-Host ""
    $response = Read-Host "Continue with debug build instead? (Y/N)"
    if ($response -ne "Y" -and $response -ne "y") {
        exit 1
    }
    
    docker run --rm -v "${PWD}:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleDebug"
    Write-Host ""
    Write-Host "Debug APK: app\build\outputs\apk\debug\app-debug.apk"
    Read-Host "Press Enter to exit"
    exit 0
}

Write-Host "[1/3] Building Docker image..."
docker build -t prayermode-builder .
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to build Docker image!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[2/3] Building Release APK..."
docker run --rm -v "${PWD}:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleRelease"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to build release APK!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[3/3] Release build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "APK location: app\build\outputs\apk\release\app-release.apk"
Write-Host ""
Read-Host "Press Enter to exit"
