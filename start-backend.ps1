# Start Backend (DB mode)
param(
    [string]$Mode = "memory",
    [string]$DbUrl = "jdbc:mysql://localhost:3308/road_crack?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
    [string]$DbUser = "root",
    [string]$DbPass = "123456",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Jar = Join-Path $ProjectRoot "server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"

$env:JAVA_HOME = "D:\开发软件\jdk-21.0.10.7-hotspot"
$env:MAVEN_HOME = "D:\Java\apache-maven-3.9.15"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

Set-Location $ProjectRoot

if (-not $SkipBuild -or -not (Test-Path $Jar)) {
    Write-Host "Building backend package..." -ForegroundColor Cyan
    Set-Location (Join-Path $ProjectRoot "server")
    & "$env:MAVEN_HOME\bin\mvn.cmd" clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed. Please fix the errors above and try again."
    }
    Set-Location $ProjectRoot
}

if (-not (Test-Path $Jar)) {
    throw "Backend jar not found: $Jar"
}

Write-Host ""
Write-Host "Starting backend in $Mode mode..." -ForegroundColor Green
Write-Host "Swagger doc: http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
Write-Host ""

& "$env:JAVA_HOME\bin\java.exe" -jar $Jar --crack.persistence.mode=$Mode