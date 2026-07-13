# Start Backend (DB mode)
param(
    [string]$Mode = "db",
    [string]$DbUrl = "jdbc:mysql://localhost:3308/road_crack?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
    [string]$DbUser = "root",
    [string]$DbPass = "123456"
)

$Jar = "D:\road-crack-detection\server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $Jar)) {
    Write-Host "JAR not found. Building..."
    Set-Location D:\road-crack-detection\server
    $env:MAVEN_HOME = "D:\road-crack-detection\.mvn-orch\apache-maven-3.9.9"
    & "D:\road-crack-detection\.mvn-orch\apache-maven-3.9.9\bin\mvn.cmd" package -DskipTests
}

if ($Mode -eq "db") {
    Write-Host "Starting backend with MySQL: $DbUser @ port 3308"
} else {
    Write-Host "Starting backend in MEMORY mode (no DB required)"
}

java -Dserver.port=8080 -jar $Jar --crack.persistence.mode=$Mode
