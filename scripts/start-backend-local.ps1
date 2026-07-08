param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jarPath = Join-Path $root "server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"
$configDir = Join-Path $root "server\crack-bootstrap\src\main\resources"
$configLocation = "file:/" + (($configDir -replace "\\", "/").TrimStart("/")) + "/"

Set-Location $root

if (-not $SkipBuild) {
    Write-Host "Building backend package..." -ForegroundColor Cyan
    mvn -f server\pom.xml -pl crack-bootstrap -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed. Please fix the errors above and try again."
    }
}

if (-not (Test-Path $jarPath)) {
    throw "Backend jar not found: $jarPath"
}

Write-Host ""
Write-Host "Starting backend with local profile..." -ForegroundColor Green
Write-Host "Swagger doc: http://127.0.0.1:7022/doc.html" -ForegroundColor Yellow
Write-Host "Using local config from: $configDir" -ForegroundColor Yellow
Write-Host ""

java -jar $jarPath --spring.profiles.active=local --spring.config.additional-location=$configLocation
