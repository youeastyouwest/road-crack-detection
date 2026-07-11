# Start All Services
Write-Host "=== Road Crack Detection - Full Stack ==="
Write-Host ""

# Start Python YOLOv8 service in background
$pyJob = Start-Job -ScriptBlock {
    $env:PORT = "5000"
    $env:YOLO_MODEL_PATH = "models/best.pt"
    Set-Location D:\road-crack-detection\python-service
    python app.py
}
Write-Host "[1/3] YOLOv8 service starting on port 5000..."

# Start Spring Boot backend
$jar = "D:\road-crack-detection\server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"

$beJob = Start-Job -ScriptBlock {
    param($j)
    java -jar $j --crack.persistence.mode=memory
} -ArgumentList $jar
Write-Host "[2/3] Backend starting on port 8080..."

Write-Host "[3/3] To start frontend, run in another terminal:"
Write-Host "  cd D:\road-crack-detection\client && npx vite --host"
Write-Host ""
Write-Host "Services:"
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend:  http://localhost:8080"
Write-Host "  YOLOv8:   http://localhost:5000"
Write-Host ""
Write-Host "Press Ctrl+C to stop all services."

# Wait for background jobs
$pyJob | Wait-Job
$beJob | Wait-Job
