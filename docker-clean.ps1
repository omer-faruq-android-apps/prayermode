# Android Prayer Mode - Docker Clean Script (PowerShell)
# This script cleans build files using Docker

Write-Host "========================================"
Write-Host "Prayer Mode - Docker Clean"
Write-Host "========================================"
Write-Host ""

try {
    docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running"
    }
} catch {
    Write-Host "ERROR: Docker is not running!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Cleaning build files..."
docker run --rm -v "${PWD}:/workspace" prayermode-builder sh -c "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon clean"

Write-Host ""
Write-Host "Clean completed!" -ForegroundColor Green
Read-Host "Press Enter to exit"
